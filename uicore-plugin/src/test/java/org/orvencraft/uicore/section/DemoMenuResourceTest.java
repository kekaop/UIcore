package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DemoMenuResourceTest {
   @org.junit.jupiter.api.io.TempDir java.nio.file.Path installed;

   @Test
   void dragDemoCombinesGroupMovementAndTargetRotation() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_drag.yml");
      assertNotNull(stream);
      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );
      assertEquals("sample-list", config.getString("demo_drag.layout.list_first.drag-group"));
      assertEquals("sample-list", config.getString("demo_drag.layout.list_second.drag-group"));
      assertEquals("move-group", config.getString("demo_drag.layout.move_list.drag.mode"));
      assertEquals("sample-list", config.getString("demo_drag.layout.move_list.drag.group"));
      assertEquals("rotate-element", config.getString("demo_drag.layout.turn_model.drag.mode"));
      assertEquals("model", config.getString("demo_drag.layout.turn_model.drag.target"));
   }

   @Test
   void cleanInstallationIncludesEveryStaticMenuTransitionTarget() throws Exception {
      java.util.Set<String> definitions = new java.util.HashSet<>();
      java.util.List<YamlConfiguration> configs = new java.util.ArrayList<>();
      try (var manifest = getClass().getResourceAsStream("/menu-resources.txt")) {
         assertNotNull(manifest);
         for (String resource : new String(manifest.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
            if (resource.isBlank() || resource.startsWith("#")) continue;
            java.nio.file.Path target = installed.resolve(resource);
            java.nio.file.Files.createDirectories(target.getParent());
            try (var source = getClass().getResourceAsStream("/" + resource)) {
               assertNotNull(source, resource);
               java.nio.file.Files.copy(source, target);
            }
            YamlConfiguration config = new YamlConfiguration();
            config.load(target.toFile());
            definitions.addAll(config.getKeys(false)); configs.add(config);
         }
      }
      for (YamlConfiguration config : configs) for (String key : config.getKeys(true)) {
         if (key.endsWith("next-menu.menu")) {
            String target = config.getString(key, "");
            if (!target.isBlank() && !target.contains("%") && !target.contains("{"))
               assertTrue(definitions.contains(target), "Missing installed transition target: " + target);
         }
      }
      assertTrue(definitions.contains("demo_styles"));
      assertTrue(definitions.contains("demo_toggle_input"));
   }

   @Test
   void entranceNpcDemoIsValidYaml() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_entrance_npc.yml");
      assertNotNull(stream);

      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );

      assertNotNull(config.getConfigurationSection("demo_entrance_npc.layout"));
      assertTrue(config.getBoolean("demo_entrance_npc.npc.enabled"));
      assertEquals(1.5, config.getDouble("demo_entrance_npc.camera-position.distance"));
      assertEquals(
         60L,
         config.getLong("demo_entrance_npc.layout.delayed_message.entrance-animation.delay")
      );
   }

   @Test
   void playerRelativeDemoIsValidYaml() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_player_menu.yml");
      assertNotNull(stream);

      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );

      assertEquals("player", config.getString("demo_player_menu.camera-position.type"));
      assertEquals(1.5, config.getDouble("demo_player_menu.camera-position.distance"));
      assertNotNull(config.getConfigurationSection("demo_player_menu.layout"));
   }

   @Test
   void slotDemoDeclaresTemplatesAndDynamicSlots() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_slots.yml");
      assertNotNull(stream);

      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );

      assertNotNull(config.getConfigurationSection("demo_slots.templates.guardian"));
      assertNotNull(config.getConfigurationSection("demo_slots.templates.haste"));
      assertNotNull(config.getConfigurationSection("demo_slots.templates.vampirism"));
      assertEquals("guardian", config.getString("demo_slots.slots.perk_left.content"));
      assertEquals("%orvenperks_roll_2%", config.getString("demo_slots.slots.perk_center.content"));
      assertEquals("{state.selected_perk}", config.getString("demo_slots.slots.perk_right.content"));
      assertEquals(3.5, config.getDouble("demo_slots.slots.perk_right.z"));
   }

   @Test
   void dynamicPositionDemoUsesExpressionsAndWorldDepth() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_dynamic_position.yml");
      assertNotNull(stream);

      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );

      assertEquals("0.9 * sin(t * 1.4)", config.getString("demo_dynamic_position.layout.horizontal.x"));
      assertEquals("0.25 * t", config.getString("demo_dynamic_position.layout.rising.y"));
      assertEquals(3.5, config.getDouble("demo_dynamic_position.layout.horizontal.z"));
      assertEquals(0.18, config.getDouble("demo_dynamic_position.layout.horizontal.position-update.smoothing"));
   }

   @Test
   void itemCarouselAvoidsEntityOutlinePostEffects() {
      InputStream stream = getClass().getResourceAsStream("/menu/item_carousel.yml");
      assertNotNull(stream);

      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );

      assertEquals(3.5, config.getDouble("item_carousel.layout.next.z"));
      assertTrue(config.getBoolean("item_carousel.layout.next.hover-enlarge.enabled"));
      for (String template : config.getConfigurationSection("item_carousel.templates").getKeys(false)) {
         assertTrue(!config.getBoolean("item_carousel.templates." + template + ".glow"));
      }
      assertTrue(config.getBoolean("item_carousel.templates.item_2_from_right.entrance-animation.enabled"));
      assertEquals("right", config.getString("item_carousel.templates.item_2_from_right.entrance-animation.from"));
   }

   @Test
   void playerAvatarDemoUsesViewerSkinAndHeadTransform() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_player_avatar.yml");
      assertNotNull(stream);

      YamlConfiguration config = YamlConfiguration.loadConfiguration(
         new InputStreamReader(stream, StandardCharsets.UTF_8)
      );

      String path = "demo_player_avatar.layout.avatar";
      assertEquals("player-avatar", config.getString(path + ".type"));
      assertEquals("self", config.getString(path + ".player"));
      assertTrue(config.getBoolean(path + ".face-only"));
      assertEquals(false, config.getBoolean(path + ".rotate"));
      assertEquals(3.5, config.getDouble(path + ".z"));
   }

   @Test
   void elementStatesDemoKeepsHitboxesOnItsButtons() {
      InputStream stream = getClass().getResourceAsStream("/menu/demo_element_states.yml");
      assertNotNull(stream);
      YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
      for (String button : new String[]{"show", "hide", "close"}) {
         assertNotNull(config.getConfigurationSection("demo_element_states.layout." + button + ".hitbox"), button);
      }
   }
}
