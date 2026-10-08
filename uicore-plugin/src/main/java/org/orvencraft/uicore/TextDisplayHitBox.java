package org.orvencraft.uicore;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class TextDisplayHitBox {
   private static final float LOCAL_HEIGHT = 0.2F;
   private static final float LOCAL_WIDTH_PER_CHAR = 0.12F;
   private static final String HITBOX_TAG_PREFIX = "uicore_hitbox:";
   static final String ITEM_HITBOX_TAG = "uicore_item_hitbox";

   public static boolean isInside(TextDisplay display, Location cursor) {
      if (!display.isValid()) {
         return false;
      } else {
         String text = display.getText() != null ? display.getText() : "";
         String strippedText = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
         float[] customHitbox = readCustomHitbox(display);
         float localWidth = customHitbox != null ? customHitbox[0] : Math.max(0.1F, strippedText.length() * 0.12F);
         float localHeight = customHitbox != null ? customHitbox[1] : 0.2F;
         float localDepth = customHitbox != null ? customHitbox[2] : 0.05F;
         Transformation tr = display.getTransformation();
         Matrix4f worldMatrix = matrixFromTransformation(display, tr);
         Matrix4f invMatrix = new Matrix4f(worldMatrix).invert();
         Vector4f worldPos = new Vector4f((float)cursor.getX(), (float)cursor.getY(), (float)cursor.getZ(), 1.0F);
         Vector4f localPos = new Vector4f();
         invMatrix.transform(worldPos, localPos);
         Vector3f scale = tr.getScale();
         float avgScale = (scale.x + scale.y + scale.z) / 3.0F;
         float expansion = (float)Math.max(0.01, 0.05 * avgScale);
         float hw = localWidth / 2.0F + expansion / Math.max(0.1F, scale.x);
         float hd = localDepth / 2.0F + expansion / Math.max(0.1F, scale.z);
         boolean itemHitbox = display.getScoreboardTags().contains(ITEM_HITBOX_TAG);
         return Math.abs(localPos.x) <= hw
            && isWithinHeight(localPos.y, localHeight, expansion / Math.max(0.1F, scale.y), itemHitbox)
            && Math.abs(localPos.z) <= hd;
      }
   }

   public static Location projectToPlane(Location rayOrigin, Location cursor, Location planePoint, Vector planeNormal) {
      if (rayOrigin == null || cursor == null || planePoint == null || planeNormal == null) {
         return null;
      }
      if (rayOrigin.getWorld() == null
         || cursor.getWorld() == null
         || planePoint.getWorld() == null
         || !rayOrigin.getWorld().equals(cursor.getWorld())
         || !rayOrigin.getWorld().equals(planePoint.getWorld())) {
         return null;
      }

      Vector projected = projectToPlane(rayOrigin.toVector(), cursor.toVector(), planePoint.toVector(), planeNormal);
      return projected == null ? null : new Location(rayOrigin.getWorld(), projected.getX(), projected.getY(), projected.getZ());
   }

   public static Vector projectToPlane(Vector rayOrigin, Vector cursor, Vector planePoint, Vector planeNormal) {
      if (rayOrigin == null || cursor == null || planePoint == null || planeNormal == null) {
         return null;
      }

      Vector direction = cursor.clone().subtract(rayOrigin);
      Vector normal = planeNormal.clone();
      if (direction.lengthSquared() < 1.0E-12 || normal.lengthSquared() < 1.0E-12) {
         return null;
      }
      normal.normalize();
      double denominator = direction.dot(normal);
      if (Math.abs(denominator) < 1.0E-9) {
         return null;
      }

      double distance = planePoint.clone().subtract(rayOrigin).dot(normal) / denominator;
      if (distance <= 0.0) {
         return null;
      }
      return rayOrigin.clone().add(direction.multiply(distance));
   }

   public static boolean isInsideMenuPlane(TextDisplay display, Location point, Vector right, Vector up, Vector normal) {
      if (display == null || point == null || !display.isValid()) {
         return false;
      }
      if (right == null || up == null || normal == null
         || right.lengthSquared() < 1.0E-12
         || up.lengthSquared() < 1.0E-12
         || normal.lengthSquared() < 1.0E-12) {
         return isInside(display, point);
      }

      float[] customHitbox = readCustomHitbox(display);
      String text = display.getText() != null ? display.getText() : "";
      String strippedText = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
      float localWidth = customHitbox != null ? customHitbox[0] : Math.max(0.1F, strippedText.length() * LOCAL_WIDTH_PER_CHAR);
      float localHeight = customHitbox != null ? customHitbox[1] : LOCAL_HEIGHT;
      float localDepth = customHitbox != null ? customHitbox[2] : 0.05F;
      Vector3f scale = display.getTransformation().getScale();
      float averageScale = (Math.abs(scale.x) + Math.abs(scale.y) + Math.abs(scale.z)) / 3.0F;
      double expansion = Math.max(0.01, 0.05 * averageScale);
      double halfWidth = localWidth * Math.max(0.01F, Math.abs(scale.x)) / 2.0 + expansion;
      double halfDepth = localDepth * Math.max(0.01F, Math.abs(scale.z)) / 2.0 + expansion;

      Vector delta = point.toVector().subtract(display.getLocation().toVector());
      double localX = delta.dot(right.clone().normalize());
      double localY = delta.dot(up.clone().normalize());
      double localZ = delta.dot(normal.clone().normalize());
      boolean itemHitbox = display.getScoreboardTags().contains(ITEM_HITBOX_TAG);
      return Math.abs(localX) <= halfWidth
         && isWithinHeight(localY, localHeight * Math.max(0.01F, Math.abs(scale.y)), expansion, itemHitbox)
         && Math.abs(localZ) <= halfDepth;
   }

   static boolean isWithinHeight(double y, double height, double expansion, boolean centered) {
      return centered
         ? Math.abs(y) <= height / 2.0 + expansion
         : y >= -expansion && y <= height + expansion;
   }

   private static float[] readCustomHitbox(TextDisplay display) {
      for (String tag : display.getScoreboardTags()) {
         if (tag.startsWith(HITBOX_TAG_PREFIX)) {
            String[] parts = tag.substring(HITBOX_TAG_PREFIX.length()).split(":");
            if (parts.length == 3) {
               try {
                  return new float[]{
                     Math.max(0.05F, Float.parseFloat(parts[0])),
                     Math.max(0.05F, Float.parseFloat(parts[1])),
                     Math.max(0.05F, Float.parseFloat(parts[2]))
                  };
               } catch (NumberFormatException var5) {
                  return null;
               }
            }
         }
      }

      return null;
   }

   private static Matrix4f matrixFromTransformation(TextDisplay td, Transformation tr) {
      Vector3f translation = new Vector3f(tr.getTranslation());
      Quaternionf leftRot = tr.getLeftRotation();
      Vector3f scale = tr.getScale();
      Location location = td.getLocation();
      translation.add((float)location.getX(), (float)location.getY(), (float)location.getZ());
      return new Matrix4f().translate(translation).rotate(leftRot).scale(scale);
   }

   private TextDisplayHitBox() {
   }
}
