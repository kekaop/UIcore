package org.orvencraft.uicore.layout;

import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/** Validated interaction settings shared by YAML and API-created elements. */
public record ElementInteraction(String parent, boolean group, boolean pointerEvents, int priority,
   boolean draggable, String target, String axis, double minX, double maxX, double minY, double maxY,
   String sliderState, double sliderMin, double sliderMax, double sliderStep,
   List<String> dropTargets, List<String> accepts, List<String> onDrop) {
   public static ElementInteraction from(ConfigurationSection c) {
      for (String path : List.of("parent", "drag.target", "drag.axis", "slider.state"))
         if (c.contains(path) && !c.isString(path)) throw new IllegalArgumentException(path + " must be a string");
      for (String path : List.of("pointer-events", "drag.enabled"))
         if (c.contains(path) && !c.isBoolean(path)) throw new IllegalArgumentException(path + " must be boolean");
      for (String path : List.of("drag", "slider"))
         if (c.contains(path) && !c.isConfigurationSection(path)) throw new IllegalArgumentException(path + " must be a map");
      String state = c.getString("slider.state", "");
      boolean slider = c.contains("slider");
      String axis = c.getString("drag.axis", "x" + (slider ? "" : "y"));
      if (!Set.of("x", "y", "xy").contains(axis) || slider && axis.equals("xy"))
         throw new IllegalArgumentException("drag.axis must be x, y or xy (sliders require x or y)");
      if (slider && !state.matches("[A-Za-z0-9_.-]{1,64}")) throw new IllegalArgumentException("slider.state is required");
      double[] x = bounds(c, "drag.bounds.x"), y = bounds(c, "drag.bounds.y");
      if (slider && !c.contains("drag.bounds." + axis)) throw new IllegalArgumentException("Slider requires drag.bounds." + axis);
      double min = number(c, "slider.min", 0), max = number(c, "slider.max", 100), step = number(c, "slider.step", 1);
      if (max <= min || step <= 0) throw new IllegalArgumentException("Slider requires max > min and step > 0");
      boolean group = "group".equals(c.getString("type"));
      double priority = number(c, "interaction-priority", 0);
      if (priority != Math.rint(priority) || Math.abs(priority) > 1000000) throw new IllegalArgumentException("Invalid interaction-priority");
      return new ElementInteraction(c.getString("parent", ""), group, c.getBoolean("pointer-events", !group), (int)priority,
         c.getBoolean("drag.enabled", slider), c.getString("drag.target", ""), axis, x[0], x[1], y[0], y[1],
         state, min, max, step, strings(c, "drag.drop-targets", "element IDs"), strings(c, "drop-accepts", "element IDs"),
         strings(c, "on-drop", "actions"));
   }
   private static List<String> strings(ConfigurationSection c, String key, String entries) {
      if (!c.contains(key)) return List.of();
      Object value = c.get(key);
      if (!(value instanceof List<?> list) || list.stream().anyMatch(v -> !(v instanceof String s) || s.isBlank()))
         throw new IllegalArgumentException(key + " must be a list of non-blank " + entries);
      return List.copyOf(c.getStringList(key));
   }
   private static double number(ConfigurationSection c, String key, double fallback) {
      Object value = c.get(key);
      if (value == null) return fallback;
      if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException(key + " must be finite");
      return ((Number)value).doubleValue();
   }
   private static double[] bounds(ConfigurationSection c, String key) {
      if (!c.contains(key)) return new double[]{-30_000_000, 30_000_000};
      Object value = c.get(key);
      if (!(value instanceof List<?> list) || list.size() != 2 || !(list.get(0) instanceof Number a)
         || !(list.get(1) instanceof Number b) || !Double.isFinite(a.doubleValue()) || !Double.isFinite(b.doubleValue())
         || Math.abs(a.doubleValue()) > 30_000_000 || Math.abs(b.doubleValue()) > 30_000_000
         || a.doubleValue() >= b.doubleValue()) throw new IllegalArgumentException(key + " requires finite [min, max], min < max");
      return new double[]{a.doubleValue(), b.doubleValue()};
   }
   public double x(double start, double delta) { return axis.equals("y") ? start : Math.max(minX, Math.min(maxX, start + delta)); }
   public double y(double start, double delta) { return axis.equals("x") ? start : Math.max(minY, Math.min(maxY, start + delta)); }
   public boolean permitsDrop(double currentX, double currentY, double x, double y) {
      return Math.abs(x(currentX, x - currentX) - x) < 1e-9 && Math.abs(y(currentY, y - currentY) - y) < 1e-9;
   }
   public double sliderValue(double x, double y) {
      double fraction = axis.equals("x") ? (x - minX) / (maxX - minX) : (y - minY) / (maxY - minY);
      return Math.max(sliderMin, Math.min(sliderMax, sliderMin + Math.round(Math.max(0, Math.min(1, fraction)) * (sliderMax - sliderMin) / sliderStep) * sliderStep));
   }
}
