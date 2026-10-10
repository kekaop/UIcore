package org.orvencraft.uicore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.entity.Pig;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.orvencraft.uicore.camera.CameraEdgePan;
import org.orvencraft.uicore.camera.MenuCameraGeometry;
import org.junit.jupiter.api.Test;

class TextDisplayHitBoxTest {
   @Test
   void textHitboxHeightExtendsUpFromDisplayOrigin() {
      assertTrue(TextDisplayHitBox.isWithinHeight(0.7, 0.8, 0.05, false));
      assertFalse(TextDisplayHitBox.isWithinHeight(-0.2, 0.8, 0.05, false));
      assertTrue(TextDisplayHitBox.isWithinHeight(0.0, 0.8, 0.05, false));
   }

   @Test
   void itemHitboxRemainsCenteredOnItemOrigin() {
      assertTrue(TextDisplayHitBox.isWithinHeight(-0.3, 0.8, 0.05, true));
      assertFalse(TextDisplayHitBox.isWithinHeight(0.7, 0.8, 0.05, true));
   }

   @Test
   void projectsVisualCursorRayOntoDeeperMenuPlane() {
      Vector projected = TextDisplayHitBox.projectToPlane(
         new Vector(0.0, 0.0, 0.0),
         new Vector(0.45, 0.3, 1.5),
         new Vector(0.0, 0.0, 3.5),
         new Vector(0.0, 0.0, 1.0)
      );

      assertEquals(1.05, projected.getX(), 1.0E-9);
      assertEquals(0.7, projected.getY(), 1.0E-9);
      assertEquals(3.5, projected.getZ(), 1.0E-9);
   }

   @Test
   void rejectsParallelCursorRay() {
      assertNull(
         TextDisplayHitBox.projectToPlane(
            new Vector(0.0, 0.0, 0.0),
            new Vector(1.0, 0.0, 0.0),
            new Vector(0.0, 0.0, 3.5),
            new Vector(0.0, 0.0, 1.0)
         )
      );
   }

   @Test
   void mountedCameraEyesAlignCursorWithTextAtDifferentDepthsAndOffsets() {
      Location eye = new Location(null, 0, 0.765, 0);
      Pig camera = camera(eye);
      Vector right = new Vector(-1, 0, 0);
      Vector up = new Vector(0, 1, 0);
      Vector forward = new Vector(0, 0, 1);
      for (CameraEdgePan.Offset pan : new CameraEdgePan.Offset[]{
         CameraEdgePan.Offset.ZERO, new CameraEdgePan.Offset(0.1, -0.06)}) {
         Location origin = MenuCameraGeometry.viewOrigin(camera, pan);
         for (double depth : new double[]{1.5, 3.5, 5.0}) {
            Location anchor = new Location(null, -0.6, 0.25, depth);
            TextDisplay display = textDisplay(anchor);
            // A scale-0.42 text button with height 0.25 extends upward from its anchor.
            Vector visibleCenter = anchor.toVector().add(new Vector(0, 0.25 * 0.42 / 2, 0));
            for (double cursorDepth : new double[]{1.5, 2.5}) {
               Vector cursor = pointOnRay(origin, visibleCenter, cursorDepth / depth);
               Vector projected = TextDisplayHitBox.projectToPlane(origin.toVector(), cursor, anchor.toVector(), forward);
               assertTrue(TextDisplayHitBox.isInsideMenuPlane(display, location(projected), right, up, forward),
                  "depth=" + depth + " cursorDepth=" + cursorDepth + " pan=" + pan);
            }
         }
      }
   }

   @Test
   void cursorBelowTextNoLongerHitsBecauseProjectionStartsAtCameraEyes() {
      Location eye = new Location(null, 0, 0.765, 0);
      Location origin = MenuCameraGeometry.viewOrigin(camera(eye), CameraEdgePan.Offset.ZERO);
      Location anchor = new Location(null, 0, 0.25, 3.5);
      TextDisplay display = textDisplay(anchor);
      Vector forward = new Vector(0, 0, 1);
      Vector right = new Vector(-1, 0, 0);
      Vector up = new Vector(0, 1, 0);
      Vector center = anchor.toVector().add(new Vector(0, 0.25 * 0.42 / 2, 0));
      Vector centeredCursor = pointOnRay(origin, center, 2.5 / 3.5);
      Vector oldCenter = TextDisplayHitBox.projectToPlane(new Vector(), centeredCursor, anchor.toVector(), forward);
      assertFalse(TextDisplayHitBox.isInsideMenuPlane(display, location(oldCenter), right, up, forward),
         "The old ray misses even when the visual cursor is over the text");
      Vector fixedCenter = TextDisplayHitBox.projectToPlane(origin.toVector(), centeredCursor, anchor.toVector(), forward);
      assertTrue(TextDisplayHitBox.isInsideMenuPlane(display, location(fixedCenter), right, up, forward));
      double previousShift = eye.getY() * (3.5 / 2.5 - 1);
      Vector below = anchor.toVector().add(new Vector(0, 0.25 * 0.42 / 2 - previousShift, 0));
      Vector cursor = pointOnRay(origin, below, 2.5 / 3.5);

      Vector oldProjection = TextDisplayHitBox.projectToPlane(new Vector(), cursor, anchor.toVector(), forward);
      assertTrue(TextDisplayHitBox.isInsideMenuPlane(display, location(oldProjection), right, up, forward),
         "Reproduce the old hit below the visible text with a ray from the entity's feet");
      Vector corrected = TextDisplayHitBox.projectToPlane(origin.toVector(), cursor, anchor.toVector(), forward);
      assertFalse(TextDisplayHitBox.isInsideMenuPlane(display, location(corrected), right, up, forward));
   }

   private static Vector pointOnRay(Location origin, Vector target, double fraction) {
      return target.clone().subtract(origin.toVector()).multiply(fraction).add(origin.toVector());
   }

   private static Location location(Vector point) {
      assertNotNull(point);
      return new Location(null, point.getX(), point.getY(), point.getZ());
   }

   private static Pig camera(Location eye) {
      return (Pig)java.lang.reflect.Proxy.newProxyInstance(Pig.class.getClassLoader(), new Class<?>[]{Pig.class},
         (proxy, method, args) -> {
            if (method.getName().equals("getEyeLocation")) return eye.clone();
            throw new UnsupportedOperationException(method.getName());
         });
   }

   private static TextDisplay textDisplay(Location anchor) {
      return (TextDisplay)java.lang.reflect.Proxy.newProxyInstance(TextDisplay.class.getClassLoader(), new Class<?>[]{TextDisplay.class},
         (proxy, method, args) -> switch (method.getName()) {
            case "isValid" -> true;
            case "getLocation" -> anchor.clone();
            case "getTransformation" -> new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.42F), new Quaternionf());
            case "getScoreboardTags" -> java.util.Set.of("uicore_hitbox:1.35:0.25:0.25");
            case "getText" -> "[ Save name ]";
            default -> throw new UnsupportedOperationException(method.getName());
         });
   }
}
