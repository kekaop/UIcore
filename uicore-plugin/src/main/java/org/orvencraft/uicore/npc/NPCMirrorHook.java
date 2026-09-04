package org.orvencraft.uicore.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class NPCMirrorHook {
   private static NPCMirrorHook instance;
   private final JavaPlugin plugin;
   private final NPCMirrorManager npcManager;
   private boolean initialized = false;

   private NPCMirrorHook(JavaPlugin plugin) {
      this.plugin = plugin;
      this.npcManager = NPCMirrorManager.getInstance(plugin);
      this.initialized = this.npcManager.isEnabled();
   }

   public static synchronized NPCMirrorHook getInstance(JavaPlugin plugin) {
      if (instance == null) {
         instance = new NPCMirrorHook(plugin);
      }

      return instance;
   }

   public static NPCMirrorHook getInstance() {
      return instance;
   }

   public void initialize() {
      if (!this.initialized) {
         this.plugin.getLogger().info("[NPCMirrorHook] NPC mirror system is disabled; initialization skipped.");
      } else {
         this.plugin.getLogger().info("[NPCMirrorHook] NPC mirror hook initialized.");
      }
   }

   public void onMenuOpen(final Player player, final Location menuLocation, final float menuYaw, final float menuPitch, final String menuKey) {
      if (this.initialized && this.npcManager.isEnabled()) {
         (new BukkitRunnable() {
            public void run() {
               if (player.isOnline()) {
                  NPCMirrorHook.this.npcManager.createMirrorNPC(player, menuLocation, menuYaw, menuPitch, menuKey);
               }
            }
         }).runTaskLater(this.plugin, 5L);
      }
   }

   public void onMenuOpen(Player player, Location menuLocation, float menuYaw, float menuPitch) {
      this.onMenuOpen(player, menuLocation, menuYaw, menuPitch, null);
   }

   public void onMenuOpen(Player player, Location menuLocation) {
      this.onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), null);
   }

   public void onMenuOpen(Player player, Location menuLocation, String menuKey) {
      this.onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), menuKey);
   }

   public void onMenuClose(Player player) {
      if (this.initialized) {
         this.npcManager.removeMirrorNPC(player);
      }
   }

   public void onMenuSwitch(final Player player, final Location newMenuLocation, final float newMenuYaw, final float newMenuPitch, final String menuKey) {
      if (this.initialized && this.npcManager.isEnabled()) {
         this.npcManager.removeMirrorNPC(player);
         (new BukkitRunnable() {
            public void run() {
               if (player.isOnline()) {
                  NPCMirrorHook.this.npcManager.createMirrorNPC(player, newMenuLocation, newMenuYaw, newMenuPitch, menuKey);
               }
            }
         }).runTaskLater(this.plugin, 3L);
      }
   }

   public void onMenuSwitch(Player player, Location newMenuLocation, float newMenuYaw, float newMenuPitch) {
      this.onMenuSwitch(player, newMenuLocation, newMenuYaw, newMenuPitch, null);
   }

   public void onMenuSwitch(Player player, Location newMenuLocation) {
      this.onMenuSwitch(player, newMenuLocation, newMenuLocation.getYaw(), newMenuLocation.getPitch(), null);
   }

   public void onMenuSwitch(Player player, Location newMenuLocation, String menuKey) {
      this.onMenuSwitch(player, newMenuLocation, newMenuLocation.getYaw(), newMenuLocation.getPitch(), menuKey);
   }

   public void onPlayerLocationUpdate(Player player, Location newLocation) {
      if (this.initialized && this.npcManager.isEnabled()) {
         if (this.npcManager.hasNPC(player)) {
         }
      }
   }

   public void rotateNPC(Player player, float yawOffset) {
      if (this.initialized) {
         this.npcManager.rotateNPC(player, yawOffset);
      }
   }

   public boolean toggleNPCForPlayer(Player player) {
      return this.npcManager.toggleNPCForPlayer(player);
   }

   public void setNPCEnabledForPlayer(Player player, boolean enabled) {
      this.npcManager.setNPCCreationEnabled(player, enabled);
   }

   public boolean isEnabled() {
      return this.initialized && this.npcManager.isEnabled();
   }

   public boolean isNPCEnabledForPlayer(Player player) {
      return this.npcManager.isNPCCreationEnabled(player);
   }

   public NPCMirrorManager getNPCManager() {
      return this.npcManager;
   }

   public void cleanup() {
      if (this.npcManager != null) {
         this.npcManager.cleanup();
      }
   }

   public void reload() {
      if (this.npcManager != null) {
         this.npcManager.reloadConfig();
      }
   }
}
