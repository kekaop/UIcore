package org.orvencraft.uicore.camera;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.orvencraft.uicore.cutscene.model.CameraPose;

/** One immutable viewpoint and orthonormal basis shared by rendering and picking. */
public final class MenuCameraFrame {
   private final CameraPose pose;
   private final Vector forward, right, up;

   public MenuCameraFrame(CameraPose pose) {
      this.pose = pose;
      double yaw = Math.toRadians(pose.yaw()), pitch = Math.toRadians(pose.pitch());
      forward = new Vector(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
      right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
      up = forward.clone().getCrossProduct(right).multiply(-1).normalize();
   }

   public CameraPose pose() { return pose; }
   public Vector forward() { return forward.clone(); }
   public Vector right() { return right.clone(); }
   public Vector up() { return up.clone(); }
   public Location origin(World world) { return new Location(world, pose.x(), pose.y(), pose.z(), (float) pose.yaw(), (float) pose.pitch()); }
   public Location localToWorld(World world, double x, double y, double z) {
      return origin(world).add(right.clone().multiply(x)).add(up.clone().multiply(y)).add(forward.clone().multiply(z));
   }
   public Vector worldToLocal(Location location) {
      Vector delta = location.toVector().subtract(new Vector(pose.x(), pose.y(), pose.z()));
      return new Vector(delta.dot(right), delta.dot(up), delta.dot(forward));
   }
}
