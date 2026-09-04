package org.orvencraft.uicore.section;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PlayerLocationOverride {
   private static boolean enabled = false;

   private PlayerLocationOverride() {
   }

   public static void reload(boolean cfgValue) {
      enabled = cfgValue;
   }

   public static Section apply(Player player, Section section) {
      return apply(player, section, player == null ? null : player.getLocation());
   }

   public static Section apply(Player player, Section section, Location anchor) {
      if (player == null || section == null || !enabled && !section.isPlayerRelative()) {
         return section;
      }

      Location loc = anchor == null ? player.getLocation() : anchor;
      return section.resolveFromPlayerLocation(loc);
   }
}
