package org.orvencraft.uicore;

import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.section.SectionManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class CreatureSpawnListener implements Listener {
   @EventHandler
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      if (UIcorePlugin.creatureSpawnLimitEnabled) {
         if (event.getSpawnReason() != SpawnReason.CUSTOM) {
            Location spawnLocation = event.getLocation();
            SectionManager sectionManager = UIcorePlugin.sectionManager;

            for (Section section : sectionManager.getAll().values()) {
               if (section.isPlayerRelative()) {
                  continue;
               }
               Location menuLocation = new Location(spawnLocation.getWorld(), section.cameraX, section.cameraY, section.cameraZ);
               if (spawnLocation.getWorld().getName().equals(section.world)
                  && menuLocation.distance(spawnLocation) <= UIcorePlugin.creatureSpawnLimitRadius) {
                  event.setCancelled(true);
                  return;
               }
            }
         }
      }
   }
}
