package org.orvencraft.uicore.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable element definition shared by YAML and Java API authoring paths. */
public final class UiElementModel {
   /** A configuration section, distinct from a map stored as a property value. */
   public record Section(Map<String, Object> values) {
      public Section { values = freezeMap(values); }
   }

   private final Map<String, Object> properties;

   private UiElementModel(Map<String, Object> properties) {
      this.properties = properties;
   }

   public static UiElementModel empty() { return new UiElementModel(Map.of()); }

   public static UiElementModel of(Map<String, ?> properties) {
      return new UiElementModel(freezeMap(properties == null ? Map.of() : properties));
   }

   public Map<String, Object> properties() { return this.properties; }

   @Override public boolean equals(Object other) {
      return other instanceof UiElementModel model && this.properties.equals(model.properties);
   }

   @Override public int hashCode() { return this.properties.hashCode(); }

   public Object get(String path) {
      Object current = this.properties;
      for (String part : parts(path)) {
         if (current instanceof Section section) current = section.values();
         if (!(current instanceof Map<?, ?> map)) return null;
         current = map.get(part);
      }
      return current;
   }

   public boolean contains(String path) { return this.get(path) != null; }

   public UiElementModel with(String path, Object value) {
      List<String> parts = parts(path);
      Map<String, Object> copy = mutableMap(this.properties);
      setPath(copy, parts, 0, value);
      return of(copy);
   }

   private static void setPath(Map<String, Object> cursor, List<String> parts, int index, Object value) {
      String part = parts.get(index);
      if (index == parts.size() - 1) {
         if (value == null) cursor.remove(part);
         else cursor.put(part, mutableValue(value));
         return;
      }
      Object child = cursor.get(part);
      boolean section = !(child instanceof Map<?, ?>);
      Map<String, Object> nested;
      if (child instanceof Section existing) nested = mutableMap(existing.values());
      else if (child instanceof Map<?, ?> map) nested = mutableMap(stringKeys(map));
      else nested = new LinkedHashMap<>();
      setPath(nested, parts, index + 1, value);
      cursor.put(part, section ? new Section(nested) : nested);
   }

   private static List<String> parts(String path) {
      Objects.requireNonNull(path, "path");
      if (path.isBlank() || path.startsWith(".") || path.endsWith(".") || path.contains(".."))
         throw new IllegalArgumentException("Invalid element property path");
      return List.of(path.split("\\."));
   }

   private static Map<String, Object> freezeMap(Map<String, ?> source) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<String, ?> entry : source.entrySet()) {
         if (entry.getKey() == null) throw new IllegalArgumentException("Element property key is null");
         result.put(entry.getKey(), freeze(entry.getValue()));
      }
      return Collections.unmodifiableMap(result);
   }

   private static Object freeze(Object value) {
      if (value instanceof Section section) return new Section(section.values());
      if (value instanceof Map<?, ?> map) {
         Map<String, Object> normalized = new LinkedHashMap<>();
         for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) normalized.put(String.valueOf(entry.getKey()), freeze(entry.getValue()));
         }
         return Collections.unmodifiableMap(normalized);
      }
      if (value instanceof List<?> list) {
         List<Object> copy = new ArrayList<>(list.size());
         for (Object item : list) copy.add(freeze(item));
         return Collections.unmodifiableList(copy);
      }
      return value;
   }

   private static Map<String, Object> mutableMap(Map<String, ?> source) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<String, ?> entry : source.entrySet()) result.put(entry.getKey(), mutableValue(entry.getValue()));
      return result;
   }

   private static Object mutableValue(Object value) {
      if (value instanceof Section section) return new Section(section.values());
      if (value instanceof Map<?, ?> map) {
         Map<String, Object> result = new LinkedHashMap<>();
         for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) result.put(String.valueOf(entry.getKey()), mutableValue(entry.getValue()));
         }
         return result;
      }
      if (value instanceof List<?> list) {
         List<Object> result = new ArrayList<>(list.size());
         for (Object item : list) result.add(mutableValue(item));
         return result;
      }
      return value;
   }

   private static Map<String, Object> stringKeys(Map<?, ?> source) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : source.entrySet()) {
         if (entry.getKey() != null) result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
      return result;
   }
}
