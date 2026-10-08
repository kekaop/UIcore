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
   void parsesIndependentGroupAndRotationDragSettings() throws Exception {
      MenuLayout groupHandle = loadLayout("""
         drag-group: controls
         drag:
           mode: move-group
           group: entries
           sensitivity: 1.5
         """);
      assertEquals("controls", groupHandle.getDragGroup());
      assertEquals("move-group", groupHandle.getDragMode());
      assertEquals("entries", groupHandle.getDragTarget());
      assertEquals(1.5, groupHandle.getDragSensitivity());

      MenuLayout rotationHandle = loadLayout("""
         drag:
           mode: rotate-element
           target: preview
         """);
      assertEquals("rotate-element", rotationHandle.getDragMode());
      assertEquals("preview", rotationHandle.getDragTarget());
      assertEquals(1.0, rotationHandle.getDragSensitivity());
   }

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
   void parsesExtensionActionAndFailsClosedForUnavailableCustomCondition() throws Exception {
      MenuLayout layout = loadLayout("""
         action:
           type: quests:accept
           quest: daily_1
         visible-if:
           not:
             custom:
               type: quests:completed
               id: tutorial
         """);
      assertEquals("quests:accept", layout.getExtensionActionId());
      assertEquals("daily_1", layout.getExtensionActionArguments().get("quest"));
      assertEquals("quests:accept", layout.copySourceConfig().getConfigurationSection("action").getString("type"));
      assertFalse(layout.isVisible(null));
   }

   @Test
   void keepsItemDataBindingsAvailableForPerPlayerResolution() throws Exception {
      MenuLayout layout = loadLayout("""
         type: item
         item:
           material: '{items:current-material}'
           custom-model-data: '{items:current-model}'
           glow: '{items:highlighted}'
         """);
      assertEquals("{items:current-material}", layout.resolveItemMaterial(null));
      assertEquals("{items:current-model}", layout.sourceModel().get("item.custom-model-data"));
      assertEquals("{items:highlighted}", layout.copySourceConfig().getConfigurationSection("item").getString("glow"));
   }

   @Test
   void elementStatesControlInitialVisibilityAndAppearance() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Panel
         initial-state: hidden
         states:
           hidden:
             visible: false
           shown:
             visible: true
             text: 'Panel is open'
             style:
               color: '#00ff88'
             material: EMERALD
             custom-model-data: 7
             glow: true
         """
      );

      assertEquals("hidden", layout.getInitialElementState());
      assertTrue(layout.hasElementState("shown"));
      assertFalse(layout.hasElementState("missing"));
      assertFalse(layout.isVisible(null));
      assertEquals("DIAMOND", layout.resolveItemMaterial(null));

      MenuLayout shown = loadLayout(
         """
         name: Panel
         initial-state: shown
         states:
           hidden:
             visible: false
           shown:
             visible: true
             text: 'Panel is open'
             style:
               color: '#00ff88'
             material: EMERALD
             custom-model-data: 7
             glow: true
         """
      );
      assertTrue(shown.isVisible(null));
      assertEquals("EMERALD", shown.resolveItemMaterial(null));
      assertEquals(7, shown.resolveItemCustomModelData(null));
      assertTrue(shown.resolveItemGlow(null));
      assertEquals("#00ff88", shown.resolveTextStyle(null, false, false).color());
   }

   @Test
   void parsesClickChangesForOtherElements() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Controller
         set-state:
           element: panel
           state: shown
         set-states:
           caption: active
         """
      );

      assertEquals(java.util.Map.of("panel", "shown", "caption", "active"), layout.getClickElementStates());
      org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
         () -> layout.getClickElementStates().put("panel", "hidden"));
   }

   @Test
   void rejectsUnknownInitialElementState() {
      org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> loadLayout(
         """
         initial-state: missing
         states:
           shown:
             visible: true
         """
      ));
   }

   @Test
   void visibleStateDoesNotBypassExistingVisibilityCondition() throws Exception {
      MenuLayout layout = loadLayout(
         """
         initial-state: shown
         states:
           shown:
             visible: true
         visible-if: false
         """
      );
      assertFalse(layout.isVisible(null));
   }

   @Test
   void shortSetStateTargetsClickedElement() throws Exception {
      MenuLayout layout = loadLayout("set-state: active\n");
      assertEquals(java.util.Map.of("element", "active"), layout.getClickElementStates());
   }

   @Test
   void nestedMapConditionsFailClosedWithoutPlayerState() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Locked
         visible-if:
           all:
             - state:
                 locked: true
             - true
         """
      );

      assertFalse(layout.isVisible(null));
   }

   @Test
   void unknownVisibleIfStructureFailsClosed() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Invalid
         visible-if:
           all: not-a-list
         """
      );

      assertFalse(layout.isVisible(null));
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
   void parsesTextButtonHitbox() throws Exception {
      MenuLayout layout = loadLayout(
         """
         name: Button
         hitbox:
           width: 1.2
           height: 0.45
           depth: 0.3
         """
      );

      assertTrue(layout.hasCustomHitbox());
      assertEquals(1.2, layout.getHitboxWidth());
      assertEquals(0.45, layout.getHitboxHeight());
      assertEquals(0.3, layout.getHitboxDepth());
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

   @Test
   void parsesPlayerAvatarAsHeadItem() throws Exception {
      MenuLayout layout = loadLayout(
         """
         type: player-avatar
         player: "%target_player%"
         scale: 0.8
         billboard: center
         transform: head
         hitbox:
           width: 0.8
           height: 0.8
         """
      );

      assertTrue(layout.isPlayerAvatarLayout());
      assertTrue(layout.isItemLayout());
      assertEquals("PLAYER_HEAD", layout.getItemMaterial());
      assertEquals("%target_player%", layout.getAvatarPlayer());
      assertEquals("head", layout.getItemDisplayTransform());
      assertEquals(0.8, layout.getItemScale(), 1.0E-9);
      assertEquals(0.8, layout.getHitboxWidth(), 1.0E-9);
      assertFalse(layout.isAvatarFaceOnly());
   }

   @Test
   void parsesFlatPlayerAvatar() throws Exception {
      MenuLayout layout = loadLayout("type: player-avatar\nface-only: true\nrotate: true\nrotate-speed: 0.6\n");
      assertTrue(layout.isAvatarFaceOnly());
      assertTrue(layout.isItemRotate());
      assertEquals(0.6, layout.getItemRotateSpeed(), 1.0E-9);
   }

   @Test
   void negationDoesNotTurnUnavailablePlaceholderIntoPermission() throws Exception {
      assertFalse(loadLayout("visible-if:\n  not:\n    placeholder: '%missing_expansion.a-b%'\n    operator: '!='\n    value: 'true'\n").isVisible(null));
      assertFalse(loadLayout("visible-if:\n  any:\n    - true\n    - placeholder: '%missing_expansion%'\n      value: true\n").isVisible(null));
      assertFalse(loadLayout("visible-if:\n  not:\n    state: test\n    operator: mystery\n    value: true\n").isVisible(null));
      assertTrue(loadLayout("visible-if:\n  not: false\n").isVisible(null));
   }

   @Test
   void regexIsCompiledAtLoadAndHandlesPathologicalInput() throws Exception {
      org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
         () -> loadLayout("visible-if:\n  placeholder: '%value%'\n  matches: '[bad'\n"));
      var layout = loadLayout("visible-if:\n  placeholder: '%value%'\n  matches: '(a+)+$'\n");
      var field = MenuLayout.class.getDeclaredField("visibleIf");
      field.setAccessible(true);
      Object condition = field.get(layout);
      Method compare = condition.getClass().getDeclaredMethod("compare", String.class, String.class, String.class);
      compare.setAccessible(true);
      assertFalse((boolean)compare.invoke(condition, "a".repeat(4095) + "!", "(a+)+$", "matches"));
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
