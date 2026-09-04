package org.orvencraft.uicore.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuSwitchEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String fromMenuId;
   private final String toMenuId;

   public MenuSwitchEvent(Player player, String fromMenuId, String toMenuId) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.fromMenuId = fromMenuId;
      this.toMenuId = toMenuId;
   }

   public Player getPlayer() {
      return this.player;
   }

   public String getFromMenuId() {
      return this.fromMenuId;
   }

   public String getToMenuId() {
      return this.toMenuId;
   }

   public HandlerList getHandlers() {
      return HANDLERS;
   }

   public static HandlerList getHandlerList() {
      return HANDLERS;
   }
}
