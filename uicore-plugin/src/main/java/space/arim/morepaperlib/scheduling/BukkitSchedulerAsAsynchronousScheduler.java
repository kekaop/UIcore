package space.arim.morepaperlib.scheduling;

import java.time.Duration;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.MorePaperLib;

final class BukkitSchedulerAsAsynchronousScheduler implements AsynchronousScheduler {
   private final BukkitScheduler scheduler;
   private final Plugin plugin;
   private final boolean hasMethodRunTask;
   private final boolean hasMethodRunTaskLater;
   private final boolean hasMethodRunTaskTimer;

   BukkitSchedulerAsAsynchronousScheduler(MorePaperLib morePaperLib) {
      Plugin plugin = morePaperLib.getPlugin();
      this.scheduler = plugin.getServer().getScheduler();
      this.plugin = plugin;
      this.hasMethodRunTask = morePaperLib.methodExists(BukkitScheduler.class, "runTaskAsynchronously", Plugin.class, Consumer.class);
      this.hasMethodRunTaskLater = morePaperLib.methodExists(BukkitScheduler.class, "runTaskLaterAsynchronously", Plugin.class, Consumer.class, long.class);
      this.hasMethodRunTaskTimer = morePaperLib.methodExists(
         BukkitScheduler.class, "runTaskTimerAsynchronously", Plugin.class, Consumer.class, long.class, long.class
      );
   }

   void cancelTasks() {
      this.scheduler.cancelTasks(this.plugin);
   }

   private static long toTicks(Duration duration) {
      return duration.toMillis() / 50L;
   }

   @Override
   public void execute(@NotNull Runnable command) {
      this.scheduler.runTaskAsynchronously(this.plugin, command);
   }

   @Override
   public ScheduledTask run(Runnable command) {
      return new PaperTask(this.scheduler.runTaskAsynchronously(this.plugin, command));
   }

   @Override
   public void run(Consumer<ScheduledTask> command) {
      if (this.hasMethodRunTask) {
         this.scheduler.runTaskAsynchronously(this.plugin, bukkitTask -> command.accept(new PaperTask(bukkitTask)));
      } else {
         BukkitTaskConsumerToRunnable.setup(command, runnable -> this.scheduler.runTaskAsynchronously(this.plugin, runnable));
      }
   }

   @Override
   public ScheduledTask runDelayed(Runnable command, Duration delay) {
      return new PaperTask(this.scheduler.runTaskLaterAsynchronously(this.plugin, command, toTicks(delay)));
   }

   @Override
   public void runDelayed(Consumer<ScheduledTask> command, Duration delay) {
      long delayInTicks = toTicks(delay);
      if (this.hasMethodRunTaskLater) {
         this.scheduler.runTaskLaterAsynchronously(this.plugin, bukkitTask -> command.accept(new PaperTask(bukkitTask)), delayInTicks);
      } else {
         BukkitTaskConsumerToRunnable.setup(command, runnable -> this.scheduler.runTaskLaterAsynchronously(this.plugin, runnable, delayInTicks));
      }
   }

   @Override
   public ScheduledTask runAtFixedRate(Runnable command, Duration initialDelay, Duration period) {
      return new PaperTask(this.scheduler.runTaskTimerAsynchronously(this.plugin, command, toTicks(initialDelay), toTicks(period)));
   }

   @Override
   public void runAtFixedRate(Consumer<ScheduledTask> command, Duration initialDelay, Duration period) {
      long initialDelayInTicks = toTicks(initialDelay);
      long periodInTicks = toTicks(period);
      if (this.hasMethodRunTaskTimer) {
         this.scheduler.runTaskTimerAsynchronously(this.plugin, bukkitTask -> command.accept(new PaperTask(bukkitTask)), initialDelayInTicks, periodInTicks);
      } else {
         BukkitTaskConsumerToRunnable.setup(
            command, runnable -> this.scheduler.runTaskTimerAsynchronously(this.plugin, runnable, initialDelayInTicks, periodInTicks)
         );
      }
   }
}
