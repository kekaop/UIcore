package org.orvencraft.uicore;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.section.Section;
import java.util.Map;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class UIcorePlaceholder extends PlaceholderExpansion {
   private final UIcorePlugin plugin;

   public UIcorePlaceholder(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "uicore";
   }

   public String getAuthor() {
      return (String)this.plugin.getDescription().getAuthors().get(0);
   }

   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, String params) {
      if (offlinePlayer instanceof Player player) {
         if (params == null) return "";
         String menuKey = this.plugin.getCurrentPlayerMenu(player);
         if (menuKey == null || menuKey.isEmpty()) {
            return "";
         } else if (params.equals("current_menu")) {
            return this.getCurrentMenu(player);
         } else if (params.equals("selected_option")) {
            return this.getSelectedOption(player);
         } else if (params.equals("display_item_id")) {
            return this.getDisplayItemId(player);
         } else if (params.equals("menu_world")) {
            return this.getMenuWorld(player);
         } else if (params.startsWith("button_")) {
            String coord = params.substring("button_".length());
            return this.getButtonCoordinate(player, coord);
         } else if (params.equals("clicked_option")) {
            return this.getClickedOption(player);
         } else if (params.equals("is_attacking_or_breaking")) {
            return this.isAttackingOrBreaking(player) ? "true" : "false";
         } else if (params.equals("hovered_option")) {
            return this.getHoveredOption(player);
         } else if (params.startsWith("input_")) {
            String field = params.substring(6);
            return this.getUserInput(player, field);
         }
         return "";
      } else {
         return "";
      }
   }

   private boolean isAttackingOrBreaking(Player player) {
      return player.hasMetadata("cursor_is_attacking_or_breaking");
   }

   private String getCurrentMenu(Player player) {
      return this.plugin.getCurrentPlayerMenu(player);
   }

   private String getSelectedOption(Player player) {
      MenuLayout selected = this.plugin.getSelectedLayout(player);
      return selected != null ? selected.name : "";
   }

   private String getDisplayItemId(Player player) {
      return this.plugin.getItemDisplayManager().getPlayerActiveItemId(player);
   }

   private String getMenuWorld(Player player) {
      String menuKey = this.plugin.getCurrentPlayerMenu(player);
      if (menuKey == null) {
         return "";
      } else {
         Section section = this.plugin.getRuntimeSection(player, menuKey);
         return section != null ? section.world : "";
      }
   }

   private String getClickedOption(Player player) {
      MenuLayout selected = this.plugin.getSelectedLayout(player);
      return selected != null ? selected.key : "";
   }

   private String getButtonCoordinate(Player player, String coord) {
      MenuLayout selected = this.plugin.getSelectedLayout(player);
      if (selected == null) {
         return "";
      } else {
         switch (coord) {
            case "x":
               return String.valueOf(selected.x);
            case "y":
               return String.valueOf(selected.y);
            case "z":
               return String.valueOf(selected.z);
            default:
               return "";
         }
      }
   }

   private String getUserInput(Player player, String field) {
      Map<String, String> values = this.plugin.getPlayerInputSnapshot(player);
      String value = values == null ? null : values.get(field);
      if (value == null) {
         Map<String, String> fallback = Commands.getUserInputData().get(player.getUniqueId());
         value = fallback == null ? null : fallback.get(field);
      }
      if (value == null) {
         return "";
      }
      return this.plugin.isPasswordInputField(field) ? "*".repeat(value.length()) : value;
   }

   private String getHoveredOption(Player player) {
      HoverEnlargeManager manager = this.plugin.getHoverEnlargeManager();
      return manager == null ? "" : manager.getHoveredElementId(player);
   }
}
