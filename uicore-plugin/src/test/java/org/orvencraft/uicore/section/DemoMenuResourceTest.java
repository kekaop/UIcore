package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.orvencraft.uicore.layout.MenuLayout;

class DemoMenuResourceTest {
   @TempDir Path installed;

   @Test
   void freshInstallationHasThreeMenusWithWorkingNavigationAndExit() throws Exception {
      Map<String, ConfigurationSection> menus = installMenus();
      assertEquals(Set.of("demo_main", "demo_gallery", "demo_states"), menus.keySet());
      for (var entry : menus.entrySet()) {
         String id = entry.getKey();
         ConfigurationSection menu = entry.getValue();
         assertEquals("player", menu.getString("camera-position.type"), id);
         assertFalse(menu.getBoolean("npc.enabled"), id);
         ConfigurationSection layout = menu.getConfigurationSection("layout");
         assertNotNull(layout, id);
         Set<String> destinations = new java.util.HashSet<>();
         for (String key : layout.getKeys(false)) {
            ConfigurationSection element = layout.getConfigurationSection(key);
            assertNotNull(element, id + "." + key);
            if (element.getBoolean("next-menu.enabled")) {
               destinations.add(element.getString("next-menu.menu"));
            }
         }
         Set<String> otherMenus = new java.util.HashSet<>(menus.keySet());
         otherMenus.remove(id);
         assertEquals(otherMenus, destinations, id + " navigation");
         assertTrue(layout.getBoolean("close.stop-menu.enabled"), id);
         assertTrue(layout.getBoolean("close.stop-menu.teleport.back-original"), id);
      }
   }

   @Test
   void shippedElementsParseAndStateChangesResolveTheirTargets() throws Exception {
      for (var entry : installMenus().entrySet()) {
         String id = entry.getKey();
         ConfigurationSection menu = entry.getValue();
         for (String group : List.of("layout", "templates")) {
            ConfigurationSection elements = menu.getConfigurationSection(group);
            if (elements == null) continue;
            for (String key : elements.getKeys(false)) {
               ConfigurationSection source = elements.getConfigurationSection(key);
               MenuLayout parsed = parse(id, key, source);
               assertFalse(parsed.isItemRotate(), id + "." + key);
               assertFalse(source.contains("drag"), id + "." + key);
               assertFalse(source.contains("drag-group"), id + "." + key);
               for (var change : parsed.getClickElementStates().entrySet()) {
                  ConfigurationSection target = menu.getConfigurationSection("layout." + change.getKey());
                  assertNotNull(target, id + " missing state target " + change.getKey());
                  assertTrue(parse(id, change.getKey(), target).hasElementState(change.getValue()),
                     id + " unknown state " + change.getValue());
               }
               boolean interactive = !source.getStringList("command").isEmpty()
                  || source.getBoolean("next-menu.enabled") || source.getBoolean("stop-menu.enabled")
                  || source.getBoolean("toggle-content.enabled") || source.contains("set-states")
                  || "input-field".equals(source.getString("type")) || source.contains("hover-tooltip");
               if (interactive) {
                  assertTrue(parsed.hasCustomHitbox(), id + "." + key);
                  assertTrue(parsed.getHitboxWidth() > 0 && parsed.getHitboxHeight() > 0
                     && parsed.getHitboxDepth() > 0, id + "." + key);
               }
            }
         }
      }
   }

   @Test
   void gallerySelectorsResolveExistingSlotAndTemplates() throws Exception {
      ConfigurationSection menu = installMenus().get("demo_gallery");
      ConfigurationSection templates = menu.getConfigurationSection("templates");
      ConfigurationSection slots = menu.getConfigurationSection("slots");
      assertNotNull(templates);
      assertNotNull(slots);
      for (String slot : slots.getKeys(false)) {
         for (String field : List.of("content", "fallback")) {
            assertTrue(templates.isConfigurationSection(slots.getString(slot + "." + field)));
         }
      }
      Set<String> selectable = new java.util.HashSet<>();
      ConfigurationSection layout = menu.getConfigurationSection("layout");
      for (String key : layout.getKeys(false)) {
         for (String command : layout.getStringList(key + ".command")) {
            if (!command.startsWith("[player] uicore slot set ")) continue;
            String[] args = command.split("\\s+");
            assertEquals(8, args.length, command);
            assertEquals("%player%", args[4], command);
            assertEquals("demo_gallery", args[5], command);
            assertTrue(slots.isConfigurationSection(args[6]), command);
            assertTrue(templates.isConfigurationSection(args[7]), command);
            assertTrue(layout.getStringList(key + ".click-requirements.permissions").contains("uicore.slot"));
            selectable.add(args[7]);
         }
      }
      assertEquals(templates.getKeys(false), selectable);
   }

   private Map<String, ConfigurationSection> installMenus() throws Exception {
      Map<String, ConfigurationSection> menus = new LinkedHashMap<>();
      try (var manifest = getClass().getResourceAsStream("/menu-resources.txt")) {
         assertNotNull(manifest);
         for (String resource : new String(manifest.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
            if (resource.isBlank() || resource.startsWith("#")) continue;
            Path target = installed.resolve(resource);
            Files.createDirectories(target.getParent());
            try (var source = getClass().getResourceAsStream("/" + resource)) {
               assertNotNull(source, resource);
               Files.copy(source, target);
            }
            YamlConfiguration config = new YamlConfiguration();
            config.load(target.toFile());
            assertEquals(1, config.getKeys(false).size(), resource);
            for (String key : config.getKeys(false)) {
               assertNull(menus.put(key, config.getConfigurationSection(key)), "Duplicate menu " + key);
            }
         }
      }
      return menus;
   }

   private static MenuLayout parse(String menu, String key, ConfigurationSection source) {
      MenuLayout layout = new MenuLayout(key, source.getString("name", key), source.getStringList("command"),
         false, source.getDouble("x"), source.getDouble("y"), source.getDouble("z", 3.5),
         false, false, null, false, List.of(), 0.0F, 0.0F, 0.0F, "", false, "", "");
      layout.setOwnerMenuKey(menu);
      layout.loadConfig(source);
      return layout;
   }
}
