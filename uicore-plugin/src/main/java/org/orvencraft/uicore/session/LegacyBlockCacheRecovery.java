package org.orvencraft.uicore.session;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Recovers journals written by pre-1.4.8 camera block clearing. Those journals only contain
 * block-data strings, so this class deliberately refuses to overwrite any new world state.
 */
public final class LegacyBlockCacheRecovery {
   private static final Pattern COORDINATES = Pattern.compile("^(-?\\d+),(-?\\d+),(-?\\d+)$");
   private final BlockLookup blocks;
   private final BlockDataCodec blockData;
   private final Consumer<String> warning;

   public LegacyBlockCacheRecovery(BlockLookup blocks, BlockDataCodec blockData, Consumer<String> warning) {
      this.blocks = Objects.requireNonNull(blocks, "blocks");
      this.blockData = Objects.requireNonNull(blockData, "blockData");
      this.warning = warning == null ? ignored -> { } : warning;
   }

   /** Recovers every UUID-named journal below {@code dataFolder/blockcache}. */
   public static RecoveryResult recoverAll(File dataFolder) {
      return new LegacyBlockCacheRecovery(new BukkitBlockLookup(), new BukkitBlockDataCodec(), Bukkit.getLogger()::warning)
         .recoverAll(dataFolder == null ? null : dataFolder.toPath());
   }

   public RecoveryResult recoverAll(Path dataFolder) {
      if (dataFolder == null) {
         return new RecoveryResult(0, 0, 0, 0, 0, 0);
      }
      Path cacheDirectory = dataFolder.resolve("blockcache");
      if (!Files.isDirectory(cacheDirectory)) {
         return new RecoveryResult(0, 0, 0, 0, 0, 0);
      }

      Counts counts = new Counts();
      try (var files = Files.list(cacheDirectory)) {
         files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".yml")).forEach(path -> recoverJournal(path, counts));
      } catch (IOException exception) {
         this.warning.accept("[UIcore] Could not list legacy blockcache journals: " + exception.getMessage());
      }
      return counts.result();
   }

   private void recoverJournal(Path journal, Counts counts) {
      counts.journals++;
      YamlConfiguration config = new YamlConfiguration();
      try {
         config.load(journal.toFile());
      } catch (IOException | InvalidConfigurationException exception) {
         counts.invalid++;
         this.warning.accept("[UIcore] Preserved malformed legacy blockcache journal " + journal.getFileName() + ": " + exception.getMessage());
         return;
      }

      for (String entryKey : config.getKeys(false)) {
         Entry entry = parseEntry(config, entryKey, journal);
         if (entry == null) {
            counts.invalid++;
            continue;
         }
         BlockAccess current = this.blocks.find(entry.world(), entry.x(), entry.y(), entry.z());
         if (current == null) {
            counts.unresolved++;
            this.warning.accept("[UIcore] Preserved legacy blockcache entry with unavailable world: " + entryKey + " in " + journal.getFileName());
            continue;
         }
         String currentData = this.blockData.canonicalize(current.blockData());
         if (entry.blockData().equals(currentData)) {
            counts.alreadyRestored++;
            removePersistedEntry(config, journal, entryKey, counts);
         } else if (current.isAir()) {
            try {
               current.setBlockData(entry.blockData());
               counts.restored++;
               this.warning.accept("[UIcore] Restored legacy blockcache entry " + entryKey
                  + "; its old journal contained BlockData only, so block-entity contents/NBT cannot be recovered.");
               removePersistedEntry(config, journal, entryKey, counts);
            } catch (RuntimeException exception) {
               counts.unresolved++;
               this.warning.accept("[UIcore] Preserved legacy blockcache entry that could not be restored: " + entryKey + " (" + exception.getMessage() + ")");
            }
         } else {
            counts.conflicts++;
            this.warning.accept("[UIcore] Preserved legacy blockcache conflict at " + entryKey + " in " + journal.getFileName() + "; the world no longer contains air.");
         }
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

   private void removePersistedEntry(YamlConfiguration config, Path journal, String key, Counts counts) {
      config.set(key, null);
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

   private static final class Counts {
      private int journals;
      private int restored;
      private int alreadyRestored;
      private int conflicts;
      private int unresolved;
      private int invalid;
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
