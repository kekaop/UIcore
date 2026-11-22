package org.orvencraft.uicore.cutscene;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.api.cutscene.CutsceneAbortReason;
import org.orvencraft.uicore.api.cutscene.CutsceneState;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;
import org.orvencraft.uicore.cutscene.model.SpeedCurve;

class CutscenePlaybackTest {
   private static final CameraPose ORIGIN = new CameraPose("world", 0, 64, 0, 0, 0);
   private static final CameraPose END = new CameraPose("world", 10, 66, 4, 45, 20);
   private static final End DONE = new End("done", List.of("done.enter"), List.of("done.exit"));

   @Test void movementArrivesExactlyAfterConfiguredServerTicksAndActionsRunOnce() {
      Probe probe = new Probe();
      CutscenePlayback playback = playback(Map.of("move", move("move", 4, "done"), "done", DONE), "move", probe);
      playback.begin();
      for (int i = 0; i < 4; i++) playback.tick();
      assertEquals(END.x(), playback.pose().x(), .0001);
      assertEquals(END.y(), playback.pose().y(), .0001);
      assertEquals(END.z(), playback.pose().z(), .0001);
      assertEquals(CutsceneState.COMPLETED, playback.snapshot().state());
      assertEquals(4, playback.snapshot().elapsedTicks());
      assertEquals(List.of("move.enter", "move.exit", "done.enter", "done.exit"), probe.actions);
      for (int i = 0; i < 20; i++) playback.tick();
      assertEquals(4, playback.snapshot().elapsedTicks());
      assertEquals(1, probe.completions);
   }

   @Test void manualPauseFreezesProgressAndResumeDoesNotRepeatActions() {
      Probe probe = new Probe();
      CutscenePlayback playback = playback(Map.of("move", move("move", 10, "done"), "done", DONE), "move", probe);
      playback.begin();
      playback.tick(); playback.tick(); playback.tick();
      CameraPose paused = playback.pose();
      assertTrue(playback.pause());
      for (int i = 0; i < 100; i++) playback.tick();
      assertEquals(paused, playback.pose());
      assertEquals(3, playback.snapshot().elapsedTicks());
      assertTrue(playback.resume());
      for (int i = 0; i < 7; i++) playback.tick();
      assertEquals(CutsceneState.COMPLETED, playback.snapshot().state());
      assertEquals(List.of("move.enter", "move.exit", "done.enter", "done.exit"), probe.actions);
   }

   @Test void asynchronousFramesFreezeClockAndPauseDefersEndpointTransitions() {
      Probe probe = new Probe();
      probe.deferFrames = true;
      CutscenePlayback playback = playback(Map.of("move", move("move", 1, "done"), "done", DONE), "move", probe);
      playback.begin(); playback.tick();
      for (int i = 0; i < 50; i++) playback.tick();
      assertEquals(1, playback.snapshot().elapsedTicks());
      assertEquals(List.of("move.enter"), probe.actions);
      assertTrue(playback.pause());
      probe.commit.run();
      assertEquals(CutsceneState.PAUSED, playback.snapshot().state());
      assertEquals(List.of("move.enter"), probe.actions);
      playback.resume();
      assertEquals(CutsceneState.COMPLETED, playback.snapshot().state());
      assertEquals(1, probe.completions);
   }

   @Test void choiceDoubleClickAndTimeoutCanOnlyTakeOneBranch() {
      Probe probe = new Probe();
      CutscenePlayback playback = choicePlayback(probe, 1, "fallback");
      playback.begin();
      assertTrue(playback.choose("left"));
      assertFalse(playback.choose("right"));
      playback.tick();
      assertEquals(List.of("left"), probe.choices);
      assertEquals(List.of("choice.enter", "choice.exit", "left.enter", "left.exit"), probe.actions);
   }

   @Test void timeoutClaimsTransitionBeforeLaterChoiceRequest() {
      Probe probe = new Probe();
      CutscenePlayback playback = choicePlayback(probe, 1, "fallback");
      playback.begin(); playback.tick();
      assertFalse(playback.choose("left"));
      assertEquals("fallback", playback.snapshot().stepId());
      assertEquals(1, probe.completions);
   }

