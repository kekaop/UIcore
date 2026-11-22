package org.orvencraft.uicore.cutscene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.orvencraft.uicore.api.cutscene.CutsceneAbortReason;
import org.orvencraft.uicore.api.cutscene.CutsceneSnapshot;
import org.orvencraft.uicore.api.cutscene.CutsceneState;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;
import org.orvencraft.uicore.cutscene.model.MoveMotion;
import org.orvencraft.uicore.cutscene.model.ArcLengthPath;

/** Deterministic, server-independent playback. Each tick advances by at most one scenario tick. */
public final class CutscenePlayback {
   public interface Listener {
      default void started() { }
      default void entered(Step step) { }
      default void actions(List<String> actions) { }
      default boolean eligible(String elementId) { return true; }
      /** Commit only after the camera frame is ready. Until then the clock stays frozen. */
      default void frame(CameraPose pose, Runnable committed) { committed.run(); }
      /** An arbitrary-position skip may need a path that could not be compiled at initial preparation. */
      default void prepareMotion(CutsceneDefinition definition, Move move, CameraPose current,
                                 java.util.function.Consumer<MoveMotion> ready) {
         ready.accept(new MoveMotion(definition, move, current));
      }
      default void paused() { }
      default void resumed() { }
      default void choice(String optionId) { }
      default void completed() { }
      default void aborted(CutsceneAbortReason reason, String diagnostic) { }
   }

   private final UUID sessionId;
   private final CutsceneDefinition definition;
   private final Listener listener;
   private final Map<String, MoveMotion> namedMotions = new HashMap<>();
   private final Map<String, Map<Position, ArcLengthPath>> currentPaths = new HashMap<>();
   private CameraPose pose;
   private final CameraPose openingPose;
   private final CameraPose initialPose;
   private CutsceneState state = CutsceneState.PREPARING;
   private CutsceneState pausedFrom;
   private Step step;
   private MoveMotion motion;
   private long elapsedTicks;
   private long stepTicks;
   private long transitionTicks;
   private boolean pendingFrame;
   private long frameSequence;
   private Runnable pausedCommit;
   private boolean transition;
   private boolean exiting;
   private int instantTransitions;

   public CutscenePlayback(UUID sessionId, CutsceneDefinition definition, CameraPose current, Listener listener) {
      this.sessionId = sessionId;
      this.definition = definition;
      this.listener = listener;
      this.pose = current;
      this.openingPose = current;
      this.initialPose = initialPose(definition, current);
      Map<Position, CameraPose> incomingPositions = new HashMap<>();
      incomingPositions.put(Position.of(current), current);
      incomingPositions.put(Position.of(this.initialPose), this.initialPose);
      for (Step candidate : definition.steps().values()) {
         if (candidate instanceof Move move && !move.to().isEmpty()) {
            CameraPose endpoint = definition.points().get(move.to().get(move.to().size() - 1));
            if (endpoint != null) incomingPositions.put(Position.of(endpoint), endpoint);
         }
      }
      // Named starting points are immutable, so their arc-length tables are compiled during preparation.
      for (Step candidate : definition.steps().values()) {
         if (candidate instanceof Move move && move.from() != null) {
            CameraPose from = definition.points().get(move.from());
            if (from != null) this.namedMotions.put(move.id(), new MoveMotion(definition, move, from));
         } else if (candidate instanceof Move move) {
            Map<Position, ArcLengthPath> paths = new HashMap<>();
            for (Map.Entry<Position, CameraPose> incoming : incomingPositions.entrySet()) {
               paths.put(incoming.getKey(), MoveMotion.preparePath(definition, move, incoming.getValue()));
            }
            this.currentPaths.put(move.id(), Map.copyOf(paths));
         }
      }
   }

   public static CameraPose initialPose(CutsceneDefinition definition, CameraPose current) {
      Step first = definition.steps().get(definition.startStep());
      return first instanceof Move move && move.from() != null
         ? definition.points().getOrDefault(move.from(), current) : current;
   }

   public void begin() {
      if (this.state != CutsceneState.PREPARING) return;
      if (!this.pose.world().equals(this.initialPose.world())) {
         abort(CutsceneAbortReason.PREPARATION_FAILED, "The menu and cutscene must be in the same world");
         return;
      }
      if (this.definition.policy().startMode() == StartMode.INITIAL && !samePose(this.pose, this.initialPose)) {
         abort(CutsceneAbortReason.PREPARATION_FAILED, "initial start requires the menu camera at its configured starting point");
         return;
      }
      this.state = CutsceneState.PLAYING;
      this.transition = this.definition.policy().startMode() == StartMode.TRANSITION
         && !samePose(this.pose, this.initialPose);
      this.listener.started();
      if (!active()) return;
      if (!this.transition) enter(this.definition.startStep());
   }

