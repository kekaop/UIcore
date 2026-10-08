package org.orvencraft.uicore.config;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFilesTest {
   @TempDir Path temp;
   @Test void invalidYamlIsNotOverwritten() throws Exception {
      Path file = temp.resolve("text.yml"); String broken = "content: [unterminated";
      Files.writeString(file, broken);
      assertThrows(IllegalArgumentException.class, () -> ConfigFiles.loadStrict(file.toFile()));
      assertEquals(broken, Files.readString(file));
   }
   @Test void npcAndTextNumbersAreValidatedBeforePublication() throws Exception {
      Path file = temp.resolve("npc_config.yml");
      Files.writeString(file, "offset:\n  x: .NaN\n");
      assertThrows(IllegalArgumentException.class, () -> ConfigFiles.loadStrict(file.toFile()));
      Files.writeString(file, "offset:\n  x: 0.5\n");
      assertEquals(0.5, ConfigFiles.loadStrict(file.toFile()).getDouble("offset.x"));
   }
}
