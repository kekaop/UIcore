package org.orvencraft.uicore.camera;

import static org.junit.jupiter.api.Assertions.*;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class MenuCameraGeometryTest {
   // Vanilla 26.1.2: pig passenger attachment 0.86875, player vehicle attachment 0.6.
   private static BoundingBox riderBounds(Location seat) {
      double feet = seat.getY() + 0.86875 - 0.6;
      return new BoundingBox(seat.getX() - 0.3, feet, seat.getZ() - 0.3,
         seat.getX() + 0.3, feet + 1.8, seat.getZ() + 0.3);
   }

   @Test
   void viewOriginIncludesEyeHeightAndClientPanAtEveryCameraOrientation() {
      for (float yaw : new float[]{-180, -90, 0, 45, 90}) {
         for (float pitch : new float[]{-90, -45, 0, 45, 90}) {
            Location eye = new Location(null, 82.56, 96.765, -355.93, yaw, pitch);
            org.bukkit.entity.Pig camera = (org.bukkit.entity.Pig)java.lang.reflect.Proxy.newProxyInstance(
               org.bukkit.entity.Pig.class.getClassLoader(), new Class<?>[]{org.bukkit.entity.Pig.class},
               (proxy, method, args) -> {
                  if (method.getName().equals("getEyeLocation")) return eye.clone();
                  throw new UnsupportedOperationException(method.getName());
               });
            CameraEdgePan.Offset pan = new CameraEdgePan.Offset(0.1, -0.06);
            Vector right = new Vector(-Math.cos(Math.toRadians(yaw)), 0, -Math.sin(Math.toRadians(yaw)));
            Vector up = eye.getDirection().getCrossProduct(right).multiply(-1).normalize();
            Vector expected = eye.toVector().add(right.multiply(pan.right())).add(up.multiply(pan.up()));
            Location actual = MenuCameraGeometry.viewOrigin(camera, pan);
            assertEquals(0, expected.distance(actual.toVector()), 1.0E-9);
            assertEquals(96.765, eye.getY(), "Do not change the entity's eye location");
            assertEquals(yaw, actual.getYaw());
            assertEquals(pitch, actual.getPitch());
         }
      }
   }

   @Test
   void separateSeatRemovesPlayerFromCameraRayAtEveryOrientation() {
      for (float yaw : new float[]{-180, -135, -90, -45, 0, 45, 90, 135}) {
         for (float pitch : new float[]{-90, -60, 0, 60, 90}) {
            Location camera = new Location(null, 82.56, 96, -355.93, yaw, pitch);
            Location eye = camera.clone().add(0, 0.765, 0);
            assertTrue(riderBounds(camera).contains(eye.toVector()), "Original shared camera/seat intercepts clicks");
            BoundingBox relocated = riderBounds(MenuCameraGeometry.seatLocation(camera));
            assertFalse(relocated.contains(eye.toVector()));
            assertNull(relocated.rayTrace(eye.toVector(), eye.getDirection(), 4.5), "yaw=" + yaw + " pitch=" + pitch);
            assertEquals(96, camera.getY(), "Geometry must not mutate the menu camera");
         }
      }
   }

}
