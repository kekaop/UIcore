package org.orvencraft.uicore.cutscene.model;

import java.util.ArrayList;
import java.util.Objects;
import static org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;

/** Position and rotation use separate integrated velocity curves over the same tick duration. */
public final class MoveMotion {
   private final Move move;
   private final CameraPose start;
   private final CameraPose end;
   private final CameraPose target;
   private final ArcLengthPath path;

   public MoveMotion(CutsceneDefinition definition, Move move, CameraPose current) {
      this(definition, move, current, null);
   }

   /** Bind rotation to a current pose while reusing a path prepared before playback. */
   public MoveMotion(CutsceneDefinition definition, Move move, CameraPose current, ArcLengthPath preparedPath) {
      this.move = Objects.requireNonNull(move);
      start = move.from() == null ? current : Objects.requireNonNull(definition.points().get(move.from()), "from point");
      if (!start.world().equals(current.world()) || start.distance(current) > .0001
         || Math.abs(shortestDelta(current.yaw(), start.yaw())) > .0001 || Math.abs(start.pitch() - current.pitch()) > .0001) {
         throw new IllegalArgumentException("Cutscene '" + definition.id() + "', step '" + move.id()
            + "', path 'steps." + move.id() + ".from': starts away from the current camera; use a continuous route from current");
      }
      end = move.to().isEmpty() ? start : Objects.requireNonNull(definition.points().get(move.to().get(move.to().size() - 1)), "to point");
      path = preparedPath == null ? preparePath(definition, move, start) : preparedPath;
      if (!path.sample(0).world().equals(start.world()) || path.sample(0).distance(start) > .0001
         || path.sample(1).distance(end) > .0001) throw new IllegalArgumentException("Prepared path does not match move endpoints");
      target = move.rotation().target() == null ? null : definition.points().get(move.rotation().target());
      if (move.durationTicks() <= 0) throw new IllegalArgumentException("Move duration must be positive");
      CameraPose firstFrame = sample(0, start);
      if (Math.abs(shortestDelta(start.yaw(), firstFrame.yaw())) > .0001 || Math.abs(start.pitch() - firstFrame.pitch()) > .0001) {
         throw new IllegalArgumentException("Cutscene '" + definition.id() + "', step '" + move.id() + "', path 'steps." + move.id()
            + ".rotation': initial rotation must match the current viewpoint; add an interpolate turn before changing the look mode");
      }
   }

   public static ArcLengthPath preparePath(CutsceneDefinition definition, Move move, CameraPose current) {
      CameraPose start = move.from() == null ? current : Objects.requireNonNull(definition.points().get(move.from()), "from point");
      ArrayList<CameraPose> points = new ArrayList<>();
      points.add(start);
      for (String point : move.to()) points.add(Objects.requireNonNull(definition.points().get(point), "to point " + point));
      return new ArcLengthPath(points, move.path(), move.tangentBefore() == null ? null : definition.points().get(move.tangentBefore()),
         move.tangentAfter() == null ? null : definition.points().get(move.tangentAfter()));
   }

   public ArcLengthPath path() { return path; }

   public CameraPose sample(double elapsedTicks, CameraPose lastPose) {
      double time = Math.max(0, Math.min(1, elapsedTicks / move.durationTicks()));
      double progress = move.speed().progress(time);
      CameraPose position = path.sample(progress);
      Rotation rotation = move.rotation();
      CameraPose previous = lastPose == null ? start : lastPose;
      return switch (rotation.mode()) {
         case FIXED -> position.withRotation(rotation.yaw() == null ? start.yaw() : rotation.yaw(),
            rotation.pitch() == null ? start.pitch() : rotation.pitch());
         case INTERPOLATE -> {
            double fraction = rotation.curve().progress(time);
            double yaw = rotation.yaw() == null ? end.yaw() : rotation.yaw();
            double pitch = rotation.pitch() == null ? end.pitch() : rotation.pitch();
            yield position.withRotation(start.yaw() + angularDelta(start.yaw(), yaw, rotation.direction(), rotation.turns()) * fraction,
               start.pitch() + (pitch - start.pitch()) * fraction);
         }
         case PATH -> {
            double[] tangent = path.tangent(progress);
            yield look(position, tangent[0], tangent[1], tangent[2], previous);
         }
         case LOOK_AT -> {
            if (target == null) throw new IllegalArgumentException("look-at rotation requires a target point");
            yield look(position, target.x() - position.x(), target.y() - position.y(), target.z() - position.z(), previous);
         }
      };
   }

   private static CameraPose look(CameraPose position, double x, double y, double z, CameraPose previous) {
      double horizontal = Math.hypot(x, z);
      if (horizontal < 1e-12 && Math.abs(y) < 1e-12) return position.withRotation(previous.yaw(), previous.pitch());
      double yaw = horizontal < 1e-12 ? previous.yaw() : Math.toDegrees(Math.atan2(-x, z));
      // Keep the equivalent yaw nearest the previous sample across the +/-180 boundary.
      yaw = previous.yaw() + shortestDelta(previous.yaw(), yaw);
      return position.withRotation(yaw, Math.toDegrees(Math.atan2(-y, horizontal)));
   }

   public static double shortestDelta(double from, double to) {
      double delta = ((to % 360 - from % 360) % 360 + 540) % 360 - 180;
      return delta;
   }

   public static double angularDelta(double from, double to, TurnDirection direction, int turns) {
      double delta = shortestDelta(from, to);
      if (direction == TurnDirection.CLOCKWISE) {
         if (delta < 0) delta += 360;
         return delta + turns * 360.0;
      }
      if (direction == TurnDirection.COUNTERCLOCKWISE) {
         if (delta > 0) delta -= 360;
         return delta - turns * 360.0;
      }
      return delta + (delta < 0 ? -1 : 1) * turns * 360.0;
   }
}
