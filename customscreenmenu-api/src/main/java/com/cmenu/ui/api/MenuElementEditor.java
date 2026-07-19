package com.cmenu.ui.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MenuElementEditor {
   private final YamlConfiguration config;

   public MenuElementEditor(ConfigurationSection source) {
      this.config = copy(source);
   }

   public MenuElementEditor text(String text) {
      this.config.set("name", text);
      return this;
   }

   public MenuElementEditor type(String type) {
      this.config.set("type", type);
      return this;
   }

   public MenuElementEditor position(double x, double y, double z) {
      this.config.set("x", x);
      this.config.set("y", y);
      this.config.set("z", z);
      return this;
   }

   public MenuElementEditor x(double x) {
      this.config.set("x", x);
      return this;
   }

   public MenuElementEditor y(double y) {
      this.config.set("y", y);
      return this;
   }

   public MenuElementEditor z(double z) {
      this.config.set("z", z);
      return this;
   }

   public MenuElementEditor textSize(double textSize) {
      this.config.set("text-size", textSize);
      return this;
   }

   public MenuElementEditor commands(List<String> commands) {
      this.config.set("command", commands == null ? List.of() : new ArrayList<>(commands));
      return this;
   }

   public MenuElementEditor permission(String permission) {
      this.config.set("permission", permission);
      return this;
   }

   public MenuElementEditor clearPermission() {
      this.config.set("permission", null);
      return this;
   }

   public MenuElementEditor nextMenu(String menuId) {
      this.config.set("next-menu.enabled", true);
      this.config.set("next-menu.menu", menuId);
      return this;
   }

   public MenuElementEditor clearNextMenu() {
      this.config.set("next-menu", null);
      return this;
   }

   public MenuElementEditor stopMenu(boolean enabled) {
      this.config.set("stop-menu.enabled", enabled);
      return this;
   }

   public MenuElementEditor hoverEnlarge(boolean enabled, double scale) {
      this.config.set("hover-enlarge.enabled", enabled);
      this.config.set("hover-enlarge.scale", scale);
      return this;
   }

   public MenuElementEditor set(String path, Object value) {
      this.config.set(path, value);
      return this;
   }

   public MenuElementEditor remove(String path) {
      this.config.set(path, null);
      return this;
   }

   public YamlConfiguration toConfiguration() {
      return copy(this.config);
   }

   static YamlConfiguration copy(ConfigurationSection source) {
      YamlConfiguration copy = new YamlConfiguration();
      if (source != null) {
         for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
               copy.set(entry.getKey(), entry.getValue());
            }
         }
      }

      return copy;
   }
}
