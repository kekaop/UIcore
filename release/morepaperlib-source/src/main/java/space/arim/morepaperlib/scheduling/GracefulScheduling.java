package space.arim.morepaperlib.scheduling;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import space.arim.morepaperlib.MorePaperLib;

public class GracefulScheduling {
   private final MorePaperLib morePaperLib;
   private final FoliaDetection foliaDetection;

   public GracefulScheduling(MorePaperLib morePaperLib, FoliaDetection foliaDetection) {
      this.morePaperLib = morePaperLib;
      this.foliaDetection = foliaDetection;
   }

   public GracefulScheduling(MorePaperLib morePaperLib) {
      this(morePaperLib, StandardFoliaDetection.INSTANCE);
   }

   public boolean isUsingFolia() {
      return this.foliaDetection.isUsingFolia();
   }

   public AsynchronousScheduler asyncScheduler() {
      return (AsynchronousScheduler)(this.isUsingFolia()
         ? new GlobalAsyncScheduler(this.morePaperLib.getPlugin())
         : new BukkitSchedulerAsAsynchronousScheduler(this.morePaperLib));
   }

   public RegionalScheduler globalRegionalScheduler() {
      return (RegionalScheduler)(this.isUsingFolia()
         ? new GlobalScheduler(this.morePaperLib.getPlugin())
         : new BukkitSchedulerAsRegionalScheduler(this.morePaperLib));
   }

   public RegionalScheduler regionSpecificScheduler(Location location) {
      return this.regionSpecificScheduler(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
   }

   public RegionalScheduler regionSpecificScheduler(World world, int chunkX, int chunkZ) {
      return (RegionalScheduler)(this.isUsingFolia()
         ? new RegionSpecificScheduler(this.morePaperLib.getPlugin(), world, chunkX, chunkZ)
         : new BukkitSchedulerAsRegionalScheduler(this.morePaperLib));
   }

   public AttachedScheduler entitySpecificScheduler(Entity entity) {
      return (AttachedScheduler)(this.isUsingFolia()
         ? new EntitySpecificScheduler(entity, this.morePaperLib.getPlugin())
         : new BukkitSchedulerAsRegionalScheduler(this.morePaperLib).asAttachedScheduler());
   }

   public boolean isOnGlobalRegionThread() {
      Server server = this.morePaperLib.getPlugin().getServer();
      if (!this.isUsingFolia()) return server.isPrimaryThread();
      try {
         return (boolean) server.getClass().getMethod("isGlobalTickThread").invoke(server);
      } catch (ReflectiveOperationException ignored) {
         return server.isPrimaryThread();
      }
   }

   public void cancelGlobalTasks() {
      if (this.isUsingFolia()) {
         new GlobalAsyncScheduler(this.morePaperLib.getPlugin()).cancelTasks();
         new GlobalScheduler(this.morePaperLib.getPlugin()).cancelTasks();
      } else {
         new BukkitSchedulerAsRegionalScheduler(this.morePaperLib).cancelTasks();
      }
   }
}
