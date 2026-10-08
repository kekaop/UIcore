package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuConfigLoaderTest {
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
