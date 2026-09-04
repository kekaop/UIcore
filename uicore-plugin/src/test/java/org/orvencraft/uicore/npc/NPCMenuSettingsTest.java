package org.orvencraft.uicore.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class NPCMenuSettingsTest {
   @Test
   void parsesPerMenuNpcAndCustomModel() throws Exception {
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.loadFromString(
         """
         npc:
           enabled: false
           offset: {x: 1.0, y: 2.0, z: -3.0}
           yaw-offset: 90
           custom-model:
             enabled: true
             model: merchant
         """
      );

      NPCMenuSettings settings = NPCMenuSettings.from(yaml.getConfigurationSection("npc"));
      assertFalse(settings.enabled());
      assertEquals(1.0, settings.offsetX());
      assertEquals(90.0F, settings.yawOffset());
      assertEquals("merchant", settings.customModel());
   }
}
