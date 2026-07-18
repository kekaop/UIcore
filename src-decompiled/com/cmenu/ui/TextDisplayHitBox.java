package com.cmenu.ui;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class TextDisplayHitBox {
   private static final float LOCAL_HEIGHT = 0.2F;
   private static final float LOCAL_WIDTH_PER_CHAR = 0.12F;

   public static boolean isInside(TextDisplay display, Location cursor) {
      if (!display.isValid()) {
         return false;
      } else {
         String text = display.getText() != null ? display.getText() : "";
         String strippedText = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
         float localWidth = Math.max(0.1F, strippedText.length() * 0.12F);
         float localHeight = 0.2F;
         float localDepth = 0.05F;
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
         float hh = localHeight / 2.0F + expansion / Math.max(0.1F, scale.y);
         float hd = localDepth / 2.0F + expansion / Math.max(0.1F, scale.z);
         return Math.abs(localPos.x) <= hw && Math.abs(localPos.y) <= hh && Math.abs(localPos.z) <= hd;
      }
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
