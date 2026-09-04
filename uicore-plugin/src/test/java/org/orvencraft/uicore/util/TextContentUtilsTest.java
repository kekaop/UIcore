package org.orvencraft.uicore.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextContentUtilsTest {
   @Test
   void supportsSlashAndEscapedNewlines() {
      assertEquals("First\nSecond\nThird", TextContentUtils.normalizeLineBreaks("First/nSecond\\nThird"));
   }
}
