package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuConfigLoaderTest {
   @Test
   void rejectsNonStringPermissionAtMenuAndElementScope() throws Exception {
      Path file = temporaryDirectory.resolve("permissions.yml");
      Files.writeString(file, "example:\n  permission: [uicore.admin]\n  camera-position:\n    type: player\n");
      IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
         () -> MenuConfigLoader.loadMenuSections(temporaryDirectory.toFile()));
      assertTrue(error.getMessage().contains("permission"));
      var config = new org.bukkit.configuration.file.YamlConfiguration();
      config.loadFromString("permission:\n  node: uicore.admin\n");
      assertThrows(IllegalArgumentException.class, () -> MenuConfigLoader.loadLayout("example", "admin", config, "world"));
   }
   @TempDir
   Path temporaryDirectory;

   @Test
   void rejectsMalformedYamlInsteadOfTreatingItAsAnEmptyConfig() throws Exception {
      Path menu = Files.writeString(temporaryDirectory.resolve("broken.yml"), "menu:\n  camera-position: [\n");

      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MenuConfigLoader.loadStrict(menu.toFile()));

      assertTrue(exception.getMessage().contains("broken.yml"));
   }

   @Test
   void rejectsUnknownSchemaVersionWithMenuContext() throws Exception {
      Files.writeString(temporaryDirectory.resolve("version.yml"),
         "example:\n  schema-version: 3\n  camera-position:\n    type: player\n");
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
         () -> MenuConfigLoader.loadMenuSections(temporaryDirectory.toFile()));
      assertTrue(exception.getMessage().contains("example"));
      assertTrue(exception.getMessage().contains("schema-version"));
   }

}
