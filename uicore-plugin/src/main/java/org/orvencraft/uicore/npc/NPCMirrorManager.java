package org.orvencraft.uicore.npc;

import org.orvencraft.uicore.UIcorePlugin;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcAttribute;
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
import org.bukkit.entity.EntityType;
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
   private final Map<UUID, NPCMenuSettings> playerMenuSettings = new ConcurrentHashMap<>();
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
            this.plugin.getLogger().info("[NPCMirror] FancyNpcs found; NPC mirror support enabled.");
         } catch (Exception var3) {
            this.plugin.getLogger().warning("[NPCMirror] Could not load FancyNpcs: " + var3.getMessage());
         }
      } else {
         this.plugin.getLogger().info("[NPCMirror] FancyNpcs not found; NPC mirror support disabled.");
      }

      if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
         try {
            this.skinsRestorer = SkinsRestorerProvider.get();
            this.skinsRestorerEnabled = true;
            this.plugin.getLogger().info("[NPCMirror] SkinsRestorer found; skin synchronization enabled.");
         } catch (Exception var2) {
            this.plugin.getLogger().warning("[NPCMirror] Could not load SkinsRestorer: " + var2.getMessage());
         }
      } else {
         this.plugin.getLogger().info("[NPCMirror] SkinsRestorer not found; the default skin will be used.");
      }
   }

   public boolean isEnabled() {
      return this.config.isEnabled() && this.fancyNpcsEnabled;
   }

   public boolean isMenuEnabled(String menuKey) {
      NPCMenuSettings settings = this.getMenuSettings(menuKey);
      if (settings != null && settings.enabled() != null) {
         return this.config.isEnabled() && settings.enabled();
      }
      return this.config.isMenuEnabled(menuKey);
   }

   public NPCMenuSettings getMenuSettings(String menuKey) {
      if (menuKey == null || UIcorePlugin.sectionManager == null) {
         return null;
      }
      org.orvencraft.uicore.section.Section section = UIcorePlugin.sectionManager.get(menuKey);
      return section == null ? null : section.npcSettings;
   }

   public boolean isNPCCreationEnabled(Player player) {
      return !this.disabledPlayers.contains(player.getUniqueId());
   }

   public void setNPCCreationEnabled(Player player, boolean enabled) {
      UUID playerId = player.getUniqueId();
      if (enabled) {
         this.disabledPlayers.remove(playerId);
         player.sendMessage("§a[NPCMirror] NPC mirror creation enabled.");
      } else {
         this.disabledPlayers.add(playerId);
         player.sendMessage("§c[NPCMirror] NPC mirror creation disabled.");
      }

      this.config.saveDisabledPlayers(this.disabledPlayers);
   }

   public boolean toggleNPCForPlayer(Player player) {
      boolean newState = this.isNPCCreationEnabled(player);
      this.setNPCCreationEnabled(player, !newState);
      return !newState;
   }

   public boolean createMirrorNPC(Player player, Location baseLocation, float yaw, float pitch, String menuKey) {
      NPCMenuSettings menuSettings = this.getMenuSettings(menuKey);
      if (this.isEnabled() && this.isNPCCreationEnabled(player) && baseLocation != null && baseLocation.getWorld() != null) {
         if (this.config.isWorldDisabled(baseLocation.getWorld().getName())) {
            return false;
         } else if (menuKey != null && !this.isMenuEnabled(menuKey)) {
            if (this.config.isDebugMode()) {
               this.plugin.getLogger().info("[NPCMirror] NPC mirrors are disabled for menu " + menuKey + "; creation skipped.");
            }

            return false;
         } else {
            this.removeMirrorNPC(player);

            try {
               NpcManager npcManager = this.fancyNpcsPlugin.getNpcManager();
               if (npcManager == null) {
                  return false;
               } else {
                  Location npcLocation = this.calculateNPCLocation(baseLocation, yaw, pitch, menuSettings);
                  NpcData npcData = new NpcData("CMP_MIRROR_" + player.getName(), player.getUniqueId(), npcLocation);
                  if (this.syncSkin(menuSettings)) {
                     this.applyPlayerSkin(player, npcData);
                  }

                  if (this.syncEquipment(menuSettings)) {
                     this.copyPlayerEquipment(player, npcData);
                  }

                  this.applyCustomModel(npcData, menuSettings, menuKey);
                  Npc npc = (Npc)this.fancyNpcsPlugin.getNpcAdapter().apply(npcData);
                  npc.setSaveToFile(false);
                  npc.create();
                  npc.spawnForAll();
                  this.updateNpcName(player, npc, menuSettings);
                  this.playerNPCs.put(player.getUniqueId(), npc);
                  if (menuSettings != null) {
                     this.playerMenuSettings.put(player.getUniqueId(), menuSettings);
                  }
                  this.startAutoRotate(player, npc, menuSettings);
                  if (this.config.isDebugMode()) {
                     this.plugin.getLogger().info("[NPCMirror] Created mirror NPC for " + player.getName() + ".");
                  }

                  return true;
               }
            } catch (Exception var10) {
               this.plugin.getLogger().warning("[NPCMirror] Could not create NPC: " + var10.getMessage());
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean createMirrorNPC(Player player, Location playerLocation) {
      return this.createMirrorNPC(player, playerLocation, 0.0F, this.config.getDefaultPitch(), null);
   }

   public boolean createMirrorNPC(Player player, Location playerLocation, String menuKey) {
      return this.createMirrorNPC(player, playerLocation, 0.0F, this.config.getDefaultPitch(), menuKey);
   }

   private Location calculateNPCLocation(Location baseLocation, float yaw, float pitch, NPCMenuSettings settings) {
      Location npcLocation = baseLocation.clone();
      npcLocation.add(
         settings != null && settings.offsetX() != null ? settings.offsetX() : this.config.getOffsetX(),
         settings != null && settings.offsetY() != null ? settings.offsetY() : this.config.getOffsetY(),
         settings != null && settings.offsetZ() != null ? settings.offsetZ() : this.config.getOffsetZ()
      );
      npcLocation.setYaw(yaw + (settings != null && settings.yawOffset() != null ? settings.yawOffset() : this.config.getDefaultYaw()));
      npcLocation.setPitch(settings != null && settings.pitch() != null ? settings.pitch() : pitch);
      return npcLocation;
   }

   private boolean syncSkin(NPCMenuSettings settings) {
      return settings != null && settings.syncSkin() != null ? settings.syncSkin() : this.config.isSyncSkin();
   }

   private boolean syncEquipment(NPCMenuSettings settings) {
      return settings != null && settings.syncEquipment() != null ? settings.syncEquipment() : this.config.isSyncEquipment();
   }

   private void applyCustomModel(NpcData npcData, NPCMenuSettings settings, String menuKey) {
      if (settings == null || settings.customModel() == null) {
         return;
      }
      if (!Bukkit.getPluginManager().isPluginEnabled("FancyNpcsModel")) {
         this.plugin.getLogger().warning("Menu '" + menuKey + "' requests custom model '" + settings.customModel() + "', but FancyNpcsModel is not enabled.");
         return;
      }

      NpcAttribute attribute = this.fancyNpcsPlugin.getAttributeManager().getAttributeByName(EntityType.PLAYER, "custom_model");
      if (attribute == null) {
         this.plugin.getLogger().warning("FancyNpcsModel did not register the custom_model NPC attribute for menu '" + menuKey + "'.");
         return;
      }
      npcData.addAttribute(attribute, settings.customModel());
   }

   private void applyPlayerSkin(Player player, NpcData npcData) {
      if (this.skinsRestorerEnabled) {
         try {
            PlayerStorage playerStorage = this.skinsRestorer.getPlayerStorage();
            Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(player.getUniqueId(), player.getName());
            if (skin.isPresent()) {
               npcData.setSkin(player.getName());
            }
         } catch (IllegalStateException var5) {
            this.plugin.getLogger().info("[NPCMirror] SkinsRestorer API unavailable; skin setup skipped.");
         } catch (Exception var6) {
            this.plugin.getLogger().warning("[NPCMirror] Could not obtain player skin: " + var6.getMessage());
         }
      }
   }

   private void copyPlayerEquipment(Player player, NpcData npcData) {
      PlayerInventory inv = player.getInventory();
      this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.HEAD, inv.getHelmet());
      this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.CHEST, inv.getChestplate());
      this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.LEGS, inv.getLeggings());
      this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.FEET, inv.getBoots());
      this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.MAINHAND, inv.getItemInMainHand());
      this.setEquipmentIfPresent(npcData, NpcEquipmentSlot.OFFHAND, inv.getItemInOffHand());
   }

   public void updateNpcName(Player player, Npc npc) {
      this.updateNpcName(player, npc, this.playerMenuSettings.get(player.getUniqueId()));
   }

   private void updateNpcName(Player player, Npc npc, NPCMenuSettings settings) {
      boolean show = settings != null && settings.showName() != null ? settings.showName() : this.config.isShowNpcName();
      if (show) {
         String format = settings != null && settings.nameFormat() != null ? settings.nameFormat() : this.config.getNpcNameFormat();
         String formattedName = format
            .replace("%player%", player.getName())
            .replace("%displayname%", player.getDisplayName())
            .replace('&', '§');
         if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedName = PlaceholderAPI.setPlaceholders(player, formattedName);
         }

         try {
            npc.getData().setDisplayName(formattedName);
            npc.updateForAll();
            if (this.config.isDebugMode()) {
               this.plugin.getLogger().info("[NPCMirror] Set NPC name: " + formattedName);
            }
         } catch (Exception var5) {
            this.plugin.getLogger().warning("[NPCMirror] Could not set NPC name: " + var5.getMessage());
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
      this.playerMenuSettings.remove(playerId);
      Npc npc = this.playerNPCs.remove(playerId);
      if (npc != null) {
         try {
            npc.removeForAll();
         } catch (Exception var5) {
            this.plugin.getLogger().warning("[NPCMirror] Could not remove NPC: " + var5.getMessage());
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
            this.plugin.getLogger().warning("[NPCMirror] Could not rotate NPC: " + var6.getMessage());
         }
      }
   }

   private void startAutoRotate(Player player, Npc npc, NPCMenuSettings settings) {
      UUID playerId = player.getUniqueId();
      this.cancelAutoRotate(playerId);
      boolean enabled = settings != null && settings.autoRotateEnabled() != null ? settings.autoRotateEnabled() : this.config.isAutoRotateEnabled();
      float speed = settings != null && settings.autoRotateSpeed() != null ? settings.autoRotateSpeed() : this.config.getAutoRotateSpeed();
      if (enabled) {
         BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (player.isOnline() && this.playerNPCs.get(playerId) == npc) {
               try {
                  Location location = npc.getData().getLocation();
                  location.setYaw(location.getYaw() + speed);
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
            Location npcLoc = this.calculateNPCLocation(
               newLocation,
               newLocation.getYaw(),
               newLocation.getPitch(),
               this.playerMenuSettings.get(player.getUniqueId())
            );
            npc.getData().setLocation(npcLoc);
            npc.updateForAll();
         } catch (Exception var5) {
            this.plugin.getLogger().warning("[NPCMirror] Could not update NPC position: " + var5.getMessage());
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
      this.playerMenuSettings.clear();
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
