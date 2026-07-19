package com.cmenu.ui.api;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MenuElementSpec {
   private final String id;
   private final YamlConfiguration config;

   private MenuElementSpec(String id, YamlConfiguration config) {
      this.id = id;
      this.config = MenuElementEditor.copy(config);
   }

   public static Builder builder(String id) {
      return new Builder(id);
   }

   public String id() {
      return this.id;
   }

   public YamlConfiguration toConfiguration() {
      return MenuElementEditor.copy(this.config);
   }

   public static final class Builder {
      private final String id;
      private final YamlConfiguration config = new YamlConfiguration();

      private Builder(String id) {
         this.id = id;
      }

      public Builder text(String text) {
         this.config.set("name", text);
         return this;
      }

      public Builder type(String type) {
         this.config.set("type", type);
         return this;
      }

      public Builder position(double x, double y, double z) {
         this.config.set("x", x);
         this.config.set("y", y);
         this.config.set("z", z);
         return this;
      }

      public Builder textSize(double textSize) {
         this.config.set("text-size", textSize);
         return this;
      }

      public Builder commands(List<String> commands) {
         this.config.set("command", commands == null ? List.of() : new ArrayList<>(commands));
         return this;
      }

      public Builder permission(String permission) {
         this.config.set("permission", permission);
         return this;
      }

      public Builder nextMenu(String menuId) {
         this.config.set("next-menu.enabled", true);
         this.config.set("next-menu.menu", menuId);
         return this;
      }

      public Builder stopMenu(boolean enabled) {
         this.config.set("stop-menu.enabled", enabled);
         return this;
      }

      public Builder hoverEnlarge(boolean enabled, double scale) {
         this.config.set("hover-enlarge.enabled", enabled);
         this.config.set("hover-enlarge.scale", scale);
         return this;
      }

      public Builder set(String path, Object value) {
         this.config.set(path, value);
         return this;
      }

      public MenuElementSpec build() {
         if (!this.config.contains("name")) {
            this.config.set("name", this.id);
         }
         if (!this.config.contains("x")) {
            this.config.set("x", 0.0);
         }
         if (!this.config.contains("y")) {
            this.config.set("y", 0.0);
         }
         if (!this.config.contains("z")) {
            this.config.set("z", 3.5);
         }
         return new MenuElementSpec(this.id, this.config);
      }
   }
}
