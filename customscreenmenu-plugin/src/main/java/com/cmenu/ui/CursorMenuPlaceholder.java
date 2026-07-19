package com.cmenu.ui;

import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.Section;
import java.util.Map;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class CursorMenuPlaceholder extends PlaceholderExpansion {
   private final CursorMenuPlugin plugin;

   public CursorMenuPlaceholder(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "cursormenu";
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
            String coord = params.split("_")[1];
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
         } else {
            return params.equals("password_visibility") ? this.getPasswordVisibility(player) : null;
         }
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
         Section section = CursorMenuPlugin.sectionManager.get(menuKey);
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
      Map<String, String> newUserData = this.plugin.getPlayerInputSnapshot(player);
      if (newUserData == null || !newUserData.containsKey(field)) {
         Map<UUID, Map<String, String>> userInputData = Commands.getUserInputData();
         Map<String, String> userData = userInputData.get(player.getUniqueId());
         if (userData != null && userData.containsKey(field)) {
            if (!this.plugin.isPasswordInputField(field)) {
               return userData.get(field);
            } else {
               String value = userData.get(field);
               if (value == null) {
                  return "";
               } else {
                  boolean isVisible = this.plugin.getPasswordVisibility(player);
                  return !isVisible ? "*".repeat(Math.max(0, value.length())) : value;
               }
            }
         } else if (!this.plugin.isPasswordInputField(field)) {
            return "";
         } else {
            boolean isVisible = this.plugin.getPasswordVisibility(player);
            return !isVisible ? "*".repeat(1) : "";
         }
      } else if (!this.plugin.isPasswordInputField(field)) {
         return newUserData.get(field);
      } else {
         String value = newUserData.get(field);
         if (value == null) {
            return "";
         } else {
            boolean isVisible = this.plugin.getPasswordVisibility(player);
            return !isVisible ? "*".repeat(Math.max(0, value.length())) : value;
         }
      }
   }

   private String getPasswordVisibility(Player player) {
      boolean isVisible = this.plugin.getPasswordVisibility(player);
      String languageKey = isVisible ? "password.visibility.button.shown" : "password.visibility.button.hidden";
      String defaultValue = isVisible ? "[密码:隐藏]" : "[密码:显示]";
      String buttonText = this.plugin.getLangMessage(languageKey, defaultValue);
      if (CursorMenuPlugin.hasPAPI) {
         buttonText = PlaceholderAPI.setPlaceholders(player, buttonText);
      }

      return buttonText;
   }

   private String getHoveredOption(Player player) {
      String menuKey = this.plugin.getCurrentPlayerMenu(player);
      if (menuKey != null && !menuKey.isEmpty()) {
         Section section = CursorMenuPlugin.sectionManager.get(menuKey);
         if (section == null) {
            return "";
         } else {
            World world = Bukkit.getWorld(section.world);
            if (world == null) {
               return "";
            } else {
               ArmorStand cursor = CursorMenuPlugin.playerCursors.get(player);
               Pig camera = CursorMenuPlugin.playerSit.get(player);
               if (cursor != null && camera != null) {
                  Location cursorLoc = cursor.getLocation();
                  Location cameraLoc = camera.getLocation();
                  Vector dir = cameraLoc.getDirection().normalize();
                  Vector right = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
                  Vector up = dir.getCrossProduct(right).multiply(-1);
                  MenuLayout closest = null;
                  double minDistance = Double.MAX_VALUE;

                  for (MenuLayout layout : section.layouts.values()) {
                     Vector offset = dir.clone().multiply(layout.z).add(right.clone().multiply(layout.x)).add(up.clone().multiply(layout.y));
                     Vector layoutVec = cameraLoc.clone().add(offset).toVector();
                     double distance = cursorLoc.toVector().distance(layoutVec);
                     if (distance < minDistance) {
                        minDistance = distance;
                        closest = layout;
                     }
                  }

                  return closest != null && minDistance < 1.1 ? closest.key : "";
               } else {
                  return "";
               }
            }
         }
      } else {
         return "";
      }
   }
}
