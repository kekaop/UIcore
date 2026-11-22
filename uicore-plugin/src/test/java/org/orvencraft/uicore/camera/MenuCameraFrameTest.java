package org.orvencraft.uicore.camera;

import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import static org.junit.jupiter.api.Assertions.*;

class MenuCameraFrameTest {
   @Test void localCoordinatesSurviveCombinedRotationTranslationAndVerticalPitch() {
      for (double pitch : new double[]{-90, -38, 0, 61, 90}) {
         var frame = new MenuCameraFrame(new CameraPose("world", 1234, 80, -42, 179, pitch));
         var point = frame.localToWorld(null, 1.2, -0.7, 2.5);
         var local = frame.worldToLocal(point);
         assertEquals(1.2, local.getX(), 1e-10);
         assertEquals(-0.7, local.getY(), 1e-10);
         assertEquals(2.5, local.getZ(), 1e-10);
         assertEquals(0, frame.forward().dot(frame.right()), 1e-10);
      }
   }
   @Test void cameraFramesAndRebasesDoNotMoveCursor() {
      var input = new MenuCursorInput(179, 30, .4, -.2);
      input.update(-179, 31, .1, .1, -1, 1, -1, 1, 1);
      assertEquals(.6, input.x(), 1e-6);
      assertEquals(-.1, input.y(), 1e-6);
      input.rebase(42, -80);
      for (int tick = 0; tick < 80; tick++) {
         new MenuCameraFrame(new CameraPose("world", tick, 80, -tick, tick * 4, tick));
         input.update(42, -80, .1, .1, -1, 1, -1, 1, 1);
      }
      assertEquals(.6, input.x(), 1e-6);
      assertEquals(-.1, input.y(), 1e-6);
   }

   @Test void displayedButtonAndCursorPickUseTheSameFrameAcrossWorldTranslationAndAngleWrap() {
      var display = (org.bukkit.entity.TextDisplay) java.lang.reflect.Proxy.newProxyInstance(
         getClass().getClassLoader(), new Class<?>[]{org.bukkit.entity.TextDisplay.class}, (proxy, method, arguments) -> switch (method.getName()) {
            case "getText" -> "Button";
            case "getScoreboardTags" -> java.util.Set.of("uicore_hitbox:1.2:0.4:0.1", "uicore_item_hitbox");
            case "getTransformation" -> new org.bukkit.util.Transformation(new org.joml.Vector3f(), new org.joml.Quaternionf(), new org.joml.Vector3f(1), new org.joml.Quaternionf());
            default -> throw new AssertionError("Picking must use cached geometry, requested " + method.getName());
         });
      var geometry = org.orvencraft.uicore.TextDisplayHitBox.snapshot(display);
      for (double yaw : new double[]{179, 181, 359, 361}) for (double pitch : new double[]{-90, -45, 0, 45, 90}) {
         var frame = new MenuCameraFrame(new CameraPose("world", 29_000_000, 180, -29_000_000, yaw, pitch));
         var button = frame.localToWorld(null, .7, -.2, 3.5);
         var cursor = frame.localToWorld(null, .8, -.1, 1.5);
         var intersection = org.orvencraft.uicore.TextDisplayHitBox.projectToPlane(
            frame.origin(null).toVector(), cursor.toVector(), button.toVector(), frame.forward());
         // A cursor plane can have a different depth; pick the ray-aligned visible button center.
         var centeredCursor = frame.localToWorld(null, .7 * 1.5/3.5, -.2 * 1.5/3.5, 1.5);
         intersection = org.orvencraft.uicore.TextDisplayHitBox.projectToPlane(frame.origin(null).toVector(), centeredCursor.toVector(), button.toVector(), frame.forward());
         var delta = intersection.subtract(button.toVector());
         assertTrue(geometry.contains(new org.bukkit.util.Vector(delta.dot(frame.right()), delta.dot(frame.up()), delta.dot(frame.forward()))));
         assertFalse(geometry.contains(new org.bukkit.util.Vector(1, 0, 0)));
      }
   }
}
