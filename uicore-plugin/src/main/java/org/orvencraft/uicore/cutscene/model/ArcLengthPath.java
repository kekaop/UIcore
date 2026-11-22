package org.orvencraft.uicore.cutscene.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Prepared lookup table: all per-tick work is a binary search and vector interpolation. */
public final class ArcLengthPath {
   private final List<CameraPose> samples;
   private final CameraPose first, last;
   private final double[] firstDirection, lastDirection;
   private final double[] distances;
   private final double length;

   public ArcLengthPath(List<CameraPose> points, CutsceneDefinition.PathType type) {
      this(points, type, null, null);
   }

   /** Optional neighboring control points preserve the tangent across adjoining smooth moves. */
   public ArcLengthPath(List<CameraPose> points, CutsceneDefinition.PathType type, CameraPose tangentBefore, CameraPose tangentAfter) {
      if (points.isEmpty()) throw new IllegalArgumentException("Path needs at least one point");
      String world = points.get(0).world();
      if (points.stream().anyMatch(p -> !p.world().equals(world))) throw new IllegalArgumentException("Path points must share one world");
      if ((tangentBefore != null && !tangentBefore.world().equals(world)) || (tangentAfter != null && !tangentAfter.world().equals(world))) {
         throw new IllegalArgumentException("Path tangent control points must share the path world");
      }
      first = points.get(0); last = points.get(points.size() - 1);
      // Repeated spatial knots would create spurious Catmull-Rom loops around a stationary point.
      ArrayList<CameraPose> distinct = new ArrayList<>();
      for (CameraPose point : points) {
         if (distinct.isEmpty() || distinct.get(distinct.size() - 1).distance(point) > 0) distinct.add(point);
      }
      points = distinct;
      if (points.size() < 2) {
         firstDirection = new double[] {0, 0, 0}; lastDirection = new double[] {0, 0, 0};
      } else {
         CameraPose before = type == CutsceneDefinition.PathType.SMOOTH && tangentBefore != null ? tangentBefore : points.get(0);
         CameraPose after = type == CutsceneDefinition.PathType.SMOOTH && tangentAfter != null ? tangentAfter : points.get(points.size() - 1);
         firstDirection = difference(points.get(1), before);
         lastDirection = difference(after, points.get(points.size() - 2));
      }
      ArrayList<CameraPose> prepared = new ArrayList<>();
      prepared.add(points.get(0));
      for (int segment = 0; segment + 1 < points.size(); segment++) {
         CameraPose a = points.get(segment), b = points.get(segment + 1);
         if (type == CutsceneDefinition.PathType.LINEAR) {
            prepared.add(b);
         } else {
            CameraPose before = segment == 0 && tangentBefore != null ? tangentBefore : points.get(Math.max(0, segment - 1));
            CameraPose after = segment + 2 == points.size() && tangentAfter != null ? tangentAfter : points.get(Math.min(points.size() - 1, segment + 2));
            for (int sample = 1; sample <= 256; sample++) {
               double t = sample / 256.0;
               prepared.add(sample == 256 ? b : new CameraPose(world,
                  catmull(before.x(), a.x(), b.x(), after.x(), t),
                  catmull(before.y(), a.y(), b.y(), after.y(), t),
                  catmull(before.z(), a.z(), b.z(), after.z(), t), a.yaw(), a.pitch()));
            }
         }
      }
      samples = List.copyOf(prepared);
      distances = new double[samples.size()];
      for (int i = 1; i < samples.size(); i++) distances[i] = distances[i - 1] + samples.get(i - 1).distance(samples.get(i));
      length = distances[distances.length - 1];
      if (!Double.isFinite(length)) throw new IllegalArgumentException("Path length must be finite");
   }

   public double length() { return length; }

   public CameraPose sample(double progress) {
      if (!Double.isFinite(progress)) throw new IllegalArgumentException("Path progress must be finite");
      if (progress <= 0) return first;
      if (progress >= 1) return last;
      if (samples.size() == 1 || length == 0) return first;
      double distance = progress * length;
      int index = Arrays.binarySearch(distances, distance);
      if (index >= 0) return samples.get(index);
      int next = -index - 1;
      CameraPose a = samples.get(next - 1), b = samples.get(next);
      double t = (distance - distances[next - 1]) / (distances[next] - distances[next - 1]);
      return new CameraPose(a.world(), mix(a.x(), b.x(), t), mix(a.y(), b.y(), t), mix(a.z(), b.z(), t), a.yaw(), a.pitch());
   }

   /** Returns a direction vector; a zero vector means the path has no positional motion. */
   public double[] tangent(double progress) {
      if (length == 0) return new double[] {0, 0, 0};
      if (progress <= 0) return firstDirection.clone();
      if (progress >= 1) return lastDirection.clone();
      double distance = Math.max(0, Math.min(1, progress)) * length;
      int index = Arrays.binarySearch(distances, distance);
      int next = index >= 0 ? Math.min(samples.size() - 1, index + 1) : -index - 1;
      next = Math.max(1, Math.min(samples.size() - 1, next));
      CameraPose a = samples.get(next - 1), b = samples.get(next);
      while (a.distance(b) == 0 && next + 1 < samples.size()) b = samples.get(++next);
      return new double[] {b.x() - a.x(), b.y() - a.y(), b.z() - a.z()};
   }

   private static double[] difference(CameraPose a, CameraPose b) { return new double[] {a.x() - b.x(), a.y() - b.y(), a.z() - b.z()}; }

   private static double mix(double a, double b, double t) { return a * (1 - t) + b * t; }
   private static double catmull(double p0, double p1, double p2, double p3, double t) {
      double t2 = t * t, t3 = t2 * t;
      return .5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
         + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
   }
}
