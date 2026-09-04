package org.orvencraft.uicore.util;

public final class TextContentUtils {
   private TextContentUtils() {
   }

   public static String normalizeLineBreaks(String text) {
      if (text == null || text.isEmpty()) {
         return text;
      }
      return text.replace("\\n", "\n").replace("/n", "\n");
   }
}
