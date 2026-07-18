package com.cmenu.ui;

import com.cmenu.ui.section.Section;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {
   private final CursorMenuPlugin plugin;

   public WorldChangeListener(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      Player player = event.getPlayer();
      String currentMenu = this.plugin.getCurrentPlayerMenu(player);
      if (currentMenu != null) {
         Section section = CursorMenuPlugin.sectionManager.get(currentMenu);
         if (section != null) {
            World world = Bukkit.getWorld(section.world);
            if (world != null) {
               if (!player.getWorld().equals(world)) {
                  Location cameraLoc = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
                  player.teleport(cameraLoc);
                  this.plugin.stopCursor(player, false);
                  this.plugin.setupCursor(player, currentMenu);
               }
            }
         }
      }
   }
}
