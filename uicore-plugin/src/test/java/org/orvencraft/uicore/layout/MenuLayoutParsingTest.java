package org.orvencraft.uicore.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.orvencraft.uicore.style.TextStyleConfig;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MenuLayoutParsingTest {
   @Test
   void parsesQuotedToggleOnOffKeys() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Toggle
         toggle-content:
           enabled: true
           'off': '[ ] agreement'
           'on': '[x] agreement'
         """
      );

      assertTrue(layout.hasToggleContent());
      assertEquals("[ ] agreement", layout.getToggleContentOff());
      assertEquals("[x] agreement", layout.getToggleContentOn());
   }

   @Test
   void parsesLegacyBooleanToggleKeys() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Toggle
         toggle-content:
           enabled: true
           false: '[ ] agreement'
           true: '[x] agreement'
         """
      );

      assertTrue(layout.hasToggleContent());
      assertEquals("[ ] agreement", layout.getToggleContentOff());
      assertEquals("[x] agreement", layout.getToggleContentOn());
   }

   @Test
   void parsesDurationAndCooldownFormats() throws Exception {
      assertEquals(250L, parseDurationMillis("250ms"));
      assertEquals(500L, parseDurationMillis("10 ticks"));
      assertEquals(1500L, parseDurationMillis("1.5s"));
      assertEquals(120000L, parseDurationMillis("2m"));
   }

   @Test
   void parsesBooleanVisibleIf() throws Exception {
      MenuLayout hidden = loadLayout(
         """
         name: Hidden
         visible-if: false
         """
      );

      assertFalse(hidden.isVisible(null));
   }

   @Test
   void mergesTextStyleOverridesInOrder() {
      TextStyleConfig base = style(
         """
         color: "#ffffff"
         bold: false
         """
      );
      TextStyleConfig hover = style(
         """
         color: "#00ff88"
         underlined: true
         transition:
           type: smooth
           duration: 6
         """
      );
      TextStyleConfig pressed = style(
         """
         text: "Pressed"
         strikethrough: true
         """
      );

      TextStyleConfig merged = base.merge(hover).merge(pressed);

      assertEquals("#00ff88", merged.color());
      assertTrue(merged.smoothTransition());
      assertEquals(6, merged.transitionDuration());
      assertTrue(merged.render("Default").contains("Pressed"));
   }

   @Test
   void parsesEntranceAnimation() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Animated
         entrance-animation:
           enabled: true
           from: right
           delay: 60
           duration: 12
           distance: 1.5
         """
      );

      assertTrue(layout.isEntranceAnimationEnabled());
      assertEquals("right", layout.getEntranceAnimationFrom());
      assertEquals(60L, layout.getEntranceAnimationDelay());
      assertEquals(12, layout.getEntranceAnimationDuration());
      assertEquals(1.5, layout.getEntranceAnimationDistance());
   }

   @Test
   void parsesDynamicCoordinatesWithoutChangingStaticDepth() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Dynamic
         x: "map(%level%, 1, 100, -1.5, 1.5)"
         y: "0.25 * t"
         z: 3.5
         position-update:
           interval: 4
           smoothing: 0.2
         """
      );

      assertTrue(layout.hasDynamicPosition());
      assertEquals(4, layout.getDynamicPositionSpec().intervalTicks());
      assertEquals(0.2, layout.getDynamicPositionSpec().smoothing(), 1.0E-9);
      assertEquals(3.5, layout.getDynamicPositionSpec().basePosition().z(), 1.0E-9);
   }

   private static MenuLayout loadLayout(String yaml) throws InvalidConfigurationException {
      YamlConfiguration config = new YamlConfiguration();
      config.loadFromString(yaml);
      MenuLayout layout = new MenuLayout(
         "element",
         config.getString("name", "element"),
         config.getStringList("command"),
         false,
         config.getDouble("x", 0.0),
         config.getDouble("y", 0.0),
         config.getDouble("z", 3.5),
         false,
         false,
         null,
         false,
         List.of(),
         0.0F,
         0.0F,
         0.0F,
         "",
         false,
         "",
         ""
      );
      layout.setOwnerMenuKey("test-menu");
      layout.loadConfig(config);
      return layout;
   }

   private static TextStyleConfig style(String yaml) {
      YamlConfiguration config = new YamlConfiguration();
      try {
         config.loadFromString(yaml);
      } catch (InvalidConfigurationException e) {
         throw new AssertionError(e);
      }
      return TextStyleConfig.from(config);
   }

   private static long parseDurationMillis(String raw) throws Exception {
      Method method = MenuLayout.class.getDeclaredMethod("parseDurationMillis", String.class);
      method.setAccessible(true);
      return (long)method.invoke(null, raw);
   }
}
