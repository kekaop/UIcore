package org.orvencraft.uicore.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record SchematicPlacement(String file, Anchor anchor, int x, int y, int z) {
   public enum Anchor { CAMERA, WORLD }

   public static List<SchematicPlacement> from(ConfigurationSection menu) {
      if (!menu.contains("schematics")) return List.of();
      List<?> configured = menu.getList("schematics");
      if (configured == null || configured.size() > 4) {
         throw new IllegalArgumentException("schematics must be a list of at most four entries");
      }
      List<SchematicPlacement> placements = new ArrayList<>();
      for (Object entry : configured) {
         if (!(entry instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("schematics entries must be maps");
         }
         String file = String.valueOf(values.containsKey("file") ? values.get("file") : "");
         if (!file.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}\\.schem")) {
            throw new IllegalArgumentException("Invalid schematic file name: " + file);
         }
         String rawAnchor = String.valueOf(values.containsKey("anchor") ? values.get("anchor") : "camera");
         Anchor anchor = switch (rawAnchor.toLowerCase(java.util.Locale.ROOT)) {
            case "camera" -> Anchor.CAMERA;
            case "world" -> Anchor.WORLD;
            default -> throw new IllegalArgumentException("Unknown schematic anchor: " + rawAnchor);
         };
         placements.add(new SchematicPlacement(file, anchor,
            coordinate(values, "x"), coordinate(values, "y"), coordinate(values, "z")));
      }
      return List.copyOf(placements);
   }

   private static int coordinate(Map<?, ?> values, String axis) {
      Object value = values.containsKey(axis) ? values.get(axis) : 0;
      if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())
         || number.doubleValue() != number.intValue() || Math.abs(number.doubleValue()) > 30_000_000) {
         throw new IllegalArgumentException("Invalid schematic " + axis + " coordinate: " + value);
      }
      return number.intValue();
   }
}
