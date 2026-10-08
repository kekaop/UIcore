package org.orvencraft.uicore.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.orvencraft.uicore.api.model.UiElementModel;

public final class MenuElementEditor {
   private UiElementModel model;

   public MenuElementEditor(ConfigurationSection source) {
      this.model = ConfigurationCopy.model(source);
   }

   public MenuElementEditor text(String text) {
      this.model = this.model.with("name", text);
      return this;
   }

   public MenuElementEditor type(String type) {
      this.model = this.model.with("type", type);
      return this;
   }

   public MenuElementEditor position(double x, double y, double z) {
      this.model = this.model.with("x", x).with("y", y).with("z", z);
      return this;
   }

   public MenuElementEditor x(double x) {
      this.model = this.model.with("x", x);
      return this;
   }

   public MenuElementEditor y(double y) {
      this.model = this.model.with("y", y);
      return this;
   }

   public MenuElementEditor z(double z) {
      this.model = this.model.with("z", z);
      return this;
   }

   public MenuElementEditor textSize(double textSize) {
      this.model = this.model.with("text-size", textSize);
      return this;
   }

   public MenuElementEditor commands(List<String> commands) {
      this.model = this.model.with("command", commands == null ? List.of() : new ArrayList<>(commands));
      return this;
   }

   public MenuElementEditor action(String id, Map<String, ?> arguments) {
      this.model = this.model.with("action.type", id);
      if (arguments != null) arguments.forEach((key, value) -> {
         if ("type".equals(key)) throw new IllegalArgumentException("Action argument cannot be named type");
         this.model = this.model.with("action." + key, value);
      });
      return this;
   }

   public MenuElementEditor visibleIfCustom(String id, Map<String, ?> arguments) {
      this.model = this.model.with("visible-if.custom.type", id);
      if (arguments != null) arguments.forEach((key, value) -> {
         if ("type".equals(key)) throw new IllegalArgumentException("Condition argument cannot be named type");
         this.model = this.model.with("visible-if.custom." + key, value);
      });
      return this;
   }

   public MenuElementEditor permission(String permission) {
      this.model = this.model.with("permission", permission);
      return this;
   }

   public MenuElementEditor clearPermission() {
      this.model = this.model.with("permission", null);
      return this;
   }

   public MenuElementEditor nextMenu(String menuId) {
      this.model = this.model.with("next-menu.enabled", true).with("next-menu.menu", menuId);
      return this;
   }

   public MenuElementEditor clearNextMenu() {
      this.model = this.model.with("next-menu", null);
      return this;
   }

   public MenuElementEditor stopMenu(boolean enabled) {
      this.model = this.model.with("stop-menu.enabled", enabled);
      return this;
   }

   public MenuElementEditor hoverEnlarge(boolean enabled, double scale) {
      this.model = this.model.with("hover-enlarge.enabled", enabled).with("hover-enlarge.scale", scale);
      return this;
   }

   public MenuElementEditor set(String path, Object value) {
      this.model = this.model.with(path, value);
      return this;
   }

   public MenuElementEditor remove(String path) {
      this.model = this.model.with(path, null);
      return this;
   }

   public YamlConfiguration toConfiguration() {
      return ConfigurationCopy.configuration(this.model.properties());
   }

   public UiElementModel model() { return this.model; }

   static YamlConfiguration copy(ConfigurationSection source) {
      return ConfigurationCopy.configuration(source);
   }
}
