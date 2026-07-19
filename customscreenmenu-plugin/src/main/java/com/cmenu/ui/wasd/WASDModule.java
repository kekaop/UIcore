package com.cmenu.ui.wasd;

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
         plugin.getLogger().warning("[WASDModule] 模块已经初始化，跳过重复初始化");
      } else {
         instance = new WASDModule(plugin);
         instance.navigationHook.initialize();
         if (instance.enabled) {
            plugin.getLogger().info("[WASDModule] WASD导航模块已启用");
         } else {
            plugin.getLogger().info("[WASDModule] WASD导航模块未启用（配置已禁用）");
         }
      }
   }

   public static synchronized void shutdown() {
      if (instance != null) {
         instance.navigationHook.cleanup();
         instance.navigationManager.shutdown();
         instance = null;
      }
   }

   public static WASDModule getInstance() {
      return instance;
   }

   public static boolean isModuleEnabled() {
      return instance != null && instance.enabled;
   }

   public static void onMenuOpen(
      Player player, String menuKey, Location[] textLocations, List<String> commands, TextDisplay[] textDisplays, List<Double> scales
   ) {
      if (instance != null && instance.enabled) {
         instance.navigationHook.onMenuOpen(player, menuKey, textLocations, commands, textDisplays, scales);
      }
   }

   public static void onMenuClose(Player player) {
      if (instance != null) {
         instance.navigationHook.onMenuClose(player);
      }
   }

   public static void onMenuSwitch(
      Player player, String newMenuKey, Location[] textLocations, List<String> commands, TextDisplay[] textDisplays, List<Double> scales
   ) {
      if (instance != null && instance.enabled) {
         instance.navigationHook.onMenuSwitch(player, newMenuKey, textLocations, commands, textDisplays, scales);
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
