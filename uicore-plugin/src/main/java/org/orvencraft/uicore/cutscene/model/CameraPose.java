package org.orvencraft.uicore.cutscene.model;

import java.util.Objects;

/** World coordinates describe the camera's viewpoint, rather than its carrier entity. */
public record CameraPose(String world, double x, double y, double z, double yaw, double pitch) {
   public CameraPose {
      Objects.requireNonNull(world, "world");
      if (world.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
         || !Double.isFinite(yaw) || !Double.isFinite(pitch) || Math.abs(pitch) > 90) {
         throw new IllegalArgumentException("Camera pose requires a world, finite coordinates/angles and pitch in [-90, 90]");
      }
   }

   public double distance(CameraPose other) {
      return Math.hypot(Math.hypot(x - other.x, z - other.z), y - other.y);
   }

   public CameraPose withRotation(double nextYaw, double nextPitch) {
      return new CameraPose(world, x, y, z, nextYaw, Math.max(-90, Math.min(90, nextPitch)));
   }
}
