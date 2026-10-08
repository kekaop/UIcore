package org.orvencraft.uicore.compat;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.TextDisplay;

public class CraftEngineCompat {
   private static boolean available = false;
   private static Object craftEngineMiniMessage;

   public static void init() {
      available = false;
      craftEngineMiniMessage = null;
      try {
         if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null) {
            return;
         }

         Object imageTag = Class.forName("net.momirealms.craftengine.core.plugin.text.minimessage.ImageTag").getField("INSTANCE").get(null);
         Object shiftTag = Class.forName("net.momirealms.craftengine.core.plugin.text.minimessage.ShiftTag").getField("INSTANCE").get(null);
         Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
         Object builder = miniMessageClass.getMethod("builder").invoke(null);
         Class<?> tagResolverClass = Class.forName("net.kyori.adventure.text.minimessage.tag.resolver.TagResolver");
         Class<?> builderClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage$Builder");
         Method editTagsMethod = builderClass.getMethod("editTags", Consumer.class);
         Class<?> resolverArrayClass = Array.newInstance(tagResolverClass, 0).getClass();
         Method resolverMethod = tagResolverClass.getMethod("resolver", resolverArrayClass);
         Object resolvers = Array.newInstance(tagResolverClass, 2);
         Array.set(resolvers, 0, imageTag);
         Array.set(resolvers, 1, shiftTag);
         Object combinedResolver = resolverMethod.invoke(null, resolvers);
         Method resolverConfigMethod = builderClass.getMethod("resolver", tagResolverClass);
         if (editTagsMethod != null) {
            Consumer<Object> tagConsumer = builderConfig -> {
               try {
                  resolverConfigMethod.invoke(builderConfig, combinedResolver);
               } catch (ReflectiveOperationException ignored) {
               }
            };
            editTagsMethod.invoke(builder, tagConsumer);
         }

         craftEngineMiniMessage = builderClass.getMethod("build").invoke(builder);
         available = true;
         Bukkit.getLogger().info("[UIcore] CraftEngine detected, enabling CraftEngine text format support.");
      } catch (ReflectiveOperationException | LinkageError var12) {
         available = false;
         Bukkit.getLogger().info("[UIcore] CraftEngine not found or incompatible, CraftEngine text format disabled.");
      }
   }

   public static boolean isAvailable() {
      return available;
   }

   public static Component parse(String input) {
      if (available && input != null && craftEngineMiniMessage != null) {
         try {
            return (Component)MiniMessage.class.getMethod("deserialize", String.class).invoke(craftEngineMiniMessage, input);
         } catch (ReflectiveOperationException | LinkageError var6) {
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
         if (available && containsCraftEngineTags(input)) {
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
