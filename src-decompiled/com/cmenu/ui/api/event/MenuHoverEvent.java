package com.cmenu.ui.api.event;

import com.cmenu.ui.layout.MenuLayout;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MenuHoverEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String menuId;
   private final String elementId;
   private final MenuLayout layout;
   private final boolean entering;

   public MenuHoverEvent(Player player, String menuId, String elementId, MenuLayout layout, boolean entering) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.menuId = menuId;
      this.elementId = elementId;
      this.layout = layout;
      this.entering = entering;
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

   public MenuLayout getLayout() {
      return this.layout;
   }

   public boolean isEntering() {
      return this.entering;
   }

   public boolean isLeaving() {
      return !this.entering;
   }

   public HandlerList getHandlers() {
      return HANDLERS;
   }

   public static HandlerList getHandlerList() {
      return HANDLERS;
   }
}
