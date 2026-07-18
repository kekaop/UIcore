package com.cmenu.ui.bstats.json;

import java.util.Arrays;
import java.util.stream.Collectors;

public class JsonObjectBuilder {
   private StringBuilder builder = new StringBuilder();
   private boolean hasAtLeastOneField = false;

   public JsonObjectBuilder() {
      this.builder.append("{");
   }

   public JsonObjectBuilder appendNull(String key) {
      this.appendFieldUnescaped(key, "null");
      return this;
   }

   public JsonObjectBuilder appendField(String key, String value) {
      if (value == null) {
         throw new IllegalArgumentException("JSON value must not be null");
      } else {
         this.appendFieldUnescaped(key, "\"" + escape(value) + "\"");
         return this;
      }
   }

   public JsonObjectBuilder appendField(String key, int value) {
      this.appendFieldUnescaped(key, String.valueOf(value));
      return this;
   }

   public JsonObjectBuilder appendField(String key, JsonObjectBuilder.JsonObject object) {
      if (object == null) {
         throw new IllegalArgumentException("JSON object must not be null");
      } else {
         this.appendFieldUnescaped(key, object.toString());
         return this;
      }
   }

   public JsonObjectBuilder appendField(String key, String[] values) {
      if (values == null) {
         throw new IllegalArgumentException("JSON values must not be null");
      } else {
         String escapedValues = Arrays.stream(values).map(value -> "\"" + escape(value) + "\"").collect(Collectors.joining(","));
         this.appendFieldUnescaped(key, "[" + escapedValues + "]");
         return this;
      }
   }

   public JsonObjectBuilder appendField(String key, int[] values) {
      if (values == null) {
         throw new IllegalArgumentException("JSON values must not be null");
      } else {
         String escapedValues = Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(","));
         this.appendFieldUnescaped(key, "[" + escapedValues + "]");
         return this;
      }
   }

   public JsonObjectBuilder appendField(String key, JsonObjectBuilder.JsonObject[] values) {
      if (values == null) {
         throw new IllegalArgumentException("JSON values must not be null");
      } else {
         String escapedValues = Arrays.stream(values).map(JsonObjectBuilder.JsonObject::toString).collect(Collectors.joining(","));
         this.appendFieldUnescaped(key, "[" + escapedValues + "]");
         return this;
      }
   }

   private void appendFieldUnescaped(String key, String escapedValue) {
      if (this.builder == null) {
         throw new IllegalStateException("JSON has already been built");
      } else if (key == null) {
         throw new IllegalArgumentException("JSON key must not be null");
      } else {
         if (this.hasAtLeastOneField) {
            this.builder.append(",");
         }

         this.builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
         this.hasAtLeastOneField = true;
      }
   }

   public JsonObjectBuilder.JsonObject build() {
      if (this.builder == null) {
         throw new IllegalStateException("JSON has already been built");
      } else {
         JsonObjectBuilder.JsonObject object = new JsonObjectBuilder.JsonObject(this.builder.append("}").toString());
         this.builder = null;
         return object;
      }
   }

   private static String escape(String value) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         if (c == '"') {
            builder.append("\\\"");
         } else if (c == '\\') {
            builder.append("\\\\");
         } else if (c <= 15) {
            builder.append("\\u000").append(Integer.toHexString(c));
         } else if (c <= 31) {
            builder.append("\\u00").append(Integer.toHexString(c));
         } else {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public static class JsonObject {
      private final String value;

      private JsonObject(String value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return this.value;
      }
   }
}
