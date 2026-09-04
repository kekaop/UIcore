package org.orvencraft.uicore.section;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.npc.NPCMenuSettings;
import org.orvencraft.uicore.util.TextContentUtils;
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
                  Bukkit.getLogger().warning("Could not load menu file: " + file.getName() + " - " + var12.getMessage());
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
         MenuPositionType positionType = MenuPositionType.parse(cameraSection.getString("type", cameraSection.getString("mode", "fixed")));
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
         section.positionType = positionType;
         ConfigurationSection playerOffset = cameraSection.getConfigurationSection("offset");
         if (playerOffset != null) {
            section.playerOffsetRight = playerOffset.getDouble("right", playerOffset.getDouble("x", 0.0));
            section.playerOffsetUp = playerOffset.getDouble("up", playerOffset.getDouble("y", 0.0));
            section.playerOffsetForward = playerOffset.getDouble("forward", playerOffset.getDouble("z", 0.0));
         }
         section.playerYawOffset = (float)cameraSection.getDouble("yaw-offset", 0.0);
         section.playerPitchOffset = (float)cameraSection.getDouble("pitch-offset", 0.0);
         section.npcSettings = NPCMenuSettings.from(menuSection.getConfigurationSection("npc"));
         ConfigurationSection templateSection = menuSection.getConfigurationSection("templates");
         if (templateSection != null) {
            for (String templateKey : templateSection.getKeys(false)) {
               ConfigurationSection templateConfig = templateSection.getConfigurationSection(templateKey);
               if (templateConfig != null) {
                  section.addTemplate(templateKey, loadLayout(menuKey, templateKey, templateConfig, world));
               }
            }
         }

         ConfigurationSection slotSection = menuSection.getConfigurationSection("slots");
         if (slotSection != null) {
            for (String slotKey : slotSection.getKeys(false)) {
               ConfigurationSection slotConfig = slotSection.getConfigurationSection(slotKey);
               if (slotConfig != null) {
                  MenuSlot slot = MenuSlot.from(slotKey, slotConfig);
                  section.addSlot(slotKey, slot);
                  validateSlotReference(menuKey, slot, section);
               }
            }
         }

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

   private static void validateSlotReference(String menuKey, MenuSlot slot, Section section) {
      if (!slot.fallback().isEmpty() && !section.hasTemplate(slot.fallback())) {
         Bukkit.getLogger().warning("Menu '" + menuKey + "' slot '" + slot.id() + "' references missing fallback template '" + slot.fallback() + "'.");
      }

      String content = slot.content();
      boolean dynamic = content.contains("%") || content.contains("{state.");
      if (!content.isEmpty() && !dynamic && !section.hasTemplate(content)) {
         Bukkit.getLogger().warning("Menu '" + menuKey + "' slot '" + slot.id() + "' references missing template '" + content + "'.");
      }
   }

   public static MenuLayout loadLayout(String menuKey, String layoutKey, ConfigurationSection layoutConfig, String defaultWorld) {
      String name = ChatColor.translateAlternateColorCodes(
         '&',
         TextContentUtils.normalizeLineBreaks(Objects.requireNonNull(layoutConfig.getString("name", layoutKey)))
      );
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
