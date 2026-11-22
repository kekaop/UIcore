package org.orvencraft.uicore.cutscene;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.orvencraft.uicore.api.cutscene.*;
import org.orvencraft.uicore.api.event.*;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;

/** Per-player cutscene ownership, asynchronous readiness and public service adapter. */
public final class CutsceneManager implements CutsceneService {
   public interface Host {
      /** A stable identity for the menu session, or null if no menu is active. */
      Object menuToken(Player player);
      CameraPose camera(Player player);
      /** Runs on the player's owning scheduler; running inline when already owned is supported. */
      void execute(Player player, Runnable operation);
      CompletionStage<Boolean> prepare(Player player, CutsceneDefinition definition, CameraPose initialPose);
      CompletionStage<Boolean> apply(Player player, CameraPose pose);
      boolean eligible(Player player, String elementId);
      void actions(Player player, List<String> actions);
      void event(Event event);
      void finished(Player player, boolean closeMenu);
   }

   private final Host host;
   private volatile Map<String, CutsceneDefinition> definitions = Map.of();
   private final ConcurrentHashMap<UUID, Session> sessions = new ConcurrentHashMap<>();

   public CutsceneManager(Host host) { this.host = java.util.Objects.requireNonNull(host); }
   public void replaceDefinitions(Map<String, CutsceneDefinition> definitions) { this.definitions = Map.copyOf(definitions); }
   public Map<String, CutsceneDefinition> definitions() { return this.definitions; }
   public Optional<CutsceneDefinition> definition(String id) { return id == null ? Optional.empty() : Optional.ofNullable(this.definitions.get(id)); }
   @Override public Set<String> ids() { return this.definitions.keySet(); }

   @Override public CutsceneStartResult start(Player player, String id) {
      CutsceneDefinition definition = id == null ? null : this.definitions.get(id);
      if (definition == null) return CutsceneStartResult.UNKNOWN_SCENE;
      Object token = this.host.menuToken(player);
      if (token == null) return CutsceneStartResult.NO_MENU;
      Session replacement = new Session(player, token, definition);
      Session[] previous = new Session[1];
      this.sessions.compute(player.getUniqueId(), (key, old) -> {
         if (old != null && old.menuToken == token && old.active() && definition.policy().restart() == Restart.REJECT) return old;
         previous[0] = old;
         return replacement;
      });
      if (this.sessions.get(player.getUniqueId()) != replacement) return CutsceneStartResult.ALREADY_RUNNING;
      this.host.execute(player, () -> {
         if (previous[0] != null && previous[0].active()) terminate(previous[0], CutsceneAbortReason.REPLACED, "Replaced by a new start request", false);
         if (!owns(replacement)) return;
         try {
            CameraPose current = this.host.camera(player);
            CameraPose initial = definition.policy().startMode() == StartMode.INITIAL
               ? CutscenePlayback.initialPose(definition, current) : current;
            await(replacement, this.host.prepare(player, definition, initial), () -> {
               // A replacement may wait for the previous physical camera transfer. Bind to its committed pose.
               replacement.playback = new CutscenePlayback(replacement.id, definition, this.host.camera(player), listener(replacement));
               replacement.playback.begin();
               replacement.update();
            });
         } catch (RuntimeException error) { terminate(replacement, CutsceneAbortReason.PREPARATION_FAILED, diagnostic(error), true); }
      });
      return CutsceneStartResult.ACCEPTED;
   }

   /** Called once per server tick on the player's scheduler, alongside the menu camera update. */
   public void tick(Player player) {
      Session session = this.sessions.get(player.getUniqueId());
      if (session == null || !session.active()) return;
      if (!owns(session)) { abort(player, CutsceneAbortReason.MENU_CHANGED); return; }
      if (session.pending) {
         if (++session.pendingTicks >= Math.max(1, session.definition.policy().chunkTimeoutTicks())) {
            terminate(session, CutsceneAbortReason.PREPARATION_TIMEOUT, "Camera preparation exceeded the configured tick timeout", true);
         }
         return;
      }
      if (session.playback == null) return;
      try { session.playback.tick(); session.update(); }
      catch (RuntimeException error) { terminate(session, CutsceneAbortReason.ERROR, diagnostic(error), true); }
   }