   public void tick() {
      if (!active() || this.state == CutsceneState.PREPARING || this.state == CutsceneState.PAUSED || this.pendingFrame) return;
      this.instantTransitions = 0;
      this.elapsedTicks++;
      if (this.transition) {
         this.transitionTicks++;
         double amount = Math.min(1.0, (double)this.transitionTicks / Math.max(1, this.definition.policy().transitionTicks()));
         double yaw = this.openingPose.yaw() + angleDelta(this.openingPose.yaw(), this.initialPose.yaw()) * amount;
         CameraPose frame = new CameraPose(this.pose.world(),
            lerp(this.openingPose.x(), this.initialPose.x(), amount),
            lerp(this.openingPose.y(), this.initialPose.y(), amount),
            lerp(this.openingPose.z(), this.initialPose.z(), amount), yaw,
            lerp(this.openingPose.pitch(), this.initialPose.pitch(), amount));
         publish(frame, () -> {
            if (amount == 1) { this.transition = false; enter(this.definition.startStep()); }
         });
      } else if (this.step instanceof Move move) {
         this.stepTicks++;
         publish(this.motion.sample(this.stepTicks, this.pose), () -> {
            if (this.stepTicks >= move.durationTicks()) leave(move.next());
         });
      } else if (this.step instanceof Wait wait) {
         this.stepTicks++;
         if (this.stepTicks >= wait.durationTicks()) leave(wait.next());
      } else if (this.step instanceof Choice choice) {
         if (!anyEligible(choice)) { unavailable(choice); return; }
         this.stepTicks++;
         if (choice.timeoutTicks() > 0 && this.stepTicks >= choice.timeoutTicks()) leave(choice.timeoutNext());
      }
   }

   private void publish(CameraPose frame, Runnable committed) {
      this.pose = frame;
      this.pendingFrame = true;
      long sequence = ++this.frameSequence;
      this.listener.frame(frame, () -> {
         if (sequence != this.frameSequence || !this.pendingFrame || !active()) return;
         this.pendingFrame = false;
         if (this.state == CutsceneState.PAUSED) this.pausedCommit = committed;
         else committed.run();
      });
   }

   private void enter(String id) {
      if (!active()) return;
      if (++this.instantTransitions > 1024) { abort(CutsceneAbortReason.INVALID_TRANSITION, "Too many instantaneous transitions"); return; }
      this.step = this.definition.steps().get(id);
      this.stepTicks = 0;
      if (this.step == null) { abort(CutsceneAbortReason.INVALID_TRANSITION, "Unknown step: " + id); return; }
      this.state = this.step instanceof Choice ? CutsceneState.WAITING_CHOICE : CutsceneState.PLAYING;
      if (this.step instanceof Move move) {
         try {
            this.motion = this.namedMotions.get(move.id());
            if (this.motion == null) {
               ArcLengthPath prepared = this.currentPaths.getOrDefault(move.id(), Map.of()).get(Position.of(this.pose));
               if (prepared == null) {
                  this.state = CutsceneState.PREPARING;
                  long sequence = this.frameSequence;
                  this.listener.prepareMotion(this.definition, move, this.pose, preparedMotion -> {
                     if (!active() || this.state != CutsceneState.PREPARING || this.step != move || sequence != this.frameSequence) return;
                     this.motion = preparedMotion;
                     this.state = CutsceneState.PLAYING;
                     entered(move);
                  });
                  return;
               }
               this.motion = new MoveMotion(this.definition, move, this.pose, prepared);
            }
            // Every visit must join the current position, including reused named motions.
            if (move.from() != null && !samePose(this.pose, this.definition.points().get(move.from()))) {
               abort(CutsceneAbortReason.INVALID_TRANSITION, "Move '" + move.id() + "' starts away from the current camera pose"); return;
            }
         } catch (RuntimeException error) { abort(CutsceneAbortReason.ERROR, error.getMessage()); return; }
      }
      entered(this.step);
   }

   private void entered(Step entered) {
      this.listener.entered(entered);
      if (!active() || this.step != entered) return;
      this.listener.actions(entered.enterActions());
      if (!active() || this.step != entered) return;
      if (entered instanceof End) {
         exitActions();
         if (!active()) return;
         this.state = CutsceneState.COMPLETED;
         this.listener.completed();
      } else if (entered instanceof Wait wait) {
         if (wait.durationTicks() < 0) {
            this.pausedFrom = CutsceneState.PLAYING;
            this.state = CutsceneState.PAUSED;
            this.listener.paused();
         } else if (wait.durationTicks() == 0) leave(wait.next());
      } else if (entered instanceof Choice choice && !anyEligible(choice)) unavailable(choice);
   }

