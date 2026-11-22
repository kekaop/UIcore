package org.orvencraft.uicore.cutscene.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A deeply immutable configuration snapshot retained for the lifetime of a playback. */
public record CutsceneDefinition(String id, Map<String, CameraPose> points, Map<String, Step> steps,
                                 String startStep, Policy policy) {
   public CutsceneDefinition {
      Objects.requireNonNull(id); Objects.requireNonNull(startStep); Objects.requireNonNull(policy);
      points = Map.copyOf(points); steps = Map.copyOf(steps);
   }

   public CameraPose initialPose(CameraPose current) {
      Step first = steps.get(startStep);
      return first instanceof Move move && move.from() != null ? points.get(move.from()) : current;
   }

   public enum PathType { LINEAR, SMOOTH }
   public enum RotationMode { FIXED, INTERPOLATE, PATH, LOOK_AT }
   public enum TurnDirection { SHORTEST, CLOCKWISE, COUNTERCLOCKWISE }
   public enum StartMode { INITIAL, TRANSITION }
   public enum Completion { KEEP_OPEN, CLOSE_MENU }
   public enum Restart { REJECT, REPLACE }

   public record Policy(StartMode startMode, long transitionTicks, Completion completion, Restart restart,
                        boolean allowSkip, String skipTo, boolean cursor, boolean clicks,
                        boolean edgePan, boolean freezeUi, long chunkTimeoutTicks) {
      public static Policy defaults() {
         return new Policy(StartMode.TRANSITION, 20, Completion.KEEP_OPEN, Restart.REJECT,
            false, null, true, true, false, false, 200);
      }
   }

   public record Rotation(RotationMode mode, Double yaw, Double pitch, String target,
                          SpeedCurve curve, TurnDirection direction, int turns) {
      public Rotation { Objects.requireNonNull(mode); Objects.requireNonNull(curve); Objects.requireNonNull(direction); }
      public static Rotation defaults() {
         return new Rotation(RotationMode.INTERPOLATE, null, null, null,
            SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0);
      }
   }

   public sealed interface Step permits Move, Wait, Choice, End {
      String id();
      List<String> enterActions();
      List<String> exitActions();
   }

   public record Move(String id, List<String> enterActions, List<String> exitActions, String from,
                      List<String> to, long durationTicks, PathType path, SpeedCurve speed,
                      Rotation rotation, String next, String tangentBefore, String tangentAfter) implements Step {
      public Move {
         enterActions = List.copyOf(enterActions); exitActions = List.copyOf(exitActions); to = List.copyOf(to);
         Objects.requireNonNull(path); Objects.requireNonNull(speed); Objects.requireNonNull(rotation);
      }
      public Move(String id, List<String> enterActions, List<String> exitActions, String from,
                  List<String> to, long durationTicks, PathType path, SpeedCurve speed, Rotation rotation, String next) {
         this(id, enterActions, exitActions, from, to, durationTicks, path, speed, rotation, next, null, null);
      }
   }

   /** A duration of -1 waits for an external resume request; zero is an instantaneous transition. */
   public record Wait(String id, List<String> enterActions, List<String> exitActions,
                      long durationTicks, String next) implements Step {
      public Wait { enterActions = List.copyOf(enterActions); exitActions = List.copyOf(exitActions); }
   }

   public record ChoiceOption(String element, String next) {}
   /** timeoutNext and fallbackNext may be null; no available options aborts if fallbackNext is absent. */
   public record Choice(String id, List<String> enterActions, List<String> exitActions,
                        Map<String, ChoiceOption> options, long timeoutTicks,
                        String timeoutNext, String fallbackNext) implements Step {
      public Choice {
         enterActions = List.copyOf(enterActions); exitActions = List.copyOf(exitActions); options = Map.copyOf(options);
      }
   }

   public record End(String id, List<String> enterActions, List<String> exitActions) implements Step {
      public End { enterActions = List.copyOf(enterActions); exitActions = List.copyOf(exitActions); }
   }
}
