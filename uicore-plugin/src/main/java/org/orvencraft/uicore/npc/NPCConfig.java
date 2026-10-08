package org.orvencraft.uicore.npc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class NPCConfig {
   private final JavaPlugin plugin;
   private File configFile;
   private FileConfiguration config;
   private boolean enabled = true;
   private boolean debugMode = false;
   private double offsetX = 0.0;
   private double offsetY = 0.0;
   private double offsetZ = 0.0;
   private float defaultYaw = 180.0F;
   private float defaultPitch = 0.0F;
   private boolean autoRotateEnabled = false;
   private float autoRotateSpeed = 1.0F;
   private boolean syncEquipment = true;
   private boolean syncSkin = true;
   private List<String> disabledWorlds = new ArrayList<>();
   private boolean enableForAllMenus = true;
   private List<String> enabledMenus = new ArrayList<>();
   private boolean useWhitelistMode = false;
   private boolean showNpcName = true;
   private String npcNameFormat = "&e%player%";
   private boolean showNpcNameAlways = true;

   public NPCConfig(JavaPlugin plugin) {
      this.plugin = plugin;
      this.load();
   }

   public void load() {
      this.configFile = new File(this.plugin.getDataFolder(), "npc_config.yml");
      if (!this.configFile.exists()) {
         this.createDefaultConfig();
      }

      this.config = org.orvencraft.uicore.config.ConfigFiles.loadStrict(this.configFile);
      this.readConfig();
   }

   private void createDefaultConfig() {
      this.plugin.saveResource("npc_config.yml", false);
   }

   private void readConfig() {
      this.enabled = this.config.getBoolean("enabled", true);
      this.debugMode = this.config.getBoolean("debug-mode", false);
      this.offsetX = this.config.getDouble("offset.x", 0.0);
      this.offsetY = this.config.getDouble("offset.y", 0.0);
      this.offsetZ = this.config.getDouble("offset.z", 0.0);
      this.defaultYaw = (float)this.config.getDouble("default-yaw", 180.0);
      this.defaultPitch = (float)this.config.getDouble("default-pitch", 0.0);
      this.autoRotateEnabled = this.config.getBoolean("auto-rotate.enabled", false);
      this.autoRotateSpeed = (float)this.config.getDouble("auto-rotate.speed", 1.0);
      this.syncEquipment = this.config.getBoolean("sync.equipment", true);
      this.syncSkin = this.config.getBoolean("sync.skin", true);
      this.disabledWorlds = this.config.getStringList("disabled-worlds");
      this.enableForAllMenus = this.config.getBoolean("menu-settings.enable-for-all-menus", true);
      this.enabledMenus = this.config.getStringList("menu-settings.enabled-menus");
      this.useWhitelistMode = this.config.getBoolean("menu-settings.use-whitelist-mode", false);
      this.showNpcName = this.config.getBoolean("name-display.show", true);
      this.npcNameFormat = this.config.getString("name-display.format", "&e%player%");
      this.showNpcNameAlways = this.config.getBoolean("name-display.always-visible", true);
   }

   public void reload() {
      this.load();
   }

   public void save() {
      try {
         this.config.save(this.configFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("[NPCConfig] Could not save configuration: " + var2.getMessage());
      }
   }

   public void saveDisabledPlayers(Set<UUID> disabledPlayers) {
      List<String> uuidStrings = new ArrayList<>();

      for (UUID uuid : disabledPlayers) {
         uuidStrings.add(uuid.toString());
      }

      this.config.set("disabled-players", uuidStrings);
      this.save();
   }

   public Set<UUID> loadDisabledPlayers() {
      Set<UUID> result = new HashSet<>();

      for (String uuidString : this.config.getStringList("disabled-players")) {
         try {
            result.add(UUID.fromString(uuidString));
         } catch (IllegalArgumentException var6) {
            this.plugin.getLogger().warning("[NPCConfig] Invalid UUID: " + uuidString);
         }
      }

      return result;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      this.config.set("enabled", enabled);
      this.save();
   }

   public boolean isDebugMode() {
      return this.debugMode;
   }

   public double getOffsetX() {
      return this.offsetX;
   }

   public double getOffsetY() {
      return this.offsetY;
   }

   public double getOffsetZ() {
      return this.offsetZ;
   }

   public float getDefaultYaw() {
      return this.defaultYaw;
   }

   public float getDefaultPitch() {
      return this.defaultPitch;
   }

   public boolean isAutoRotateEnabled() {
      return this.autoRotateEnabled;
   }

   public float getAutoRotateSpeed() {
      return this.autoRotateSpeed;
   }

   public boolean isSyncEquipment() {
      return this.syncEquipment;
   }

   public boolean isSyncSkin() {
      return this.syncSkin;
   }

   public List<String> getDisabledWorlds() {
      return this.disabledWorlds;
   }

   public boolean isWorldDisabled(String worldName) {
      return this.disabledWorlds.contains(worldName);
   }

   public boolean isMenuEnabled(String menuKey) {
      if (!this.enabled) {
         return false;
      } else if (this.enableForAllMenus) {
         return this.useWhitelistMode ? this.enabledMenus.contains(menuKey) : !this.enabledMenus.contains(menuKey);
      } else {
         return this.enabledMenus.contains(menuKey);
      }
   }

   public List<String> getEnabledMenus() {
      return new ArrayList<>(this.enabledMenus);
   }

   public void addEnabledMenu(String menuKey) {
      if (!this.enabledMenus.contains(menuKey)) {
         this.enabledMenus.add(menuKey);
         this.config.set("menu-settings.enabled-menus", this.enabledMenus);
         this.save();
      }
   }

   public void removeEnabledMenu(String menuKey) {
      this.enabledMenus.remove(menuKey);
      this.config.set("menu-settings.enabled-menus", this.enabledMenus);
      this.save();
   }

   public void setEnableForAllMenus(boolean enable) {
      this.enableForAllMenus = enable;
      this.config.set("menu-settings.enable-for-all-menus", enable);
      this.save();
   }

   public void setUseWhitelistMode(boolean useWhitelist) {
      this.useWhitelistMode = useWhitelist;
      this.config.set("menu-settings.use-whitelist-mode", useWhitelist);
      this.save();
   }

   public boolean isShowNpcName() {
      return this.showNpcName;
   }

   public String getNpcNameFormat() {
      return this.npcNameFormat;
   }

   public boolean isShowNpcNameAlways() {
      return this.showNpcNameAlways;
   }

   public String getFormattedNpcName(String playerName, String displayName) {
      if (!this.showNpcName) {
         return "";
      } else {
         String formatted = this.npcNameFormat
            .replace("%player%", playerName != null ? playerName : "")
            .replace("%displayname%", displayName != null ? displayName : (playerName != null ? playerName : ""));
         return formatted.replace('&', '§');
      }
   }

   public void setShowNpcName(boolean show) {
      this.showNpcName = show;
      this.config.set("name-display.show", show);
      this.save();
   }

   public void setNpcNameFormat(String format) {
      this.npcNameFormat = format;
      this.config.set("name-display.format", format);
      this.save();
   }

   public void setShowNpcNameAlways(boolean always) {
      this.showNpcNameAlways = always;
      this.config.set("name-display.always-visible", always);
      this.save();
   }

   public FileConfiguration getRawConfig() {
      return this.config;
   }
}
