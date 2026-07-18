package com.cmenu.ui.npc;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.NpcManager;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.PlaceholderAPI;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class NPCMirrorManager {
   private static NPCMirrorManager instance;
   private final JavaPlugin plugin;
   private FancyNpcsPlugin fancyNpcsPlugin;
   private SkinsRestorer skinsRestorer;
   private boolean fancyNpcsEnabled = false;
   private boolean skinsRestorerEnabled = false;
   private final Map<UUID, Npc> playerNPCs = new ConcurrentHashMap<>();
   private final Map<UUID, BukkitTask> autoRotateTasks = new ConcurrentHashMap<>();
   private final Set<UUID> disabledPlayers = new HashSet<>();
   private NPCConfig config;

   private NPCMirrorManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.config = new NPCConfig(plugin);
      this.initializeDependencies();
   }

   public static synchronized NPCMirrorManager getInstance(JavaPlugin plugin) {
      if (instance == null) {
         instance = new NPCMirrorManager(plugin);
      }

      return instance;
   }

   public static NPCMirrorManager getInstance() {
      return instance;
   }

   private void initializeDependencies() {
      if (Bukkit.getPluginManager().isPluginEnabled("FancyNpcs")) {
         try {
            this.fancyNpcsPlugin = (FancyNpcsPlugin)Bukkit.getPluginManager().getPlugin("FancyNpcs");
            this.fancyNpcsEnabled = true;
            this.plugin.getLogger().info("[NPCMirror] FancyNpcs 插件已找到，NPC镜像功能已启用");
         } catch (Exception var3) {
            this.plugin.getLogger().warning("[NPCMirror] 无法加载 FancyNpcs: " + var3.getMessage());
         }
      } else {
         this.plugin.getLogger().info("[NPCMirror] FancyNpcs 插件未找到，NPC镜像功能已禁用");
      }

      if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
         try {
            this.skinsRestorer = SkinsRestorerProvider.get();
            this.skinsRestorerEnabled = true;
            this.plugin.getLogger().info("[NPCMirror] SkinsRestorer 插件已找到，皮肤同步功能已启用");
         } catch (Exception var2) {
            this.plugin.getLogger().warning("[NPCMirror] 无法加载 SkinsRestorer: " + var2.getMessage());
         }
      } else {
         this.plugin.getLogger().info("[NPCMirror] SkinsRestorer 插件未找到，将使用默认皮肤");
      }
   }

   public boolean isEnabled() {
      return this.config.isEnabled() && this.fancyNpcsEnabled;
   }

   public boolean isMenuEnabled(String menuKey) {
      return this.config.isMenuEnabled(menuKey);
   }

   public boolean isNPCCreationEnabled(Player player) {
      return !this.disabledPlayers.contains(player.getUniqueId());
   }

   public void setNPCCreationEnabled(Player player, boolean enabled) {
      UUID playerId = player.getUniqueId();
      if (enabled) {
         this.disabledPlayers.remove(playerId);
         player.sendMessage("§a[NPCMirror] 已启用NPC镜像创建");
      } else {
         this.disabledPlayers.add(playerId);
         player.sendMessage("§c[NPCMirror] 已禁用NPC镜像创建");
      }

      this.config.saveDisabledPlayers(this.disabledPlayers);
   }

   public boolean toggleNPCForPlayer(Player player) {
      boolean newState = this.isNPCCreationEnabled(player);
      this.setNPCCreationEnabled(player, !newState);
      return !newState;
   }

   public boolean createMirrorNPC(Player player, Location baseLocation, float yaw, float pitch, String menuKey) {
      if (this.isEnabled() && this.isNPCCreationEnabled(player) && baseLocation != null && baseLocation.getWorld() != null) {
         if (this.config.isWorldDisabled(baseLocation.getWorld().getName())) {
            return false;
         } else if (menuKey != null && !this.isMenuEnabled(menuKey)) {
            if (this.config.isDebugMode()) {
               this.plugin.getLogger().info("[NPCMirror] 菜单 " + menuKey + " 未启用NPC，跳过创建");
            }

            return false;
         } else {
            this.removeMirrorNPC(player);

            try {
               NpcManager npcManager = this.fancyNpcsPlugin.getNpcManager();
               if (npcManager == null) {
                  return false;
               } else {
                  Location npcLocation = this.calculateNPCLocation(baseLocation, yaw, pitch);
                  NpcData npcData = new NpcData("CMP_MIRROR_" + player.getName(), player.getUniqueId(), npcLocation);
                  if (this.config.isSyncSkin()) {
                     this.applyPlayerSkin(player, npcData);
                  }

                  if (this.config.isSyncEquipment()) {
                     this.copyPlayerEquipment(player, npcData);
                  }

                  this.applyNpcName(player, npcData);
                  Npc npc = (Npc)this.fancyNpcsPlugin.getNpcAdapter().apply(npcData);
                  npc.setSaveToFile(false);
                  npc.create();
                  npc.spawnForAll();
                  this.updateNpcName(player, npc);
                  this.playerNPCs.put(player.getUniqueId(), npc);
                  this.startAutoRotate(player, npc);
                  if (this.config.isDebugMode()) {
                     this.plugin.getLogger().info("[NPCMirror] 已为玩家 " + player.getName() + " 创建镜像NPC");
                  }

                  return true;
               }
            } catch (Exception var10) {
               this.plugin.getLogger().warning("[NPCMirror] 创建NPC时出错: " + var10.getMessage());
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean createMirrorNPC(Player player, Location playerLocation) {
      return this.createMirrorNPC(player, playerLocation, this.config.getDefaultYaw(), this.config.getDefaultPitch(), null);
   }

   public boolean createMirrorNPC(Player player, Location playerLocation, String menuKey) {
      return this.createMirrorNPC(player, playerLocation, this.config.getDefaultYaw(), this.config.getDefaultPitch(), menuKey);
   }

   private Location calculateNPCLocation(Location baseLocation, float yaw, float pitch) {
      Location npcLocation = baseLocation.clone();
      npcLocation.add(this.config.getOffsetX(), this.config.getOffsetY(), this.config.getOffsetZ());
      npcLocation.setYaw(yaw);
      npcLocation.setPitch(pitch);
      return npcLocation;
   }

   private void applyPlayerSkin(Player player, NpcData npcData) {
      if (this.config.isSyncSkin() && this.skinsRestorerEnabled) {
         try {
            PlayerStorage playerStorage = this.skinsRestorer.getPlayerStorage();
            Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(player.getUniqueId(), player.getName());
            if (skin.isPresent()) {
               npcData.setSkin(player.getName());
            }
         } catch (IllegalStateException var5) {
            this.plugin.getLogger().info("[NPCMirror] SkinsRestorer API不可用，跳过皮肤设置");
         } catch (Exception var6) {
            this.plugin.getLogger().warning("[NPCMirror] 无法获取玩家皮肤: " + var6.getMessage());
         }
      }
   }

   private void copyPlayerEquipment(Player player, NpcData npcData) {
      if (this.config.isSyncEquipment()) {
         PlayerInventory inv = player.getInventory();
         this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.HEAD, inv.getHelmet());
         this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.CHEST, inv.getChestplate());
         this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.LEGS, inv.getLeggings());
         this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.FEET, inv.getBoots());
         this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.MAINHAND, inv.getItemInMainHand());
         this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.OFFHAND, inv.getItemInOffHand());
      }
   }

   private void applyNpcName(Player player, NpcData npcData) {
      if (this.config.isShowNpcName()) {
         ;
      }
   }

   public void updateNpcName(Player player, Npc npc) {
      if (this.config.isShowNpcName()) {
         String formattedName = this.config.getFormattedNpcName(player.getName(), player.getDisplayName());
         if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedName = PlaceholderAPI.setPlaceholders(player, formattedName);
         }

         try {
            npc.getData().setDisplayName(formattedName);
            npc.updateForAll();
            if (this.config.isDebugMode()) {
               this.plugin.getLogger().info("[NPCMirror] 设置NPC名称: " + formattedName);
            }
         } catch (Exception var5) {
            this.plugin.getLogger().warning("[NPCMirror] 设置NPC名称时出错: " + var5.getMessage());
         }
      }
   }

   private void setEquipmentIfPresent(NpcData npcData, NpcEquipmentSlot slot, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         npcData.addEquipment(slot, item.clone());
      }
   }

   public void removeMirrorNPC(Player player) {
      UUID playerId = player.getUniqueId();
      this.cancelAutoRotate(playerId);
      Npc npc = this.playerNPCs.remove(playerId);
      if (npc != null) {
         try {
            npc.removeForAll();
         } catch (Exception var5) {
            this.plugin.getLogger().warning("[NPCMirror] 移除NPC时出错: " + var5.getMessage());
         }
      }
   }

   public void rotateNPC(Player player, float yawOffset) {
      Npc npc = this.playerNPCs.get(player.getUniqueId());
      if (npc != null) {
         try {
            Location location = npc.getData().getLocation();
            float newYaw = (location.getYaw() + yawOffset + 360.0F) % 360.0F;
            location.setYaw(newYaw);
            npc.getData().setLocation(location);
            npc.updateForAll();
         } catch (Exception var6) {
            this.plugin.getLogger().warning("[NPCMirror] 旋转NPC时出错: " + var6.getMessage());
         }
      }
   }

   private void startAutoRotate(Player player, Npc npc) {
      UUID playerId = player.getUniqueId();
      this.cancelAutoRotate(playerId);
      if (this.config.isAutoRotateEnabled()) {
         BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (player.isOnline() && this.playerNPCs.get(playerId) == npc) {
               try {
                  Location location = npc.getData().getLocation();
                  location.setYaw(location.getYaw() + this.config.getAutoRotateSpeed());
                  npc.getData().setLocation(location);
                  npc.updateForAll();
               } catch (Exception var5) {
                  this.cancelAutoRotate(playerId);
               }
            } else {
               this.cancelAutoRotate(playerId);
            }
         }, 1L, 1L);
         this.autoRotateTasks.put(playerId, task);
      }
   }

   private void cancelAutoRotate(UUID playerId) {
      BukkitTask task = this.autoRotateTasks.remove(playerId);
      if (task != null) {
         task.cancel();
      }
   }

   public void updateNPCLocation(Player player, Location newLocation) {
      Npc npc = this.playerNPCs.get(player.getUniqueId());
      if (npc != null) {
         try {
            Location npcLoc = this.calculateNPCLocation(newLocation, newLocation.getYaw(), newLocation.getPitch());
            npc.getData().setLocation(npcLoc);
            npc.updateForAll();
         } catch (Exception var5) {
            this.plugin.getLogger().warning("[NPCMirror] 更新NPC位置时出错: " + var5.getMessage());
         }
      }
   }

   public boolean hasNPC(Player player) {
      return this.playerNPCs.containsKey(player.getUniqueId());
   }

   public Npc getPlayerNPC(Player player) {
      return this.playerNPCs.get(player.getUniqueId());
   }

   public void cleanup() {
      this.autoRotateTasks.values().forEach(BukkitTask::cancel);
      this.autoRotateTasks.clear();

      for (Npc npc : this.playerNPCs.values()) {
         try {
            npc.removeForAll();
         } catch (Exception var4) {
         }
      }

      this.playerNPCs.clear();
   }

   public NPCConfig getConfig() {
      return this.config;
   }

   public void reloadConfig() {
      this.config.reload();
      this.disabledPlayers.clear();
      this.disabledPlayers.addAll(this.config.loadDisabledPlayers());
   }

   public FancyNpcsPlugin getFancyNpcsPlugin() {
      return this.fancyNpcsPlugin;
   }

   public NpcManager getNpcManager() {
      return this.fancyNpcsPlugin != null ? this.fancyNpcsPlugin.getNpcManager() : null;
   }
}
