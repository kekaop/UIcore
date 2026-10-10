package org.orvencraft.uicore.camera;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public final class MenuCameraGeometry {
   private MenuCameraGeometry() {}

   /** Keep the viewer's bounding box behind the camera, including at vertical camera pitches. */
   public static Location seatLocation(Location cameraBase) {
      double yaw = Math.toRadians(cameraBase.getYaw());
      return cameraBase.clone().add(new Vector(Math.sin(yaw), 0.0, -Math.cos(yaw)));
   }

   /** Match the client view: the camera entity's eyes plus its client-only edge pan. */
   public static Location viewOrigin(LivingEntity camera, CameraEdgePan.Offset pan) {
      Location eye = camera.getEyeLocation();
      double yaw = Math.toRadians(eye.getYaw());
      Vector right = new Vector(-Math.cos(yaw), 0.0, -Math.sin(yaw));
      Vector up = eye.getDirection().getCrossProduct(right).multiply(-1.0).normalize();
      return eye.add(right.multiply(pan.right())).add(up.multiply(pan.up()));
   }

   public static Location captureBlock(Location cameraEye, CameraEdgePan.Offset pan) {
      Vector forward = cameraEye.getDirection().normalize();
      double yaw = Math.toRadians(cameraEye.getYaw());
      Vector right = new Vector(-Math.cos(yaw), 0.0, -Math.sin(yaw));
      Vector up = forward.getCrossProduct(right).multiply(-1.0).normalize();
      Location target = cameraEye.clone().add(right.multiply(pan.right())).add(up.multiply(pan.up())).add(forward);
      return new Location(target.getWorld(), target.getBlockX(), target.getBlockY(), target.getBlockZ());
   }
}
