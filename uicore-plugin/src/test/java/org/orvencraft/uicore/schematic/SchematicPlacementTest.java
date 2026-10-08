package org.orvencraft.uicore.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SchematicPlacementTest {
   @Test
   void parsesCameraAndWorldAnchors() throws Exception {
      YamlConfiguration config = new YamlConfiguration();
      config.loadFromString("schematics:\n  - file: first.schem\n    x: 2\n  - file: second.schem\n    anchor: world\n    x: 5\n    y: 64\n    z: -3\n");
      var placements = SchematicPlacement.from(config);
      assertEquals(new SchematicPlacement("first.schem", SchematicPlacement.Anchor.CAMERA, 2, 0, 0), placements.get(0));
      assertEquals(new SchematicPlacement("second.schem", SchematicPlacement.Anchor.WORLD, 5, 64, -3), placements.get(1));
   }

   @Test
   void rejectsTraversalAndFractionalCoordinates() throws Exception {
      YamlConfiguration config = new YamlConfiguration();
      config.loadFromString("schematics:\n  - file: ../outside.schem\n");
      assertThrows(IllegalArgumentException.class, () -> SchematicPlacement.from(config));
      config.loadFromString("schematics:\n  - file: sample.schem\n    x: 0.5\n");
      assertThrows(IllegalArgumentException.class, () -> SchematicPlacement.from(config));
   }
}
