package com.cmenu.ui.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuInputEvent extends Event implements Cancellable {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String menuId;
   private final String field;
   private String value;
   private boolean cancelled;

   public MenuInputEvent(Player player, String menuId, String field, String value) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.menuId = menuId;
      this.field = field;
      this.value = value;
   }

   public Player getPlayer() {
      return this.player;
   }

   public String getMenuId() {
      return this.menuId;
   }

   public String getField() {
      return this.field;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value == null ? "" : value;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }

   public HandlerList getHandlers() {
      return HANDLERS;
   }

   public static HandlerList getHandlerList() {
      return HANDLERS;
   }
}
