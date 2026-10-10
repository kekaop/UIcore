package org.orvencraft.uicore.session;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Recovers journals written by pre-1.4.8 camera block clearing. Those journals only contain
 * block-data strings, so this class deliberately refuses to overwrite any new world state.
 */
public final class LegacyBlockCacheRecovery {
   private static final Pattern COORDINATES = Pattern.compile("^(-?\\d+),(-?\\d+),(-?\\d+)$");
   private final BlockLookup blocks;
   private final BlockDataCodec blockData;
   private final Consumer<String> warning;
   private final RegionExecutor regions;
   private final Executor persistence;

   public LegacyBlockCacheRecovery(BlockLookup blocks, BlockDataCodec blockData, Consumer<String> warning) {
      this(blocks, blockData, warning, (world, x, z, work) -> {
         work.run();
         return CompletableFuture.completedFuture(null);
      }, Runnable::run);
   }

   public LegacyBlockCacheRecovery(BlockLookup blocks, BlockDataCodec blockData, Consumer<String> warning,
                                   RegionExecutor regions, Executor persistence) {
      this.blocks = Objects.requireNonNull(blocks, "blocks");
      this.blockData = Objects.requireNonNull(blockData, "blockData");
      this.warning = warning == null ? ignored -> { } : warning;
      this.regions = Objects.requireNonNull(regions, "regions");
      this.persistence = Objects.requireNonNull(persistence, "persistence");
   }

   /** Schedules world access on each block's owning region without blocking plugin startup. */
   public static CompletableFuture<RecoveryResult> recoverAll(Plugin plugin) {
      RegionExecutor regions = (worldName, x, z, work) -> {
         var world = Bukkit.getWorld(worldName);
         if (world == null) return CompletableFuture.failedFuture(new IllegalStateException("World is unavailable: " + worldName));
         CompletableFuture<Void> completion = new CompletableFuture<>();
         try {
            Bukkit.getRegionScheduler().execute(plugin, world, x >> 4, z >> 4, () -> {
               try { work.run(); completion.complete(null); }
               catch (RuntimeException exception) { completion.completeExceptionally(exception); }
            });
         } catch (RuntimeException exception) { completion.completeExceptionally(exception); }
         return completion;
      };
      Executor persistence = work -> Bukkit.getAsyncScheduler().runNow(plugin, task -> work.run());
      return new LegacyBlockCacheRecovery(new BukkitBlockLookup(), new BukkitBlockDataCodec(), plugin.getLogger()::warning, regions, persistence)
         .recoverAllAsync(plugin.getDataFolder().toPath());
   }

   /** Synchronous facade for inline lookups; production uses recoverAll(Plugin). */
   public RecoveryResult recoverAll(Path dataFolder) {
      return recoverAllAsync(dataFolder).join();
   }

