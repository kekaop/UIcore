package org.orvencraft.uicore.util;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import space.arim.morepaperlib.scheduling.ScheduledTask;

class TypewriterEffectTest {
   @Test void naturalCompletionReleasesTask() {
      FakeScheduler scheduler = new FakeScheduler();
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay display = new FakeDisplay();
      effect.applyEffect(display.proxy, "go", 1, 0, "|");
      scheduler.tick(); scheduler.tick(); scheduler.tick();
      assertEquals("go", display.text);
      assertFalse(effect.isRunning(display.proxy));
      assertEquals(1, scheduler.cancelled);
      assertTrue(scheduler.tasks.isEmpty());
   }
   @Test void manualCompletionShowsFullText() {
      FakeScheduler scheduler = new FakeScheduler();
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay display = new FakeDisplay();
      effect.applyEffect(display.proxy, "complete", 2, 4, "|");
      effect.completeEffect(display.proxy);
      assertEquals("complete", display.text);
      assertFalse(effect.isRunning(display.proxy));
      assertEquals(1, scheduler.cancelled);
   }
   @Test void invalidDisplayReleasesTask() {
      FakeScheduler scheduler = new FakeScheduler();
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay display = new FakeDisplay();
      effect.applyEffect(display.proxy, "gone", 1, 0, "|");
      display.valid = false; scheduler.tick();
      assertFalse(effect.isRunning(display.proxy));
      assertEquals(1, scheduler.cancelled);
   }
   @Test void removedDisplaySchedulerReleasesTask() {
      FakeScheduler scheduler = new FakeScheduler();
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay display = new FakeDisplay();
      effect.applyEffect(display.proxy, "gone", 1, 0, "|");
      scheduler.remove(1);
      assertFalse(effect.isRunning(display.proxy));
      assertTrue(scheduler.tasks.isEmpty());
   }
   @Test void cleanupCancelsEveryTask() {
      FakeScheduler scheduler = new FakeScheduler();
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay first = new FakeDisplay(), second = new FakeDisplay();
      effect.applyEffect(first.proxy, "first", 1, 0, "|");
      effect.applyEffect(second.proxy, "second", 1, 0, "|");
      effect.cleanupAll();
      assertFalse(effect.isRunning(first.proxy));
      assertFalse(effect.isRunning(second.proxy));
      assertEquals(2, scheduler.cancelled);
      assertTrue(scheduler.tasks.isEmpty());
   }
   @Test void cancellationFailureStillReleasesState() {
      FakeScheduler scheduler = new FakeScheduler(); scheduler.failCancellation = true;
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay display = new FakeDisplay();
      effect.applyEffect(display.proxy, "text", 1, 0, "|");
      assertThrows(IllegalStateException.class, () -> effect.cancelEffect(display.proxy));
      assertFalse(effect.isRunning(display.proxy));
   }
   @Test void componentAnimationPreservesFontAndUnicode() {
      FakeScheduler scheduler = new FakeScheduler();
      TypewriterEffect effect = new TypewriterEffect(scheduler);
      FakeDisplay display = new FakeDisplay();
      Key font = Key.key("custom:icons");
      Component component = Component.text("\uD83D\uDE00A").font(font);
      effect.applyEffect(display.proxy, component, 1, 0, ""); scheduler.tick();
      assertEquals("\uD83D\uDE00", ((TextComponent)display.component).content());
      assertEquals(font, display.component.font());
      effect.completeEffect(display.proxy);
      assertEquals(component, display.component);
      assertFalse(effect.isRunning(display.proxy));
   }
   private static final class FakeScheduler implements TypewriterEffect.TaskScheduler {
      final Map<Integer, Runnable> tasks = new LinkedHashMap<>();
      final Map<Integer, Runnable> alternates = new LinkedHashMap<>();
      int nextId, cancelled; boolean failCancellation;
      public ScheduledTask scheduleRepeating(TextDisplay display, Runnable task, Runnable alternateIfRemoved) {
         int id = ++nextId;
         tasks.put(id, task);
         alternates.put(id, alternateIfRemoved);
         return new FakeTask(id);
      }
      public void cancel(ScheduledTask task) {
         cancelled++; tasks.remove(((FakeTask) task).id); alternates.remove(((FakeTask) task).id);
         if (failCancellation) throw new IllegalStateException("simulated cancellation failure");
      }
      void tick() { for (Runnable task : tasks.values().toArray(Runnable[]::new)) task.run(); }
      void remove(int id) { tasks.remove(id); Runnable alternate = alternates.remove(id); if (alternate != null) alternate.run(); }

      private static final class FakeTask implements ScheduledTask {
         private final int id;
         private boolean cancelled;
         private FakeTask(int id) { this.id = id; }
         public Plugin owningPlugin() { return null; }
         public void cancel() { cancelled = true; }
         public boolean isCancelled() { return cancelled; }
      }
   }
   private static final class FakeDisplay {
      final UUID id = UUID.randomUUID(); boolean valid = true; String text; Component component;
      final TextDisplay proxy = (TextDisplay)Proxy.newProxyInstance(TextDisplay.class.getClassLoader(), new Class<?>[]{TextDisplay.class},
         (ignored, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> id;
            case "isValid" -> valid;
            case "setText" -> { text = (String)args[0]; yield null; }
            case "text" -> { if (args == null || args.length == 0) yield component; component = (Component)args[0]; yield null; }
            case "toString" -> "FakeDisplay";
            default -> throw new UnsupportedOperationException(method.getName());
         });
   }
}
