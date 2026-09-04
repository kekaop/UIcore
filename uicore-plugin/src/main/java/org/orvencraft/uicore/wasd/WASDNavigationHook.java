package org.orvencraft.uicore.wasd;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

public class WASDNavigationHook {
   private static WASDNavigationHook instance;
   private final JavaPlugin plugin;
   private final WASDNavigationManager navigationManager;
   private boolean initialized = false;

   private WASDNavigationHook(JavaPlugin plugin) {
      this.plugin = plugin;
      this.navigationManager = WASDNavigationManager.getInstance(plugin);
   }

   public static synchronized WASDNavigationHook getInstance(JavaPlugin plugin) {
      if (instance == null) {
         instance = new WASDNavigationHook(plugin);
      }

      return instance;
   }

   public static WASDNavigationHook getInstance() {
      return instance;
   }

   public void initialize() {
      if (!this.navigationManager.getConfig().isEnabled()) {
         this.plugin.getLogger().info("[WASDHook] WASD navigation is disabled; initialization skipped.");
      } else {
         this.navigationManager.initialize();
         this.initialized = true;
         this.plugin.getLogger().info("[WASDHook] WASD navigation hook initialized.");
      }
   }

   public void cleanup() {
      if (this.navigationManager != null) {
         this.navigationManager.shutdown();
      }
   }

   public void onMenuOpen(Player player, String menuKey, Location[] textLocations, List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
      if (this.initialized && this.navigationManager.getConfig().isMenuEnabled(menuKey)) {
         if (!this.navigationManager.getConfig().isPlayerDisabled(player.getName())) {
            WASDSession session = new WASDSession(this.plugin, player, true);
            session.setTextDisplays(textDisplays, scales);
            this.navigationManager.startSession(player, menuKey, textLocations, commands, 0);
            if (this.navigationManager.getConfig().isDebugMode()) {
               this.plugin.getLogger().info("[WASDHook] Enabled WASD navigation for " + player.getName() + ", menu: " + menuKey);
            }
         }
      }
   }

   public void onMenuClose(Player player) {
      if (this.initialized) {
         this.navigationManager.stopSession(player);
      }
   }

   public void onMenuSwitch(Player player, String newMenuKey, Location[] textLocations, List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
      if (this.initialized) {
         this.navigationManager.stopSession(player);
         if (this.navigationManager.getConfig().isMenuEnabled(newMenuKey) && !this.navigationManager.getConfig().isPlayerDisabled(player.getName())) {
            WASDSession session = new WASDSession(this.plugin, player, true);
            session.setTextDisplays(textDisplays, scales);
            this.navigationManager.startSession(player, newMenuKey, textLocations, commands, 0);
         }
      }
   }

   public int getSelectedIndex(Player player) {
      return this.navigationManager.getSelectedIndex(player);
   }

   public boolean toggleWASDForPlayer(Player player) {
      return this.navigationManager.getConfig().togglePlayer(player.getName());
   }

   public boolean isEnabled() {
      return this.initialized && this.navigationManager.getConfig().isEnabled();
   }

   public boolean isMenuEnabled(String menuKey) {
      return this.navigationManager.getConfig().isMenuEnabled(menuKey);
   }

   public boolean canPlayerUseWASD(Player player, String menuKey) {
      return this.isEnabled() && this.isMenuEnabled(menuKey) ? !this.navigationManager.getConfig().isPlayerDisabled(player.getName()) : false;
   }

   public WASDNavigationManager getNavigationManager() {
      return this.navigationManager;
   }

   public void reload() {
      if (this.navigationManager != null) {
         this.navigationManager.reloadConfig();
      }
   }
}
