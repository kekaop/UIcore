package org.orvencraft.uicore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.util.Vector;
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
}
