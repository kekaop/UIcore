package org.orvencraft.uicore.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyBlockCacheRecoveryTest {
   @TempDir Path temporaryDirectory;

   @Test
   void restoresOnlyAirAndDeletesAFullyResolvedJournal() throws Exception {
      Path journal = journal("a.yml", "0,64,0", "world", "stone");
      FakeLookup lookup = new FakeLookup();
      FakeBlock block = lookup.add("world", 0, 64, 0, "air", true);
      List<String> warnings = new java.util.ArrayList<>();

      LegacyBlockCacheRecovery.RecoveryResult result = recovery(lookup, warnings).recoverAll(temporaryDirectory);

      assertEquals("stone", block.data);
      assertEquals(1, result.restored());
      assertFalse(Files.exists(journal));
      assertTrue(warnings.stream().anyMatch(message -> message.contains("NBT cannot be recovered")));
   }

   @Test
   void keepsConflictsAndRewritesOnlySafeEntries() throws Exception {
      Path journal = journal("b.yml", "0,64,0", "world", "stone", "1,64,0", "world", "dirt");
      FakeLookup lookup = new FakeLookup();
      lookup.add("world", 0, 64, 0, "diamond_block", false);
      FakeBlock air = lookup.add("world", 1, 64, 0, "air", true);

      LegacyBlockCacheRecovery.RecoveryResult result = recovery(lookup, new java.util.ArrayList<>()).recoverAll(temporaryDirectory);

      assertEquals("dirt", air.data);
      assertEquals(1, result.conflicts());
      assertTrue(Files.exists(journal));
      YamlConfiguration remaining = YamlConfiguration.loadConfiguration(journal.toFile());
      assertEquals("stone", remaining.getString("0,64,0.type"));
      assertFalse(remaining.contains("1,64,0"));
   }

   @Test
   void retainsMissingWorldInvalidDataAndAlreadyMatchingEntriesSafely() throws Exception {
      Path journal = journal("c.yml", "0,64,0", "missing", "stone", "1,64,0", "world", "not valid", "2,64,0", "world", "dirt");
      FakeLookup lookup = new FakeLookup();
      lookup.add("world", 2, 64, 0, "dirt", false);

      LegacyBlockCacheRecovery.RecoveryResult result = recovery(lookup, new java.util.ArrayList<>()).recoverAll(temporaryDirectory);

      assertEquals(1, result.alreadyRestored());
      assertEquals(1, result.unresolved());
      assertEquals(1, result.invalid());
      YamlConfiguration remaining = YamlConfiguration.loadConfiguration(journal.toFile());
      assertTrue(remaining.contains("0,64,0"));
      assertTrue(remaining.contains("1,64,0"));
      assertFalse(remaining.contains("2,64,0"));
   }

   private LegacyBlockCacheRecovery recovery(FakeLookup lookup, List<String> warnings) {
      return new LegacyBlockCacheRecovery(lookup, value -> value != null && value.matches("[a-z_]+") ? value : null, warnings::add);
   }

   private Path journal(String name, String... values) throws Exception {
      Path directory = Files.createDirectories(temporaryDirectory.resolve("blockcache"));
      YamlConfiguration yaml = new YamlConfiguration();
      for (int index = 0; index < values.length; index += 3) {
         yaml.set(values[index] + ".world", values[index + 1]);
         yaml.set(values[index] + ".type", values[index + 2]);
      }
      Path journal = directory.resolve(name);
      yaml.save(journal.toFile());
      return journal;
   }

   private static final class FakeLookup implements LegacyBlockCacheRecovery.BlockLookup {
      private final Map<String, FakeBlock> blocks = new HashMap<>();
      FakeBlock add(String world, int x, int y, int z, String data, boolean air) {
         FakeBlock block = new FakeBlock(data, air);
         blocks.put(key(world, x, y, z), block);
         return block;
      }
      @Override public LegacyBlockCacheRecovery.BlockAccess find(String world, int x, int y, int z) { return blocks.get(key(world, x, y, z)); }
      private static String key(String world, int x, int y, int z) { return world + ':' + x + ':' + y + ':' + z; }
   }

   private static final class FakeBlock implements LegacyBlockCacheRecovery.BlockAccess {
      private String data;
      private boolean air;
      FakeBlock(String data, boolean air) { this.data = data; this.air = air; }
      @Override public boolean isAir() { return air; }
      @Override public String blockData() { return data; }
      @Override public void setBlockData(String data) { this.data = data; this.air = false; }
   }
}
