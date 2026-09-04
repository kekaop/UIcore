package space.arim.morepaperlib.scheduling;

import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import space.arim.morepaperlib.MorePaperLib;

final class BukkitSchedulerAsRegionalScheduler implements RegionalScheduler {
   private final BukkitScheduler scheduler;
   private final Plugin plugin;
   private final boolean hasMethodRunTask;
   private final boolean hasMethodRunTaskLater;
   private final boolean hasMethodRunTaskTimer;

   BukkitSchedulerAsRegionalScheduler(MorePaperLib morePaperLib) {
      Plugin plugin = morePaperLib.getPlugin();
      this.scheduler = plugin.getServer().getScheduler();
      this.plugin = plugin;
      this.hasMethodRunTask = morePaperLib.methodExists(BukkitScheduler.class, "runTask", Plugin.class, Consumer.class);
      this.hasMethodRunTaskLater = morePaperLib.methodExists(BukkitScheduler.class, "runTaskLater", Plugin.class, Consumer.class, long.class);
      this.hasMethodRunTaskTimer = morePaperLib.methodExists(BukkitScheduler.class, "runTaskTimer", Plugin.class, Consumer.class, long.class, long.class);
   }

   void cancelTasks() {
      this.scheduler.cancelTasks(this.plugin);
   }

   @Override
   public ScheduledTask run(Runnable command) {
      return new PaperTask(this.scheduler.runTask(this.plugin, command));
   }

   @Override
   public void run(Consumer<ScheduledTask> command) {
      if (this.hasMethodRunTask) {
         this.scheduler.runTask(this.plugin, bukkitTask -> command.accept(new PaperTask(bukkitTask)));
      } else {
         BukkitTaskConsumerToRunnable.setup(command, runnable -> this.scheduler.runTask(this.plugin, runnable));
      }
   }

   @Override
   public ScheduledTask runDelayed(Runnable command, long delay) {
      return new PaperTask(this.scheduler.runTaskLater(this.plugin, command, delay));
   }

   @Override
   public void runDelayed(Consumer<ScheduledTask> command, long delay) {
      if (this.hasMethodRunTaskLater) {
         this.scheduler.runTaskLater(this.plugin, bukkitTask -> command.accept(new PaperTask(bukkitTask)), delay);
      } else {
         BukkitTaskConsumerToRunnable.setup(command, runnable -> this.scheduler.runTaskLater(this.plugin, runnable, delay));
      }
   }

   @Override
   public ScheduledTask runAtFixedRate(Runnable command, long initialDelay, long period) {
      return new PaperTask(this.scheduler.runTaskTimer(this.plugin, command, initialDelay, period));
   }

   @Override
   public void runAtFixedRate(Consumer<ScheduledTask> command, long initialDelay, long period) {
      if (this.hasMethodRunTaskTimer) {
         this.scheduler.runTaskTimer(this.plugin, bukkitTask -> command.accept(new PaperTask(bukkitTask)), initialDelay, period);
      } else {
         BukkitTaskConsumerToRunnable.setup(command, runnable -> this.scheduler.runTaskTimer(this.plugin, runnable, initialDelay, period));
      }
   }
}
