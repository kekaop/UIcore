package org.orvencraft.uicore.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class CraftEngineCompatTest {
   @Test
   void registersBothCustomTagsWithoutReplacingStandardTags() {
      var parser = CraftEngineCompat.createMiniMessage(
         TagResolver.resolver("image", (arguments, context) -> Tag.inserting(Component.text("image:" + arguments.pop().value()))),
         TagResolver.resolver("shift", (arguments, context) -> Tag.inserting(Component.text("shift:" + arguments.pop().value()))));
      Component result = parser.deserialize("<green><image:icon> <shift:4></green>");
      assertEquals("image:icon shift:4", PlainTextComponentSerializer.plainText().serialize(result));
      assertTrue(result.color() == NamedTextColor.GREEN || result.children().stream().anyMatch(child -> child.color() == NamedTextColor.GREEN));
   }
}
