package org.orvencraft.uicore.layout;

import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.orvencraft.uicore.position.DynamicPositionSpec.Position;

/** Opt-in screen-space zoom for a menu or one group subtree. */
public record MenuZoom(String group, double min, double max, double step, double sensitivity,
                       double smoothing, double pivotX, double pivotY) {
   public static MenuZoom from(ConfigurationSection menu) {
      if (!menu.contains("zoom")) return null;
      ConfigurationSection config = menu.getConfigurationSection("zoom");
      if (config == null) throw new IllegalArgumentException("zoom must be a map");
      if (config.contains("enabled") && !config.isBoolean("enabled"))
         throw new IllegalArgumentException("zoom.enabled must be boolean");
      if (config.contains("group") && !config.isString("group"))
         throw new IllegalArgumentException("zoom.group must be a group ID");
      if (config.contains("pivot") && !config.isConfigurationSection("pivot"))
         throw new IllegalArgumentException("zoom.pivot must be a map");
      double min = number(config, "min", .5, .25, 1);
      double max = number(config, "max", 2, 1, 4);
      if (min >= max) throw new IllegalArgumentException("zoom requires min < max");
      var zoom = new MenuZoom(config.getString("group", "").trim(), min, max,
         number(config, "step", .15, .01, 1), number(config, "sensitivity", 1, .1, 4),
         number(config, "smoothing", 1, .05, 1), number(config, "pivot.x", 0, -32, 32),
         number(config, "pivot.y", 0, -32, 32));
      return config.getBoolean("enabled", false) ? zoom : null;
   }

   private static double number(ConfigurationSection config, String key, double fallback, double low, double high) {
      Object raw = config.get(key, fallback);
      if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue())
         || number.doubleValue() < low || number.doubleValue() > high)
         throw new IllegalArgumentException("zoom." + key + " must be a number between " + low + " and " + high);
      return number.doubleValue();
   }

   public boolean affects(MenuLayout layout, Map<String, MenuLayout> layouts) {
      if (layout.isWorldAnchored()) return false;
      if (group.isEmpty()) return true;
      MenuLayout root = layouts.get(group);
      if (root == null || !root.interaction().group() || root.isWorldAnchored()) return false;
      for (int remaining = layouts.size(); layout != null && remaining-- > 0; ) {
         if (layout.key.equals(group)) return true;
         layout = layouts.get(layout.interaction().parent());
      }
      return false;
   }

   public Position project(Position position, Position groupOrigin, double factor) {
      double x = groupOrigin.x() + pivotX, y = groupOrigin.y() + pivotY;
      return new Position(x + (position.x() - x) * factor, y + (position.y() - y) * factor, position.z());
   }

   public double step(double target, int steps) {
      return Math.max(min, Math.min(max, target + steps * step * sensitivity));
   }

   public double approach(double current, double target) {
      return Math.abs(current - target) <= .001 ? target : current + (target - current) * smoothing;
   }

   /** Vanilla scroll-up decrements the selected index, including from 0 to 8. */
   public static int scrollDirection(int previous, int requested) {
      if (previous < 0 || previous > 8 || requested < 0 || requested > 8) return 0;
      int offset = Math.floorMod(requested - previous, 9);
      return offset == 8 ? 1 : offset == 1 ? -1 : 0;
   }
}
