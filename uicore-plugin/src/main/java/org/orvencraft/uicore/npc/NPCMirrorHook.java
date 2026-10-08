package org.orvencraft.uicore.npc;

import org.orvencraft.uicore.UIcorePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

   static synchronized void clearInstance() {
      instance = null;
   }

   public void initialize() {
      this.initialized = this.npcManager.isEnabled();
      if (!this.initialized) {
         this.plugin.getLogger().info("[NPCMirrorHook] NPC mirror system is disabled; initialization skipped.");
      } else {
         this.plugin.getLogger().info("[NPCMirrorHook] NPC mirror hook initialized.");
      }
   }

   public void onMenuOpen(final Player player, final Location menuLocation, final float menuYaw, final float menuPitch, final String menuKey) {
      if (player == null) return;
      final Location location = menuLocation == null ? null : menuLocation.clone();
      UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
         this.npcManager.removeMirrorNPC(player);
         if (!this.initialized || !this.npcManager.isEnabled() || location == null) return;
         long sessionGeneration = ((UIcorePlugin) this.plugin).getMenuSessionGeneration(player);
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(() -> {
            if (((UIcorePlugin) this.plugin).isMenuSessionCurrent(player, sessionGeneration)) {
               this.npcManager.createMirrorNPC(player, location, menuYaw, menuPitch, menuKey);
            }
         }, () -> { }, 5L);
      }, () -> { });
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
      if (player != null && this.initialized) org.orvencraft.uicore.scheduler.CleanupTasks.entity(this.plugin, player,
         () -> this.npcManager.removeMirrorNPC(player));
   }

   public void onMenuSwitch(final Player player, final Location newMenuLocation, final float newMenuYaw, final float newMenuPitch, final String menuKey) {
      if (player == null) return;
      final Location location = newMenuLocation == null ? null : newMenuLocation.clone();
      UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
         this.npcManager.removeMirrorNPC(player);
         if (!this.initialized || !this.npcManager.isEnabled() || location == null) return;
         long sessionGeneration = ((UIcorePlugin) this.plugin).getMenuSessionGeneration(player);
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(() -> {
            if (((UIcorePlugin) this.plugin).isMenuSessionCurrent(player, sessionGeneration)) {
               this.npcManager.createMirrorNPC(player, location, newMenuYaw, newMenuPitch, menuKey);
            }
         }, () -> { }, 3L);
      }, () -> { });
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
      if (player != null && newLocation != null && this.initialized && this.npcManager.isEnabled() && this.npcManager.hasNPC(player)) {
         Location location = newLocation.clone();
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player)
            .run(() -> this.npcManager.updateNPCLocation(player, location), () -> { });
      }
   }

   public void rotateNPC(Player player, float yawOffset) {
      if (player != null && this.initialized) UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player)
         .run(() -> this.npcManager.rotateNPC(player, yawOffset), () -> { });
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
         this.initialized = this.npcManager.isEnabled();
         if (!this.initialized) {
            this.npcManager.cleanup();
         }
      }
   }
}
