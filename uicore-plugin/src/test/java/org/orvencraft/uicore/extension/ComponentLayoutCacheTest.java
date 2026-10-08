package org.orvencraft.uicore.extension;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.api.model.UiElementModel;
import org.orvencraft.uicore.layout.MenuLayout;

class ComponentLayoutCacheTest {
   @Test
   void keepsChildIdentityUntilDefinitionChangesOrChildDisappears() {
      ComponentLayoutCache cache = new ComponentLayoutCache();
      UUID player = UUID.randomUUID();
      UiElementModel original = UiElementModel.empty().with("name", "First");
      MenuLayout first = cache.getOrCreate(player, "menu", "card/title", "world", original, this::layout);
      assertSame(first, cache.getOrCreate(player, "menu", "card/title", "world",
         UiElementModel.empty().with("name", "First"), this::layout));

      UiElementModel changed = original.with("name", "Second");
      MenuLayout second = cache.getOrCreate(player, "menu", "card/title", "world", changed, this::layout);
      assertNotSame(first, second);
      cache.retain(player, "menu", Set.of());
      assertNotSame(second, cache.getOrCreate(player, "menu", "card/title", "world", changed, this::layout));
   }

   private MenuLayout layout() {
      return new MenuLayout("title", "Title", List.of(), false, 0, 0, 3.5, false,
         false, null, false, List.of(), 0, 0, 0, "", false, "");
   }
}
