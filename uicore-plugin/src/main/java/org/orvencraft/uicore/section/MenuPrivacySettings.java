package org.orvencraft.uicore.section;

import org.bukkit.configuration.ConfigurationSection;

/** Visibility and incoming player damage for one menu visit. */
public record MenuPrivacySettings(boolean hideMenuForOtherPlayer, boolean hidePlayerForOtherPlayer,
                                  boolean hideOtherPlayers, boolean canDamaged) {
   public static final MenuPrivacySettings DEFAULT = new MenuPrivacySettings(true, true, false, false);

   public static MenuPrivacySettings from(ConfigurationSection menu) {
      return new MenuPrivacySettings(
         readBoolean(menu, "hide-menu-for-other-player", true),
         readBoolean(menu, "hide-player-for-other-player", true),
         readBoolean(menu, "hide-other-players", false),
         readBoolean(menu, "can-damaged", false));
   }

   private static boolean readBoolean(ConfigurationSection menu, String key, boolean fallback) {
      if (!menu.contains(key)) return fallback;
      if (!menu.isBoolean(key)) throw new IllegalArgumentException("Menu '" + menu.getName() + "': " + key + " must be true or false");
      return menu.getBoolean(key);
   }
}
