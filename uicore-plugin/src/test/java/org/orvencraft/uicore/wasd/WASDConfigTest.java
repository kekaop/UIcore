package org.orvencraft.uicore.wasd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WASDConfigTest {
   @Test
   void appliesTheDocumentedAllWhitelistAndBlacklistMenuModes() {
      assertTrue(WASDConfig.isMenuEnabled(true, true, false, List.of(), "main"));
      assertFalse(WASDConfig.isMenuEnabled(true, true, false, List.of("blocked"), "blocked"));
      assertTrue(WASDConfig.isMenuEnabled(true, true, true, List.of("selected"), "selected"));
      assertFalse(WASDConfig.isMenuEnabled(true, true, true, List.of("selected"), "other"));
      assertTrue(WASDConfig.isMenuEnabled(true, false, false, List.of("selected"), "selected"));
      assertFalse(WASDConfig.isMenuEnabled(true, false, false, List.of("selected"), "other"));
   }
}
