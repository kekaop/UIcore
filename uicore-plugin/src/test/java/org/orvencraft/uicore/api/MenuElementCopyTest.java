package org.orvencraft.uicore.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MenuElementCopyTest {
   @Test
   void specificationAndEditorDoNotShareNestedCollections() {
      List<String> commands = new ArrayList<>(List.of("first"));
      Map<String, Object> nested = new LinkedHashMap<>();
      nested.put("commands", commands);
      MenuElementSpec spec = MenuElementSpec.builder("button").set("nested", nested).build();

      commands.add("outside");
      @SuppressWarnings("unchecked")
      List<String> exported = (List<String>)((Map<?, ?>)spec.toConfiguration().get("nested")).get("commands");
      exported.add("returned");
      @SuppressWarnings("unchecked")
      List<String> secondCopy = (List<String>)((Map<?, ?>)spec.toConfiguration().get("nested")).get("commands");

      assertEquals(List.of("first"), secondCopy);

      YamlConfiguration source = new YamlConfiguration();
      source.set("nested", nested);
      MenuElementEditor editor = new MenuElementEditor(source);
      commands.add("later");
      @SuppressWarnings("unchecked")
      List<String> editorCopy = (List<String>)((Map<?, ?>)editor.toConfiguration().get("nested")).get("commands");
      assertEquals(List.of("first", "outside"), editorCopy);
   }

   @Test
   void snapshotIsDeeplyImmutable() {
      Map<String, Object> source = new LinkedHashMap<>();
      source.put("list", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("key", "value")))));
      MenuElementSnapshot snapshot = new MenuElementSnapshot("menu", "button", "text", "Button", 0, 0, 3.5, 1, source);
      source.clear();

      @SuppressWarnings("unchecked")
      List<Map<String, String>> list = (List<Map<String, String>>)snapshot.configValues().get("list");
      assertEquals("value", list.get(0).get("key"));
      assertThrows(UnsupportedOperationException.class, () -> list.add(Map.of()));
      assertThrows(UnsupportedOperationException.class, () -> list.get(0).put("other", "value"));
   }
}
