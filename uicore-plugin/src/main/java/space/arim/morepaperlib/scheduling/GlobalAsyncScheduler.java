package space.arim.morepaperlib.scheduling;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class GlobalAsyncScheduler implements AsynchronousScheduler {
   private final AsyncScheduler asyncScheduler;
   private final Plugin plugin;

   GlobalAsyncScheduler(Plugin plugin) {
      this.asyncScheduler = plugin.getServer().getAsyncScheduler();
      this.plugin = plugin;
   }

   void cancelTasks() {
      this.asyncScheduler.cancelTasks(this.plugin);
   }

   @Override
   public void execute(@NotNull Runnable command) {
      this.asyncScheduler.runNow(this.plugin, task -> command.run());
   }

   @Override
   public ScheduledTask run(Runnable command) {
      return new FoliaTask(this.asyncScheduler.runNow(this.plugin, task -> command.run()));
   }

   @Override
   public void run(Consumer<ScheduledTask> command) {
      this.asyncScheduler.runNow(this.plugin, task -> command.accept(new FoliaTask(task)));
   }

   @Override
   public ScheduledTask runDelayed(Runnable command, Duration delay) {
      return new FoliaTask(this.asyncScheduler.runDelayed(this.plugin, task -> command.run(), delay.toNanos(), TimeUnit.NANOSECONDS));
   }

   @Override
   public void runDelayed(Consumer<ScheduledTask> command, Duration delay) {
      this.asyncScheduler.runDelayed(this.plugin, task -> command.accept(new FoliaTask(task)), delay.toNanos(), TimeUnit.NANOSECONDS);
   }

   @Override
   public ScheduledTask runAtFixedRate(Runnable command, Duration initialDelay, Duration period) {
      return new FoliaTask(
         this.asyncScheduler.runAtFixedRate(this.plugin, task -> command.run(), initialDelay.toNanos(), period.toNanos(), TimeUnit.NANOSECONDS)
      );
   }

   @Override
   public void runAtFixedRate(Consumer<ScheduledTask> command, Duration initialDelay, Duration period) {
      this.asyncScheduler
         .runAtFixedRate(this.plugin, task -> command.accept(new FoliaTask(task)), initialDelay.toNanos(), period.toNanos(), TimeUnit.NANOSECONDS);
   }
}
