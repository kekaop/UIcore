package com.cmenu.ui.section;

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
      if (!enabled) {
         return section;
      } else {
         Location loc = player.getLocation();
         return section.copyWithCamera(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
      }
   }
}
