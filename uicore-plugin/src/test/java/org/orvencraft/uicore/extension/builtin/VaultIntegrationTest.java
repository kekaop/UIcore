package org.orvencraft.uicore.extension.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class VaultIntegrationTest {
   @Test
   void rejectsAmountsThatCannotBeUsedForBalanceChecks() {
      assertEquals(100.0, VaultIntegration.amount(100));
      assertEquals(12.5, VaultIntegration.amount("12.5"));
      assertNull(VaultIntegration.amount(-1));
      assertNull(VaultIntegration.amount("NaN"));
      assertNull(VaultIntegration.amount("Infinity"));
      assertNull(VaultIntegration.amount("invalid"));
      assertNull(VaultIntegration.amount(null));
   }
}
