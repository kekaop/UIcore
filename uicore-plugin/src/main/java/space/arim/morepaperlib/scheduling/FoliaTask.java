package space.arim.morepaperlib.scheduling;

import org.bukkit.plugin.Plugin;

class FoliaTask implements ScheduledTask {
   private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

   FoliaTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
      this.task = task;
   }

   static FoliaTask ofNullable(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
      return task == null ? null : new FoliaTask(task);
   }

   @Override
   public Plugin owningPlugin() {
      return this.task.getOwningPlugin();
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
         FoliaTask foliaTask = (FoliaTask)o;
         return this.task.equals(foliaTask.task);
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
      return "FoliaTask{task=" + this.task + '}';
   }
}
