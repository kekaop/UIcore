package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MenuSlotTest {
   @Test
   void parsesPositionContentAndFallback() {
      YamlConfiguration config = new YamlConfiguration();
      config.set("x", -1.25);
      config.set("y", 0.5);
      config.set("z", 3.5);
      config.set("content", "{state.perk}");
      config.set("fallback", "empty");

      MenuSlot slot = MenuSlot.from("left", config);

      assertEquals("left", slot.id());
      assertEquals(-1.25, slot.x());
      assertEquals(0.5, slot.y());
      assertEquals(3.5, slot.z());
      assertEquals("{state.perk}", slot.content());
      assertEquals("empty", slot.fallback());
   }
}
