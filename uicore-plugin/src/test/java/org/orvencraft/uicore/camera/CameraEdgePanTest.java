package org.orvencraft.uicore.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraEdgePanTest {
   @Test
   void staysCenteredInsideDeadZone() {
      CameraEdgePan.Offset target = CameraEdgePan.target(0.5, -1.0, -2.0, 2.0, -3.0, 3.0, 0.75, 0.1, 0.06);

      assertEquals(CameraEdgePan.Offset.ZERO, target);
   }

   @Test
   void reachesConfiguredOffsetsAtEdges() {
      CameraEdgePan.Offset rightBottom = CameraEdgePan.target(2.0, 3.0, -2.0, 2.0, -3.0, 3.0, 0.75, 0.1, 0.06);
      CameraEdgePan.Offset leftTop = CameraEdgePan.target(-2.0, -3.0, -2.0, 2.0, -3.0, 3.0, 0.75, 0.1, 0.06);

      assertEquals(0.1, rightBottom.right(), 1.0E-9);
      assertEquals(-0.06, rightBottom.up(), 1.0E-9);
      assertEquals(-0.1, leftTop.right(), 1.0E-9);
      assertEquals(0.06, leftTop.up(), 1.0E-9);
   }

   @Test
   void interpolatesWithoutSnapping() {
      CameraEdgePan.Offset current = CameraEdgePan.interpolate(CameraEdgePan.Offset.ZERO, new CameraEdgePan.Offset(0.1, -0.06), 0.08);

      assertTrue(current.right() > 0.0 && current.right() < 0.1);
      assertTrue(current.up() < 0.0 && current.up() > -0.06);
      assertEquals(0.008, current.right(), 1.0E-9);
   }
}
