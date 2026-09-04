package org.orvencraft.uicore.wasd;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WASDExpansion extends PlaceholderExpansion {
   private final WASDNavigationManager navigationManager;
   private final Map<UUID, String> playerCurrentMenus = new ConcurrentHashMap<>();
   private final Map<UUID, Location> playerCurrentLocations = new ConcurrentHashMap<>();

   public WASDExpansion(WASDNavigationManager navigationManager) {
      this.navigationManager = navigationManager;
   }

   @NotNull
   public String getIdentifier() {
      return "uicorewasd";
   }

   @NotNull
   public String getAuthor() {
      return "UIcore";
   }

   @NotNull
   public String getVersion() {
      return "1.0.0";
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
      if (offlinePlayer != null && offlinePlayer.isOnline()) {
         Player player = offlinePlayer.getPlayer();
         if (player == null) {
            return "";
         } else {
            UUID playerId = player.getUniqueId();
            String lowerParams = params.toLowerCase();
            if (lowerParams.equals("wasd_menu")) {
               String menuKey = this.playerCurrentMenus.get(playerId);
               return menuKey != null ? menuKey : "";
            } else if (lowerParams.equals("wasd_enabled")) {
               WASDSession session = this.navigationManager.getPlayerSession(playerId);
               return session != null && session.isEnabled() ? "true" : "false";
            } else if (lowerParams.equals("wasd_index")) {
               int index = this.navigationManager.getSelectedIndex(player);
               return String.valueOf(index);
            } else if (lowerParams.endsWith("_xyz")) {
               String menuKey = params.substring(0, params.length() - 4);
               return this.getMenuCoordinate(playerId, menuKey, "xyz");
            } else if (lowerParams.endsWith("_x")) {
               String menuKey = params.substring(0, params.length() - 2);
               return this.getMenuCoordinate(playerId, menuKey, "x");
            } else if (lowerParams.endsWith("_y")) {
               String menuKey = params.substring(0, params.length() - 2);
               return this.getMenuCoordinate(playerId, menuKey, "y");
            } else if (lowerParams.endsWith("_z")) {
               String menuKey = params.substring(0, params.length() - 2);
               return this.getMenuCoordinate(playerId, menuKey, "z");
            } else if (lowerParams.equals("wasd_x")) {
               return this.getCurrentCoordinate(playerId, "x");
            } else if (lowerParams.equals("wasd_y")) {
               return this.getCurrentCoordinate(playerId, "y");
            } else if (lowerParams.equals("wasd_z")) {
               return this.getCurrentCoordinate(playerId, "z");
            } else {
               return lowerParams.equals("wasd_location") ? this.getCurrentLocationString(playerId) : null;
            }
         }
      } else {
         return "";
      }
   }

   private String getMenuCoordinate(UUID playerId, String menuKey, String type) {
      String currentMenu = this.playerCurrentMenus.get(playerId);
      if (currentMenu != null && currentMenu.equalsIgnoreCase(menuKey)) {
         Location loc = this.playerCurrentLocations.get(playerId);
         return loc == null ? "" : this.formatCoordinate(loc, type);
      } else {
         return "";
      }
   }

   private String getCurrentCoordinate(UUID playerId, String type) {
      Location loc = this.playerCurrentLocations.get(playerId);
      return loc == null ? "" : this.formatCoordinate(loc, type);
   }

   private String getCurrentLocationString(UUID playerId) {
      String menuKey = this.playerCurrentMenus.get(playerId);
      Location loc = this.playerCurrentLocations.get(playerId);
      return menuKey != null && loc != null ? String.format("%s,%.2f,%.2f,%.2f", menuKey, loc.getX(), loc.getY(), loc.getZ()) : "";
   }

   private String formatCoordinate(Location loc, String type) {
      String var3 = type.toLowerCase();
      switch (var3) {
         case "x":
            return String.format("%.2f", loc.getX());
         case "y":
            return String.format("%.2f", loc.getY());
         case "z":
            return String.format("%.2f", loc.getZ());
         case "xyz":
            return String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
         default:
            return "";
      }
   }

   public void updatePlayerMenu(UUID playerId, String menuKey, Location location) {
      this.playerCurrentMenus.put(playerId, menuKey);
      if (location != null) {
         this.playerCurrentLocations.put(playerId, location.clone());
      }
   }

   public void clearPlayerMenu(UUID playerId) {
      this.playerCurrentMenus.remove(playerId);
      this.playerCurrentLocations.remove(playerId);
   }

   public String getPlayerCurrentMenu(UUID playerId) {
      return this.playerCurrentMenus.get(playerId);
   }

   public Location getPlayerCurrentLocation(UUID playerId) {
      return this.playerCurrentLocations.get(playerId);
   }
}
