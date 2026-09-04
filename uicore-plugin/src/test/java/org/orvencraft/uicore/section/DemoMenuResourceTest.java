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
}
