package org.orvencraft.uicore.camera;

/** Accumulates only client mouse deltas. Camera animation never enters this calculation. */
public final class MenuCursorInput {
   private float rawYaw, rawPitch;
   private double x, y;
   public MenuCursorInput(float yaw, float pitch, double x, double y) {
      rawYaw = yaw; rawPitch = pitch; this.x = x; this.y = y;
   }
   public void update(float yaw, float pitch, double scaleX, double scaleY,
                      double minX, double maxX, double minY, double maxY, double smoothing) {
      if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
      double delta = ((double) yaw - rawYaw) % 360;
      if (delta > 180) delta -= 360;
      if (delta < -180) delta += 360;
      x = clamp(x + delta * scaleX * smoothing, minX, maxX);
      y = clamp(y + (pitch - rawPitch) * scaleY * smoothing, minY, maxY);
      rebase(yaw, pitch);
   }
   public void rebase(float yaw, float pitch) { rawYaw = yaw; rawPitch = pitch; }
   public double x() { return x; }
   public double y() { return y; }
   public float rawYaw() { return rawYaw; }
   public float rawPitch() { return rawPitch; }
   private static double clamp(double value, double min, double max) { return Math.max(Math.min(min, max), Math.min(Math.max(min, max), value)); }
}
