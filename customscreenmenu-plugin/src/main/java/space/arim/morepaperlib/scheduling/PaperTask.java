package space.arim.morepaperlib.scheduling;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

class PaperTask implements ScheduledTask {
   private final BukkitTask task;

   PaperTask(BukkitTask task) {
      this.task = task;
   }

   @Override
   public Plugin owningPlugin() {
      return this.task.getOwner();
   }

   @Override
   public void cancel() {
      this.task.cancel();
   }

   @Override
   public boolean isCancelled() {
      return this.task.isCancelled();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         PaperTask paperTask = (PaperTask)o;
         return this.task.equals(paperTask.task);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.task.hashCode();
   }

   @Override
   public String toString() {
      return "PaperTask{task=" + this.task + '}';
   }
}
