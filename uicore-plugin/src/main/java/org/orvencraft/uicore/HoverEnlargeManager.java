package org.orvencraft.uicore;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.api.UIcoreApiImpl;
import org.orvencraft.uicore.api.event.MenuHoverEvent;
import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.style.TextStyleConfig;
import org.orvencraft.uicore.util.CommandUtils;
import org.orvencraft.uicore.util.HoverSoundManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import net.kyori.adventure.text.Component;
import space.arim.morepaperlib.scheduling.ScheduledTask;

public class HoverEnlargeManager {
   private final UIcorePlugin plugin;
   private final Map<UUID, TextDisplay> enlargedTextDisplays = new ConcurrentHashMap<>();
   private final Map<UUID, Vector3f> originalScales = new ConcurrentHashMap<>();
   private final HoverSoundManager hoverSoundManager;
   private final Map<UUID, TextDisplay> currentHovered = new ConcurrentHashMap<>();
   private final Map<UUID, MenuLayout> currentHoveredLayout = new ConcurrentHashMap<>();
   private final Map<UUID, ScheduledTask> textStyleTasks = new ConcurrentHashMap<>();
   private final Map<UUID, ItemStack> originalCursorItems = new ConcurrentHashMap<>();
   private final Map<UUID, Vector3f> originalCursorScales = new ConcurrentHashMap<>();


   public HoverEnlargeManager(UIcorePlugin plugin) {
      this.plugin = plugin;
      this.hoverSoundManager = new HoverSoundManager(plugin);

   }

   public HoverSoundManager getHoverSoundManager() {
      return this.hoverSoundManager;
   }

   /** Called on the viewer entity scheduler every two ticks. */
   public void tickPlayer(Player player) {
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
                                 if (display != null
                                    && display.isValid()
                                    && HoverEnlargeManager.this.plugin.isCursorInsideMenuDisplay(player, section, display, cursorLoc)) {
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
         ScheduledTask previousTask = this.textStyleTasks.remove(display.getUniqueId());
         if (previousTask != null) {
            previousTask.cancel();
         }

         String startColor = layout.resolveTextColor(player, !hovered, false);
         String targetColor = layout.resolveTextColor(player, hovered, false);
         int[] from = TextStyleConfig.parseRgb(startColor);
         int[] to = TextStyleConfig.parseRgb(targetColor);
         if (hovered && layout.hasSmoothHoverTransition() && from != null && to != null) {
            int duration = Math.max(1, layout.getHoverTransitionDuration());
            UUID displayId = display.getUniqueId();
            List<Component> frames = new ArrayList<>(duration + 1);
            for (int frame = 0; frame <= duration; frame++) {
               double progress = Math.min(1.0, (double) frame / duration);
               int red = (int) Math.round(from[0] + (to[0] - from[0]) * progress);
               int green = (int) Math.round(from[1] + (to[1] - from[1]) * progress);
               int blue = (int) Math.round(from[2] + (to[2] - from[2]) * progress);
               frames.add(layout.renderStyledComponent(player, true, false, TextStyleConfig.toHex(red, green, blue)));
            }
            int[] tick = {0};
            ScheduledTask[] taskReference = new ScheduledTask[1];
            ScheduledTask task = UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(() -> {
               if (!display.isValid()) {
                  if (taskReference[0] != null) taskReference[0].cancel();
                  HoverEnlargeManager.this.textStyleTasks.remove(displayId);
                  return;
               }
               display.text(frames.get(tick[0]));
               if (tick[0]++ >= duration) {
                  if (taskReference[0] != null) taskReference[0].cancel();
                  HoverEnlargeManager.this.textStyleTasks.remove(displayId);
               }
            }, () -> HoverEnlargeManager.this.textStyleTasks.remove(displayId), 1L, 1L);
            if (task != null) {
               taskReference[0] = task;
               this.textStyleTasks.put(displayId, task);
            }
         } else {
            Component styled = layout.renderStyledComponent(player, hovered, false);
            UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).run(
               () -> { if (display.isValid()) display.text(styled); },
               () -> { }
            );
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
            ScheduledTask previousTask = this.textStyleTasks.remove(display.getUniqueId());
            if (previousTask != null) {
               previousTask.cancel();
            }

            UUID displayId = display.getUniqueId();
            Component releasedStyle = layout.renderStyledComponent(player, this.isCurrentHovered(player, layout), false);
            ScheduledTask task = UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).runDelayed(() -> {
               if (display.isValid()) {
                  display.text(releasedStyle);
               }
               HoverEnlargeManager.this.textStyleTasks.remove(displayId);
            }, () -> HoverEnlargeManager.this.textStyleTasks.remove(displayId), Math.max(1, layout.getPressedStyleDuration()));
            if (task != null) {
               this.textStyleTasks.put(displayId, task);
            }
         }
      }
   }

   public String getHoveredElementId(Player player) {
      MenuLayout layout = player == null ? null : this.currentHoveredLayout.get(player.getUniqueId());
      return layout == null || !layout.isVisible(player) ? "" : layout.key;
   }

   public boolean isCurrentHovered(Player player, MenuLayout layout) {
      return player != null && layout != null && this.currentHoveredLayout.get(player.getUniqueId()) == layout;
   }

   private void showHoverTooltip(Player player, MenuLayout layout) {
      if (player != null && layout != null && layout.hasHoverTooltip()) {
         String text = layout.getHoverTooltipText();
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
         ScheduledTask styleTask = this.textStyleTasks.remove(hoveredDisplay.getUniqueId());
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


      for (TextDisplay display : this.enlargedTextDisplays.values()) {
         this.resetTextDisplaySize(display);
      }

      for (ScheduledTask task : this.textStyleTasks.values()) {
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
         CommandUtils.executeCommand(player, cmd);
      }
   }
}
