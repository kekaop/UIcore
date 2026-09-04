package org.orvencraft.uicore.section;

import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.layout.MenuLayout;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SectionManager {
   private static HashMap<String, Section> sections = new HashMap<>();

   public void addSection(String key, Section section) {
      sections.put(key, section);
   }

   public Section get(String key) {
      return sections.get(key);
   }

   public boolean has(String key) {
      return sections.containsKey(key);
   }

   public void clear() {
      sections.clear();
   }

   public Set<String> keySet() {
      return sections.keySet();
   }

   public MenuLayout getLayout(String full) {
      if (full != null && full.contains(":")) {
         String[] parts = full.split(":", 2);
         Section section = sections.get(parts[0]);
         return section == null ? null : section.get(parts[1]);
      } else {
         return null;
      }
   }

   public Map<String, Section> getAll() {
      return new HashMap<>(sections);
   }

   public boolean hasSection(String key) {
      return sections.containsKey(key);
   }

   public void loadAllMenuConfigs() {
      this.clear();
      File menuFolder = new File(UIcorePlugin.plugin.getDataFolder(), "menu");
      if (!menuFolder.exists()) {
         menuFolder.mkdirs();
      } else {
         MenuConfigLoader.loadAllMenuFiles(menuFolder, this);
      }
   }
}
