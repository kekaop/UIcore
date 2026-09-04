package org.orvencraft.uicore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
   private final UIcorePlugin plugin;

   public TeleportListener(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.consumeInternalMenuTeleport(player)) {
         return;
      }

      if (this.plugin.isMenuTransitionActive(player) || this.plugin.isCurrentMenuCameraLocation(player, event.getTo())) {
         return;
      }

      if (UIcorePlugin.playerCursors.containsKey(player)) {
         this.plugin.stopCursor(player, false);
      }
   }
}
