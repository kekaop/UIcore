package org.orvencraft.uicore.scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.section.Section;
import space.arim.morepaperlib.scheduling.ScheduledTask;

/**
 * Holds chunk tickets only while at least one active menu viewer needs them.
 * Viewer reconciliation runs on that viewer's entity scheduler; ticket changes
 * run on the scheduler for the affected chunk's owning region.
 */
public class ChunkLoaderManager {
   private final UIcorePlugin plugin;
   private final Map<UUID, Set<ChunkReference>> viewerTickets = new ConcurrentHashMap<>();
   private final Map<ChunkReference, Set<UUID>> viewersByChunk = new ConcurrentHashMap<>();
   private volatile ScheduledTask reconcileTask;
   private volatile boolean isTaskRunning;

   public ChunkLoaderManager(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   public void startChunkLoaderTask() {
      this.stopChunkLoaderTask();
      this.isTaskRunning = true;
      this.reconcileActiveViewers();
      this.reconcileTask = UIcorePlugin.foliaLib.scheduling().globalRegionalScheduler()
         .runAtFixedRate(this::reconcileActiveViewers, 20L, 20L);
      if (this.plugin.getConfig().getBoolean("Debug", false)) this.plugin.getLogger().info("Chunk loading task started.");
   }

   public void stopChunkLoaderTask() {
      ScheduledTask task = this.reconcileTask;
      this.reconcileTask = null;
      if (task != null) task.cancel();
      this.isTaskRunning = false;

      Set<ChunkReference> ticketed = Set.copyOf(this.viewersByChunk.keySet());
      this.viewerTickets.clear();
      this.viewersByChunk.clear();
      // Paper's plugin manager removes all of this plugin's tickets after onDisable.
      // Scheduling here is only needed when stopping/reloading an enabled plugin.
      if (this.plugin.isEnabled()) ticketed.forEach(this::scheduleTicketRemoval);
      if (this.plugin.getConfig().getBoolean("Debug", false)) this.plugin.getLogger().info("Chunk loading task stopped.");
   }

   /**
    * Does not touch worlds: it only dispatches each viewer's own reconciliation
    * onto that player's entity scheduler.
    */
   public void updatePersistentChunks() {
      this.reconcileActiveViewers();
   }

   private void reconcileActiveViewers() {
      if (!this.isTaskRunning) return;
      Set<UUID> activeViewers = new HashSet<>();
      for (Player player : this.plugin.getActiveMenuPlayers()) {
         activeViewers.add(player.getUniqueId());
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player)
            .run(() -> this.reconcileViewer(player), () -> this.releaseViewer(player.getUniqueId()));
      }
      for (UUID previousViewer : Set.copyOf(this.viewerTickets.keySet())) {
         if (!activeViewers.contains(previousViewer)) this.releaseViewer(previousViewer);
      }
   }

   private void reconcileViewer(Player player) {
      if (!this.isTaskRunning || !player.isOnline()) {
         this.releaseViewer(player.getUniqueId());
         return;
      }
      Set<ChunkReference> required = this.requiredChunks(player);
      UUID playerId = player.getUniqueId();
      Set<ChunkReference> previous = this.viewerTickets.put(playerId, required);
      if (previous == null) previous = Set.of();

      for (ChunkReference reference : previous) if (!required.contains(reference)) this.release(reference, playerId);
      for (ChunkReference reference : required) if (!previous.contains(reference)) this.acquire(reference, playerId);
   }

   private Set<ChunkReference> requiredChunks(Player player) {
      String menuKey = this.plugin.getCurrentPlayerMenu(player);
      Section section = this.plugin.getRuntimeSection(player, menuKey);
      if (section == null || section.world == null) return Set.of();
      Set<ChunkReference> required = new HashSet<>();
      addChunk(required, section.world, section.cameraX, section.cameraZ);
      if (section.definition().layouts() != null) {
         for (MenuLayout layout : section.definition().layouts().values()) {
            Location location = layoutLocation(section, layout.x, layout.y, layout.z);
            addChunk(required, section.world, location.getX(), location.getZ());
         }
      }
      return Set.copyOf(required);
   }

   private void acquire(ChunkReference reference, UUID playerId) {
      Set<UUID> viewers = viewersByChunk.computeIfAbsent(reference, ignored -> ConcurrentHashMap.newKeySet());
      boolean wasEmpty = viewers.isEmpty();
      viewers.add(playerId);
      if (wasEmpty) this.scheduleTicketAddition(reference);
   }

   private void release(ChunkReference reference, UUID playerId) {
      Set<UUID> viewers = viewersByChunk.get(reference);
      if (viewers == null || !viewers.remove(playerId) || !viewers.isEmpty()) return;
      if (viewersByChunk.remove(reference, viewers)) this.scheduleTicketRemoval(reference);
   }

   private void releaseViewer(UUID playerId) {
      Set<ChunkReference> references = this.viewerTickets.remove(playerId);
      if (references != null) references.forEach(reference -> this.release(reference, playerId));
   }

   private void scheduleTicketAddition(ChunkReference reference) {
      if (!this.plugin.isEnabled()) return;
      World world = Bukkit.getWorld(reference.worldName());
      if (world == null) return;
      CleanupTasks.region(this.plugin, new Location(world, reference.chunkX() * 16.0, 0, reference.chunkZ() * 16.0), () -> {
         if (!this.plugin.isEnabled() || !this.viewersByChunk.containsKey(reference)) return;
         world.getChunkAt(reference.chunkX(), reference.chunkZ()).addPluginChunkTicket(this.plugin);
      });
   }

   private void scheduleTicketRemoval(ChunkReference reference) {
      if (!this.plugin.isEnabled()) return;
      World world = Bukkit.getWorld(reference.worldName());
      if (world == null) return;
      CleanupTasks.region(this.plugin, new Location(world, reference.chunkX() * 16.0, 0, reference.chunkZ() * 16.0), () -> {
         if (this.viewersByChunk.containsKey(reference) || !world.isChunkLoaded(reference.chunkX(), reference.chunkZ())) return;
         world.getChunkAt(reference.chunkX(), reference.chunkZ()).removePluginChunkTicket(this.plugin);
      });
   }

   public int getLoadedChunkCount() {
      return this.viewersByChunk.size();
   }

   public int getLoadedWorldCount() {
      return (int) this.viewersByChunk.keySet().stream().map(ChunkReference::worldName).distinct().count();
   }

   public boolean isTaskRunning() {
      return this.isTaskRunning;
   }

   private static void addChunk(Set<ChunkReference> chunks, String worldName, double x, double z) {
      chunks.add(new ChunkReference(worldName, ((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4));
   }

   static Location layoutLocation(Section section, double x, double y, double z) {
      Location base = new Location(null, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
      Vector forward = base.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX());
      if (right.lengthSquared() < 1.0E-8D) {
         double yawRadians = Math.toRadians(section.yaw);
         right = new Vector(-Math.cos(yawRadians), 0.0D, -Math.sin(yawRadians));
      }
      right.normalize();
      Vector up = forward.getCrossProduct(right).multiply(-1.0D).normalize();
      return base.add(forward.multiply(z)).add(right.multiply(x)).add(up.multiply(y));
   }

   private record ChunkReference(String worldName, int chunkX, int chunkZ) { }
}
