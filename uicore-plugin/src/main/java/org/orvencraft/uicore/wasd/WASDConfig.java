package org.orvencraft.uicore.wasd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WASDConfig {
   private final JavaPlugin plugin;
   private File configFile;
   private FileConfiguration config;
   private boolean enabled = true;
   private boolean debugMode = false;
   private double horizontalThreshold = 0.5;
   private double dotProductThreshold = 0.5;
   private long selectionCooldown = 500L;
   private boolean soundEnabled = true;
   private String selectionSound = "minecraft:entity.experience_orb.pickup";
   private float soundVolume = 1.0F;
   private float soundPitch = 1.0F;
   private boolean enableForAllMenus = true;
   private List<String> enabledMenus = new ArrayList<>();
   private boolean useWhitelistMode = false;
   private Set<String> disabledPlayers = new HashSet<>();

   public WASDConfig(JavaPlugin plugin) {
      this.plugin = plugin;
      this.load();
   }

   public void load() {
      this.configFile = new File(this.plugin.getDataFolder(), "wasd_config.yml");
      if (!this.configFile.exists()) {
         this.createDefaultConfig();
      }

      this.config = org.orvencraft.uicore.config.ConfigFiles.loadStrict(this.configFile);
      this.readConfig();
   }

   private void createDefaultConfig() {
      this.config = new YamlConfiguration();
      this.config.set("enabled", true);
      this.config.set("debug-mode", false);
      this.config.set("navigation.horizontal-threshold", 0.5);
      this.config.set("navigation.dot-product-threshold", 0.5);
      this.config.set("navigation.selection-cooldown", 500);
      this.config.set("sound.enabled", true);
      this.config.set("sound.selection-sound", "minecraft:entity.experience_orb.pickup");
      this.config.set("sound.volume", 1.0);
      this.config.set("sound.pitch", 1.0);
      this.config.set("menu-settings.enable-for-all-menus", true);
      this.config.set("menu-settings.enabled-menus", new ArrayList());
      this.config.set("menu-settings.use-whitelist-mode", false);
      this.config.set("disabled-players", new ArrayList());
      List<String> header = new ArrayList<>();
      header.add("===========================================");
      header.add("WASD navigation configuration");
      header.add("UIcore WASD Navigation Module");
      header.add("===========================================");
      header.add("");
      header.add("enabled: enables WASD navigation");
      header.add("debug-mode: writes detailed diagnostics to the server log");
      header.add("");
      header.add("navigation: navigation settings");
      header.add("  horizontal-threshold: horizontal selection threshold");
      header.add("  dot-product-threshold: left/right dot-product threshold");
      header.add("  selection-cooldown: selection cooldown in milliseconds");
      header.add("");
      header.add("sound: sound settings");
      header.add("  enabled: enables selection sounds");
      header.add("  selection-sound: sound played on selection");
      header.add("  volume/pitch: sound volume and pitch");
      header.add("");
      header.add("menu-settings: menu assignment settings");
      header.add("  enable-for-all-menus: enables WASD navigation for every menu");
      header.add("  enabled-menus: menus affected by the selected list mode");
      header.add("  use-whitelist-mode: true uses the list as an allowlist");
      this.config.options().setHeader(header);
      this.save();
   }

   private void readConfig() {
      this.enabled = this.config.getBoolean("enabled", true);
      this.debugMode = this.config.getBoolean("debug-mode", false);
      this.horizontalThreshold = this.config.getDouble("navigation.horizontal-threshold", 0.5);
      this.dotProductThreshold = this.config.getDouble("navigation.dot-product-threshold", 0.5);
      this.selectionCooldown = this.config.getLong("navigation.selection-cooldown", 500L);
      this.soundEnabled = this.config.getBoolean("sound.enabled", true);
      this.selectionSound = this.config.getString("sound.selection-sound", "minecraft:entity.experience_orb.pickup");
      this.soundVolume = (float)this.config.getDouble("sound.volume", 1.0);
      this.soundPitch = (float)this.config.getDouble("sound.pitch", 1.0);
      this.enableForAllMenus = this.config.getBoolean("menu-settings.enable-for-all-menus", true);
      this.enabledMenus = this.config.getStringList("menu-settings.enabled-menus");
      this.useWhitelistMode = this.config.getBoolean("menu-settings.use-whitelist-mode", false);
      this.disabledPlayers = new HashSet<>(this.config.getStringList("disabled-players"));
   }

   public void reload() {
      this.load();
   }

   public void save() {
      try {
         this.config.save(this.configFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("[WASDConfig] Could not save configuration: " + var2.getMessage());
      }
   }

   public boolean isMenuEnabled(String menuKey) {
      return isMenuEnabled(this.enabled, this.enableForAllMenus, this.useWhitelistMode, this.enabledMenus, menuKey);
   }

   static boolean isMenuEnabled(boolean enabled, boolean enableForAllMenus, boolean useWhitelistMode, List<String> enabledMenus, String menuKey) {
      if (!enabled || menuKey == null) {
         return false;
      }
      boolean listed = enabledMenus != null && enabledMenus.contains(menuKey);
      if (enableForAllMenus) {
         return useWhitelistMode ? listed : !listed;
      }
      return listed;
   }

   public boolean isPlayerDisabled(String playerName) {
      return this.disabledPlayers.contains(playerName.toLowerCase());
   }

   public boolean togglePlayer(String playerName) {
      String lowerName = playerName.toLowerCase();
      if (this.disabledPlayers.contains(lowerName)) {
         this.disabledPlayers.remove(lowerName);
         this.saveDisabledPlayers();
         return true;
      } else {
         this.disabledPlayers.add(lowerName);
         this.saveDisabledPlayers();
         return false;
      }
   }

   private void saveDisabledPlayers() {
      this.config.set("disabled-players", new ArrayList<>(this.disabledPlayers));
      this.save();
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isDebugMode() {
      return this.debugMode;
   }

   public double getHorizontalThreshold() {
      return this.horizontalThreshold;
   }

   public double getDotProductThreshold() {
      return this.dotProductThreshold;
   }

   public long getSelectionCooldown() {
      return this.selectionCooldown;
   }

   public boolean isSoundEnabled() {
      return this.soundEnabled;
   }

   public String getSelectionSound() {
      return this.selectionSound;
   }

   public float getSoundVolume() {
      return this.soundVolume;
   }

   public float getSoundPitch() {
      return this.soundPitch;
   }

   public boolean isEnableForAllMenus() {
      return this.enableForAllMenus;
   }

   public List<String> getEnabledMenus() {
      return new ArrayList<>(this.enabledMenus);
   }

   public boolean isUseWhitelistMode() {
      return this.useWhitelistMode;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      this.config.set("enabled", enabled);
      this.save();
   }

   public void setDebugMode(boolean debugMode) {
      this.debugMode = debugMode;
      this.config.set("debug-mode", debugMode);
      this.save();
   }
}
