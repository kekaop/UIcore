package org.orvencraft.uicore.section;

import org.orvencraft.uicore.layout.MenuLayout;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MenuSlot {
   private final String id;
   private final double x;
   private final double y;
   private final double z;
   private final String content;
   private final String fallback;
   private final Object rawX;
   private final Object rawY;
   private final Object rawZ;
   private final YamlConfiguration sourceConfig;

   private MenuSlot(
      String id,
      double x,
      double y,
      double z,
      String content,
      String fallback,
      Object rawX,
      Object rawY,
      Object rawZ,
      YamlConfiguration sourceConfig
   ) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
      this.content = content == null ? "" : content.trim();
      this.fallback = fallback == null ? "" : fallback.trim();
      this.rawX = rawX;
      this.rawY = rawY;
      this.rawZ = rawZ;
      this.sourceConfig = sourceConfig;
   }

   public static MenuSlot from(String id, ConfigurationSection section) {
      Object rawX = section.get("x", 0.0);
      Object rawY = section.get("y", 0.0);
      Object rawZ = section.get("z", 0.0);
      return new MenuSlot(
         id,
         numericValue(rawX),
         numericValue(rawY),
         numericValue(rawZ),
         section.getString("content", ""),
         section.getString("fallback", ""),
         rawX,
         rawY,
         rawZ,
         copyConfig(section)
      );
   }

   public String id() {
      return this.id;
   }

   public String content() {
      return this.content;
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public double z() {
      return this.z;
   }

   public String fallback() {
      return this.fallback;
   }

   public MenuLayout materialize(String menuId, String templateId, MenuLayout template, String defaultWorld) {
      YamlConfiguration config = template.copySourceConfig();
      if (!config.contains("name")) {
         config.set("name", template.name);
      }
      config.set("x", this.rawX);
      config.set("y", this.rawY);
      config.set("z", this.rawZ);
      for (Map.Entry<String, Object> entry : this.sourceConfig.getValues(true).entrySet()) {
         if (entry.getKey().startsWith("position-update.") && !(entry.getValue() instanceof ConfigurationSection)) {
            config.set(entry.getKey(), entry.getValue());
         }
      }
      config.set("runtime-slot-id", this.id);
      config.set("runtime-template-id", templateId);
      return MenuConfigLoader.loadLayout(menuId, this.id, config, defaultWorld);
   }

   private static double numericValue(Object value) {
      return value instanceof Number number ? number.doubleValue() : 0.0;
   }

   private static YamlConfiguration copyConfig(ConfigurationSection section) {
      YamlConfiguration copy = new YamlConfiguration();
      for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
         if (!(entry.getValue() instanceof ConfigurationSection)) {
            copy.set(entry.getKey(), entry.getValue());
         }
      }
      return copy;
   }
}
