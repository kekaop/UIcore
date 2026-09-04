package org.orvencraft.uicore.section;

import java.util.Locale;

public enum MenuPositionType {
   FIXED,
   PLAYER;

   public static MenuPositionType parse(String value) {
      if (value == null || value.isBlank()) {
         return FIXED;
      }

      return switch (value.trim().toLowerCase(Locale.ROOT).replace('-', '_')) {
         case "player", "player_relative", "relative", "in_front_of_player" -> PLAYER;
         default -> FIXED;
      };
   }
}
