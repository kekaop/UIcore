package org.orvencraft.uicore.cutscene;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.api.cutscene.*;
import org.orvencraft.uicore.api.event.*;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;

class CutsceneManagerTest {
   private Field serverField;
   private Object originalServer;

   @BeforeEach void installMinimalEventThreadProbe() throws Exception {
      serverField = Bukkit.class.getDeclaredField("server");
      serverField.setAccessible(true);
      originalServer = serverField.get(null);
      serverField.set(null, Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
         (proxy, method, arguments) -> method.getName().equals("isPrimaryThread") ? true : defaultValue(method.getReturnType())));
   }
   @AfterEach void restoreServer() throws Exception { serverField.set(null, originalServer); }

   @Test void requestAcceptancePrecedesActualStartAndClockStaysFrozenDuringPreparation() {
      Host host = new Host(); host.preparation = new CompletableFuture<>();
      Player player = player();
      CutsceneManager manager = manager(host, scene(2, 200));
      assertEquals(CutsceneStartResult.ACCEPTED, manager.start(player, "scene"));
      assertEquals(CutsceneState.PREPARING, manager.state(player).orElseThrow().state());
      manager.tick(player); manager.tick(player);
      assertTrue(host.events.isEmpty());
      assertEquals(0, manager.state(player).orElseThrow().elapsedTicks());
      host.preparation.complete(true);
      assertEquals(CutsceneState.PLAYING, manager.state(player).orElseThrow().state());
      assertEquals(1, host.count(CutsceneStartEvent.class));
      manager.tick(player); manager.tick(player);
      assertEquals(1, host.count(CutsceneCompleteEvent.class));
      assertFalse(manager.active(player));
   }

   @Test void latePreparationCannotStartAnObsoleteMenuSession() {
      Host host = new Host();
      CompletableFuture<Boolean> stale = new CompletableFuture<>(); host.preparation = stale;
      Player player = player();
      CutsceneManager manager = manager(host, scene(3, 200));
      manager.start(player, "scene");
      UUID oldSession = manager.state(player).orElseThrow().sessionId();
      host.token = new Object(); host.preparation = CompletableFuture.completedFuture(true);
      assertEquals(CutsceneStartResult.ACCEPTED, manager.start(player, "scene"));
      UUID newSession = manager.state(player).orElseThrow().sessionId();
      assertNotEquals(oldSession, newSession);
      stale.complete(true);
      assertEquals(newSession, manager.state(player).orElseThrow().sessionId());
      assertEquals(1, host.count(CutsceneStartEvent.class));
      assertEquals(1, host.count(CutsceneAbortEvent.class));
   }

   @Test void preparationTimeoutClosesSafelyAndInvalidatesItsLateCompletion() {
      Host host = new Host(); host.preparation = new CompletableFuture<>();
      Player player = player();
      CutsceneManager manager = manager(host, scene(3, 2));
      manager.start(player, "scene"); manager.tick(player); manager.tick(player);
      assertEquals(CutsceneState.ABORTED, manager.state(player).orElseThrow().state());
      CutsceneAbortEvent abort = (CutsceneAbortEvent)host.events.get(0);
      assertEquals(CutsceneAbortReason.PREPARATION_TIMEOUT, abort.getReason());
      assertFalse(abort.getDiagnostic().isBlank());
      assertEquals(List.of(true), host.finishes);
      host.preparation.complete(true);
      assertEquals(0, host.count(CutsceneStartEvent.class));
   }

   @Test void activeSessionKeepsItsImmutableDefinitionAfterReload() {
      Host host = new Host(); Player player = player();
      CutsceneManager manager = manager(host, scene(2, 200));
      manager.start(player, "scene");
      manager.replaceDefinitions(Map.of("scene", scene(20, 200)));
      manager.tick(player); manager.tick(player);
      assertEquals(CutsceneState.COMPLETED, manager.state(player).orElseThrow().state());
      manager.start(player, "scene"); manager.tick(player); manager.tick(player);
      assertEquals(CutsceneState.PLAYING, manager.state(player).orElseThrow().state());
      assertEquals(.1, manager.state(player).orElseThrow().stepProgress(), 1e-12);
   }

   @Test void motionStartsAtThePoseCommittedWhilePreparationWasWaiting() {
      Host host = new Host(); Player player = player();
      host.preparation = new CompletableFuture<>();
      CameraPose end = new CameraPose("world", 10, 64, 0, 0, 0);
      Move move = new Move("move", List.of(), List.of(), null, List.of("end"), 2, PathType.LINEAR,
         org.orvencraft.uicore.cutscene.model.SpeedCurve.builtins().get("uniform"), Rotation.defaults(), "done");
      CutsceneDefinition scene = new CutsceneDefinition("scene", Map.of("end", end),
         Map.of("move", move, "done", new End("done", List.of(), List.of())), "move", Policy.defaults());
      CutsceneManager manager = manager(host, scene);
      manager.start(player, "scene");
      host.pose = new CameraPose("world", 4, 64, 0, 0, 0);
      host.preparation.complete(true);
      manager.tick(player);
      assertEquals(7, host.pose.x(), 1e-10, "The first new frame must start from the settled camera, not the stale request pose");
   }

   @Test void unavailableRequestsDoNotCreateSessionsAndLifecycleInvalidatesPendingWork() {
      Host host = new Host(); Player player = player();
      CutsceneManager manager = manager(host, scene(2, 200));
      assertEquals(CutsceneStartResult.UNKNOWN_SCENE, manager.start(player, "missing"));
      host.token = null;
      assertEquals(CutsceneStartResult.NO_MENU, manager.start(player, "scene"));
      assertTrue(manager.state(player).isEmpty());
      host.token = new Object(); host.preparation = new CompletableFuture<>();
      manager.start(player, "scene");
      assertFalse(manager.choose(player, "option"));
      assertEquals(CutsceneStartResult.ALREADY_RUNNING, manager.start(player, "scene"));
      manager.abort(player, CutsceneAbortReason.PLAYER_QUIT);
      host.preparation.complete(true);
      assertTrue(manager.state(player).isEmpty());
      assertEquals(0, host.count(CutsceneStartEvent.class));
   }

   @Test void explicitStopReleasesRouteWithoutClosingMenuAndReplacementDoesNotFinalizeNewRoute() {
      Host host = new Host(); Player player = player();
      CutsceneManager manager = manager(host, scene(4, 200));
      manager.start(player, "scene");
      assertTrue(manager.stop(player));
      assertEquals(List.of(false), host.finishes);
      assertFalse(manager.stop(player));
      host.finishes.clear();
      manager.start(player, "scene");
      CutsceneDefinition original = scene(4, 200);
      Policy defaults = original.policy();
      Policy replace = new Policy(defaults.startMode(), defaults.transitionTicks(), defaults.completion(), Restart.REPLACE,
         defaults.allowSkip(), defaults.skipTo(), defaults.cursor(), defaults.clicks(), defaults.edgePan(), defaults.freezeUi(), defaults.chunkTimeoutTicks());
      manager.replaceDefinitions(Map.of("scene", new CutsceneDefinition("scene", original.points(), original.steps(), original.startStep(), replace)));
      manager.start(player, "scene");
      assertTrue(manager.active(player));
      assertTrue(host.finishes.isEmpty());
   }

   @Test void anInternalStartPoseFailureRestoresMenuSafely() {
      Host host = new Host(); Player player = player();
      CameraPose distant = new CameraPose("world", 10, 64, 0, 0, 0);
      Policy policy = new Policy(StartMode.INITIAL, 2, Completion.KEEP_OPEN, Restart.REJECT, false, null, true, true, false, false, 200);
      Move move = new Move("move", List.of(), List.of(), "distant", List.of("distant"), 2, PathType.LINEAR,
         org.orvencraft.uicore.cutscene.model.SpeedCurve.builtins().get("uniform"), Rotation.defaults(), "end");
      CutsceneDefinition definition = new CutsceneDefinition("scene", Map.of("distant", distant),
         Map.of("move", move, "end", new End("end", List.of(), List.of())), "move", policy);
      CutsceneManager manager = manager(host, definition);
      manager.start(player, "scene");
      assertEquals(CutsceneState.ABORTED, manager.state(player).orElseThrow().state());
      assertEquals(List.of(true), host.finishes);
      assertEquals(0, host.count(CutsceneStartEvent.class));
   }

   private static CutsceneManager manager(Host host, CutsceneDefinition definition) {
      CutsceneManager manager = new CutsceneManager(host);
      manager.replaceDefinitions(Map.of(definition.id(), definition));
      return manager;
   }
   private static CutsceneDefinition scene(long ticks, long timeout) {
      Policy policy = new Policy(StartMode.TRANSITION, 2, Completion.KEEP_OPEN, Restart.REJECT, false, null, true, true, false, false, timeout);
      return new CutsceneDefinition("scene", Map.of(), Map.of(
         "wait", new Wait("wait", List.of(), List.of(), ticks, "end"),
         "end", new End("end", List.of(), List.of())), "wait", policy);
   }
   private static Player player() {
      UUID id = UUID.randomUUID();
      return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) ->
         method.getName().equals("getUniqueId") ? id : defaultValue(method.getReturnType()));
   }
   private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) return null;
      if (type == boolean.class) return false;
      if (type == int.class) return 0;
      if (type == long.class) return 0L;
      if (type == double.class) return 0.0;
      if (type == float.class) return 0F;
      if (type == byte.class) return (byte)0;
      if (type == short.class) return (short)0;
      if (type == char.class) return '\0';
      return null;
   }
   private static final class Host implements CutsceneManager.Host {
      Object token = new Object();
      CompletableFuture<Boolean> preparation = CompletableFuture.completedFuture(true);
      final List<Event> events = new ArrayList<>();
      final List<Boolean> finishes = new ArrayList<>();
      CameraPose pose = new CameraPose("world", 0, 64, 0, 0, 0);
      @Override public Object menuToken(Player player) { return token; }
      @Override public CameraPose camera(Player player) { return pose; }
      @Override public void execute(Player player, Runnable operation) { operation.run(); }
      @Override public CompletionStage<Boolean> prepare(Player player, CutsceneDefinition definition, CameraPose initial) { return preparation; }
      @Override public CompletionStage<Boolean> apply(Player player, CameraPose pose) { this.pose = pose; return CompletableFuture.completedFuture(true); }
      @Override public boolean eligible(Player player, String element) { return true; }
      @Override public void actions(Player player, List<String> actions) { }
      @Override public void event(Event event) { events.add(event); }
      @Override public void finished(Player player, boolean close) { finishes.add(close); }
      long count(Class<? extends Event> type) { return events.stream().filter(type::isInstance).count(); }
   }
}
