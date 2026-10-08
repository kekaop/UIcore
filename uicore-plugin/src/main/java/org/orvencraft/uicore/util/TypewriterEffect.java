package org.orvencraft.uicore.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.orvencraft.uicore.ColorParser;
import org.orvencraft.uicore.UIcorePlugin;
import org.bukkit.entity.TextDisplay;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.morepaperlib.scheduling.ScheduledTask;

/** Reveals text on the scheduler that owns the display being animated. */
public class TypewriterEffect {
   private final Map<UUID, ScheduledTask> typewriterTasks = new ConcurrentHashMap<>();
   private final Map<UUID, Component> fullComponents = new ConcurrentHashMap<>();
   private final Map<UUID, String> fullTexts = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> currentIndexes = new ConcurrentHashMap<>();
   private final TaskScheduler scheduler;

   public TypewriterEffect(UIcorePlugin plugin) {
      this(new EntityTaskScheduler());
   }

   TypewriterEffect(TaskScheduler scheduler) {
      this.scheduler = scheduler;
   }

   public void applyEffect(TextDisplay display, String fullText, int speed, int startDelay, String cursorChar) {
      applyEffectInternal(display, fullText, null, speed, startDelay, cursorChar);
   }

   public void applyEffect(TextDisplay display, Component fullText, int speed, int startDelay, String cursorChar) {
      applyEffectInternal(display, fullText == null ? null : " ".repeat(componentLength(fullText)), fullText, speed, startDelay, cursorChar);
   }

   private void applyEffectInternal(TextDisplay display, String fullText, Component component, int speed, int startDelay, String cursorChar) {
      if (display == null) {
         return;
      }
      UUID displayId = display.getUniqueId();
      cancelEffectInternal(displayId);
      if (!display.isValid() || fullText == null) {
         return;
      }
      if (fullText.isEmpty()) {
         write(display, "", component, 0, "");
         return;
      }

      int safeSpeed = Math.max(1, speed);
      int safeStartDelay = Math.max(0, startDelay);
      String safeCursor = cursorChar == null ? "" : cursorChar;
      write(display, "", component, 0, safeCursor);
      if (component != null) {
         fullComponents.put(displayId, component);
      }
      fullTexts.put(displayId, fullText);
      currentIndexes.put(displayId, 0);

      Runnable task = new Runnable() {
         private boolean cursorVisible = true;
         private long tick;
         private boolean finished;

         @Override
         public void run() {
            if (finished || !display.isValid()) {
               finish();
               return;
            }
            tick++;
            if (tick <= safeStartDelay) {
               if (tick % 10L == 0L) {
                  cursorVisible = !cursorVisible;
                  write(display, "", component, 0, cursorVisible ? safeCursor : "");
               }
               return;
            }

            int currentIndex = currentIndexes.getOrDefault(displayId, 0);
            String text = fullTexts.get(displayId);
            if (text != null && currentIndex < text.length()) {
               if ((tick - safeStartDelay) % safeSpeed == 0L) {
                  currentIndexes.put(displayId, ++currentIndex);
               }
               String displayedText = text.substring(0, Math.min(currentIndex, text.length()));
               cursorVisible = !cursorVisible;
               write(display, displayedText, component, currentIndex, currentIndex >= text.length() && !cursorVisible ? "" : safeCursor);
               return;
            }
            if (text != null) {
               write(display, text, component, text.length(), "");
            }
            finish();
         }

         private void finish() {
            if (!finished) {
               finished = true;
               cancelEffectInternal(displayId);
            }
         }
      };
      boolean[] removed = {false};
      ScheduledTask scheduled = scheduler.scheduleRepeating(display, task, () -> {
         removed[0] = true;
         cancelEffectInternal(displayId);
      });
      if (scheduled != null && !removed[0]) {
         typewriterTasks.put(displayId, scheduled);
      } else {
         if (scheduled != null) {
            scheduler.cancel(scheduled);
         }
         clearEffectState(displayId);
      }
   }

   public void completeEffect(TextDisplay display) {
      if (display != null && display.isValid()) {
         UUID displayId = display.getUniqueId();
         String fullText = fullTexts.get(displayId);
         Component component = fullComponents.get(displayId);
         cancelEffectInternal(displayId);
         if (fullText != null) {
            write(display, fullText, component, fullText.length(), "");
         }
      }
   }

   public void cancelEffect(TextDisplay display) {
      if (display != null) {
         cancelEffect(display.getUniqueId());
      }
   }

   public void cancelEffect(UUID displayId) {
      if (displayId != null) {
         cancelEffectInternal(displayId);
      }
   }

   private void cancelEffectInternal(UUID displayId) {
      ScheduledTask task = typewriterTasks.remove(displayId);
      try {
         if (task != null) {
            scheduler.cancel(task);
         }
      } finally {
         clearEffectState(displayId);
      }
   }

   private void clearEffectState(UUID displayId) {
      fullTexts.remove(displayId);
      fullComponents.remove(displayId);
      currentIndexes.remove(displayId);
   }

   public void cleanupAll() {
      RuntimeException failure = null;
      for (ScheduledTask task : typewriterTasks.values()) {
         try {
            scheduler.cancel(task);
         } catch (RuntimeException exception) {
            if (failure == null) {
               failure = exception;
            }
         }
      }
      typewriterTasks.clear();
      fullTexts.clear();
      fullComponents.clear();
      currentIndexes.clear();
      if (failure != null) {
         throw failure;
      }
   }

   public boolean isRunning(TextDisplay display) {
      return display != null && typewriterTasks.containsKey(display.getUniqueId());
   }

   private static void write(TextDisplay display, String text, Component component, int length, String cursor) {
      if (component == null) {
         display.setText(text + cursor);
      } else {
         display.text(prefix(component, new int[]{length}).append(ColorParser.parse(cursor)));
      }
   }

   static int componentLength(Component component) {
      int count = component instanceof TextComponent text ? text.content().codePointCount(0, text.content().length()) : 1;
      for (Component child : component.children()) {
         count += componentLength(child);
      }
      return count;
   }

   /** Retains styles and custom fonts, revealing complete Unicode code points. */
   static Component prefix(Component component, int[] remaining) {
      if (remaining[0] <= 0) {
         return Component.empty();
      }
      Component result = component.children(java.util.List.of());
      if (component instanceof TextComponent text) {
         int count = Math.min(remaining[0], text.content().codePointCount(0, text.content().length()));
         result = ((TextComponent) result).content(text.content().substring(0, text.content().offsetByCodePoints(0, count)));
         remaining[0] -= count;
      } else {
         remaining[0]--;
      }
      for (Component child : component.children()) {
         if (remaining[0] <= 0) {
            break;
         }
         result = result.append(prefix(child, remaining));
      }
      return result;
   }

   interface TaskScheduler {
      @Nullable ScheduledTask scheduleRepeating(TextDisplay display, Runnable task, Runnable alternateIfRemoved);

      void cancel(ScheduledTask task);
   }

   private static final class EntityTaskScheduler implements TaskScheduler {
      @Override
      public @Nullable ScheduledTask scheduleRepeating(TextDisplay display, Runnable task, Runnable alternateIfRemoved) {
         return UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(task, alternateIfRemoved, 1L, 1L);
      }

      @Override
      public void cancel(ScheduledTask task) {
         task.cancel();
      }
   }
}
