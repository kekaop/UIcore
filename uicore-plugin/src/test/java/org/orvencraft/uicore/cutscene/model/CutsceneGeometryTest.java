package org.orvencraft.uicore.cutscene.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CutsceneGeometryTest {
   @Test void smoothCurveRemainsNonnegativeAndIntegratesPlateaus() {
      SpeedCurve curve = new SpeedCurve(List.of(new SpeedCurve.Point(0, 0), new SpeedCurve.Point(.2, 0),
         new SpeedCurve.Point(.4, 4), new SpeedCurve.Point(.6, 0), new SpeedCurve.Point(1, 0)), SpeedCurve.Interpolation.SMOOTH);
      double previous = 0;
      for (int i = 0; i <= 1000; i++) {
         double time = i / 1000.0;
         assertTrue(curve.velocity(time) >= 0);
         assertTrue(curve.progress(time) + 1e-12 >= previous);
         previous = curve.progress(time);
      }
      assertEquals(0, curve.progress(.2), 0);
      assertEquals(1, curve.progress(.6), 1e-12);
      assertEquals(1, curve.progress(1), 0);
   }

   @Test void linearVelocityUsesIntegratedAreaRatherThanVelocityAsPosition() {
      SpeedCurve curve = new SpeedCurve(List.of(new SpeedCurve.Point(0, 0), new SpeedCurve.Point(1, 2)), SpeedCurve.Interpolation.LINEAR);
      assertEquals(.25, curve.progress(.5), 1e-12);
      assertEquals(1, curve.progress(1), 0);
      assertThrows(IllegalArgumentException.class, () -> new SpeedCurve(List.of(new SpeedCurve.Point(0, 0), new SpeedCurve.Point(1, 0)), SpeedCurve.Interpolation.SMOOTH));
      assertThrows(IllegalArgumentException.class, () -> new SpeedCurve(List.of(new SpeedCurve.Point(0, 1), new SpeedCurve.Point(.5, 1), new SpeedCurve.Point(.5, 2), new SpeedCurve.Point(1, 1)), SpeedCurve.Interpolation.LINEAR));
      assertThrows(IllegalArgumentException.class, () -> new SpeedCurve.Point(0, -1));
      assertThrows(IllegalArgumentException.class, () -> new SpeedCurve.Point(Double.NaN, 1));
   }

   @Test void splineIsTraversedByDistanceAndEndsExactlyAtTheLastPoint() {
      ArcLengthPath path = new ArcLengthPath(List.of(pose(0, 0, 0), pose(1, 3, 0), pose(11, 3, 2), pose(12, 0, 2)), PathType.SMOOTH);
      List<Double> lengths = new ArrayList<>();
      CameraPose previous = path.sample(0);
      for (int i = 1; i <= 100; i++) {
         CameraPose next = path.sample(i / 100.0);
         lengths.add(previous.distance(next)); previous = next;
      }
      double mean = lengths.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
      for (double distance : lengths) assertEquals(mean, distance, mean * .06);
      assertEquals(pose(12, 0, 2), path.sample(1));
      assertEquals(pose(0, 0, 0), path.sample(0));
   }

   @Test void polylineSpeedDoesNotDependOnSegmentLength() {
      ArcLengthPath path = new ArcLengthPath(List.of(pose(0, 0, 0), pose(1, 0, 0), pose(10, 0, 0)), PathType.LINEAR);
      assertEquals(5, path.sample(.5).x(), 1e-12);
      assertEquals(10, path.length(), 1e-12);
   }

   @Test void neighboringSplineControlsPreserveTangentAndSpeedAcrossSteps() {
      CameraPose a = pose(-10, 0, 0), b = pose(0, 0, 0), c = pose(10, 0, 0), d = pose(20, 0, 0);
      ArcLengthPath first = new ArcLengthPath(List.of(a, b), PathType.SMOOTH, null, c);
      ArcLengthPath second = new ArcLengthPath(List.of(b, c), PathType.SMOOTH, a, d);
      double[] left = first.tangent(1), right = second.tangent(0);
      assertEquals(left[0] / Math.hypot(left[0], left[2]), right[0] / Math.hypot(right[0], right[2]), 1e-6);
      assertEquals(first.sample(1).x() - first.sample(.99).x(), second.sample(.01).x() - second.sample(0).x(), 1e-6);
   }

   @Test void repeatedSpatialKnotsDoNotCreateUnintendedLoops() {
      ArcLengthPath path = new ArcLengthPath(List.of(pose(0, 0, 0), pose(0, 0, 0), pose(10, 0, 0), pose(10, 0, 0)), PathType.SMOOTH);
      assertEquals(10, path.length(), 1e-10);
      for (int i = 0; i <= 100; i++) assertEquals(i / 10.0, path.sample(i / 100.0).x(), 1e-10);
   }

   @Test void turnInPlaceUsesShortestYawAndAnIndependentRotationCurve() {
      CameraPose from = new CameraPose("world", 5, 7, 9, 179, 20);
      CameraPose to = new CameraPose("world", 5, 7, 9, -179, -20);
      Move move = move(new Rotation(RotationMode.INTERPOLATE, null, null, null, SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0));
      MoveMotion motion = new MoveMotion(definition(from, to, move), move, from);
      CameraPose half = motion.sample(50, from);
      assertEquals(180, half.yaw(), 1e-12);
      assertEquals(0, half.pitch(), 1e-12);
      assertEquals(0, half.distance(from), 0);
      assertEquals(181, motion.sample(100, half).yaw(), 1e-12);
      assertEquals(5, motion.sample(100, half).x(), 0);
      assertEquals(722, MoveMotion.angularDelta(179, -179, TurnDirection.CLOCKWISE, 2), 1e-12);
      assertEquals(-362, MoveMotion.angularDelta(-179, 179, TurnDirection.COUNTERCLOCKWISE, 1), 1e-12);
   }

   @Test void movementCurveAndRotationCurveAreIndependent() {
      CameraPose from = pose(0, 0, 0), to = new CameraPose("world", 10, 0, 0, 90, 0);
      Move move = new Move("move", List.of(), List.of(), null, List.of("end"), 100, PathType.LINEAR,
         new SpeedCurve(List.of(new SpeedCurve.Point(0, 0), new SpeedCurve.Point(1, 2)), SpeedCurve.Interpolation.LINEAR), Rotation.defaults(), "done");
      CameraPose half = new MoveMotion(definition(from, to, move), move, from).sample(50, from);
      assertEquals(2.5, half.x(), 1e-12);
      assertEquals(45, half.yaw(), 1e-12);
   }

   @Test void coincidentLookTargetRetainsLastValidRotationAndMissingContinuityIsRejected() {
      CameraPose from = new CameraPose("world", 0, 0, 0, -90, 0), to = pose(10, 0, 0);
      Move move = move(new Rotation(RotationMode.LOOK_AT, null, null, "end", SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0));
      MoveMotion motion = new MoveMotion(definition(from, to, move), move, from);
      CameraPose previous = new CameraPose("world", 9, 0, 0, -90, 0);
      assertEquals(-90, motion.sample(100, previous).yaw(), 0);
      Move namedFrom = new Move("move", List.of(), List.of(), "start", List.of("end"), 100, PathType.LINEAR,
         SpeedCurve.builtins().get("uniform"), Rotation.defaults(), "done");
      assertThrows(IllegalArgumentException.class, () -> new MoveMotion(definition(from, to, namedFrom), namedFrom, to));
   }

   @Test void changingLookModesCannotSilentlyJumpAtTheStepBoundary() {
      CameraPose from = pose(0, 0, 0), to = pose(10, 0, 0);
      for (Rotation rotation : List.of(
         new Rotation(RotationMode.FIXED, 45D, 0D, null, SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0),
         new Rotation(RotationMode.PATH, null, null, null, SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0),
         new Rotation(RotationMode.LOOK_AT, null, null, "end", SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0))) {
         Move move = move(rotation);
         IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new MoveMotion(definition(from, to, move), move, from));
         assertTrue(error.getMessage().contains("steps.move.rotation"));
      }
   }

   @Test void pathModeUsesTheExactEndpointTangentForContinuityValidation() {
      CameraPose from = new CameraPose("world", 0, 0, 0, -90, 0), to = pose(10, 0, 0);
      Rotation rotation = new Rotation(RotationMode.PATH, null, null, null, SpeedCurve.builtins().get("uniform"), TurnDirection.SHORTEST, 0);
      Move move = new Move("move", List.of(), List.of(), null, List.of("end", "after"), 100, PathType.SMOOTH,
         SpeedCurve.builtins().get("uniform"), rotation, "done");
      CutsceneDefinition def = new CutsceneDefinition("test", Map.of("end", to, "after", pose(20, 5, 10)), Map.of("move", move), "move", Policy.defaults());
      assertEquals(-90, new MoveMotion(def, move, from).sample(0, from).yaw(), 1e-12);
   }

   private static CameraPose pose(double x, double y, double z) { return new CameraPose("world", x, y, z, 0, 0); }
   private static Move move(Rotation rotation) {
      return new Move("move", List.of(), List.of(), null, List.of("end"), 100, PathType.LINEAR, SpeedCurve.builtins().get("uniform"), rotation, "done");
   }
   private static CutsceneDefinition definition(CameraPose start, CameraPose end, Move move) {
      return new CutsceneDefinition("test", Map.of("start", start, "end", end),
         Map.of("move", move, "done", new End("done", List.of(), List.of())), "move", Policy.defaults());
   }
}
