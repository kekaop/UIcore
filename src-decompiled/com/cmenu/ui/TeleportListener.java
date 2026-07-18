package com.cmenu.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
   private final CursorMenuPlugin plugin;

   public TeleportListener(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Player player = event.getPlayer();
      if (CursorMenuPlugin.playerCursors.containsKey(player)) {
         this.plugin.stopCursor(player, false);
      }
   }
}