   private void await(Session session, CompletionStage<Boolean> stage, Runnable ready) {
      if (!owns(session) || !session.active()) return;
      session.pending = true;
      session.pendingTicks = 0;
      long operation = ++session.operation;
      stage.whenComplete((success, failure) -> this.host.execute(session.player, () -> {
         if (!owns(session) || !session.active() || session.operation != operation) return;
         session.pending = false;
         if (failure != null || !Boolean.TRUE.equals(success)) {
            terminate(session, CutsceneAbortReason.PREPARATION_FAILED,
               failure == null ? "The camera route could not be prepared" : diagnostic(failure), true);
            return;
         }
         try {
            Runnable override = session.afterPending;
            session.afterPending = null;
            if (override != null) override.run();
            else ready.run();
            session.update();
         }
         catch (RuntimeException error) { terminate(session, CutsceneAbortReason.ERROR, diagnostic(error), true); }
      }));
   }

   private CutscenePlayback.Listener listener(Session session) {
      return new CutscenePlayback.Listener() {
         @Override public void started() { emit(session, snapshot -> new CutsceneStartEvent(session.player, snapshot)); }
         @Override public void entered(Step step) { emit(session, snapshot -> new CutsceneStepEnterEvent(session.player, snapshot)); }
         @Override public void actions(List<String> actions) {
            if (owns(session) && session.active() && !actions.isEmpty()) host.actions(session.player, actions);
            if (!owns(session) && session.playback.active()) session.playback.abort(CutsceneAbortReason.MENU_CHANGED, "Menu session changed during a step action");
         }
         @Override public boolean eligible(String elementId) { return owns(session) && host.eligible(session.player, elementId); }
         @Override public void frame(CameraPose pose, Runnable committed) {
            if (owns(session)) await(session, host.apply(session.player, pose), committed);
         }
         @Override public void prepareMotion(CutsceneDefinition definition, Move move, CameraPose current,
                                             java.util.function.Consumer<org.orvencraft.uicore.cutscene.model.MoveMotion> ready) {
            var prepared = java.util.concurrent.CompletableFuture.supplyAsync(() ->
               new org.orvencraft.uicore.cutscene.model.MoveMotion(definition, move, current));
            await(session, prepared.thenApply(ignored -> true), () -> ready.accept(prepared.join()));
         }
         @Override public void paused() { emit(session, snapshot -> new CutscenePauseEvent(session.player, snapshot)); }
         @Override public void resumed() { emit(session, snapshot -> new CutsceneResumeEvent(session.player, snapshot)); }
         @Override public void choice(String id) { emit(session, snapshot -> new CutsceneChoiceEvent(session.player, snapshot, id)); }
         @Override public void completed() {
            emit(session, snapshot -> new CutsceneCompleteEvent(session.player, snapshot));
            if (owns(session)) host.finished(session.player, session.definition.policy().completion() == Completion.CLOSE_MENU);
         }
         @Override public void aborted(CutsceneAbortReason reason, String diagnostic) {
            emit(session, snapshot -> new CutsceneAbortEvent(session.player, snapshot, reason, diagnostic));
            if (!session.terminating && owns(session)) {
               host.finished(session.player, reason == CutsceneAbortReason.ERROR
                  || reason == CutsceneAbortReason.INVALID_TRANSITION || reason == CutsceneAbortReason.PREPARATION_FAILED);
            }
         }
      };
   }

   private void emit(Session session, java.util.function.Function<CutsceneSnapshot, Event> event) {
      session.update();
      this.host.event(event.apply(session.snapshot));
      // Event listeners can replace/close the menu. Never enter more steps in the obsolete playback.
      if (session.playback != null && session.playback.active() && !owns(session)) {
         session.playback.abort(CutsceneAbortReason.MENU_CHANGED, "Menu session changed during a cutscene event");
      }
   }

   private boolean owns(Session session) {
      return this.sessions.get(session.player.getUniqueId()) == session && this.host.menuToken(session.player) == session.menuToken;
   }

   private void terminate(Session session, CutsceneAbortReason reason, String diagnostic, boolean close) {
      session.pending = false;
      session.operation++;
      session.terminating = true;
      try {
         if (session.playback != null) session.playback.abort(reason, diagnostic);
         else if (session.active()) {
            session.snapshot = new CutsceneSnapshot(session.id, session.definition.id(), CutsceneState.ABORTED, null, 0, 0, 0);
            this.host.event(new CutsceneAbortEvent(session.player, session.snapshot, reason, diagnostic));
         }
      } finally {
         session.terminating = false;
      }
      session.update();
      if (owns(session) && (close || reason == CutsceneAbortReason.STOPPED)) this.host.finished(session.player, close);
   }

   private boolean request(Player player, java.util.function.Predicate<CutsceneSnapshot> allowed, Consumer<CutscenePlayback> operation) {
      Session session = this.sessions.get(player.getUniqueId());
      if (session == null || !session.active() || !allowed.test(session.snapshot)) return false;
      this.host.execute(player, () -> {
         if (!owns(session) || !session.active() || session.playback == null) return;
         try { operation.accept(session.playback); session.update(); }
         catch (RuntimeException error) { terminate(session, CutsceneAbortReason.ERROR, diagnostic(error), true); }
      });
      return true;
   }

