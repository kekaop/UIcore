package org.orvencraft.uicore.section;

import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.layout.MenuLayout;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SectionManager {
   private static volatile Map<String, Section> sections = Collections.emptyMap();

   public synchronized void addSection(String key, Section section) {
      Map<String, Section> updated = new LinkedHashMap<>(sections);
      updated.put(key, section);
      sections = Collections.unmodifiableMap(updated);
   }

   public Section get(String key) {
      return sections.get(key);
   }

   public boolean has(String key) {
      return sections.containsKey(key);
   }

   public synchronized void clear() {
      sections = Collections.emptyMap();
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
      return new LinkedHashMap<>(sections);
   }

   public boolean hasSection(String key) {
      return sections.containsKey(key);
   }

   public synchronized Map<String, Section> validateMenuConfigs() {
      File menuFolder = new File(UIcorePlugin.plugin.getDataFolder(), "menu");
      if (!menuFolder.exists()) {
         if (!menuFolder.mkdirs() && !menuFolder.isDirectory()) {
            throw new IllegalStateException("Could not create menu directory: " + menuFolder);
         }
      }
      return MenuConfigLoader.loadMenuSections(menuFolder);
   }

   /** Publishes a fully parsed menu set only after every input file succeeds. */
   public synchronized void loadAllMenuConfigs() {
      Map<String, Section> staged = this.validateMenuConfigs();
      sections = Collections.unmodifiableMap(new LinkedHashMap<>(staged));
   }
}
