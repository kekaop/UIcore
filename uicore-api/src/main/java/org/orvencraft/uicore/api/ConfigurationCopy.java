package org.orvencraft.uicore.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.orvencraft.uicore.api.model.UiElementModel;

/** Copies configuration values without retaining mutable YAML maps or lists. */
public final class ConfigurationCopy {
   private ConfigurationCopy() {
   }

   public static YamlConfiguration configuration(ConfigurationSection source) {
      return configuration(model(source).properties());
   }

   public static YamlConfiguration configuration(Map<String, ?> source) {
      YamlConfiguration copy = new YamlConfiguration();
      if (source != null) setValues(copy, source);
      return copy;
   }

   private static void setValues(ConfigurationSection target, Map<String, ?> values) {
      for (Map.Entry<String, ?> entry : values.entrySet()) {
         Object value = entry.getValue();
         if (value instanceof ConfigurationSection section) value = section.getValues(false);
         if (value instanceof UiElementModel.Section section) {
            setValues(target.createSection(entry.getKey()), section.values());
         } else if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> child : map.entrySet()) {
               if (child.getKey() != null) nested.put(String.valueOf(child.getKey()), child.getValue());
            }
            target.set(entry.getKey(), mutableValue(nested));
         } else {
            target.set(entry.getKey(), mutableValue(value));
         }
      }
   }

   public static UiElementModel model(ConfigurationSection source) {
      Map<String, Object> values = new LinkedHashMap<>();
      if (source != null) {
         for (Map.Entry<String, Object> entry : source.getValues(false).entrySet()) {
            Object value = entry.getValue();
            values.put(entry.getKey(), value instanceof ConfigurationSection section
               ? new UiElementModel.Section(model(section).properties()) : mutableValue(value));
         }
      }
      return UiElementModel.of(values);
   }

   public static Map<String, Object> immutableMap(Map<String, ?> source) {
      Map<String, Object> copy = new LinkedHashMap<>();
      if (source != null) {
         for (Map.Entry<String, ?> entry : source.entrySet()) {
            copy.put(entry.getKey(), immutableValue(entry.getValue()));
         }
      }
      return Collections.unmodifiableMap(copy);
   }

   public static Object mutableValue(Object value) {
      if (value instanceof ConfigurationSection section) {
         Map<String, Object> copy = new LinkedHashMap<>();
         for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            copy.put(entry.getKey(), mutableValue(entry.getValue()));
         }
         return copy;
      }
      if (value instanceof Map<?, ?> values) {
         Map<String, Object> copy = new LinkedHashMap<>();
         for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() != null) {
               copy.put(String.valueOf(entry.getKey()), mutableValue(entry.getValue()));
            }
         }
         return copy;
      }
      if (value instanceof List<?> values) {
         List<Object> copy = new ArrayList<>(values.size());
         for (Object entry : values) {
            copy.add(mutableValue(entry));
         }
         return copy;
      }
      if (value instanceof Object[] values) {
         List<Object> copy = new ArrayList<>(values.length);
         for (Object entry : values) {
            copy.add(mutableValue(entry));
         }
         return copy;
      }
      return value;
   }

   private static Object immutableValue(Object value) {
      if (value instanceof ConfigurationSection section) {
         return immutableMap(section.getValues(false));
      }
      if (value instanceof Map<?, ?> values) {
         Map<String, Object> copy = new LinkedHashMap<>();
         for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() != null) {
               copy.put(String.valueOf(entry.getKey()), immutableValue(entry.getValue()));
            }
         }
         return Collections.unmodifiableMap(copy);
      }
      if (value instanceof List<?> values) {
         List<Object> copy = new ArrayList<>(values.size());
         for (Object entry : values) {
            copy.add(immutableValue(entry));
         }
         return Collections.unmodifiableList(copy);
      }
      if (value instanceof Object[] values) {
         List<Object> copy = new ArrayList<>(values.length);
         for (Object entry : values) {
            copy.add(immutableValue(entry));
         }
         return Collections.unmodifiableList(copy);
      }
      return value;
   }
}
