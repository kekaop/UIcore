package org.orvencraft.uicore.schematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.section.Section;

/** Per-viewer block previews; never writes to the world. */
public final class SchematicPreviewManager {
   private static final int MAX_MENU_BLOCKS = 65_536;
   private static final int PACKET_BATCH = 512;
   private final UIcorePlugin plugin;
   private final Map<UUID, ActivePreview> active = new ConcurrentHashMap<>();
   private volatile Map<String, PreparedSchematic> loaded = Map.of();

   public SchematicPreviewManager(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   public void reload(Iterable<Section> sections) {
      Path folder = plugin.getDataFolder().toPath().resolve("schematics");
      try { Files.createDirectories(folder); }
      catch (IOException exception) { plugin.getLogger().warning("Cannot create schematics folder: " + exception.getMessage()); return; }
      Set<String> references = new HashSet<>();
      for (Section section : sections) for (SchematicPlacement placement : section.schematics) references.add(placement.file());
      Map<String, PreparedSchematic> next = new HashMap<>();
      for (String fileName : references) {
         try {
            Path file = folder.resolve(fileName).toRealPath();
            if (!file.getParent().equals(folder.toRealPath()) || !Files.isRegularFile(file)) {
               throw new IOException("File must be inside the schematics folder");
            }
            SpongeSchematic schematic = SpongeSchematicReader.read(file);
            Map<Integer, BlockData> palette = new HashMap<>();
            for (Map.Entry<Integer, String> entry : schematic.palette().entrySet()) {
               try { palette.put(entry.getKey(), Bukkit.createBlockData(entry.getValue())); }
               catch (IllegalArgumentException exception) {
                  throw new IOException("Unsupported block state " + entry.getValue(), exception);
               }
            }
            next.put(fileName, new PreparedSchematic(schematic, Map.copyOf(palette)));
         } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning("Cannot load schematic '" + fileName + "': " + exception.getMessage());
         }
      }
      loaded = Map.copyOf(next);
   }

