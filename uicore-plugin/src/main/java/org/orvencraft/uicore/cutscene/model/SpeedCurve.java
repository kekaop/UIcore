package org.orvencraft.uicore.cutscene.model;

import java.util.List;
import java.util.Map;

/** Relative, nonnegative velocity integrated and normalized to distance progress. */
public final class SpeedCurve {
   public enum Interpolation { LINEAR, SMOOTH }
   public record Point(double time, double speed) {
      public Point {
         if (!Double.isFinite(time) || !Double.isFinite(speed) || time < 0 || time > 1 || speed < 0) {
            throw new IllegalArgumentException("Curve points require time in [0, 1] and finite speed >= 0");
         }
      }
   }

   private final List<Point> points;
   private final Interpolation interpolation;
   private final double[] integral;
   private final double total;

   public SpeedCurve(List<Point> points, Interpolation interpolation) {
      this.points = List.copyOf(points);
      this.interpolation = java.util.Objects.requireNonNull(interpolation, "interpolation");
      if (points.size() < 2 || points.get(0).time() != 0 || points.get(points.size() - 1).time() != 1) {
         throw new IllegalArgumentException("Curve must contain at least two points covering exactly time 0 to 1");
      }
      integral = new double[points.size()];
      for (int i = 1; i < points.size(); i++) {
         Point a = points.get(i - 1), b = points.get(i);
         if (a.time() >= b.time()) throw new IllegalArgumentException("Curve times must be strictly increasing");
         integral[i] = integral[i - 1] + (b.time() - a.time()) * (a.speed() / 2 + b.speed() / 2);
      }
      total = integral[points.size() - 1];
      if (!(total > 0) || !Double.isFinite(total)) throw new IllegalArgumentException("Curve integrated speed must be finite and positive");
   }

   public List<Point> points() { return points; }
   public Interpolation interpolation() { return interpolation; }

   public double progress(double time) {
      if (!Double.isFinite(time)) throw new IllegalArgumentException("Time must be finite");
      if (time <= 0) return 0;
      if (time >= 1) return 1;
      int i = segment(time);
      Point a = points.get(i), b = points.get(i + 1);
      double u = (time - a.time()) / (b.time() - a.time());
      double blendIntegral = interpolation == Interpolation.LINEAR ? u * u / 2 : u * u * u - u * u * u * u / 2;
      return Math.max(0, Math.min(1, (integral[i] + (b.time() - a.time()) *
         (a.speed() * u + (b.speed() - a.speed()) * blendIntegral)) / total));
   }

   /** Normalized velocity per unit scenario time; the same semantics apply to rotation. */
   public double velocity(double time) {
      if (!Double.isFinite(time)) throw new IllegalArgumentException("Time must be finite");
      int i = segment(Math.max(0, Math.min(1, time)));
      Point a = points.get(i), b = points.get(i + 1);
      double u = Math.max(0, Math.min(1, (time - a.time()) / (b.time() - a.time())));
      double blend = interpolation == Interpolation.LINEAR ? u : u * u * (3 - 2 * u);
      return (a.speed() * (1 - blend) + b.speed() * blend) / total;
   }

   private int segment(double time) {
      int lo = 0, hi = points.size() - 2;
      while (lo < hi) {
         int mid = (lo + hi + 1) >>> 1;
         if (points.get(mid).time() <= time) lo = mid; else hi = mid - 1;
      }
      return lo;
   }

   public static Map<String, SpeedCurve> builtins() {
      return Map.of("uniform", of(1, 1), "accelerate", of(0, 1), "decelerate", of(1, 0),
         "ease-in-out", new SpeedCurve(List.of(new Point(0, 0), new Point(.5, 1), new Point(1, 0)), Interpolation.SMOOTH));
   }

   private static SpeedCurve of(double start, double end) {
      return new SpeedCurve(List.of(new Point(0, start), new Point(1, end)), Interpolation.SMOOTH);
   }
}
