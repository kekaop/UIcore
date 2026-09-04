package org.orvencraft.uicore.camera;

public final class CameraEdgePan {
   private CameraEdgePan() {
   }

   public static Offset target(
      double cursorX,
      double cursorY,
      double minX,
      double maxX,
      double minY,
      double maxY,
      double threshold,
      double maxOffsetX,
      double maxOffsetY
   ) {
      double normalizedX = normalize(cursorX, minX, maxX);
      double normalizedY = normalize(cursorY, minY, maxY);
      double safeThreshold = clamp(threshold, 0.0, 0.99);
      double right = edgeAmount(normalizedX, safeThreshold) * Math.max(0.0, maxOffsetX);
      double up = -edgeAmount(normalizedY, safeThreshold) * Math.max(0.0, maxOffsetY);
      if (right == 0.0) {
         right = 0.0;
      }
      if (up == 0.0) {
         up = 0.0;
      }
      return new Offset(right, up);
   }

   public static Offset interpolate(Offset current, Offset target, double smoothing) {
      Offset start = current == null ? Offset.ZERO : current;
      Offset end = target == null ? Offset.ZERO : target;
      double amount = clamp(smoothing, 0.01, 1.0);
      double right = start.right() + (end.right() - start.right()) * amount;
      double up = start.up() + (end.up() - start.up()) * amount;
      if (Math.abs(right) < 1.0E-5) {
         right = 0.0;
      }
      if (Math.abs(up) < 1.0E-5) {
         up = 0.0;
      }
      return new Offset(right, up);
   }

   private static double normalize(double value, double min, double max) {
      if (!Double.isFinite(value) || !Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
         return 0.0;
      }

      double center = (min + max) * 0.5;
      double range = value >= center ? max - center : center - min;
      return range <= 1.0E-9 ? 0.0 : clamp((value - center) / range, -1.0, 1.0);
   }

   private static double edgeAmount(double normalized, double threshold) {
      double absolute = Math.abs(normalized);
      if (absolute <= threshold) {
         return 0.0;
      }

      double progress = clamp((absolute - threshold) / (1.0 - threshold), 0.0, 1.0);
      double eased = progress * progress * (3.0 - 2.0 * progress);
      return Math.copySign(eased, normalized);
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   public record Offset(double right, double up) {
      public static final Offset ZERO = new Offset(0.0, 0.0);
   }
}
