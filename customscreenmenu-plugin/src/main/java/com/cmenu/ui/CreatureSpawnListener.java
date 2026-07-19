package com.cmenu.ui;

import com.cmenu.ui.section.Section;
import com.cmenu.ui.section.SectionManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class CreatureSpawnListener implements Listener {
   @EventHandler
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      if (CursorMenuPlugin.creatureSpawnLimitEnabled) {
         if (event.getSpawnReason() != SpawnReason.CUSTOM) {
            Location spawnLocation = event.getLocation();
            SectionManager sectionManager = CursorMenuPlugin.sectionManager;

            for (Section section : sectionManager.getAll().values()) {
               Location menuLocation = new Location(spawnLocation.getWorld(), section.cameraX, section.cameraY, section.cameraZ);
               if (spawnLocation.getWorld().getName().equals(section.world)
                  && menuLocation.distance(spawnLocation) <= CursorMenuPlugin.creatureSpawnLimitRadius) {
                  event.setCancelled(true);
                  return;
               }
            }
         }
      }
   }
}
