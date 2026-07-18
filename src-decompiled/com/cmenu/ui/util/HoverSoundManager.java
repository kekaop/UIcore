package com.cmenu.ui.util;

import com.cmenu.ui.CursorMenuPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public class HoverSoundManager {
   private final Map<UUID, Map<UUID, Long>> lastPlayTimes = new ConcurrentHashMap<>();
   private static final String[] FALLBACK_SOUNDS = new String[]{"block.note.pling", "block.note.harp", "ui.button.click", "entity.experience_orb.pickup"};
   private final CursorMenuPlugin plugin;

   public HoverSoundManager(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   public void tryPlayHoverSound(Player player, TextDisplay display, String soundName, float volume, float pitch, long cooldownTicks) {
      if (player != null && player.isOnline() && display != null && display.isValid()) {
         if (soundName != null && !soundName.isEmpty()) {
            UUID playerId = player.getUniqueId();
            UUID displayId = display.getUniqueId();
            Map<UUID, Long> playerTimes = this.lastPlayTimes.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            long lastPlayTime = playerTimes.getOrDefault(displayId, 0L);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlayTime >= cooldownTicks * 50L) {
               boolean success = this.playSound(player, display.getLocation(), soundName, volume, pitch);
               if (!success) {
                  for (String fallback : FALLBACK_SOUNDS) {
                     success = this.playSound(player, display.getLocation(), fallback, volume, pitch);
                     if (success) {
                        break;
                     }
                  }

                  if (!success) {
                     this.plugin.getLogger().warning("无法播放悬停音效: " + soundName + " (所有备选音效也失败)");
                  }
               }

               if (success) {
                  playerTimes.put(displayId, currentTime);
               }
            }
         }
      }
   }

   private boolean playSound(Player player, Location location, String soundName, float volume, float pitch) {
      try {
         Sound sound;
         try {
            sound = Sound.valueOf(soundName.toUpperCase());
         } catch (IllegalArgumentException var8) {
            player.playSound(location, soundName, volume, pitch);
            return true;
         }

         player.playSound(location, sound, volume, pitch);
         return true;
      } catch (Exception var9) {
         return false;
      }
   }

   public void cleanupPlayer(UUID playerId) {
      this.lastPlayTimes.remove(playerId);
   }

   public void cleanupAll() {
      this.lastPlayTimes.clear();
   }

   public long getRemainingCooldown(Player player, TextDisplay display) {
      if (player != null && display != null) {
         UUID playerId = player.getUniqueId();
         UUID displayId = display.getUniqueId();
         Map<UUID, Long> playerTimes = this.lastPlayTimes.get(playerId);
         if (playerTimes == null) {
            return 0L;
         } else {
            Long lastPlayTime = playerTimes.get(displayId);
            return lastPlayTime == null ? 0L : Math.max(0L, System.currentTimeMillis() - lastPlayTime);
         }
      } else {
         return 0L;
      }
   }

   public void resetCooldowns(Player player) {
      if (player != null) {
         this.lastPlayTimes.remove(player.getUniqueId());
      }
   }
}
