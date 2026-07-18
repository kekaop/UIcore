package com.cmenu.ui;

import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.Section;
import com.cmenu.ui.util.CommandUtils;
import com.cmenu.ui.util.HoverSoundManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class HoverEnlargeManager {
   private final CursorMenuPlugin plugin;
   private final Map<UUID, TextDisplay> enlargedTextDisplays = new ConcurrentHashMap<>();
   private final Map<UUID, Vector3f> originalScales = new ConcurrentHashMap<>();
   private final HoverSoundManager hoverSoundManager;
   private final Map<UUID, TextDisplay> currentHovered = new ConcurrentHashMap<>();
   private final Map<UUID, MenuLayout> currentHoveredLayout = new ConcurrentHashMap<>();
   private BukkitRunnable hoverDetectionTask;

   public HoverEnlargeManager(CursorMenuPlugin plugin) {
      this.plugin = plugin;
      this.hoverSoundManager = new HoverSoundManager(plugin);
      this.startHoverDetection();
   }

   public HoverSoundManager getHoverSoundManager() {
      return this.hoverSoundManager;
   }

   private void startHoverDetection() {
      this.hoverDetectionTask = new BukkitRunnable() {
         public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
               if (CursorMenuPlugin.playerCursors.containsKey(player)) {
                  Location cursorLoc = CursorMenuPlugin.cursorExactLocations.get(player);
                  if (cursorLoc != null) {
                     String menuKey = HoverEnlargeManager.this.plugin.getCurrentPlayerMenu(player);
                     if (menuKey != null) {
                        Section section = CursorMenuPlugin.sectionManager.get(menuKey);
                        if (section != null) {
                           TextDisplay closestDisplay = null;
                           MenuLayout closestLayout = null;
                           double minDistance = Double.MAX_VALUE;

                           for (MenuLayout layout : section.layouts.values()) {
                              if (layout.isHoverEnlargeEnabled() || layout.getHoverSound() != null || layout.hasHoverCommands() || layout.hasLeaveCommands()) {
                                 TextDisplay display = HoverEnlargeManager.this.plugin.getTextDisplayByKeys(player, menuKey, layout.key);
                                 if (display != null && display.isValid() && TextDisplayHitBox.isInside(display, cursorLoc)) {
                                    double distance = display.getLocation().distanceSquared(cursorLoc);
                                    if (distance < minDistance) {
                                       minDistance = distance;
                                       closestDisplay = display;
                                       closestLayout = layout;
                                    }
                                 }
                              }
                           }

                           UUID playerId = player.getUniqueId();
                           TextDisplay currentlyEnlarged = HoverEnlargeManager.this.enlargedTextDisplays.get(playerId);
                           TextDisplay previouslyHovered = HoverEnlargeManager.this.currentHovered.get(playerId);
                           MenuLayout previousLayout = HoverEnlargeManager.this.currentHoveredLayout.get(playerId);
                           if (closestLayout != previousLayout) {
                              if (previousLayout != null && previousLayout.hasLeaveCommands()) {
                                 HoverEnlargeManager.this.executeCommands(player, previousLayout.getLeaveCommands());
                              }

                              if (closestLayout != null && closestLayout.hasHoverCommands()) {
                                 HoverEnlargeManager.this.executeCommands(player, closestLayout.getHoverCommands());
                              }

                              if (closestLayout != null) {
                                 HoverEnlargeManager.this.currentHoveredLayout.put(playerId, closestLayout);
                              } else {
                                 HoverEnlargeManager.this.currentHoveredLayout.remove(playerId);
                              }
                           }

                           if (closestDisplay != null && closestLayout != null) {
                              String soundName = closestLayout.getHoverSound();
                              if (soundName != null && !soundName.isEmpty()) {
                                 HoverEnlargeManager.this.hoverSoundManager
                                    .tryPlayHoverSound(
                                       player,
                                       closestDisplay,
                                       soundName,
                                       closestLayout.getHoverSoundVolume(),
                                       closestLayout.getHoverSoundPitch(),
                                       closestLayout.getHoverSoundCooldown()
                                    );
                              }

                              if (currentlyEnlarged != closestDisplay && closestLayout.isHoverEnlargeEnabled()) {
                                 if (currentlyEnlarged != null) {
                                    HoverEnlargeManager.this.resetTextDisplaySize(currentlyEnlarged);
                                    HoverEnlargeManager.this.enlargedTextDisplays.remove(playerId);
                                 }

                                 HoverEnlargeManager.this.enlargeTextDisplay(closestDisplay, closestLayout);
                                 HoverEnlargeManager.this.enlargedTextDisplays.put(playerId, closestDisplay);
                              }

                              HoverEnlargeManager.this.currentHovered.put(playerId, closestDisplay);
                           } else if (currentlyEnlarged != null || previouslyHovered != null) {
                              if (currentlyEnlarged != null) {
                                 HoverEnlargeManager.this.resetTextDisplaySize(currentlyEnlarged);
                                 HoverEnlargeManager.this.enlargedTextDisplays.remove(playerId);
                              }

                              HoverEnlargeManager.this.currentHovered.remove(playerId);
                           }
                        }
                     }
                  }
               }
            }
         }
      };
      this.hoverDetectionTask.runTaskTimer(this.plugin, 0L, 2L);
   }

   private void enlargeTextDisplay(TextDisplay textDisplay, MenuLayout layout) {
      this.resetTextDisplaySize(textDisplay);
      Transformation oldTrans = textDisplay.getTransformation();
      Vector3f currentScale = oldTrans.getScale();
      UUID displayId = textDisplay.getUniqueId();
      if (!this.originalScales.containsKey(displayId)) {
         this.originalScales.put(displayId, new Vector3f(currentScale));
      }

      if (layout != null && layout.isHoverEnlargeEnabled()) {
         float scaleValue = (float)layout.getHoverEnlargeScale();
         Transformation newTrans = new Transformation(
            oldTrans.getTranslation(),
            oldTrans.getLeftRotation(),
            new Vector3f(currentScale.x * scaleValue, currentScale.y * scaleValue, currentScale.z * scaleValue),
            oldTrans.getRightRotation()
         );
         textDisplay.setTransformation(newTrans);
      }
   }

   private void resetTextDisplaySize(TextDisplay textDisplay) {
      if (textDisplay != null && textDisplay.isValid()) {
         UUID displayId = textDisplay.getUniqueId();
         Vector3f originalScale = this.originalScales.remove(displayId);
         if (originalScale != null) {
            Transformation oldTrans = textDisplay.getTransformation();
            Transformation newTrans = new Transformation(oldTrans.getTranslation(), oldTrans.getLeftRotation(), originalScale, oldTrans.getRightRotation());
            textDisplay.setTransformation(newTrans);
         }
      }
   }

   public void cleanupPlayer(Player player) {
      UUID playerId = player.getUniqueId();
      TextDisplay display = this.enlargedTextDisplays.remove(playerId);
      if (display != null) {
         this.resetTextDisplaySize(display);
      }

      this.currentHovered.remove(playerId);
      this.currentHoveredLayout.remove(playerId);
      this.hoverSoundManager.cleanupPlayer(playerId);
   }

   public void cleanup() {
      if (this.hoverDetectionTask != null) {
         this.hoverDetectionTask.cancel();
         this.hoverDetectionTask = null;
      }

      for (TextDisplay display : this.enlargedTextDisplays.values()) {
         this.resetTextDisplaySize(display);
      }

      this.enlargedTextDisplays.clear();
      this.originalScales.clear();
      this.currentHovered.clear();
      this.currentHoveredLayout.clear();
      this.hoverSoundManager.cleanupAll();
   }

   private void executeCommands(Player player, List<String> commands) {
      for (String cmd : commands) {
         String processedCmd = cmd;
         if (CursorMenuPlugin.hasPAPI) {
            processedCmd = PlaceholderAPI.setPlaceholders(player, cmd);
         }

         CommandUtils.executeCommand(player, processedCmd);
      }
   }
}
