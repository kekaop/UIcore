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

   @Test
   void waitsForAllRegionsThenPersistsOnceOffTheRegion() throws Exception {
      Path journal = journal("regions.yml", "0,64,0", "world", "stone", "48,64,0", "world", "dirt", "96,64,0", "world", "stone");
      FakeLookup lookup = new FakeLookup();
      FakeBlock first = lookup.add("world", 0, 64, 0, "air", true);
      FakeBlock second = lookup.add("world", 48, 64, 0, "air", true);
      lookup.add("world", 96, 64, 0, "diamond_block", false);
      Map<Integer, Runnable> regions = new HashMap<>();
      List<Runnable> persistence = new java.util.ArrayList<>();
      ThreadLocal<Integer> owner = new ThreadLocal<>();
      var recovery = new LegacyBlockCacheRecovery((world, x, y, z) -> {
         assertEquals(x, owner.get(), "Block lookup must happen inside its region callback");
         return lookup.find(world, x, y, z);
      }, value -> value, ignored -> { }, (world, x, z, work) -> {
         var complete = new java.util.concurrent.CompletableFuture<Void>();
         regions.put(x, () -> {
            owner.set(x);
            try { work.run(); complete.complete(null); }
            finally { owner.remove(); }
         });
         return complete;
      }, work -> persistence.add(() -> { assertEquals(null, owner.get()); work.run(); }));

      var result = recovery.recoverAllAsync(temporaryDirectory);
      assertEquals("air", first.data);
      assertTrue(persistence.isEmpty());
      regions.get(48).run();
      regions.get(0).run();
      assertEquals("stone", first.data);
      assertEquals("dirt", second.data);
      assertFalse(result.isDone());
      assertTrue(YamlConfiguration.loadConfiguration(journal.toFile()).contains("0,64,0"));
      regions.get(96).run();
      assertEquals(1, persistence.size());
      persistence.get(0).run();
      assertEquals(2, result.join().restored());
      assertEquals(1, result.join().conflicts());
      var remaining = YamlConfiguration.loadConfiguration(journal.toFile());
      assertEquals(java.util.Set.of("96,64,0"), remaining.getKeys(false));
   }

   @Test
   void lookupReadWriteAndSchedulingFailuresKeepEntriesAndAllowOtherRegionsToFinish() throws Exception {
      Path journal = journal("failure.yml", "0,64,0", "world", "stone", "16,64,0", "world", "stone",
         "32,64,0", "world", "stone", "48,64,0", "world", "stone", "64,64,0", "world", "stone");
      var recovery = new LegacyBlockCacheRecovery((world, x, y, z) -> {
         if (x == 0) throw new IllegalStateException("lookup failed");
         return new LegacyBlockCacheRecovery.BlockAccess() {
            public boolean isAir() { return true; }
            public String blockData() { if (x == 16) throw new IllegalStateException("read failed"); return "air"; }
            public void setBlockData(String data) { if (x == 32) throw new IllegalStateException("write failed"); }
         };
      }, value -> value, ignored -> { }, (world, x, z, work) -> {
         if (x == 48) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("region unavailable"));
         work.run();
         return java.util.concurrent.CompletableFuture.completedFuture(null);
      }, Runnable::run);

      var result = recovery.recoverAllAsync(temporaryDirectory).join();
      assertEquals(4, result.unresolved());
      assertEquals(1, result.restored());
      var remaining = YamlConfiguration.loadConfiguration(journal.toFile());
      assertEquals(java.util.Set.of("0,64,0", "16,64,0", "32,64,0", "48,64,0"), remaining.getKeys(false));
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

   @Test
   void retainsJournalWhenPersistenceIsRejectedAndRetryIsIdempotent() throws Exception {
      Path journal = journal("retry.yml", "0,64,0", "world", "stone");
      String original = Files.readString(journal);
      FakeLookup lookup = new FakeLookup();
      FakeBlock block = lookup.add("world", 0, 64, 0, "air", true);
      LegacyBlockCacheRecovery recovery = new LegacyBlockCacheRecovery(lookup, value -> value, ignored -> { },
         (world, x, z, work) -> { work.run(); return java.util.concurrent.CompletableFuture.completedFuture(null); },
         work -> { throw new java.util.concurrent.RejectedExecutionException("plugin disabled"); });
      org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.CompletionException.class,
         () -> recovery.recoverAllAsync(temporaryDirectory).join());
      assertEquals("stone", block.data);
      assertEquals(original, Files.readString(journal));
      LegacyBlockCacheRecovery.RecoveryResult retry = recovery(lookup, new java.util.ArrayList<>()).recoverAll(temporaryDirectory);
      assertEquals(0, retry.restored());
      assertEquals(1, retry.alreadyRestored());
      assertFalse(Files.exists(journal));
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
