package space.arim.morepaperlib.scheduling;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import java.util.function.Consumer;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

final class EntitySpecificScheduler implements AttachedScheduler {
   private final EntityScheduler entityScheduler;
   private final Plugin plugin;

   EntitySpecificScheduler(Entity entity, Plugin plugin) {
      this.entityScheduler = entity.getScheduler();
      this.plugin = plugin;
   }

   @Nullable
   @Override
   public ScheduledTask run(Runnable command, Runnable alternateIfRemoved) {
      return FoliaTask.ofNullable(this.entityScheduler.run(this.plugin, task -> command.run(), alternateIfRemoved));
   }

   @Override
   public boolean run(Consumer<ScheduledTask> command, Runnable alternateIfRemoved) {
      return this.entityScheduler.run(this.plugin, task -> command.accept(new FoliaTask(task)), alternateIfRemoved) != null;
   }

   @Nullable
   @Override
   public ScheduledTask runDelayed(Runnable command, Runnable alternateIfRemoved, long delay) {
      return FoliaTask.ofNullable(this.entityScheduler.runDelayed(this.plugin, task -> command.run(), alternateIfRemoved, delay));
   }

   @Override
   public boolean runDelayed(Consumer<ScheduledTask> command, Runnable alternateIfRemoved, long delay) {
      return this.entityScheduler.runDelayed(this.plugin, task -> command.accept(new FoliaTask(task)), alternateIfRemoved, delay) != null;
   }

   @Nullable
   @Override
   public ScheduledTask runAtFixedRate(Runnable command, Runnable alternateIfRemoved, long initialDelay, long period) {
      return FoliaTask.ofNullable(this.entityScheduler.runAtFixedRate(this.plugin, task -> command.run(), alternateIfRemoved, initialDelay, period));
   }

   @Override
   public boolean runAtFixedRate(Consumer<ScheduledTask> command, Runnable alternateIfRemoved, long initialDelay, long period) {
      return this.entityScheduler.runAtFixedRate(this.plugin, task -> command.accept(new FoliaTask(task)), alternateIfRemoved, initialDelay, period) != null;
   }
}
