package com.cmenu.ui.scheduler;

import com.cmenu.ui.CursorMenuPlugin;
import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.Section;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

public class ChunkLoaderManager {
   private final CursorMenuPlugin plugin;
   private BukkitTask chunkLoaderTask;
   private final Map<String, Set<Long>> forcedLoadedChunks = new ConcurrentHashMap<>();
   private final Map<String, Set<Long>> persistentChunks = new ConcurrentHashMap<>();
   private boolean isTaskRunning = false;

   public ChunkLoaderManager(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   public void startChunkLoaderTask() {
      this.stopChunkLoaderTask();
      this.updatePersistentChunks();
      this.chunkLoaderTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::updatePersistentChunks, 0L, 600L);
      this.isTaskRunning = true;
      if (this.plugin.getConfig().getBoolean("Debug", false)) {
         this.plugin.getLogger().info("区块加载任务已启动");
      }
   }

   public void stopChunkLoaderTask() {
      if (this.chunkLoaderTask != null) {
         this.chunkLoaderTask.cancel();
         this.chunkLoaderTask = null;
      }

      for (Entry<String, Set<Long>> entry : this.forcedLoadedChunks.entrySet()) {
         String worldName = entry.getKey();
         World world = Bukkit.getWorld(worldName);
         if (world != null) {
            for (long chunkKey : entry.getValue()) {
               int x = (int)(chunkKey >> 32);
               int z = (int)(chunkKey & 4294967295L);
               Chunk chunk = world.getChunkAt(x, z);
               if (chunk != null) {
                  chunk.setForceLoaded(false);
               }
            }
         }
      }

      this.forcedLoadedChunks.clear();
      this.persistentChunks.clear();
      this.isTaskRunning = false;
      if (this.plugin.getConfig().getBoolean("Debug", false)) {
         this.plugin.getLogger().info("区块加载任务已停止");
      }
   }

   public void updatePersistentChunks() {
      Map<String, Set<Long>> oldChunks = new HashMap<>();

      for (Entry<String, Set<Long>> entry : this.forcedLoadedChunks.entrySet()) {
         oldChunks.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }

      Map<String, Set<Long>> newChunks = new HashMap<>();
      Map<String, Section> allSections = CursorMenuPlugin.sectionManager.getAll();

      for (Section section : allSections.values()) {
         World world = Bukkit.getWorld(section.world);
         if (world != null) {
            int chunkX = (int)Math.floor(section.cameraX / 16.0);
            int chunkZ = (int)Math.floor(section.cameraZ / 16.0);
            long chunkKey = (long)chunkX << 32 | chunkZ & 4294967295L;
            newChunks.computeIfAbsent(section.world, k -> new HashSet<>()).add(chunkKey);
            if (section.layouts != null) {
               for (MenuLayout layout : section.layouts.values()) {
                  int layoutChunkX = (int)Math.floor((section.cameraX + layout.x) / 16.0);
                  int layoutChunkZ = (int)Math.floor((section.cameraZ + layout.z) / 16.0);
                  long layoutChunkKey = (long)layoutChunkX << 32 | layoutChunkZ & 4294967295L;
                  newChunks.get(section.world).add(layoutChunkKey);
               }
            }
         }
      }

      for (Entry<String, Set<Long>> entry : newChunks.entrySet()) {
         String worldName = entry.getKey();
         World world = Bukkit.getWorld(worldName);
         if (world != null) {
            for (long chunkKey : entry.getValue()) {
               int x = (int)(chunkKey >> 32);
               int z = (int)(chunkKey & 4294967295L);
               if (!world.isChunkLoaded(x, z)) {
                  world.loadChunk(x, z, true);
                  Chunk chunk = world.getChunkAt(x, z);
                  if (chunk != null) {
                     chunk.setForceLoaded(true);
                  }
               } else {
                  Chunk chunk = world.getChunkAt(x, z);
                  chunk.setForceLoaded(true);
               }

               this.forcedLoadedChunks.computeIfAbsent(worldName, k -> new HashSet<>()).add(chunkKey);
            }
         }
      }

      for (Entry<String, Set<Long>> entryx : oldChunks.entrySet()) {
         String worldName = entryx.getKey();
         World world = Bukkit.getWorld(worldName);
         if (world != null) {
            for (long chunkKey : entryx.getValue()) {
               Set<Long> newChunkKeys = newChunks.getOrDefault(worldName, Collections.emptySet());
               if (!newChunkKeys.contains(chunkKey)) {
                  int x = (int)(chunkKey >> 32);
                  int z = (int)(chunkKey & 4294967295L);
                  Chunk chunk = world.getChunkAt(x, z);
                  if (chunk != null) {
                     chunk.setForceLoaded(false);
                  }

                  this.forcedLoadedChunks.getOrDefault(worldName, new HashSet<>()).remove(chunkKey);
               }
            }
         }
      }

      this.forcedLoadedChunks.entrySet().removeIf(e -> e.getValue().isEmpty());
      if (this.plugin.getConfig().getBoolean("Debug", false)) {
         this.plugin
            .getLogger()
            .info("已更新强制加载区块：共 " + this.forcedLoadedChunks.size() + " 个世界，" + this.forcedLoadedChunks.values().stream().mapToInt(Set::size).sum() + " 个区块");
      }
   }

   public int getLoadedChunkCount() {
      return this.forcedLoadedChunks.values().stream().mapToInt(Set::size).sum();
   }

   public int getLoadedWorldCount() {
      return this.forcedLoadedChunks.size();
   }

   public boolean isTaskRunning() {
      return this.isTaskRunning;
   }
}
