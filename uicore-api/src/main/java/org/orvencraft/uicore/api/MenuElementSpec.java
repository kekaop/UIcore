package org.orvencraft.uicore.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.orvencraft.uicore.api.model.UiElementModel;

public final class MenuElementSpec {
   private final String id;
   private final UiElementModel model;

   private MenuElementSpec(String id, UiElementModel model) {
      this.id = id;
      this.model = model;
   }

   public static Builder builder(String id) {
      return new Builder(id);
   }

   public String id() {
      return this.id;
   }

   public YamlConfiguration toConfiguration() {
      return ConfigurationCopy.configuration(this.model.properties());
   }

   public UiElementModel model() { return this.model; }

   public static final class Builder {
      private final String id;
      private UiElementModel model = UiElementModel.empty();

      private Builder(String id) {
         this.id = id;
      }

      public Builder text(String text) {
         this.model = this.model.with("name", text);
         return this;
      }

      public Builder type(String type) {
         this.model = this.model.with("type", type);
         return this;
      }

      public Builder position(double x, double y, double z) {
         this.model = this.model.with("x", x).with("y", y).with("z", z);
         return this;
      }

      public Builder textSize(double textSize) {
         this.model = this.model.with("text-size", textSize);
         return this;
      }

      public Builder commands(List<String> commands) {
         this.model = this.model.with("command", commands == null ? List.of() : new ArrayList<>(commands));
         return this;
      }

      public Builder action(String id, Map<String, ?> arguments) {
         this.model = this.model.with("action.type", id);
         if (arguments != null) arguments.forEach((key, value) -> {
            if ("type".equals(key)) throw new IllegalArgumentException("Action argument cannot be named type");
            this.model = this.model.with("action." + key, value);
         });
         return this;
      }

      public Builder visibleIfCustom(String id, Map<String, ?> arguments) {
         this.model = this.model.with("visible-if.custom.type", id);
         if (arguments != null) arguments.forEach((key, value) -> {
            if ("type".equals(key)) throw new IllegalArgumentException("Condition argument cannot be named type");
            this.model = this.model.with("visible-if.custom." + key, value);
         });
         return this;
      }

      public Builder permission(String permission) {
         this.model = this.model.with("permission", permission);
         return this;
      }

      public Builder nextMenu(String menuId) {
         this.model = this.model.with("next-menu.enabled", true).with("next-menu.menu", menuId);
         return this;
      }

      public Builder stopMenu(boolean enabled) {
         this.model = this.model.with("stop-menu.enabled", enabled);
         return this;
      }

      public Builder hoverEnlarge(boolean enabled, double scale) {
         this.model = this.model.with("hover-enlarge.enabled", enabled).with("hover-enlarge.scale", scale);
         return this;
      }

      public Builder set(String path, Object value) {
         this.model = this.model.with(path, value);
         return this;
      }

      public MenuElementSpec build() {
         if (!this.model.contains("name")) {
            this.model = this.model.with("name", this.id);
         }
         if (!this.model.contains("x")) {
            this.model = this.model.with("x", 0.0);
         }
         if (!this.model.contains("y")) {
            this.model = this.model.with("y", 0.0);
         }
         if (!this.model.contains("z")) {
            this.model = this.model.with("z", 3.5);
         }
         return new MenuElementSpec(this.id, this.model);
      }
   }
}
