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

      if (this.plugin.getCurrentPlayerMenu(player) != null) this.plugin.stopCursor(player, false);
   }
}
