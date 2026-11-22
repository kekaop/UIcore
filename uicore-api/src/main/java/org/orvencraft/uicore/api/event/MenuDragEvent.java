package org.orvencraft.uicore.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Player-scheduler event. Cancelling START/MOVE/DROP rolls back the gesture.
 * CANCEL is a notification. DROP precedes the commit; see {@link MenuDropEvent}
 * for the confirmed result.
 */
public final class MenuDragEvent extends Event implements Cancellable {
   public enum Phase { START, MOVE, DROP, CANCEL }
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String menuId, elementId, targetId, dropTargetId;
   private final Phase phase;
   private final double x, y, z;
   private boolean cancelled;
   public MenuDragEvent(Player player, String menuId, String elementId, String targetId, Phase phase,
                        double x, double y, double z, String dropTargetId) {
      super(!Bukkit.isPrimaryThread());
      this.player = player; this.menuId = menuId; this.elementId = elementId; this.targetId = targetId;
      this.phase = phase; this.x = x; this.y = y; this.z = z; this.dropTargetId = dropTargetId;
   }
   public Player getPlayer() { return player; }
   public String getMenuId() { return menuId; }
   public String getElementId() { return elementId; }
   public String getTargetId() { return targetId; }
   public String getDropTargetId() { return dropTargetId; }
   public Phase getPhase() { return phase; }
   public double getX() { return x; }
   public double getY() { return y; }
   public double getZ() { return z; }
   @Override public boolean isCancelled() { return cancelled; }
   @Override public void setCancelled(boolean value) { cancelled = value; }
   @Override public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
