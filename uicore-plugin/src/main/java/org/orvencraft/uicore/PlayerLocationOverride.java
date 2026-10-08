package org.orvencraft.uicore;

import org.orvencraft.uicore.section.Section;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Legacy mutable adapter. New callers should use section.PlayerLocationOverride.apply and its copied Section. */
@Deprecated
public final class PlayerLocationOverride {
   private static boolean enabled = false;

   private PlayerLocationOverride() {
   }

   public static void reload(boolean cfgValue) {
      enabled = cfgValue;
   }

   public static boolean apply(Player player, Section section) {
      if (!enabled) {
         return false;
      } else {
         Location loc = player.getLocation();
         section.world = loc.getWorld().getName();
         section.cameraX = loc.getX();
         section.cameraY = loc.getY();
         section.cameraZ = loc.getZ();
         section.yaw = loc.getYaw();
         section.pitch = loc.getPitch();
         return true;
      }
   }
}
