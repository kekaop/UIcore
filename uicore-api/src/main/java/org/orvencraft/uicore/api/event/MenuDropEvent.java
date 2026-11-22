package org.orvencraft.uicore.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A completed drop on the player's scheduler, after the final menu position is
 * stored and before YAML on-drop actions run. Use {@link MenuDragEvent.Phase#DROP} to veto a drop.
 */
public final class MenuDropEvent extends Event {
   private static final HandlerList HANDLERS = new HandlerList();
   private final Player player;
   private final String menuId, elementId, targetId, dropTargetId;
   private final double x, y, z;

   public MenuDropEvent(Player player, String menuId, String elementId, String targetId,
                        String dropTargetId, double x, double y, double z) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.menuId = menuId;
      this.elementId = elementId;
      this.targetId = targetId;
      this.dropTargetId = dropTargetId;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Player getPlayer() { return player; }
   public String getMenuId() { return menuId; }
   /** The handle used to start the gesture. */
   public String getElementId() { return elementId; }
   /** The element or group that moved. */
   public String getTargetId() { return targetId; }
   /** The receiving element ID, or null for a free drop. */
   public String getDropTargetId() { return dropTargetId; }
   /** Final x coordinate in the moved element's parent space. */
   public double getX() { return x; }
   public double getY() { return y; }
   public double getZ() { return z; }
   @Override public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}