   @Test void eligibilityIsRecheckedAndUnavailableChoicesUseFallback() {
      Probe probe = new Probe();
      CutscenePlayback playback = choicePlayback(probe, 20, "fallback");
      playback.begin();
      probe.eligible.set(false);
      assertFalse(playback.choose("left"));
      playback.tick();
      assertEquals("fallback", playback.snapshot().stepId());
      assertTrue(probe.choices.isEmpty());
   }

   @Test void unavailableChoiceWithoutFallbackAbortsWithDiagnostic() {
      Probe probe = new Probe(); probe.eligible.set(false);
      CutscenePlayback playback = choicePlayback(probe, 20, null);
      playback.begin();
      assertEquals(CutsceneState.ABORTED, playback.snapshot().state());
      assertEquals(CutsceneAbortReason.NO_AVAILABLE_CHOICE, probe.reason);
   }

   @Test void externalWaitAndTimedWaitHaveIndependentClocks() {
      Probe probe = new Probe();
      Map<String, Step> steps = Map.of("external", new Wait("external", List.of("external.enter"), List.of("external.exit"), -1, "timed"),
         "timed", new Wait("timed", List.of(), List.of(), 3, "done"), "done", DONE);
      CutscenePlayback playback = playback(steps, "external", probe);
      playback.begin();
      for (int i = 0; i < 30; i++) playback.tick();
      assertEquals(0, playback.snapshot().elapsedTicks());
      assertTrue(playback.resume());
      playback.tick(); playback.tick();
      assertEquals("timed", playback.snapshot().stepId());
      playback.tick();
      assertEquals(CutsceneState.COMPLETED, playback.snapshot().state());
      assertEquals(3, playback.snapshot().elapsedTicks());
   }

   @Test void playersUsingSameSnapshotHaveIndependentProgress() {
      Probe one = new Probe(), two = new Probe();
      CutsceneDefinition definition = definition(Map.of("move", move("move", 4, "done"), "done", DONE), "move", Policy.defaults());
      CutscenePlayback first = new CutscenePlayback(UUID.randomUUID(), definition, ORIGIN, one);
      CutscenePlayback second = new CutscenePlayback(UUID.randomUUID(), definition, ORIGIN, two);
      first.begin(); second.begin(); first.pause();
      for (int i = 0; i < 4; i++) { first.tick(); second.tick(); }
      assertEquals(ORIGIN, first.pose());
      assertEquals(CutsceneState.PAUSED, first.snapshot().state());
      assertEquals(CutsceneState.COMPLETED, second.snapshot().state());
   }

   @Test void skippedStepsDoNotRunActionsAndStaleFrameCannotContinue() {
      Probe probe = new Probe(); probe.deferFrames = true;
      Policy policy = new Policy(StartMode.TRANSITION, 4, Completion.KEEP_OPEN, Restart.REJECT,
         true, "done", true, true, false, false, 200);
      Map<String, Step> steps = Map.of("move", move("move", 2, "skipped"), "skipped", new Wait("skipped", List.of("must-not-run"), List.of(), 4, "done"), "done", DONE);
      CutscenePlayback playback = new CutscenePlayback(UUID.randomUUID(), definition(steps, "move", policy), ORIGIN, probe);
      playback.begin(); playback.tick();
      Runnable stale = probe.commit;
      assertTrue(playback.skip()); stale.run();
      assertEquals(CutsceneState.COMPLETED, playback.snapshot().state());
      assertEquals(List.of("move.enter", "move.exit", "done.enter", "done.exit"), probe.actions);
      assertEquals(1, probe.completions);
   }

   @Test void initialMismatchFailsAndTransitionModeMovesSmoothlyToNamedStart() {
      CameraPose offset = new CameraPose("world", -10, 64, 0, 0, 0);
      Move named = new Move("move", List.of(), List.of(), "origin", List.of("end"), 3, PathType.LINEAR,
         SpeedCurve.builtins().get("uniform"), Rotation.defaults(), "done");
      Policy defaults = Policy.defaults();
      Policy initial = new Policy(StartMode.INITIAL, 2, defaults.completion(), defaults.restart(), false, null, true, true, false, false, 200);
      Probe rejected = new Probe();
      CutscenePlayback mismatch = new CutscenePlayback(UUID.randomUUID(), definition(Map.of("move", named, "done", DONE), "move", initial), offset, rejected);
      mismatch.begin();
      assertEquals(CutsceneAbortReason.PREPARATION_FAILED, rejected.reason);
      Policy transition = new Policy(StartMode.TRANSITION, 2, defaults.completion(), defaults.restart(), false, null, true, true, false, false, 200);
      CutscenePlayback smooth = new CutscenePlayback(UUID.randomUUID(), definition(Map.of("move", named, "done", DONE), "move", transition), offset, new Probe());
      smooth.begin(); smooth.tick();
      assertEquals(-5, smooth.pose().x(), .0001);
      smooth.tick();
      assertEquals(ORIGIN, smooth.pose());
      assertEquals("move", smooth.snapshot().stepId());
   }

