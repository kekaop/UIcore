package org.orvencraft.uicore.avatar;

import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Keeps the skull facing the viewer before applying the configured tilt. */
public final class AvatarDisplayTransform {
   private AvatarDisplayTransform() {
   }

   public static Transformation create(double scale, boolean faceOnly, double tiltX, double tiltY, double tiltZ,
                                       ItemDisplayTransform itemTransform) {
      // A small nonzero depth preserves face normals and separates the skin and hat surfaces.
      Vector3f dimensions = new Vector3f((float)scale, (float)scale, (float)(scale * (faceOnly ? 0.02 : 1.0)));
      Quaternionf tilt = new Quaternionf().rotationYXZ(
         (float)Math.toRadians(tiltY), (float)Math.toRadians(tiltX), (float)Math.toRadians(tiltZ)
      );
      // Vanilla's FIXED skull transform already includes this half-turn. Flat avatars use HEAD.
      Quaternionf facing = new Quaternionf().rotationY(itemTransform == ItemDisplayTransform.FIXED ? 0.0F : (float)Math.PI);
      return new Transformation(new Vector3f(), tilt, dimensions, facing);
   }
}