   public void show(Player player, Section section) {
      if (player == null || !player.isOnline()) return;
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> show(player, section), null);
         return;
      }
      clear(player);
      if (section == null || section.schematics.isEmpty()) return;
      World world = Bukkit.getWorld(section.world);
      if (world == null || !world.equals(player.getWorld())) return;
      Map<Location, BlockData> changes = new LinkedHashMap<>();
      int processed = 0;
      try {
         for (SchematicPlacement placement : section.schematics) {
            PreparedSchematic prepared = loaded.get(placement.file());
            if (prepared == null) continue;
            SpongeSchematic schematic = prepared.schematic();
            int originX = Math.addExact(placement.anchor() == SchematicPlacement.Anchor.CAMERA ? (int) Math.floor(section.cameraX) : 0, placement.x());
            int originY = Math.addExact(placement.anchor() == SchematicPlacement.Anchor.CAMERA ? (int) Math.floor(section.cameraY) : 0, placement.y());
            int originZ = Math.addExact(placement.anchor() == SchematicPlacement.Anchor.CAMERA ? (int) Math.floor(section.cameraZ) : 0, placement.z());
            for (int index = 0; index < schematic.blockCount(); index++) {
               if (++processed > MAX_MENU_BLOCKS) throw new IllegalArgumentException("Menu schematic preview exceeds " + MAX_MENU_BLOCKS + " blocks");
               SpongeSchematic.Block block = schematic.at(index);
               int x = Math.addExact(originX, block.x());
               int y = Math.addExact(originY, block.y());
               int z = Math.addExact(originZ, block.z());
               if (Math.abs((long) x) > 29_999_984L || Math.abs((long) z) > 29_999_984L
                  || y < world.getMinHeight() || y >= world.getMaxHeight()) {
                  throw new IllegalArgumentException("Schematic extends outside the world bounds");
               }
               changes.put(new Location(world, x, y, z), prepared.palette().get(schematic.blockIds()[index]));
            }
         }
      } catch (ArithmeticException | IllegalArgumentException exception) {
         plugin.getLogger().warning("Cannot show schematic for menu '" + section.key + "': " + exception.getMessage());
         return;
      }
      if (changes.isEmpty()) return;
      ActivePreview preview = new ActivePreview(world.getUID(), Set.copyOf(changes.keySet()));
      active.put(player.getUniqueId(), preview);
      sendBatches(player, preview, new ArrayList<>(changes.entrySet()), 0);
   }

   private void sendBatches(Player player, ActivePreview preview, List<Map.Entry<Location, BlockData>> entries, int index) {
      if (!player.isOnline() || active.get(player.getUniqueId()) != preview || !player.getWorld().getUID().equals(preview.worldId())) return;
      int end = Math.min(entries.size(), index + PACKET_BATCH);
      Map<Location, BlockData> batch = new LinkedHashMap<>();
      for (int i = index; i < end; i++) batch.put(entries.get(i).getKey(), entries.get(i).getValue());
      player.sendMultiBlockChange(batch);
      if (end < entries.size()) {
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player)
            .runDelayed(ignored -> sendBatches(player, preview, entries, end), null, 1L);
      }
   }

   public boolean contains(UUID playerId, Location location) {
      ActivePreview preview = active.get(playerId);
      return preview != null && preview.positions().contains(location);
   }

   public void clear(Player player) {
      if (player == null) return;
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> clear(player), null);
         return;
      }
      ActivePreview previous = active.remove(player.getUniqueId());
      if (previous == null || !player.isOnline()) return;
      if (!plugin.isEnabled()) {
         // Paper can restore packets immediately. Never read another Folia region during disable.
         Map<Location, BlockData> restored = new LinkedHashMap<>();
         for (Location location : previous.positions()) {
            if (location.getWorld() != null && location.getWorld().equals(player.getWorld())
               && Bukkit.isOwnedByCurrentRegion(location)
               && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
               && !plugin.isClientHiddenBlock(player.getUniqueId(), location)) {
               restored.put(location, location.getBlock().getBlockData());
               if (restored.size() >= PACKET_BATCH) {
                  player.sendMultiBlockChange(restored);
                  restored.clear();
               }
            }
         }
         if (!restored.isEmpty()) player.sendMultiBlockChange(restored);
         return;
      }
      Map<Long, List<Location>> byChunk = new HashMap<>();
      for (Location location : previous.positions()) {
         long key = ((long) (location.getBlockX() >> 4) << 32) ^ ((location.getBlockZ() >> 4) & 0xffffffffL);
         byChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(location);
      }
      List<CompletableFuture<Map<Location, BlockData>>> snapshots = new ArrayList<>();
      for (List<Location> positions : byChunk.values()) {
         CompletableFuture<Map<Location, BlockData>> snapshot = new CompletableFuture<>();
         snapshots.add(snapshot);
         UIcorePlugin.foliaLib.scheduling().regionSpecificScheduler(positions.get(0)).run(() -> {
            World world = positions.get(0).getWorld();
            if (world == null || !world.isChunkLoaded(positions.get(0).getBlockX() >> 4, positions.get(0).getBlockZ() >> 4)) {
               snapshot.complete(Map.of());
               return;
            }
            Map<Location, BlockData> current = new LinkedHashMap<>();
            for (Location location : positions) current.put(location, location.getBlock().getBlockData());
            snapshot.complete(current);
         });
      }
      CompletableFuture.allOf(snapshots.toArray(new CompletableFuture[0])).whenComplete((ignored, failure) -> {
         if (failure != null) return;
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
            if (!player.isOnline() || !player.getWorld().getUID().equals(previous.worldId())) return;
            List<Map.Entry<Location, BlockData>> entries = new ArrayList<>();
            for (CompletableFuture<Map<Location, BlockData>> snapshot : snapshots) entries.addAll(snapshot.join().entrySet());
            sendRestorationBatches(player, previous.worldId(), entries, 0);
         }, null);
      });
   }

   private void sendRestorationBatches(Player player, UUID worldId, List<Map.Entry<Location, BlockData>> entries, int index) {
      if (!player.isOnline() || !player.getWorld().getUID().equals(worldId)) return;
      int end = Math.min(entries.size(), index + PACKET_BATCH);
      Map<Location, BlockData> batch = new LinkedHashMap<>();
      for (int i = index; i < end; i++) {
         Map.Entry<Location, BlockData> entry = entries.get(i);
         if (!contains(player.getUniqueId(), entry.getKey())
            && !plugin.isClientHiddenBlock(player.getUniqueId(), entry.getKey()))
            batch.put(entry.getKey(), entry.getValue());
      }
      if (!batch.isEmpty()) player.sendMultiBlockChange(batch);
      if (end < entries.size()) {
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player)
            .runDelayed(ignored -> sendRestorationBatches(player, worldId, entries, end), null, 1L);
      }
   }

   public void discard(Player player) {
      if (player != null) active.remove(player.getUniqueId());
   }

   private record PreparedSchematic(SpongeSchematic schematic, Map<Integer, BlockData> palette) {}
   private record ActivePreview(UUID worldId, Set<Location> positions) {}
}
