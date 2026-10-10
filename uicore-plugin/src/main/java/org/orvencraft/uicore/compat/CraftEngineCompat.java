package org.orvencraft.uicore.compat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.TextDisplay;

public class CraftEngineCompat {
   private static volatile MiniMessage craftEngineMiniMessage;

   public static void init() {
      craftEngineMiniMessage = null;
      var plugin = Bukkit.getPluginManager().getPlugin("CraftEngine");
      if (plugin == null) return;
      try {
         ClassLoader loader = plugin.getClass().getClassLoader();
         TagResolver imageTag = (TagResolver) Class.forName("net.momirealms.craftengine.core.plugin.text.minimessage.ImageTag", true, loader).getField("INSTANCE").get(null);
         TagResolver shiftTag = (TagResolver) Class.forName("net.momirealms.craftengine.core.plugin.text.minimessage.ShiftTag", true, loader).getField("INSTANCE").get(null);
         craftEngineMiniMessage = createMiniMessage(imageTag, shiftTag);
         Bukkit.getLogger().info("[UIcore] CraftEngine detected, enabling CraftEngine text format support.");
      } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
         Bukkit.getLogger().warning("[UIcore] CraftEngine text format disabled: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
      }
   }

   static MiniMessage createMiniMessage(TagResolver... resolvers) {
      return MiniMessage.builder().editTags(tags -> tags.resolvers(resolvers)).build();
   }

   public static boolean isAvailable() {
      return craftEngineMiniMessage != null;
   }

   public static Component parse(String input) {
      MiniMessage parser = craftEngineMiniMessage;
      if (input != null && parser != null) {
         try {
            return parser.deserialize(input);
         } catch (RuntimeException | LinkageError exception) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static String parseToLegacy(String input) {
      Component component = parse(input);
      return component == null ? null : LegacyComponentSerializer.legacySection().serialize(component);
   }

   public static boolean containsCraftEngineTags(String input) {
      return input == null ? false : input.contains("<image:") || input.contains("<shift:");
   }

   public static void applyToTextDisplay(TextDisplay display, String input) {
      if (display != null && input != null) {
         if (isAvailable() && containsCraftEngineTags(input)) {
            Component component = parse(input);
            if (component != null) {
               display.text(component);
               return;
            }
         }

         display.setText(
            LegacyComponentSerializer.legacySection()
               .serialize(
                  org.orvencraft.uicore.ColorParser.parse(input)
               )
         );
      }
   }
}
