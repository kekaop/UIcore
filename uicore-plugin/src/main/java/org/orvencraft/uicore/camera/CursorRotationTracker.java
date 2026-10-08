package org.orvencraft.uicore.camera;

public final class CursorRotationTracker {
   private boolean initialized;
   private float lastRawYaw;
   private double accumulatedYaw;

   public synchronized double update(float rawYaw, float referenceYaw, double minYaw, double maxYaw) {
      double lower = Math.min(minYaw, maxYaw);
      double upper = Math.max(minYaw, maxYaw);
      if (!Float.isFinite(rawYaw) || !Float.isFinite(referenceYaw) || !Double.isFinite(lower) || !Double.isFinite(upper)) {
         return this.accumulatedYaw;
      }

      if (!this.initialized) {
         this.accumulatedYaw = clamp(shortestDelta(rawYaw, referenceYaw), lower, upper);
         this.initialized = true;
      } else {
         this.accumulatedYaw = clamp(this.accumulatedYaw + shortestDelta(rawYaw, this.lastRawYaw), lower, upper);
      }
      this.lastRawYaw = rawYaw;
      return this.accumulatedYaw;
   }

   /** Keep the displayed cursor angle after another gesture consumed camera rotation. */
   public synchronized void rebase(float rawYaw) {
      if (this.initialized && Float.isFinite(rawYaw)) this.lastRawYaw = rawYaw;
   }

   public static boolean rotationChanged(float fromYaw, float fromPitch, float toYaw, float toPitch) {
      if (!Float.isFinite(fromYaw) || !Float.isFinite(fromPitch) || !Float.isFinite(toYaw) || !Float.isFinite(toPitch)) {
         return false;
      }
      return Math.abs(shortestDelta(toYaw, fromYaw)) > 1.0E-4 || Math.abs(toPitch - fromPitch) > 1.0E-4;
   }

   static double shortestDelta(float current, float previous) {
      double delta = ((double) current - (double) previous) % 360.0;
      if (delta > 180.0) delta -= 360.0;
      if (delta < -180.0) delta += 360.0;
      return delta;
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }
}
