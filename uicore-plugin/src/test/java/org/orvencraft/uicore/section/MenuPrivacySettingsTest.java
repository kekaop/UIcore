package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuPrivacySettingsTest {
   @TempDir Path directory;

   private Section load(String parameters) throws Exception {
      Files.writeString(directory.resolve("menu.yml"), "example:\n  camera-position: {type: player}\n" + parameters);
      return MenuConfigLoader.loadMenuSections(directory.toFile()).get("example");
   }

   @Test void existingMenusReceiveTheRequestedDefaults() throws Exception {
      assertEquals(new MenuPrivacySettings(true, true, false, false), load("").privacy);
   }

   @Test void optionsAreIndependentAndFollowCameraCopies() throws Exception {
      Section section = load("  hide-menu-for-other-player: false\n  hide-player-for-other-player: false\n"
         + "  hide-other-players: true\n  can-damaged: true\n");
      assertEquals(new MenuPrivacySettings(false, false, true, true), section.privacy);
      Section runtime = section.copyWithCamera("world", 1, 2, 3, 4, 5);
      assertSame(section.privacy, runtime.privacy);
      Section replacement = load("  hide-other-players: true\n");
      runtime.syncDefinitionFrom(replacement);
      assertEquals(new MenuPrivacySettings(true, true, true, false), runtime.privacy);
      assertEquals(1, runtime.cameraX);
   }

   @Test void eachOptionCanChangeWithoutChangingTheOthers() throws Exception {
      assertEquals(new MenuPrivacySettings(false, true, false, false), load("  hide-menu-for-other-player: false\n").privacy);
      assertEquals(new MenuPrivacySettings(true, false, false, false), load("  hide-player-for-other-player: false\n").privacy);
      assertEquals(new MenuPrivacySettings(true, true, true, false), load("  hide-other-players: true\n").privacy);
      assertEquals(new MenuPrivacySettings(true, true, false, true), load("  can-damaged: true\n").privacy);
   }

   @Test void invalidBooleanRejectsTheMenuWithContext() {
      for (String key : java.util.List.of("hide-menu-for-other-player", "hide-player-for-other-player", "hide-other-players", "can-damaged")) {
         IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> load("  " + key + ": maybe\n"));
         assertTrue(error.getMessage().contains(key));
         assertTrue(error.getMessage().contains("example"));
      }
   }
}
