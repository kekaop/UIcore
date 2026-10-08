package org.orvencraft.uicore.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.api.model.UiElementModel;

class UiElementModelTest {
   @Test
   void modelIsIndependentOfMutableYamlAndAuthoringMaps() {
      List<String> commands = new ArrayList<>(List.of("say original"));
      Map<String, Object> source = new LinkedHashMap<>();
      source.put("command", commands);
      source.put("action", new LinkedHashMap<>(Map.of("type", "example:hello", "target", "player")));

      UiElementModel model = UiElementModel.of(source);
      commands.add("say changed");
      ((Map<String, Object>)source.get("action")).put("target", "other");
      assertEquals(List.of("say original"), model.get("command"));
      assertEquals("player", model.get("action.target"));
      assertThrows(UnsupportedOperationException.class, () -> model.properties().put("name", "changed"));

      UiElementModel edited = model.with("action.target", "updated");
      assertEquals("player", model.get("action.target"));
      assertEquals("updated", edited.get("action.target"));
      assertEquals("updated", ((Map<?, ?>)ConfigurationCopy.configuration(edited.properties()).get("action")).get("target"));
   }

   @Test
   void javaBuilderAndYamlEditorShareModelShape() {
      MenuElementSpec spec = MenuElementSpec.builder("button")
         .text("Accept")
         .action("quests:accept", Map.of("quest", "daily_1"))
         .build();
      YamlConfiguration yaml = spec.toConfiguration();
      MenuElementEditor editor = new MenuElementEditor(yaml).text("Updated");

      assertEquals(spec.model().get("action.quest"), editor.model().get("action.quest"));
      assertEquals("quests:accept", yaml.getConfigurationSection("action").getString("type"));
      assertEquals("Updated", editor.toConfiguration().getString("name"));
      assertFalse(spec.model().properties().values().stream()
         .anyMatch(value -> value instanceof org.bukkit.configuration.ConfigurationSection));
   }
}