   private void unavailable(Choice choice) {
      if (choice.fallbackNext() != null) leave(choice.fallbackNext());
      else abort(CutsceneAbortReason.NO_AVAILABLE_CHOICE, "No available options in step '" + choice.id() + "'");
   }

   private boolean anyEligible(Choice choice) {
      for (ChoiceOption option : choice.options().values()) if (this.listener.eligible(option.element())) return true;
      return false;
   }

   private void exitActions() {
      if (this.exiting) return;
      this.exiting = true;
      try { this.listener.actions(this.step.exitActions()); }
      finally { this.exiting = false; }
   }

   private void leave(String next) {
      if (!active() || this.exiting) return;
      // Claim this transition before running callbacks so reentrant API calls cannot accept another choice.
      this.state = CutsceneState.PLAYING;
      Step leaving = this.step;
      exitActions();
      if (active() && this.step == leaving) enter(next);
   }

   public boolean choose(String optionId) {
      if (this.state != CutsceneState.WAITING_CHOICE || !(this.step instanceof Choice choice) || this.exiting) return false;
      ChoiceOption option = choice.options().get(optionId);
      if (option == null || !this.listener.eligible(option.element())) return false;
      this.state = CutsceneState.PLAYING;
      this.listener.choice(optionId);
      if (active() && this.step == choice) leave(option.next());
      return true;
   }

   public boolean pause() {
      if (this.state != CutsceneState.PLAYING && this.state != CutsceneState.WAITING_CHOICE) return false;
      this.pausedFrom = this.state;
      this.state = CutsceneState.PAUSED;
      this.listener.paused();
      return true;
   }

   public boolean resume() {
      if (this.state != CutsceneState.PAUSED) return false;
      Step resumedStep = this.step;
      boolean externalWait = this.pausedCommit == null && resumedStep instanceof Wait wait && wait.durationTicks() < 0;
      this.state = this.pausedFrom;
      this.listener.resumed();
      if (this.pausedCommit != null && active()) {
         Runnable committed = this.pausedCommit;
         this.pausedCommit = null;
         committed.run();
      }
      if (active() && this.state != CutsceneState.PAUSED && this.step == resumedStep && externalWait) leave(((Wait)resumedStep).next());
      return true;
   }

   public boolean skip() {
      if (!active() || !this.definition.policy().allowSkip() || this.exiting) return false;
      this.pendingFrame = false;
      this.frameSequence++;
      this.pausedCommit = null;
      this.transition = false;
      this.state = CutsceneState.PLAYING;
      if (this.definition.policy().skipTo() != null) {
         if (this.step != null) leave(this.definition.policy().skipTo());
         else enter(this.definition.policy().skipTo());
      } else {
         if (this.step != null) exitActions();
         if (active()) { this.state = CutsceneState.COMPLETED; this.listener.completed(); }
      }
      return true;
   }

   public void abort(CutsceneAbortReason reason, String diagnostic) {
      if (!active()) return;
      this.pendingFrame = false;
      this.frameSequence++;
      this.pausedCommit = null;
      this.state = CutsceneState.ABORTED;
      this.listener.aborted(reason, diagnostic == null ? "" : diagnostic);
   }

   public boolean active() { return this.state != CutsceneState.COMPLETED && this.state != CutsceneState.ABORTED; }
   public boolean pendingFrame() { return this.pendingFrame; }
   public CameraPose pose() { return this.pose; }
   public Step step() { return this.step; }
   public CutsceneSnapshot snapshot() {
      double progress = this.step instanceof Move move ? (double)this.stepTicks / move.durationTicks()
         : this.step instanceof Wait wait && wait.durationTicks() > 0 ? (double)this.stepTicks / wait.durationTicks() : 0;
      return new CutsceneSnapshot(this.sessionId, this.definition.id(), this.state,
         this.step == null ? null : this.step.id(), this.elapsedTicks, this.stepTicks, Math.max(0, Math.min(1, progress)));
   }

   private static boolean samePose(CameraPose a, CameraPose b) {
      return b != null && a.world().equals(b.world()) && a.distance(b) < 0.0001
         && Math.abs(angleDelta(a.yaw(), b.yaw())) < 0.0001 && Math.abs(a.pitch() - b.pitch()) < 0.0001;
   }
   private static double angleDelta(double from, double to) { return MoveMotion.shortestDelta(from, to); }
   private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
   private record Position(String world, double x, double y, double z) {
      static Position of(CameraPose pose) { return new Position(pose.world(), pose.x(), pose.y(), pose.z()); }
   }
}
