package org.orvencraft.uicore.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NumericExpressionTest {
   @Test
   void evaluatesTimeOperatorsAndFunctions() {
      NumericExpression expression = NumericExpression.compile("clamp(0.25 * t + sin(pi / 2), 0, 2)");

      assertEquals(1.5, expression.evaluate(40, reference -> 0.0), 1.0E-9);
   }

   @Test
   void mapsPlaceholderIntoWorldRange() {
      NumericExpression expression = NumericExpression.compile("map(%mmocore_level%, 1, 100, -1.5, 1.5)");

      assertEquals(1.5, expression.evaluate(0, reference -> 100.0), 1.0E-9);
      assertEquals(-1.5, expression.evaluate(0, reference -> -20.0), 1.0E-9);
   }

   @Test
   void resolvesStateAndModulo() {
      NumericExpression expression = NumericExpression.compile("{state.page} + ticks % 4");

      assertEquals(5.0, expression.evaluate(23, reference -> 2.0), 1.0E-9);
   }

   @Test
   void resolvesNamespacedDataReferencesInCoordinates() {
      NumericExpression expression = NumericExpression.compile("{example:balance} / 100");
      assertEquals(0.75, expression.evaluate(0, reference -> {
         assertEquals("{example:balance}", reference);
         return 75.0;
      }), 1.0E-9);
   }

   @Test
   void rejectsUnknownCode() {
      assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile("System.exit(0)"));
   }

   @Test
   void rejectsNonFiniteLiteralAtCompileTime() {
      assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile("1e309"));
   }
}
