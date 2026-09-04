package org.orvencraft.uicore;

import org.orvencraft.uicore.section.Section;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {
   private final UIcorePlugin plugin;

   public WorldChangeListener(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.isMenuTransitionActive(player)) {
         return;
      }

      String currentMenu = this.plugin.getCurrentPlayerMenu(player);
      if (currentMenu != null) {
         Section section = this.plugin.getRuntimeSection(player, currentMenu);
         if (section != null) {
            World world = Bukkit.getWorld(section.world);
            if (world != null) {
               if (!player.getWorld().equals(world)) {
                  Location cameraLoc = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
                  this.plugin.teleportMenuInternal(player, cameraLoc);
                  this.plugin.stopCursor(player, false);
                  this.plugin.setupCursor(player, currentMenu);
               }
            }
         }
      }
   }
}
