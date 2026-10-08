package org.orvencraft.uicore.api.state;

import java.util.Objects;
import java.util.function.Function;

/** A typed view of UIcore's existing per-player, per-menu string state. */
public final class StateKey<T> {
   private final String name;
   private final T defaultValue;
   private final Function<String, T> parser;
   private final Function<T, String> formatter;

   private StateKey(String name, T defaultValue, Function<String, T> parser, Function<T, String> formatter) {
      if (name == null || name.isBlank()) throw new IllegalArgumentException("State key is empty");
      this.name = name;
      this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
      this.parser = parser;
      this.formatter = formatter;
   }

   public static StateKey<String> string(String name, String defaultValue) {
      return new StateKey<>(name, defaultValue, Function.identity(), Function.identity());
   }
   public static StateKey<Integer> integer(String name, int defaultValue) {
      return new StateKey<>(name, defaultValue, Integer::parseInt, String::valueOf);
   }
   public static StateKey<Double> decimal(String name, double defaultValue) {
      if (!Double.isFinite(defaultValue)) throw new IllegalArgumentException("Default value must be finite");
      return new StateKey<>(name, defaultValue, raw -> {
         double parsed = Double.parseDouble(raw);
         if (!Double.isFinite(parsed)) throw new IllegalArgumentException("State value must be finite");
         return parsed;
      }, value -> {
         if (!Double.isFinite(value)) throw new IllegalArgumentException("State value must be finite");
         return String.valueOf(value);
      });
   }
   public static StateKey<Boolean> bool(String name, boolean defaultValue) {
      return new StateKey<>(name, defaultValue, raw -> {
         if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) throw new IllegalArgumentException("Invalid boolean state");
         return Boolean.parseBoolean(raw);
      }, String::valueOf);
   }

   public String name() { return this.name; }
   public T defaultValue() { return this.defaultValue; }
   public T parse(String raw) { return this.parser.apply(raw); }
   public String format(T value) { return this.formatter.apply(Objects.requireNonNull(value, "value")); }
}
