package org.orvencraft.uicore.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommandUtilsTest {
   @Test
   void parsesExplicitInlineDelay() {
      CommandUtils.DelayedCommand parsed = CommandUtils.parseDelayedCommand("<delay=60>[message] Ready", 20L);
      assertEquals(60L, parsed.delayTicks());
      assertEquals("[message] Ready", parsed.command());
   }

   @Test
   void usesElementDelayWhenInlineDelayIsMissing() {
      CommandUtils.DelayedCommand parsed = CommandUtils.parseDelayedCommand("[player] spawn", 12L);
      assertEquals(12L, parsed.delayTicks());
      assertEquals("[player] spawn", parsed.command());
   }
}
