package org.orvencraft.uicore.section;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.api.ConfigurationCopy;
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

   public YamlConfiguration positionConfig() { return copyConfig(this.sourceConfig); }

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

   private volatile MenuLayout emptyLayout;
   public MenuLayout materializeEmpty(String menuId, String world) {
      MenuLayout cached = emptyLayout;
      if (cached != null) return cached;
      YamlConfiguration config = positionConfig();
      boolean zone = !config.getStringList("drop-accepts").isEmpty() || !config.getStringList("on-drop").isEmpty();
      config.set("type", zone ? "text" : "group");
      config.set("name", "");
      config.set("pointer-events", false);
      config.set("drag", null);
      config.set("slider", null);
      if (zone && !config.contains("hitbox")) {
         config.set("hitbox.width", .7); config.set("hitbox.height", .7); config.set("hitbox.depth", .25);
      }
      emptyLayout = MenuConfigLoader.loadLayout(menuId, id, config, world);
      return emptyLayout;
   }

   public MenuLayout materialize(String menuId, String templateId, MenuLayout template, String defaultWorld) {
      YamlConfiguration config = materializedConfig(template);
      config.set("runtime-slot-id", this.id);
      config.set("runtime-template-id", templateId);
      return MenuConfigLoader.loadLayout(menuId, this.id, config, defaultWorld);
   }

   YamlConfiguration materializedConfig(MenuLayout template) {
      var model = template.sourceModel().with("x", rawX).with("y", rawY).with("z", rawZ);
      if (!model.contains("name")) model = model.with("name", template.name);
      var slot = ConfigurationCopy.model(this.sourceConfig);
      for (String key : java.util.List.of("parent", "drag", "slider", "drop-accepts", "on-drop", "interaction-priority", "pointer-events")) {
         model = model.with(key, slot.get(key));
      }
      if (slot.contains("hitbox")) model = model.with("hitbox", slot.get("hitbox"));
      YamlConfiguration config = ConfigurationCopy.configuration(model.properties());
      for (Map.Entry<String, Object> entry : this.sourceConfig.getValues(true).entrySet()) {
         if (entry.getKey().startsWith("position-update.") && !(entry.getValue() instanceof ConfigurationSection)) {
            config.set(entry.getKey(), ConfigurationCopy.mutableValue(entry.getValue()));
         }
      }
      return config;
   }

   private static double numericValue(Object value) {
      return value instanceof Number number ? number.doubleValue() : 0.0;
   }

   private static YamlConfiguration copyConfig(ConfigurationSection section) {
      return ConfigurationCopy.configuration(section);
   }
}
