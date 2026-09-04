package org.orvencraft.uicore.compat;

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
      try {
         if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null) {
            return;
         }

         Object imageTag = Class.forName("net.momirealms.craftengine.core.plugin.text.minimessage.ImageTag").getField("INSTANCE").get(null);
         Object shiftTag = Class.forName("net.momirealms.craftengine.core.plugin.text.minimessage.ShiftTag").getField("INSTANCE").get(null);
         Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
         Object builder = miniMessageClass.getMethod("builder").invoke(null);
         Class<?> tagResolverClass = Class.forName("net.kyori.adventure.text.minimessage.tagresolver.TagResolver");
         Class<?> builderClass = builder.getClass();
         Method editTagsMethod = null;

         for (Method m : builderClass.getMethods()) {
            if (m.getName().equals("editTags") && m.getParameterCount() == 1) {
               editTagsMethod = m;
               break;
            }
         }

         if (editTagsMethod != null) {
            Method resolverMethod = tagResolverClass.getMethod("resolver", tagResolverClass, tagResolverClass);
            Object combinedResolver = resolverMethod.invoke(null, imageTag, shiftTag);
            Consumer<Object> tagConsumer = builderConfig -> {
               try {
                  Class<?> configClass = builderConfig.getClass();
                  Method resolverConfigMethod = null;

                  for (Method mx : configClass.getMethods()) {
                     if (mx.getName().equals("resolver") && mx.getParameterCount() == 1) {
                        resolverConfigMethod = mx;
                        break;
                     }
                  }

                  if (resolverConfigMethod != null) {
                     resolverConfigMethod.invoke(builderConfig, combinedResolver);
                  }
               } catch (Exception var8) {
               }
            };
            editTagsMethod.invoke(builder, tagConsumer);
         }

         craftEngineMiniMessage = builder.getClass().getMethod("build").invoke(builder);
         available = true;
         Bukkit.getLogger().info("[UIcore] CraftEngine detected, enabling CraftEngine text format support.");
      } catch (Exception var12) {
         available = false;
         Bukkit.getLogger().info("[UIcore] CraftEngine not found or incompatible, CraftEngine text format disabled.");
      }
   }

   public static boolean isAvailable() {
      return available;
   }

   public static String parseToLegacy(String input) {
      if (available && input != null && craftEngineMiniMessage != null) {
         try {
            Class<?> miniMessageClass = craftEngineMiniMessage.getClass();
            Object component = miniMessageClass.getMethod("deserialize", String.class).invoke(craftEngineMiniMessage, input);
            Class<?> legacySerializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Object serializer = legacySerializerClass.getMethod("legacySection").invoke(null);
            Object result = serializer.getClass().getMethod("serialize", Class.forName("net.kyori.adventure.text.Component")).invoke(serializer, component);
            return (String)result;
         } catch (Exception var6) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean containsCraftEngineTags(String input) {
      return input == null ? false : input.contains("<image:") || input.contains("<shift:");
   }

   public static void applyToTextDisplay(TextDisplay display, String input) {
      if (display != null && input != null) {
         if (available && containsCraftEngineTags(input)) {
            String legacy = parseToLegacy(input);
            if (legacy != null) {
               display.setText(legacy);
               return;
            }
         }

         display.setText(
            LegacyComponentSerializer.legacySection()
               .serialize(
                  (Component)(input.indexOf(60) >= 0
                     ? MiniMessage.miniMessage().deserialize(input)
                     : LegacyComponentSerializer.legacyAmpersand().deserialize(input))
               )
         );
      }
   }
}
