package org.orvencraft.uicore.section;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.api.ConfigurationCopy;
import org.orvencraft.uicore.api.model.UiElementModel;
import org.orvencraft.uicore.npc.NPCMenuSettings;
import org.orvencraft.uicore.schematic.SchematicPlacement;
import org.orvencraft.uicore.util.TextContentUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class MenuConfigLoader {
   public static Map<String, Section> loadMenuSections(File folder) {
      if (!folder.isDirectory()) {
         throw new IllegalArgumentException("Menu path is not a directory: " + folder);
      }

      File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files == null) {
         throw new IllegalStateException("Could not list menu directory: " + folder);
      }

      Arrays.sort(files, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
      Map<String, Section> staged = new LinkedHashMap<>();
      for (File file : files) {
         YamlConfiguration config = loadStrict(file);
         for (String menuKey : config.getKeys(false)) {
            ConfigurationSection menuSection = config.getConfigurationSection(menuKey);
            if (menuSection == null) {
               throw new IllegalArgumentException("Menu '" + menuKey + "' in " + file.getName() + " must be a configuration section");
            }
            if (staged.containsKey(menuKey)) {
               throw new IllegalArgumentException("Duplicate menu id '" + menuKey + "' in " + file.getName());
            }
            staged.put(menuKey, loadSingleMenu(menuKey, menuSection));
         }
      }
      return staged;
   }

   static YamlConfiguration loadStrict(File file) {
      YamlConfiguration config = new YamlConfiguration();
      try {
         config.load(file);
         return config;
      } catch (IOException | InvalidConfigurationException exception) {
         throw new IllegalArgumentException("Could not parse menu file '" + file.getName() + "': " + exception.getMessage(), exception);
      }
   }

   private static Section loadSingleMenu(String menuKey, ConfigurationSection menuSection) {
      validateSchemaVersion(menuKey, menuSection);
      ConfigurationSection cameraSection = menuSection.getConfigurationSection("camera-position");
      if (cameraSection == null) {
         throw new IllegalArgumentException("Menu '" + menuKey + "' is missing camera-position");
      } else {
         validateValues(menuKey, menuSection);
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

         Section section = new Section(distance, world, x, y, z, yaw, pitch, permission, autoCommands, autoCommandDelays, autoCommandsEnabled);
         section.privacy = MenuPrivacySettings.from(menuSection);
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
         section.schematics = SchematicPlacement.from(menuSection);
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

         section.key = menuKey;
         return section;
      }
   }

   private static void validateSchemaVersion(String menuKey, ConfigurationSection menuSection) {
      if (!menuSection.contains("schema-version")) return; // Existing menus use version 1 implicitly.
      Object version = menuSection.get("schema-version");
      if (!(version instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())
         || (number.intValue() != 1 && number.intValue() != 2))
         throw new IllegalArgumentException("Menu '" + menuKey + "' has unsupported schema-version; expected 1 or 2");
   }

   private static void validateValues(String path, ConfigurationSection config) {
      if (config.contains("permission") && !config.isString("permission"))
         throw new IllegalArgumentException(path + ".permission must be a string");
      for (String key : config.getKeys(true)) {
         Object value = config.get(key);
         if (value instanceof Number number && (!Double.isFinite(number.doubleValue()) || Math.abs(number.doubleValue()) > 30_000_000D))
            throw new IllegalArgumentException("Non-finite or excessive numeric value at " + path + "." + key);
         if (value instanceof String text && text.length() > 16384)
            throw new IllegalArgumentException("Text too long at " + path + "." + key);
      }
      if (config.contains("material")) {
         String raw = config.getString("material", "");
         if (!raw.contains("%") && !raw.contains("{") && org.bukkit.Material.matchMaterial(raw) == null)
            throw new IllegalArgumentException("Unknown material at " + path + ".material");
      }
      for (String key : List.of("scale", "text-size", "hitbox.width", "hitbox.height", "hitbox.depth", "hover-enlarge.scale")) {
         if (config.contains(key) && (config.getDouble(key) <= 0D || config.getDouble(key) > 128D))
            throw new IllegalArgumentException("Invalid size at " + path + "." + key);
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
      UiElementModel model = ConfigurationCopy.model(layoutConfig);
      return loadLayout(menuKey, layoutKey, model, defaultWorld);
   }

   public static MenuLayout loadLayout(String menuKey, String layoutKey, UiElementModel model, String defaultWorld) {
      YamlConfiguration layoutConfig = ConfigurationCopy.configuration(model.properties());
      validateValues(menuKey + ":" + layoutKey, layoutConfig);
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
      if (worldObj == null && teleportEnabled && !backOriginal)
         throw new IllegalArgumentException(menuKey + ":" + layoutKey + " has unknown teleport world: " + targetWorld);

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
