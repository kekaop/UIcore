package space.arim.morepaperlib.scheduling;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

final class GlobalScheduler implements RegionalScheduler {
   private final GlobalRegionScheduler globalRegionScheduler;
   private final Plugin plugin;

   GlobalScheduler(Plugin plugin) {
      this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
      this.plugin = plugin;
   }

   void cancelTasks() {
      this.globalRegionScheduler.cancelTasks(this.plugin);
   }

   @Override
   public ScheduledTask run(Runnable command) {
      return new FoliaTask(this.globalRegionScheduler.run(this.plugin, task -> command.run()));
   }

   @Override
   public void run(Consumer<ScheduledTask> command) {
      this.globalRegionScheduler.run(this.plugin, task -> command.accept(new FoliaTask(task)));
   }

   @Override
   public ScheduledTask runDelayed(Runnable command, long delay) {
      return new FoliaTask(this.globalRegionScheduler.runDelayed(this.plugin, task -> command.run(), delay));
   }

   @Override
   public void runDelayed(Consumer<ScheduledTask> command, long delay) {
      this.globalRegionScheduler.runDelayed(this.plugin, task -> command.accept(new FoliaTask(task)), delay);
   }

   @Override
   public ScheduledTask runAtFixedRate(Runnable command, long initialDelay, long period) {
      return new FoliaTask(this.globalRegionScheduler.runAtFixedRate(this.plugin, task -> command.run(), initialDelay, period));
   }

   @Override
   public void runAtFixedRate(Consumer<ScheduledTask> command, long initialDelay, long period) {
      this.globalRegionScheduler.runAtFixedRate(this.plugin, task -> command.accept(new FoliaTask(task)), initialDelay, period);
   }
}
