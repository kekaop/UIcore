package com.cmenu.ui.util;

import com.cmenu.ui.CursorMenuPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

public class TypewriterEffect {
   private final Map<UUID, Integer> typewriterTasks = new ConcurrentHashMap<>();
   private final Map<UUID, String> fullTexts = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> currentIndexes = new ConcurrentHashMap<>();
   private final CursorMenuPlugin plugin;

   public TypewriterEffect(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   public void applyEffect(final TextDisplay display, String fullText, final int speed, final int startDelay, final String cursorChar) {
      if (display != null && display.isValid() && fullText != null && !fullText.isEmpty()) {
         final UUID displayId = display.getUniqueId();
         this.cancelEffectInternal(displayId);
         display.setText(cursorChar);
         this.fullTexts.put(displayId, fullText);
         this.currentIndexes.put(displayId, 0);
         BukkitRunnable task = new BukkitRunnable() {
            private boolean cursorVisible = true;
            private long tick = 0L;
            private boolean completed = false;

            public void run() {
               if (!this.completed && display.isValid()) {
                  this.tick++;
                  if (this.tick <= startDelay) {
                     if (this.tick % 10L == 0L) {
                        this.cursorVisible = !this.cursorVisible;
                        display.setText(this.cursorVisible ? cursorChar : "");
                     }
                  } else {
                     int currentIdx = TypewriterEffect.this.currentIndexes.getOrDefault(displayId, 0);
                     String text = TypewriterEffect.this.fullTexts.get(displayId);
                     if (text != null && currentIdx < text.length()) {
                        if ((this.tick - startDelay) % speed == 0L && currentIdx < text.length()) {
                           TypewriterEffect.this.currentIndexes.put(displayId, ++currentIdx);
                        }

                        String displayedText = text.substring(0, Math.min(currentIdx, text.length()));
                        this.cursorVisible = !this.cursorVisible;
                        if (currentIdx >= text.length() && !this.cursorVisible) {
                           display.setText(displayedText);
                        } else {
                           display.setText(displayedText + cursorChar);
                        }
                     } else {
                        this.completed = true;
                        if (text != null) {
                           display.setText(text);
                        }

                        this.cancel();
                     }
                  }
               } else {
                  this.cancel();
               }
            }

            public synchronized void cancel() {
               if (!this.completed) {
                  this.completed = true;
                  super.cancel();
               }
            }
         };
         int taskId = task.runTaskTimer(this.plugin, 1L, 1L).getTaskId();
         this.typewriterTasks.put(displayId, taskId);
      }
   }

   public void completeEffect(TextDisplay display) {
      if (display != null && display.isValid()) {
         UUID displayId = display.getUniqueId();
         String fullText = this.fullTexts.get(displayId);
         this.cancelEffect(display);
         if (fullText != null) {
            display.setText(fullText);
         }
      }
   }

   public void cancelEffect(TextDisplay display) {
      if (display != null) {
         this.cancelEffectInternal(display.getUniqueId());
      }
   }

   private void cancelEffectInternal(UUID displayId) {
      Integer taskId = this.typewriterTasks.remove(displayId);
      if (taskId != null) {
         Bukkit.getScheduler().cancelTask(taskId);
      }

      this.fullTexts.remove(displayId);
      this.currentIndexes.remove(displayId);
   }

   public void cleanupAll() {
      for (Integer taskId : this.typewriterTasks.values()) {
         Bukkit.getScheduler().cancelTask(taskId);
      }

      this.typewriterTasks.clear();
      this.fullTexts.clear();
      this.currentIndexes.clear();
   }

   public boolean isRunning(TextDisplay display) {
      return display == null ? false : this.typewriterTasks.containsKey(display.getUniqueId());
   }
}