   @Test void staleFrameCommitCannotCompleteAMovementEnteredBySkip() {
      Probe probe = new Probe(); probe.deferFrames = true;
      Policy policy = new Policy(StartMode.TRANSITION, 4, Completion.KEEP_OPEN, Restart.REJECT,
         true, "second", true, true, false, false, 200);
      Map<String, Step> steps = Map.of("first", move("first", 2, "second"), "second", move("second", 5, "done"), "done", DONE);
      CutscenePlayback playback = new CutscenePlayback(UUID.randomUUID(), definition(steps, "first", policy), ORIGIN, probe);
      playback.begin(); playback.tick();
      Runnable stale = probe.commit;
      playback.skip(); playback.tick();
      stale.run();
      assertTrue(playback.pendingFrame());
      assertEquals("second", playback.snapshot().stepId());
      assertFalse(probe.actions.contains("second.exit"));
      probe.commit.run();
      assertFalse(playback.pendingFrame());
   }

   @Test void eventCallbackAbortingPlaybackPreventsEntryActions() {
      CutscenePlayback[] current = new CutscenePlayback[1];
      Probe probe = new Probe() {
         @Override public void entered(Step step) { current[0].abort(CutsceneAbortReason.MENU_CLOSED, "closed in event"); }
      };
      current[0] = playback(Map.of("move", move("move", 2, "done"), "done", DONE), "move", probe);
      current[0].begin();
      assertTrue(probe.actions.isEmpty());
      assertEquals(CutsceneAbortReason.MENU_CLOSED, probe.reason);
   }

   private static Move move(String id, int ticks, String next) {
      return new Move(id, List.of(id + ".enter"), List.of(id + ".exit"), null, List.of("end"), ticks,
         PathType.LINEAR, SpeedCurve.builtins().get("uniform"), Rotation.defaults(), next);
   }
   private static CutsceneDefinition definition(Map<String, Step> steps, String start, Policy policy) {
      return new CutsceneDefinition("scene", Map.of("origin", ORIGIN, "end", END), steps, start, policy);
   }
   private static CutscenePlayback playback(Map<String, Step> steps, String start, Probe probe) {
      return new CutscenePlayback(UUID.randomUUID(), definition(steps, start, Policy.defaults()), ORIGIN, probe);
   }
   private static CutscenePlayback choicePlayback(Probe probe, long timeout, String fallback) {
      Choice choice = new Choice("choice", List.of("choice.enter"), List.of("choice.exit"),
         Map.of("left", new ChoiceOption("button-left", "left"), "right", new ChoiceOption("button-right", "right")), timeout, "fallback", fallback);
      return playback(Map.of("choice", choice,
         "left", new End("left", List.of("left.enter"), List.of("left.exit")),
         "right", new End("right", List.of("right.enter"), List.of("right.exit")),
         "fallback", new End("fallback", List.of(), List.of())), "choice", probe);
   }
   private static class Probe implements CutscenePlayback.Listener {
      final List<String> actions = new ArrayList<>();
      final List<String> choices = new ArrayList<>();
      final AtomicBoolean eligible = new AtomicBoolean(true);
      int completions;
      CutsceneAbortReason reason;
      boolean deferFrames;
      Runnable commit;
      @Override public void actions(List<String> actions) { this.actions.addAll(actions); }
      @Override public boolean eligible(String elementId) { return eligible.get(); }
      @Override public void choice(String id) { choices.add(id); }
      @Override public void completed() { completions++; }
      @Override public void aborted(CutsceneAbortReason reason, String diagnostic) { this.reason = reason; }
      @Override public void frame(CameraPose pose, Runnable committed) { if (deferFrames) commit = committed; else committed.run(); }
   }
}
