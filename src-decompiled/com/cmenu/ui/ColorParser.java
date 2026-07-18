package com.cmenu.ui;

import com.cmenu.ui.compat.CraftEngineCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorParser {
   private static final MiniMessage MINI = MiniMessage.miniMessage();
   private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
   private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

   public static String toLegacyString(String input) {
      if (input == null) {
         return "";
      } else {
         if (CraftEngineCompat.isAvailable() && CraftEngineCompat.containsCraftEngineTags(input)) {
            String ceLegacy = CraftEngineCompat.parseToLegacy(input);
            if (ceLegacy != null) {
               return ceLegacy;
            }
         }

         Component component = parse(input);
         return LEGACY_SECTION.serialize(component);
      }
   }

   public static Component parse(String input) {
      if (input == null) {
         return Component.empty();
      } else {
         return (Component)(input.indexOf(60) >= 0 ? MINI.deserialize(input) : LEGACY_AMPERSAND.deserialize(input));
      }
   }

   public static boolean hasCraftEngineTags(String input) {
      return CraftEngineCompat.containsCraftEngineTags(input);
   }

   private ColorParser() {
   }
}
