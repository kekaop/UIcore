package org.orvencraft.uicore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ColorParserTest {
   @Test
   void parsesExistingSectionCodesWithoutTreatingThemAsPlainText() {
      assertEquals("Green", PlainTextComponentSerializer.plainText().serialize(ColorParser.parse("§aGreen")));
      assertTrue(ColorParser.toLegacyString("§aGreen").contains("§aGreen"));
   }

   @Test
   void preservesRgbWhenSerializingMiniMessage() {
      String legacy = ColorParser.toLegacyString("<#12ab34>RGB");

      assertTrue(legacy.contains("§x§1§2§a§b§3§4RGB"));
   }

   @Test
   void parsesMiniMessageAcrossLineBreaks() {
      String legacy = ColorParser.toLegacyString("<gray>First line\nSecond line</gray>");
      assertTrue(legacy.contains("§7First line\nSecond line"));
   }
}
