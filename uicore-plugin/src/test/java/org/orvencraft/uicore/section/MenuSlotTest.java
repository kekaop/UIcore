package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MenuSlotTest {
   @Test
   void preservesNestedInteractionSectionsWhenMaterializingASlot() throws Exception {
      var templateConfig = new YamlConfiguration();
      templateConfig.loadFromString("name: Knob\nhitbox: {width: 0.2, height: 0.2, depth: 0.2}\n");
      var template = new org.orvencraft.uicore.layout.MenuLayout("knob", "Knob", java.util.List.of(), false,
         0, 0, 0, false, false, null, false, java.util.List.of(), 0, 0, 0, "", false, "", "");
      template.loadConfig(templateConfig);
      var slotConfig = new YamlConfiguration();
      slotConfig.loadFromString("parent: root\nslider: {state: value}\ndrag: {axis: x, bounds: {x: [-1, 1]}}\nhitbox: {width: 0.8, height: 0.5, depth: 0.3}\non-drop: ['[state] dropped yes']\n");
      var merged = MenuSlot.from("slider", slotConfig).materializedConfig(template);
      var interaction = org.orvencraft.uicore.layout.ElementInteraction.from(merged);
      assertEquals("root", interaction.parent());
      assertEquals("value", interaction.sliderState());
      assertEquals(0.8, merged.getDouble("hitbox.width"));
      assertEquals(-1, interaction.minX());
      assertEquals(java.util.List.of("[state] dropped yes"), interaction.onDrop());
      assertEquals(0.2, template.copySourceConfig().getDouble("hitbox.width"));
   }

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
