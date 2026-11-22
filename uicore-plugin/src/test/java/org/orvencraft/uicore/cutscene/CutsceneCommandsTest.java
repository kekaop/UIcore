package org.orvencraft.uicore.cutscene;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CutsceneCommandsTest {
   @TempDir Path directory;

   @Test
   void pointPersistencePreservesScenarioAndUsesExactEyeCoordinates() throws Exception {
      Files.writeString(directory.resolve("tour.yml"), "id: tour\nstart: done\nsteps:\n  done: {type: end}\n");
      CutsceneCommands.savePointFile(directory, "tour", "start", "world", 12.125, 70.625, -4.25, 179.0F, -20.0F);
      CutsceneCommands.savePointFile(directory, "tour", "end", "world", 16.75, 71.125, -8.125, -179.0F, 20.0F);
      YamlConfiguration config = new YamlConfiguration();
      config.load(directory.resolve("tour.yml").toFile());
      assertEquals("done", config.getString("start"));
      assertEquals("end", config.getString("steps.done.type"));
      assertEquals(70.625, config.getDouble("points.start.y"), 0.0);
      assertEquals(-179.0, config.getDouble("points.end.yaw"), 0.0);
      assertEquals(2, config.getConfigurationSection("points").getKeys(false).size());
      try (var files = Files.list(directory)) { assertEquals(1L, files.count(), "No temporary file survives replacement"); }
   }

   @Test
   void failedEditPreservesExistingFile() throws Exception {
      Path file = directory.resolve("broken.yml");
      String original = "id: broken\npoints: [unterminated\n";
      Files.writeString(file, original);
      assertThrows(IllegalArgumentException.class, () ->
         CutsceneCommands.savePointFile(directory, "broken", "p", "world", 0, 64, 0, 0, 0));
      assertEquals(original, Files.readString(file));
   }

   @Test
   void duplicateKeysCannotBeSilentlyLostWhenEditingAPoint() throws Exception {
      Path file = directory.resolve("tour.yml");
      String original = "id: tour\npoints:\n  p: {world: world, x: 1, y: 64, z: 0}\n  p: {world: world, x: 2, y: 64, z: 0}\n";
      Files.writeString(file, original);
      assertThrows(IllegalArgumentException.class, () ->
         CutsceneCommands.savePointFile(directory, "tour", "other", "world", 0, 64, 0, 0, 0));
      assertEquals(original, Files.readString(file));
   }

   @Test
   void fileAndYamlPathsCannotEscapeThroughAnId() {
      for (String id : new String[] {"../outside", "a.b", "a/b", "C:\\outside", ""}) {
         assertThrows(IllegalArgumentException.class, () -> CutsceneCommands.scenarioFile(directory, id));
         assertThrows(IllegalArgumentException.class, () ->
            CutsceneCommands.savePointFile(directory, "tour", id, "world", 0, 64, 0, 0, 0));
      }
   }

   @Test
   void invalidCoordinatesAndMismatchedIdsDoNotOverwriteScenario() throws Exception {
      Path file = directory.resolve("tour.yml");
      String original = "id: another\n";
      Files.writeString(file, original);
      assertThrows(IllegalArgumentException.class, () ->
         CutsceneCommands.savePointFile(directory, "tour", "p", "world", 0, 64, 0, 0, 0));
      assertThrows(IllegalArgumentException.class, () ->
         CutsceneCommands.savePointFile(directory, "tour", "p", "world", Double.NaN, 64, 0, 0, 0));
      assertThrows(IllegalArgumentException.class, () ->
         CutsceneCommands.savePointFile(directory, "tour", "p", "world", 0, 64, 0, 0, 91));
      assertEquals(original, Files.readString(file));
   }
}