   public CompletableFuture<RecoveryResult> recoverAllAsync(Path dataFolder) {
      if (dataFolder == null || !Files.isDirectory(dataFolder.resolve("blockcache"))) {
         return CompletableFuture.completedFuture(new Counts().result());
      }
      List<CompletableFuture<RecoveryResult>> journals = new ArrayList<>();
      try (var files = Files.list(dataFolder.resolve("blockcache"))) {
         files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".yml"))
            .forEach(path -> journals.add(recoverJournal(path)));
      } catch (IOException exception) {
         this.warning.accept("[UIcore] Could not list legacy blockcache journals: " + exception.getMessage());
      }
      return CompletableFuture.allOf(journals.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
         Counts counts = new Counts();
         for (var journal : journals) counts.add(journal.join());
         return counts.result();
      });
   }

   private CompletableFuture<RecoveryResult> recoverJournal(Path journal) {
      Counts counts = new Counts();
      counts.journals++;
      YamlConfiguration config = new YamlConfiguration();
      try {
         config.load(journal.toFile());
      } catch (IOException | InvalidConfigurationException exception) {
         counts.invalid++;
         this.warning.accept("[UIcore] Preserved malformed legacy blockcache journal " + journal.getFileName() + ": " + exception.getMessage());
         return CompletableFuture.completedFuture(counts.result());
      }

      List<CompletableFuture<Outcome>> entries = new ArrayList<>();
      for (String key : config.getKeys(false)) {
         Entry entry = parseEntry(config, key, journal);
         if (entry == null) { counts.invalid++; continue; }
         CompletableFuture<Kind> outcome = new CompletableFuture<>();
         try {
            this.regions.execute(entry.world(), entry.x(), entry.z(), () -> outcome.complete(recoverEntry(entry, key, journal)))
               .whenComplete((ignored, failure) -> {
                  if (failure != null) {
                     this.warning.accept("[UIcore] Preserved legacy blockcache entry that could not be scheduled: " + key + " (" + failure.getMessage() + ")");
                     outcome.complete(Kind.UNRESOLVED);
                  }
               });
         } catch (RuntimeException exception) {
            outcome.complete(Kind.UNRESOLVED);
            this.warning.accept("[UIcore] Preserved legacy blockcache entry that could not be scheduled: " + key + " (" + exception.getMessage() + ")");
         }
         entries.add(outcome.thenApply(kind -> new Outcome(key, kind)));
      }

      // Only this continuation edits YAML, after every region callback has finished.
      return CompletableFuture.allOf(entries.toArray(CompletableFuture[]::new)).thenApplyAsync(ignored -> {
         boolean changed = false;
         for (var future : entries) {
            Outcome outcome = future.join();
            switch (outcome.kind()) {
               case RESTORED -> counts.restored++;
               case ALREADY_RESTORED -> counts.alreadyRestored++;
               case CONFLICT -> counts.conflicts++;
               case UNRESOLVED -> counts.unresolved++;
            }
            if (outcome.kind() == Kind.RESTORED || outcome.kind() == Kind.ALREADY_RESTORED) {
               config.set(outcome.key(), null);
               changed = true;
            }
         }
         if (changed) persistJournal(config, journal, counts);
         return counts.result();
      }, this.persistence);
   }

   private Kind recoverEntry(Entry entry, String key, Path journal) {
      try {
         BlockAccess current = this.blocks.find(entry.world(), entry.x(), entry.y(), entry.z());
         if (current == null) {
            this.warning.accept("[UIcore] Preserved legacy blockcache entry with unavailable world or coordinate: " + key + " in " + journal.getFileName());
            return Kind.UNRESOLVED;
         }
         String currentData = this.blockData.canonicalize(current.blockData());
         if (entry.blockData().equals(currentData)) return Kind.ALREADY_RESTORED;
         if (!current.isAir()) {
            this.warning.accept("[UIcore] Preserved legacy blockcache conflict at " + key + " in " + journal.getFileName() + "; the world no longer contains air.");
            return Kind.CONFLICT;
         }
         current.setBlockData(entry.blockData());
         this.warning.accept("[UIcore] Restored legacy blockcache entry " + key
            + "; its old journal contained BlockData only, so block-entity contents/NBT cannot be recovered.");
         return Kind.RESTORED;
      } catch (RuntimeException exception) {
         this.warning.accept("[UIcore] Preserved legacy blockcache entry that could not be restored: " + key + " (" + exception.getMessage() + ")");
         return Kind.UNRESOLVED;
      }
   }

   private Entry parseEntry(YamlConfiguration config, String key, Path journal) {
      Matcher coordinates = COORDINATES.matcher(key);
      ConfigurationSection section = config.getConfigurationSection(key);
      if (!coordinates.matches() || section == null || !section.getKeys(false).equals(java.util.Set.of("world", "type"))) {
         this.warning.accept("[UIcore] Preserved invalid legacy blockcache entry " + key + " in " + journal.getFileName());
         return null;
      }
      String world = section.getString("world");
      String data = section.getString("type");
      if (world == null || world.isBlank() || data == null || data.isBlank()) {
         this.warning.accept("[UIcore] Preserved incomplete legacy blockcache entry " + key + " in " + journal.getFileName());
         return null;
      }
      String canonicalData = this.blockData.canonicalize(data);
      if (canonicalData == null) {
         this.warning.accept("[UIcore] Preserved legacy blockcache entry with invalid BlockData " + key + " in " + journal.getFileName());
         return null;
      }
      try {
         return new Entry(world, Integer.parseInt(coordinates.group(1)), Integer.parseInt(coordinates.group(2)), Integer.parseInt(coordinates.group(3)), canonicalData);
      } catch (NumberFormatException exception) {
         this.warning.accept("[UIcore] Preserved legacy blockcache entry with out-of-range coordinates " + key + " in " + journal.getFileName());
         return null;
      }
   }

   private void persistJournal(YamlConfiguration config, Path journal, Counts counts) {
      try {
         writeAtomically(config, journal);
         if (config.getKeys(false).isEmpty()) {
            Files.deleteIfExists(journal);
         }
      } catch (IOException exception) {
         counts.unresolved++;
         this.warning.accept("[UIcore] Kept legacy blockcache journal after restore because it could not be updated atomically: " + journal.getFileName() + " (" + exception.getMessage() + ")");
      }
   }

   private static void writeAtomically(YamlConfiguration config, Path journal) throws IOException {
      Path parent = journal.getParent();
      Path temporary = Files.createTempFile(parent, journal.getFileName().toString(), ".tmp");
      try {
         config.save(temporary.toFile());
         try {
            Files.move(temporary, journal, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, journal, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(temporary);
      }
   }

   public interface BlockLookup {
      /** Returns null when the named world is not loaded or the coordinate cannot be accessed. */
      BlockAccess find(String worldName, int x, int y, int z);
   }

   public interface RegionExecutor {
      CompletableFuture<Void> execute(String worldName, int blockX, int blockZ, Runnable work);
   }

   public interface BlockAccess {
      boolean isAir();
      String blockData();
      void setBlockData(String blockData);
   }

   public interface BlockDataCodec {
      /** Returns canonical BlockData or null when the legacy string is invalid. */
      String canonicalize(String blockData);
   }

   public record RecoveryResult(int journals, int restored, int alreadyRestored, int conflicts, int unresolved, int invalid) { }

   private record Entry(String world, int x, int y, int z, String blockData) { }
   private enum Kind { RESTORED, ALREADY_RESTORED, CONFLICT, UNRESOLVED }
   private record Outcome(String key, Kind kind) { }

   private static final class Counts {
      private int journals;
      private int restored;
      private int alreadyRestored;
      private int conflicts;
      private int unresolved;
      private int invalid;
      private void add(RecoveryResult other) {
         journals += other.journals(); restored += other.restored(); alreadyRestored += other.alreadyRestored();
         conflicts += other.conflicts(); unresolved += other.unresolved(); invalid += other.invalid();
      }
      private RecoveryResult result() { return new RecoveryResult(journals, restored, alreadyRestored, conflicts, unresolved, invalid); }
   }

   private static final class BukkitBlockLookup implements BlockLookup {
      @Override public BlockAccess find(String worldName, int x, int y, int z) {
         var world = Bukkit.getWorld(worldName);
         if (world == null || y < world.getMinHeight() || y >= world.getMaxHeight()) return null;
         Block block = world.getBlockAt(x, y, z);
         return new BlockAccess() {
            @Override public boolean isAir() { return block.getType().isAir(); }
            @Override public String blockData() { return block.getBlockData().getAsString(); }
            @Override public void setBlockData(String data) { block.setBlockData(Bukkit.createBlockData(data), false); }
         };
      }
   }

   private static final class BukkitBlockDataCodec implements BlockDataCodec {
      @Override public String canonicalize(String data) {
         try {
            BlockData parsed = Bukkit.createBlockData(data);
            return parsed.getAsString();
         } catch (IllegalArgumentException exception) {
            return null;
         }
      }
   }
}
