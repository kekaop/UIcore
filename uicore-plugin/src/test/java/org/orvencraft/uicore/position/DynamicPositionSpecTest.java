package org.orvencraft.uicore.position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DynamicPositionSpecTest {
   @Test
   void parsesAndEvaluatesDynamicAxes() throws InvalidConfigurationException {
      YamlConfiguration config = yaml(
         """
         x: "map(%level%, 1, 100, -1.5, 1.5)"
         y: "0.25 * t"
         z: 3.5
         position-update:
           interval: 2
           smoothing: 0.15
           clamp:
             y: [-0.4, 0.4]
         """
      );
      DynamicPositionSpec spec = DynamicPositionSpec.from(config, 0.0, 0.0, 3.5);

      DynamicPositionSpec.Evaluation result = spec.evaluate(40, reference -> 100.0, spec.basePosition());

      assertTrue(spec.isDynamic());
      assertEquals(2, spec.intervalTicks());
      assertEquals(0.15, spec.smoothing(), 1.0E-9);
      assertEquals(1.5, result.position().x(), 1.0E-9);
      assertEquals(0.4, result.position().y(), 1.0E-9);
      assertEquals(3.5, result.position().z(), 1.0E-9);
      assertTrue(result.failures().isEmpty());
   }

   @Test
   void keepsPreviousAxisValueWhenRuntimeInputIsInvalid() throws InvalidConfigurationException {
      DynamicPositionSpec spec = DynamicPositionSpec.from(yaml("x: \"1 / %value%\""), 0.0, 0.0, 3.5);

      DynamicPositionSpec.Evaluation result = spec.evaluate(0, reference -> 0.0, new DynamicPositionSpec.Position(0.75, 0.0, 3.5));

      assertEquals(0.75, result.position().x(), 1.0E-9);
      assertFalse(result.failures().isEmpty());
   }

   @Test
   void reportsInvalidConfigurationWithoutMakingLayoutDynamic() throws InvalidConfigurationException {
      DynamicPositionSpec spec = DynamicPositionSpec.from(yaml("y: \"unknown(1)\""), 0.0, 0.25, 3.5);

      assertFalse(spec.isDynamic());
      assertFalse(spec.configurationErrors().isEmpty());
      assertEquals(0.25, spec.basePosition().y(), 1.0E-9);
   }

   private static YamlConfiguration yaml(String source) throws InvalidConfigurationException {
      YamlConfiguration configuration = new YamlConfiguration();
      configuration.loadFromString(source);
      return configuration;
   }
}
