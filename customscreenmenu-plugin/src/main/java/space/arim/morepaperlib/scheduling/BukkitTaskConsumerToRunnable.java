package space.arim.morepaperlib.scheduling;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.scheduler.BukkitTask;

final class BukkitTaskConsumerToRunnable implements Runnable {
   private final CompletableFuture<BukkitTask> taskHolder;
   private final Consumer<ScheduledTask> command;

   BukkitTaskConsumerToRunnable(CompletableFuture<BukkitTask> taskHolder, Consumer<ScheduledTask> command) {
      this.taskHolder = taskHolder;
      this.command = command;
   }

   static void setup(Consumer<ScheduledTask> command, Function<Runnable, BukkitTask> taskSubmitter) {
      CompletableFuture<BukkitTask> futureTask = new CompletableFuture<>();
      BukkitTask task = null;

      try {
         task = taskSubmitter.apply(new BukkitTaskConsumerToRunnable(futureTask, command));
      } finally {
         futureTask.complete(task);
      }
   }

   @Override
   public void run() {
      BukkitTask task = this.taskHolder.join();
      this.command.accept(new PaperTask(task));
   }
}
