package org.orvencraft.uicore;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.api.UIcoreApiImpl;
import org.orvencraft.uicore.api.event.MenuHoverEvent;
import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.style.TextStyleConfig;
import org.orvencraft.uicore.util.CommandUtils;
import org.orvencraft.uicore.util.HoverSoundManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class HoverEnlargeManager {
   private final UIcorePlugin plugin;
   private final Map<UUID, TextDisplay> enlargedTextDisplays = new ConcurrentHashMap<>();
   private final Map<UUID, Vector3f> originalScales = new ConcurrentHashMap<>();
   private final HoverSoundManager hoverSoundManager;
   private final Map<UUID, TextDisplay> currentHovered = new ConcurrentHashMap<>();
   private final Map<UUID, MenuLayout> currentHoveredLayout = new ConcurrentHashMap<>();
   private final Map<UUID, BukkitTask> textStyleTasks = new ConcurrentHashMap<>();
   private final Map<UUID, ItemStack> originalCursorItems = new ConcurrentHashMap<>();
   private final Map<UUID, Vector3f> originalCursorScales = new ConcurrentHashMap<>();
   private BukkitRunnable hoverDetectionTask;

   public HoverEnlargeManager(UIcorePlugin plugin) {
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
               if (UIcorePlugin.playerCursors.containsKey(player)) {
                  Location cursorLoc = UIcorePlugin.cursorExactLocations.get(player);
                  if (cursorLoc != null) {
                     String menuKey = HoverEnlargeManager.this.plugin.getCurrentPlayerMenu(player);
                     if (menuKey != null) {
                        Section section = HoverEnlargeManager.this.plugin.getRuntimeSection(player, menuKey);
                        if (section != null) {
                           TextDisplay closestDisplay = null;
                           MenuLayout closestLayout = null;
                           double minDistance = Double.MAX_VALUE;

                           for (MenuLayout layout : HoverEnlargeManager.this.plugin.getLayoutsForPlayer(player, menuKey, section).values()) {
                              if (layout.isHoverEnlargeEnabled()
                                 || layout.getHoverSound() != null
                                 || layout.hasHoverCommands()
                                 || layout.hasLeaveCommands()
                                 || layout.hasHoverVisualEffect()) {
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
                              if (previousLayout != null && previouslyHovered != null) {
                                 HoverEnlargeManager.this.applyTextStyle(player, previouslyHovered, previousLayout, false);
                                 HoverEnlargeManager.this.clearHoverTooltip(player, previousLayout);
                              }

                              HoverEnlargeManager.this.resetHoverCursor(player);
                              if (previousLayout != null && previousLayout.hasLeaveCommands()) {
                                 HoverEnlargeManager.this.executeCommands(player, previousLayout.getLeaveCommands());
                              }

                              if (previousLayout != null) {
                                 Bukkit.getPluginManager()
                                    .callEvent(new MenuHoverEvent(player, menuKey, previousLayout.key, UIcoreApiImpl.snapshot(menuKey, previousLayout), false));
                              }

                              if (closestLayout != null && closestLayout.hasHoverCommands()) {
                                 HoverEnlargeManager.this.executeCommands(player, closestLayout.getHoverCommands());
                              }

                              if (closestLayout != null) {
                                 Bukkit.getPluginManager()
                                    .callEvent(new MenuHoverEvent(player, menuKey, closestLayout.key, UIcoreApiImpl.snapshot(menuKey, closestLayout), true));
                                 HoverEnlargeManager.this.currentHoveredLayout.put(playerId, closestLayout);
                                 if (closestDisplay != null) {
                                    HoverEnlargeManager.this.applyTextStyle(player, closestDisplay, closestLayout, true);
                                 }

                                 HoverEnlargeManager.this.applyHoverCursor(player, closestLayout);
                                 HoverEnlargeManager.this.showHoverTooltip(player, closestLayout);
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

                              if (currentlyEnlarged != null && currentlyEnlarged != closestDisplay) {
                                 HoverEnlargeManager.this.resetTextDisplaySize(currentlyEnlarged);
                                 HoverEnlargeManager.this.enlargedTextDisplays.remove(playerId);
                              }

                              if (currentlyEnlarged != closestDisplay && closestLayout.isHoverEnlargeEnabled()) {
                                 HoverEnlargeManager.this.enlargeTextDisplay(closestDisplay, closestLayout);
                                 HoverEnlargeManager.this.enlargedTextDisplays.put(playerId, closestDisplay);
                              }

                              HoverEnlargeManager.this.currentHovered.put(playerId, closestDisplay);
                           } else if (currentlyEnlarged != null || previouslyHovered != null) {
                              if (previousLayout != null && previouslyHovered != null) {
                                 HoverEnlargeManager.this.applyTextStyle(player, previouslyHovered, previousLayout, false);
                                 HoverEnlargeManager.this.clearHoverTooltip(player, previousLayout);
                              }

                              HoverEnlargeManager.this.resetHoverCursor(player);
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

   private void applyTextStyle(Player player, TextDisplay display, MenuLayout layout, boolean hovered) {
      if (player != null && display != null && display.isValid() && layout != null && layout.hasAnyTextStyle()) {
         BukkitTask previousTask = this.textStyleTasks.remove(display.getUniqueId());
         if (previousTask != null) {
            previousTask.cancel();
         }

         String startColor = layout.resolveTextColor(player, !hovered, false);
         String targetColor = layout.resolveTextColor(player, hovered, false);
         int[] from = TextStyleConfig.parseRgb(startColor);
         int[] to = TextStyleConfig.parseRgb(targetColor);
         if (hovered && layout.hasSmoothHoverTransition() && from != null && to != null) {
            int duration = Math.max(1, layout.getHoverTransitionDuration());
            BukkitRunnable task = new BukkitRunnable() {
               private int tick = 0;

               public void run() {
                  if (!display.isValid()) {
                     this.cancel();
                     HoverEnlargeManager.this.textStyleTasks.remove(display.getUniqueId());
                     return;
                  }

                  double progress = Math.min(1.0, (double)this.tick / (double)duration);
                  int red = (int)Math.round(from[0] + (to[0] - from[0]) * progress);
                  int green = (int)Math.round(from[1] + (to[1] - from[1]) * progress);
                  int blue = (int)Math.round(from[2] + (to[2] - from[2]) * progress);
                  display.setText(layout.renderStyledName(player, true, false, TextStyleConfig.toHex(red, green, blue)));
                  if (this.tick++ >= duration) {
                     this.cancel();
                     HoverEnlargeManager.this.textStyleTasks.remove(display.getUniqueId());
                  }
               }
            };
            this.textStyleTasks.put(display.getUniqueId(), task.runTaskTimer(this.plugin, 0L, 1L));
         } else {
            display.setText(layout.renderStyledName(player, hovered, false));
         }
      }
   }

   public void applyPressedStyle(Player player, MenuLayout layout) {
      if (player != null && layout != null && layout.hasPressedStyle()) {
         String menuKey = this.plugin.getCurrentPlayerMenu(player);
         if (menuKey != null) {
            this.plugin.updateLayoutDisplayText(player, menuKey, layout, this.isCurrentHovered(player, layout), true);
         }

         for (TextDisplay display : menuKey == null ? List.<TextDisplay>of() : this.plugin.getTextDisplaysByKeys(player, menuKey, layout.key)) {
            BukkitTask previousTask = this.textStyleTasks.remove(display.getUniqueId());
            if (previousTask != null) {
               previousTask.cancel();
            }

            BukkitRunnable task = new BukkitRunnable() {
               public void run() {
                  if (display.isValid()) {
                     String rendered = layout.renderStyledName(player, HoverEnlargeManager.this.isCurrentHovered(player, layout), false);
                     display.setText(rendered);
                     display.text(ColorParser.parse(rendered));
                  }

                  HoverEnlargeManager.this.textStyleTasks.remove(display.getUniqueId());
               }
            };
            this.textStyleTasks.put(display.getUniqueId(), task.runTaskLater(this.plugin, Math.max(1, layout.getPressedStyleDuration())));
         }
      }
   }

   private boolean isCurrentHovered(Player player, MenuLayout layout) {
      return player != null && layout != null && this.currentHoveredLayout.get(player.getUniqueId()) == layout;
   }

   private void showHoverTooltip(Player player, MenuLayout layout) {
      if (player != null && layout != null && layout.hasHoverTooltip()) {
         String text = UIcorePlugin.parsePlaceholders(player, layout.getHoverTooltipText());
         if ("message".equalsIgnoreCase(layout.getHoverTooltipType()) || "chat".equalsIgnoreCase(layout.getHoverTooltipType())) {
            CommandUtils.sendChatMessage(player, text);
         } else {
            CommandUtils.sendActionBar(player, text);
         }
      }
   }

   private void clearHoverTooltip(Player player, MenuLayout layout) {
      if (player != null
         && layout != null
         && layout.hasHoverTooltip()
         && !"message".equalsIgnoreCase(layout.getHoverTooltipType())
         && !"chat".equalsIgnoreCase(layout.getHoverTooltipType())) {
         player.sendActionBar(net.kyori.adventure.text.Component.empty());
      }
   }

   private void applyHoverCursor(Player player, MenuLayout layout) {
      if (player != null && layout != null && layout.hasHoverCursor()) {
         ItemDisplay cursor = this.plugin.getCursorItemDisplay(player);
         if (cursor != null && cursor.isValid()) {
            Material material = Material.matchMaterial(layout.getHoverCursorMaterial());
            if (material != null) {
               UUID playerId = player.getUniqueId();
               ItemStack currentItem = cursor.getItemStack();
               if (currentItem != null) {
                  this.originalCursorItems.putIfAbsent(playerId, currentItem.clone());
               }

               Transformation oldTrans = cursor.getTransformation();
               this.originalCursorScales.putIfAbsent(playerId, new Vector3f(oldTrans.getScale()));
               ItemStack stack = new ItemStack(material);
               if (layout.getHoverCursorCustomModelData() > 0) {
                  ItemMeta meta = stack.getItemMeta();
                  if (meta != null) {
                     meta.setCustomModelData(layout.getHoverCursorCustomModelData());
                     stack.setItemMeta(meta);
                  }
               }

               cursor.setItemStack(stack);
               if (layout.getHoverCursorScale() > 0.0) {
                  Vector3f originalScale = this.originalCursorScales.get(playerId);
                  Vector3f scaled = new Vector3f(originalScale == null ? oldTrans.getScale() : originalScale).mul((float)layout.getHoverCursorScale());
                  cursor.setTransformation(new Transformation(oldTrans.getTranslation(), oldTrans.getLeftRotation(), scaled, oldTrans.getRightRotation()));
               }
            }
         }
      }
   }

   private void resetHoverCursor(Player player) {
      if (player != null) {
         UUID playerId = player.getUniqueId();
         ItemDisplay cursor = this.plugin.getCursorItemDisplay(player);
         ItemStack originalItem = this.originalCursorItems.remove(playerId);
         Vector3f originalScale = this.originalCursorScales.remove(playerId);
         if (cursor != null && cursor.isValid()) {
            if (originalItem != null) {
               cursor.setItemStack(originalItem);
            }

            if (originalScale != null) {
               Transformation oldTrans = cursor.getTransformation();
               cursor.setTransformation(new Transformation(oldTrans.getTranslation(), oldTrans.getLeftRotation(), originalScale, oldTrans.getRightRotation()));
            }
         }
      }
   }

   public void cleanupPlayer(Player player) {
      UUID playerId = player.getUniqueId();
      TextDisplay display = this.enlargedTextDisplays.remove(playerId);
      if (display != null) {
         this.resetTextDisplaySize(display);
      }

      TextDisplay hoveredDisplay = this.currentHovered.get(playerId);
      if (hoveredDisplay != null) {
         BukkitTask styleTask = this.textStyleTasks.remove(hoveredDisplay.getUniqueId());
         if (styleTask != null) {
            styleTask.cancel();
         }
      }

      this.resetHoverCursor(player);
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

      for (BukkitTask task : this.textStyleTasks.values()) {
         task.cancel();
      }

      this.enlargedTextDisplays.clear();
      this.originalScales.clear();
      this.textStyleTasks.clear();
      this.originalCursorItems.clear();
      this.originalCursorScales.clear();
      this.currentHovered.clear();
      this.currentHoveredLayout.clear();
      this.hoverSoundManager.cleanupAll();
   }

   private void executeCommands(Player player, List<String> commands) {
      for (String cmd : commands) {
         String processedCmd = cmd;
         if (UIcorePlugin.hasPAPI) {
            processedCmd = PlaceholderAPI.setPlaceholders(player, cmd);
         }

         CommandUtils.executeCommand(player, processedCmd);
      }
   }
}
