package org.orvencraft.uicore.wasd;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

public class WASDModule {
   private static WASDModule instance;
   private final JavaPlugin plugin;
   private final WASDNavigationManager navigationManager;
   private final WASDNavigationHook navigationHook;
   private boolean enabled = false;

   private WASDModule(JavaPlugin plugin) {
      this.plugin = plugin;
      this.navigationManager = WASDNavigationManager.getInstance(plugin);
      this.navigationHook = WASDNavigationHook.getInstance(plugin);
      this.enabled = this.navigationManager.getConfig().isEnabled();
   }

   public static synchronized void initialize(JavaPlugin plugin) {
      if (instance != null) {
         plugin.getLogger().warning("[WASDModule] Module is already initialized; duplicate initialization skipped.");
      } else {
         instance = new WASDModule(plugin);
         instance.navigationHook.initialize();
         if (instance.enabled) {
            plugin.getLogger().info("[WASDModule] WASD navigation module enabled.");
         } else {
            plugin.getLogger().info("[WASDModule] WASD navigation module disabled by configuration.");
         }
      }
   }

   public static synchronized void shutdown() {
      if (instance != null) {
         instance.navigationHook.cleanup();
         instance = null;
         WASDNavigationHook.clearInstance();
         WASDNavigationManager.clearInstance();
      }
   }

   public static WASDModule getInstance() {
      return instance;
   }

   public static boolean isModuleEnabled() {
      return instance != null && instance.enabled;
   }

   public static void onMenuOpen(
      Player player, String menuKey, Location[] textLocations, List<String> elementIds, TextDisplay[] textDisplays, List<Double> scales
   ) {
      if (instance != null) {
         instance.navigationHook.onMenuOpen(player, menuKey, textLocations, elementIds, textDisplays, scales);
      }
   }

   public static void onMenuClose(Player player) {
      if (instance != null) {
         instance.navigationHook.onMenuClose(player);
      }
   }

   public static void onMenuSwitch(
      Player player, String newMenuKey, Location[] textLocations, List<String> elementIds, TextDisplay[] textDisplays, List<Double> scales
   ) {
      if (instance != null) {
         instance.navigationHook.onMenuSwitch(player, newMenuKey, textLocations, elementIds, textDisplays, scales);
      }
   }

   public static int getSelectedIndex(Player player) {
      return instance != null ? instance.navigationManager.getSelectedIndex(player) : -1;
   }

   public static String getPlayerCurrentMenu(Player player) {
      return instance != null ? instance.navigationManager.getPlayerCurrentMenu(player.getUniqueId()) : null;
   }

   public static Location getPlayerCurrentLocation(Player player) {
      return instance != null ? instance.navigationManager.getPlayerCurrentLocation(player.getUniqueId()) : null;
   }

   public static boolean toggleWASDForPlayer(Player player) {
      return instance != null ? instance.navigationHook.toggleWASDForPlayer(player) : false;
   }

   public static boolean isMenuEnabled(String menuKey) {
      return instance != null && instance.navigationHook.isMenuEnabled(menuKey);
   }

   public static boolean canPlayerUseWASD(Player player, String menuKey) {
      return instance != null && instance.navigationHook.canPlayerUseWASD(player, menuKey);
   }

   public static void reload() {
      if (instance != null) {
         instance.navigationHook.reload();
         instance.enabled = instance.navigationManager.getConfig().isEnabled();
      }
   }

   public WASDNavigationManager getNavigationManager() {
      return this.navigationManager;
   }

   public WASDNavigationHook getNavigationHook() {
      return this.navigationHook;
   }

   public boolean isEnabled() {
      return this.enabled;
   }
}
