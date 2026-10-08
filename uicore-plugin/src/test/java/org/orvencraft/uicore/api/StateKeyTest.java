package org.orvencraft.uicore.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.api.state.StateKey;

class StateKeyTest {
   @Test
   void convertsSupportedTypesAndRejectsInvalidValues() {
      assertEquals(3, StateKey.integer("page", 1).parse("3"));
      assertEquals("3", StateKey.integer("page", 1).format(3));
      assertEquals(1.5, StateKey.decimal("scale", 1.0).parse("1.5"));
      assertEquals(true, StateKey.bool("enabled", false).parse("true"));
      assertThrows(IllegalArgumentException.class, () -> StateKey.bool("enabled", false).parse("yes"));
      assertThrows(IllegalArgumentException.class, () -> StateKey.decimal("scale", 1.0).parse("NaN"));
   }
}
