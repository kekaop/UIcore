package org.orvencraft.uicore.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class AvatarDisplayTransformTest {
   @Test
   void flatAvatarPreservesFaceSizeAndHasNonzeroDepthEvenWhenTilted() {
      Transformation transform = AvatarDisplayTransform.create(0.9, true, 25, 40, 15, ItemDisplayTransform.HEAD);
      Matrix4f matrix = matrix(transform);

      assertEquals(0.9F, matrix.transformDirection(new Vector3f(1, 0, 0)).length(), 0.00001F);
      assertEquals(0.9F, matrix.transformDirection(new Vector3f(0, 1, 0)).length(), 0.00001F);
      assertEquals(0.018F, matrix.transformDirection(new Vector3f(0, 0, 1)).length(), 0.00001F);
      assertTrue(matrix.determinant() > 0, "Do not collapse the geometry or reverse face winding");
   }

   @Test
   void correctsHeadFacingWithoutInvertingItsVerticalAxis() {
      Matrix4f matrix = matrix(AvatarDisplayTransform.create(1, true, 0, 0, 0, ItemDisplayTransform.HEAD));
      Vector3f face = matrix.transformDirection(new Vector3f(0, 0, -1)).normalize();
      assertEquals(0, face.x, 0.00001F);
      assertEquals(0, face.y, 0.00001F);
      assertEquals(1, face.z, 0.00001F);
      assertEquals(1, matrix.transformDirection(new Vector3f(0, 1, 0)).y, 0.00001F);
   }

   @Test
   void fullHeadKeepsItsDepthAndDoesNotDoubleRotateFixedTransform() {
      Matrix4f matrix = matrix(AvatarDisplayTransform.create(2, false, 0, 0, 0, ItemDisplayTransform.FIXED));
      Vector3f forward = matrix.transformDirection(new Vector3f(0, 0, -1));
      assertEquals(-2, forward.z, 0.00001F);
      assertEquals(2, matrix.transformDirection(new Vector3f(1, 0, 0)).x, 0.00001F);
      assertEquals(2, matrix.transformDirection(new Vector3f(0, 1, 0)).y, 0.00001F);
   }

   private static Matrix4f matrix(Transformation transform) {
      return new Matrix4f().translate(transform.getTranslation()).rotate(transform.getLeftRotation())
         .scale(transform.getScale()).rotate(transform.getRightRotation());
   }
}
