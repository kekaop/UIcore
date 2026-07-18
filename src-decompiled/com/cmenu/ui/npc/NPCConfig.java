package com.cmenu.ui.npc;

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

      this.config = YamlConfiguration.loadConfiguration(this.configFile);
      this.readConfig();
   }

   private void createDefaultConfig() {
      this.config = new YamlConfiguration();
      this.config.set("enabled", true);
      this.config.set("debug-mode", false);
      this.config.set("offset.x", 0.0);
      this.config.set("offset.y", 0.0);
      this.config.set("offset.z", 0.0);
      this.config.set("default-yaw", 180.0);
      this.config.set("default-pitch", 0.0);
      this.config.set("auto-rotate.enabled", false);
      this.config.set("auto-rotate.speed", 1.0);
      this.config.set("sync.equipment", true);
      this.config.set("sync.skin", true);
      this.config.set("disabled-worlds", new ArrayList());
      this.config.set("disabled-players", new ArrayList());
      this.config.set("menu-settings.enable-for-all-menus", true);
      this.config.set("menu-settings.enabled-menus", new ArrayList());
      this.config.set("menu-settings.use-whitelist-mode", false);
      this.config.set("name-display.show", true);
      this.config.set("name-display.format", "&e%player%");
      this.config.set("name-display.always-visible", true);
      List<String> header = new ArrayList<>();
      header.add("===========================================");
      header.add("NPC镜像系统配置文件");
      header.add("CustomScreenMenu NPC Mirror Module");
      header.add("===========================================");
      header.add("");
      header.add("enabled: 是否启用NPC镜像功能");
      header.add("debug-mode: 调试模式，输出详细日志");
      header.add("");
      header.add("offset: NPC相对于玩家位置的偏移量");
      header.add("default-yaw/pitch: NPC默认朝向");
      header.add("");
      header.add("auto-rotate: 自动旋转设置");
      header.add("sync: 同步设置（装备、皮肤）");
      header.add("disabled-worlds: 禁用NPC的世界列表");
      header.add("disabled-players: 禁用NPC的玩家UUID列表");
      header.add("");
      header.add("menu-settings: 菜单关联设置");
      header.add("  enable-for-all-menus: 是否对所有菜单启用NPC（true=全部启用）");
      header.add("  enabled-menus: 启用NPC的菜单列表（仅当enable-for-all-menus为false时生效）");
      header.add("  use-whitelist-mode: 是否使用白名单模式（true=仅列表中的菜单启用，false=列表中的菜单禁用）");
      header.add("");
      header.add("name-display: NPC名称显示设置");
      header.add("  show: 是否显示NPC头顶名称");
      header.add("  format: 名称格式，支持颜色代码和占位符");
      header.add("         %player% - 玩家名称");
      header.add("         %displayname% - 玩家显示名称");
      header.add("         & 颜色代码，如 &e 表示黄色");
      header.add("         支持PlaceholderAPI变量，如 %player_level%, %player_money%");
      header.add("  always-visible: 名称是否始终可见（不被方块遮挡）");
      header.add("");
      header.add("示例配置:");
      header.add("  如果只想在特定菜单启用NPC:");
      header.add("    enable-for-all-menus: false");
      header.add("    use-whitelist-mode: true");
      header.add("    enabled-menus: [\"main_menu\", \"lobby\", \"settings\"]");
      header.add("");
      header.add("  如果想禁用某些菜单的NPC:");
      header.add("    enable-for-all-menus: true");
      header.add("    use-whitelist-mode: false");
      header.add("    enabled-menus: [\"login\", \"register\"]");
      this.config.options().setHeader(header);
      this.save();
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
         this.plugin.getLogger().severe("[NPCConfig] 无法保存配置文件: " + var2.getMessage());
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
            this.plugin.getLogger().warning("[NPCConfig] 无效的UUID格式: " + uuidString);
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
