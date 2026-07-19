package com.cmenu.ui.section;

import com.cmenu.ui.layout.MenuLayout;
import java.io.File;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class MenuConfigLoader {
   public static void loadAllMenuFiles(File folder, SectionManager sectionManager) {
      if (folder.exists() && folder.isDirectory()) {
         File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
         if (files != null) {
            for (File file : files) {
               try {
                  YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                  for (String menuKey : config.getKeys(false)) {
                     ConfigurationSection menuSection = config.getConfigurationSection(menuKey);
                     if (menuSection != null) {
                        loadSingleMenu(menuKey, menuSection, sectionManager);
                     }
                  }
               } catch (Exception var12) {
                  Bukkit.getLogger().warning("加载菜单文件失败: " + file.getName() + " - " + var12.getMessage());
               }
            }
         }
      }
   }

   private static void loadSingleMenu(String menuKey, ConfigurationSection menuSection, SectionManager sectionManager) {
      ConfigurationSection cameraSection = menuSection.getConfigurationSection("camera-position");
      if (cameraSection != null) {
         String world = cameraSection.getString("world", "world");
         double distance = cameraSection.getDouble("distance", 1.5);
         double x = cameraSection.getDouble("x", 0.0);
         double y = cameraSection.getDouble("y", 0.0);
         double z = cameraSection.getDouble("z", 0.0);
         float yaw = (float)cameraSection.getDouble("yaw", 0.0);
         float pitch = (float)cameraSection.getDouble("pitch", 0.0);
         String permission = menuSection.getString("permission", "");
         ConfigurationSection autoCmdSection = menuSection.getConfigurationSection("auto-commands");
         boolean autoCommandsEnabled = false;
         List<String> autoCommands = List.of();
         List<Long> autoCommandDelays = List.of();
         if (autoCmdSection != null) {
            autoCommandsEnabled = autoCmdSection.getBoolean("enabled", false);
            autoCommands = autoCmdSection.getStringList("commands");
            autoCommandDelays = autoCmdSection.getLongList("delays");
         }

         boolean wasdEnabled = menuSection.getBoolean("wasd-enabled", true);
         Section section = new Section(distance, world, x, y, z, yaw, pitch, permission, autoCommands, autoCommandDelays, autoCommandsEnabled, wasdEnabled);
         ConfigurationSection layoutSection = menuSection.getConfigurationSection("layout");
         if (layoutSection != null) {
            for (String layoutKey : layoutSection.getKeys(false)) {
               ConfigurationSection layoutConfig = layoutSection.getConfigurationSection(layoutKey);
               if (layoutConfig != null) {
                  MenuLayout layout = loadLayout(menuKey, layoutKey, layoutConfig, world);
                  section.add(layoutKey, layout);
               }
            }
         }

         sectionManager.addSection(menuKey, section);
      }
   }

   public static MenuLayout loadLayout(String menuKey, String layoutKey, ConfigurationSection layoutConfig, String defaultWorld) {
      String name = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(layoutConfig.getString("name", layoutKey)));
      double lx = layoutConfig.getDouble("x", 0.0);
      double ly = layoutConfig.getDouble("y", 0.0);
      double lz = layoutConfig.getDouble("z", 0.0);
      List<String> commands = layoutConfig.getStringList("command");
      boolean stop = layoutConfig.getBoolean("stop-menu.enabled", false);
      boolean teleportEnabled = layoutConfig.getBoolean("stop-menu.teleport.enabled", false);
      boolean backOriginal = layoutConfig.getBoolean("stop-menu.teleport.back-original", false);
      String targetWorld = layoutConfig.getString("stop-menu.teleport.world", defaultWorld);
      double targetX = layoutConfig.getDouble("stop-menu.teleport.x", 0.0);
      double targetY = layoutConfig.getDouble("stop-menu.teleport.y", 64.0);
      double targetZ = layoutConfig.getDouble("stop-menu.teleport.z", 0.0);
      boolean stopCmdEnabled = layoutConfig.getBoolean("stop-menu.command.enabled", false);
      List<String> stopCommands = layoutConfig.getStringList("stop-menu.command.list");
      ConfigurationSection tiltSection = layoutConfig.getConfigurationSection("tilt");
      float tiltX = 0.0F;
      float tiltY = 0.0F;
      float tiltZ = 0.0F;
      if (tiltSection != null) {
         tiltX = (float)tiltSection.getDouble("x", 0.0);
         tiltY = (float)tiltSection.getDouble("y", 0.0);
         tiltZ = (float)tiltSection.getDouble("z", 0.0);
      }

      boolean nextMenuEnabled = layoutConfig.getBoolean("next-menu.enabled", false);
      String nextMenuKey = layoutConfig.getString("next-menu.menu", "");
      String buttonPermission = layoutConfig.getString("permission", "");
      String linkCommand = layoutConfig.getString("link", "");
      World worldObj = Bukkit.getWorld(targetWorld);
      if (worldObj == null) {
         worldObj = Bukkit.getWorld(defaultWorld);
         if (worldObj == null && !Bukkit.getWorlds().isEmpty()) {
            worldObj = (World)Bukkit.getWorlds().get(0);
         }
      }

      Location teleportLoc = new Location(worldObj, targetX, targetY, targetZ, 0.0F, 0.0F);
      MenuLayout layout = new MenuLayout(
         layoutKey,
         name,
         commands,
         stop,
         lx,
         ly,
         lz,
         teleportEnabled,
         backOriginal,
         teleportLoc,
         stopCmdEnabled,
         stopCommands,
         tiltX,
         tiltY,
         tiltZ,
         buttonPermission,
         nextMenuEnabled,
         nextMenuKey,
         linkCommand
      );
      layout.setOwnerMenuKey(menuKey);
      layout.loadConfig(layoutConfig);
      return layout;
   }
}
