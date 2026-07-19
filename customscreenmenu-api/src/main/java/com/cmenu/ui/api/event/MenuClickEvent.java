package com.cmenu.ui.api.event;

import com.cmenu.ui.api.MenuElementSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuClickEvent extends Event implements Cancellable {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String menuId;
   private final String elementId;
   private final MenuElementSnapshot element;
   private boolean cancelled;

   public MenuClickEvent(Player player, String menuId, String elementId, MenuElementSnapshot element) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.menuId = menuId;
      this.elementId = elementId;
      this.element = element;
   }

   public Player getPlayer() {
      return this.player;
   }

   public String getMenuId() {
      return this.menuId;
   }

   public String getElementId() {
      return this.elementId;
   }

   public MenuElementSnapshot getElement() {
      return this.element;
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
