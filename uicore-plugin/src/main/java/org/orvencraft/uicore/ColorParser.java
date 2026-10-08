package org.orvencraft.uicore;

import org.orvencraft.uicore.compat.CraftEngineCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import java.util.regex.Pattern;

public final class ColorParser {
   private static final MiniMessage MINI = MiniMessage.miniMessage();
   private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("</?(?:#[0-9a-fA-F]{6}|[a-zA-Z_][a-zA-Z0-9_-]*)(?::[^>]*)?>");
   private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer
      .builder()
      .character('§')
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .build();

   public static String toLegacyString(String input) {
      if (input == null) {
         return "";
      } else {
         if (CraftEngineCompat.isAvailable() && CraftEngineCompat.containsCraftEngineTags(input)) {
            Component ceComponent = CraftEngineCompat.parse(input);
            if (ceComponent != null) {
               return LEGACY_SECTION.serialize(ceComponent);
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
         if (CraftEngineCompat.isAvailable() && CraftEngineCompat.containsCraftEngineTags(input)) {
            Component component = CraftEngineCompat.parse(input);
            if (component != null) {
               return component;
            }
         }
         if (looksLikeMiniMessage(input)) {
            return MINI.deserialize(input);
         }
         return LEGACY_SECTION.deserialize(ChatColor.translateAlternateColorCodes('&', input));
      }
   }

   public static boolean hasCraftEngineTags(String input) {
      return CraftEngineCompat.containsCraftEngineTags(input);
   }

   private static boolean looksLikeMiniMessage(String input) {
      return input != null
         && input.indexOf('<') >= 0
         && MINI_MESSAGE_TAG.matcher(input).find();
   }

   private ColorParser() {
   }
}