   @Override public boolean pause(Player player) {
      return request(player, s -> s.state() == CutsceneState.PLAYING || s.state() == CutsceneState.WAITING_CHOICE, CutscenePlayback::pause);
   }
   @Override public boolean resume(Player player) { return request(player, s -> s.state() == CutsceneState.PAUSED, CutscenePlayback::resume); }
   @Override public boolean choose(Player player, String optionId) {
      Session session = this.sessions.get(player.getUniqueId());
      if (session == null || optionId == null || session.snapshot.state() != CutsceneState.WAITING_CHOICE
         || !(session.definition.steps().get(session.snapshot.stepId()) instanceof Choice choice)
         || !choice.options().containsKey(optionId)) return false;
      return request(player, s -> s.state() == CutsceneState.WAITING_CHOICE, p -> p.choose(optionId));
   }
   /** Returns true for a mapped element even when a repeated click can no longer accept its option. */
   public boolean chooseElement(Player player, String elementId) {
      Session session = this.sessions.get(player.getUniqueId());
      if (session == null || !session.active() || session.playback == null || !(session.playback.step() instanceof Choice choice)) return false;
      for (Map.Entry<String, ChoiceOption> entry : choice.options().entrySet()) {
         if (entry.getValue().element().equals(elementId)) { choose(player, entry.getKey()); return true; }
      }
      return false;
   }
   @Override public boolean skip(Player player) {
      Session session = this.sessions.get(player.getUniqueId());
      if (session == null || !session.definition.policy().allowSkip()) return false;
      return request(player, s -> s.state() != CutsceneState.PREPARING, playback -> {
         // Let an already submitted camera frame settle before entering the skip target.
         if (session.pending) session.afterPending = playback::skip;
         else playback.skip();
      });
   }
   @Override public boolean stop(Player player) { return stop(player, CutsceneAbortReason.STOPPED); }
   public boolean stop(Player player, CutsceneAbortReason reason) {
      Session session = this.sessions.get(player.getUniqueId());
      if (session == null || !session.active()) return false;
      this.host.execute(player, () -> { if (owns(session)) terminate(session, reason, "", false); });
      return true;
   }
   /** Immediate lifecycle hook; call on the player's scheduler before discarding the menu session. */
   public void abort(Player player, CutsceneAbortReason reason) {
      Session session = this.sessions.remove(player.getUniqueId());
      if (session != null && session.active()) terminate(session, reason, "", false);
   }
   /** Invalidates every outstanding completion; the plugin owns physical menu cleanup. */
   public void shutdown() {
      for (Session session : this.sessions.values()) {
         if (this.sessions.remove(session.player.getUniqueId(), session) && session.active()) {
            terminate(session, CutsceneAbortReason.PLUGIN_DISABLED, "", false);
         }
      }
   }
   public boolean active(Player player) {
      Session session = this.sessions.get(player.getUniqueId());
      return session != null && session.active();
   }
   public boolean isPending(Player player) {
      Session session = this.sessions.get(player.getUniqueId());
      return session != null && session.active() && session.pending;
   }
   public Optional<Policy> policy(Player player) {
      Session session = this.sessions.get(player.getUniqueId());
      return session == null || !session.active() ? Optional.empty() : Optional.of(session.definition.policy());
   }
   @Override public Optional<CutsceneSnapshot> state(Player player) {
      Session session = this.sessions.get(player.getUniqueId());
      return session == null ? Optional.empty() : Optional.of(session.snapshot);
   }

   private static String diagnostic(Throwable error) {
      return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
   }

   private static final class Session {
      final UUID id = UUID.randomUUID();
      final Player player;
      final Object menuToken;
      final CutsceneDefinition definition;
      volatile CutsceneSnapshot snapshot;
      CutscenePlayback playback;
      volatile boolean pending;
      long pendingTicks;
      long operation;
      Runnable afterPending;
      boolean terminating;
      Session(Player player, Object menuToken, CutsceneDefinition definition) {
         this.player = player;
         this.menuToken = menuToken;
         this.definition = definition;
         this.snapshot = new CutsceneSnapshot(this.id, definition.id(), CutsceneState.PREPARING, null, 0, 0, 0);
      }
      void update() { if (this.playback != null) this.snapshot = this.playback.snapshot(); }
      boolean active() { return this.snapshot.state() != CutsceneState.COMPLETED && this.snapshot.state() != CutsceneState.ABORTED; }
   }
}
