package org.orvencraft.uicore.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.bukkit.Location;
import org.orvencraft.uicore.section.Section;
import org.junit.jupiter.api.Test;

class ChunkLoaderManagerTest {
   @Test
   void transformsLayoutCoordinatesUsingTheCameraAxes() {
      Section section = new Section(1.5, "world", 10, 20, 30, 90, 0, "", List.of(), List.of(), false);

      Location location = ChunkLoaderManager.layoutLocation(section, 3, 2, 4);

      assertEquals(6, location.getX(), 1.0E-9);
      assertEquals(22, location.getY(), 1.0E-9);
      assertEquals(27, location.getZ(), 1.0E-9);
   }
}
