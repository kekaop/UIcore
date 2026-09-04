package org.orvencraft.uicore.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuOpenEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String menuId;

   public MenuOpenEvent(Player player, String menuId) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.menuId = menuId;
   }

   public Player getPlayer() {
      return this.player;
   }

   public String getMenuId() {
      return this.menuId;
   }

   public HandlerList getHandlers() {
      return HANDLERS;
   }

   public static HandlerList getHandlerList() {
      return HANDLERS;
   }
}
