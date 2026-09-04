package org.orvencraft.uicore.position;

import org.orvencraft.uicore.expression.NumericExpression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public final class DynamicPositionSpec {
   private final Axis x;
   private final Axis y;
   private final Axis z;
   private final int intervalTicks;
   private final double smoothing;
   private final List<String> configurationErrors;

   private DynamicPositionSpec(
      Axis x,
      Axis y,
      Axis z,
      int intervalTicks,
      double smoothing,
      List<String> configurationErrors
   ) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.intervalTicks = intervalTicks;
      this.smoothing = smoothing;
      this.configurationErrors = List.copyOf(configurationErrors);
   }

   public static DynamicPositionSpec from(ConfigurationSection layout, double baseX, double baseY, double baseZ) {
      ConfigurationSection update = layout == null ? null : layout.getConfigurationSection("position-update");
      List<String> errors = new ArrayList<>();
      Axis x = parseAxis("x", layout == null ? null : layout.get("x"), fallback(update, "x", baseX), bounds(update, "x"), errors);
      Axis y = parseAxis("y", layout == null ? null : layout.get("y"), fallback(update, "y", baseY), bounds(update, "y"), errors);
      Axis z = parseAxis("z", layout == null ? null : layout.get("z"), fallback(update, "z", baseZ), bounds(update, "z"), errors);
      int interval = update == null ? 1 : Math.max(1, update.getInt("interval", update.getInt("interval-ticks", 1)));
      double smoothing = update == null ? 1.0 : clamp(update.getDouble("smoothing", 1.0), 0.01, 1.0);
      return new DynamicPositionSpec(x, y, z, interval, smoothing, errors);
   }

   public boolean isDynamic() {
      return this.x.expression != null || this.y.expression != null || this.z.expression != null;
   }

   public int intervalTicks() {
      return this.intervalTicks;
   }

   public double smoothing() {
      return this.smoothing;
   }

   public Position basePosition() {
      return new Position(this.x.fallback, this.y.fallback, this.z.fallback);
   }

   public List<String> configurationErrors() {
      return Collections.unmodifiableList(this.configurationErrors);
   }

   public Evaluation evaluate(long elapsedTicks, NumericExpression.ExternalValueResolver resolver, Position previous) {
      Position fallback = previous == null ? this.basePosition() : previous;
      List<String> failures = new ArrayList<>();
      double resolvedX = this.x.evaluate(elapsedTicks, resolver, fallback.x, failures);
      double resolvedY = this.y.evaluate(elapsedTicks, resolver, fallback.y, failures);
      double resolvedZ = this.z.evaluate(elapsedTicks, resolver, fallback.z, failures);
      return new Evaluation(new Position(resolvedX, resolvedY, resolvedZ), List.copyOf(failures));
   }

   public record Position(double x, double y, double z) {
      public Position interpolate(Position target, double factor) {
         double amount = clamp(factor, 0.0, 1.0);
         return new Position(
            this.x + (target.x - this.x) * amount,
            this.y + (target.y - this.y) * amount,
            this.z + (target.z - this.z) * amount
         );
      }
   }

   public record Evaluation(Position position, List<String> failures) {
   }

   private static Axis parseAxis(String name, Object raw, double fallback, Bounds bounds, List<String> errors) {
      if (!(raw instanceof String expressionSource)) {
         double staticValue = raw instanceof Number number ? number.doubleValue() : fallback;
         return new Axis(name, null, bounds.clamp(staticValue), bounds);
      }

      try {
         return new Axis(name, NumericExpression.compile(expressionSource), bounds.clamp(fallback), bounds);
      } catch (IllegalArgumentException exception) {
         errors.add(name + ": " + exception.getMessage());
         return new Axis(name, null, bounds.clamp(fallback), bounds);
      }
   }

   private static double fallback(ConfigurationSection update, String axis, double defaultValue) {
      if (update == null) {
         return defaultValue;
      }
      Object raw = update.get("fallback." + axis);
      return raw instanceof Number number ? number.doubleValue() : defaultValue;
   }

   private static Bounds bounds(ConfigurationSection update, String axis) {
      if (update == null) {
         return Bounds.UNBOUNDED;
      }
      List<?> values = update.getList("clamp." + axis);
      if (values == null || values.size() < 2 || !(values.get(0) instanceof Number first) || !(values.get(1) instanceof Number second)) {
         return Bounds.UNBOUNDED;
      }
      return new Bounds(Math.min(first.doubleValue(), second.doubleValue()), Math.max(first.doubleValue(), second.doubleValue()));
   }

   private static double clamp(double value, double minimum, double maximum) {
      return Math.max(minimum, Math.min(maximum, value));
   }

   private record Bounds(double minimum, double maximum) {
      private static final Bounds UNBOUNDED = new Bounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

      double clamp(double value) {
         return DynamicPositionSpec.clamp(value, this.minimum, this.maximum);
      }
   }

   private record Axis(String name, NumericExpression expression, double fallback, Bounds bounds) {
      double evaluate(
         long elapsedTicks,
         NumericExpression.ExternalValueResolver resolver,
         double previous,
         List<String> failures
      ) {
         if (this.expression == null) {
            return this.bounds.clamp(this.fallback);
         }
         try {
            return this.bounds.clamp(this.expression.evaluate(elapsedTicks, resolver));
         } catch (RuntimeException exception) {
            failures.add(this.name + ": " + exception.getMessage());
            return this.bounds.clamp(previous);
         }
      }
   }
}
