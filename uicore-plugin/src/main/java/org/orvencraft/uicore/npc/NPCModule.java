package org.orvencraft.uicore.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NPCModule {
   private static NPCModule instance;
   private final JavaPlugin plugin;
   private final NPCMirrorManager mirrorManager;
   private final NPCMirrorHook mirrorHook;
   private boolean enabled = false;

   private NPCModule(JavaPlugin plugin) {
      this.plugin = plugin;
      this.mirrorManager = NPCMirrorManager.getInstance(plugin);
      this.mirrorHook = NPCMirrorHook.getInstance(plugin);
      this.enabled = this.mirrorManager.isEnabled();
   }

   public static synchronized void initialize(JavaPlugin plugin) {
      if (instance != null) {
         plugin.getLogger().warning("[NPCModule] Module is already initialized; duplicate initialization skipped.");
      } else {
         instance = new NPCModule(plugin);
         instance.mirrorHook.initialize();
         if (instance.enabled) {
            plugin.getLogger().info("[NPCModule] NPC mirror module enabled.");
         } else {
            plugin.getLogger().info("[NPCModule] NPC mirror module disabled (FancyNpcs is missing or configuration is disabled).");
         }
      }
   }

   public static synchronized void shutdown() {
      if (instance != null) {
         instance.mirrorHook.cleanup();
         instance = null;
         NPCMirrorHook.clearInstance();
         NPCMirrorManager.clearInstance();
      }
   }

   public static NPCModule getInstance() {
      return instance;
   }

   public static boolean isModuleEnabled() {
      return instance != null && instance.enabled;
   }

   public static void onMenuOpen(Player player, Location menuLocation, float yaw, float pitch, String menuKey) {
      if (instance != null) {
         instance.mirrorHook.onMenuOpen(player, menuLocation, yaw, pitch, menuKey);
      }
   }

   public static void onMenuOpen(Player player, Location menuLocation, float yaw, float pitch) {
      onMenuOpen(player, menuLocation, yaw, pitch, null);
   }

   public static void onMenuOpen(Player player, Location menuLocation) {
      onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), null);
   }

   public static void onMenuOpen(Player player, Location menuLocation, String menuKey) {
      onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), menuKey);
   }

   public static void onMenuClose(Player player) {
      if (instance != null) {
         instance.mirrorHook.onMenuClose(player);
      }
   }

   public static void onMenuSwitch(Player player, Location newLocation, float yaw, float pitch, String menuKey) {
      if (instance != null) {
         instance.mirrorHook.onMenuSwitch(player, newLocation, yaw, pitch, menuKey);
      }
   }

   public static void onMenuSwitch(Player player, Location newLocation, float yaw, float pitch) {
      onMenuSwitch(player, newLocation, yaw, pitch, null);
   }

   public static void reload() {
      if (instance != null) {
         instance.mirrorHook.reload();
         instance.enabled = instance.mirrorManager.isEnabled();
      }
   }

   public NPCMirrorManager getMirrorManager() {
      return this.mirrorManager;
   }

   public NPCMirrorHook getMirrorHook() {
      return this.mirrorHook;
   }

   public boolean isEnabled() {
      return this.enabled;
   }
}
