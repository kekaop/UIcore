package org.orvencraft.uicore.layout;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.position.DynamicPositionSpec.Position;
import static org.junit.jupiter.api.Assertions.*;

class ElementInteractionTest {
   @Test void removingAParentDoesNotPreventUnrelatedRuntimeEdits() throws Exception {
      var child = layout("child", "parent: root\ndrag: {enabled: true, target: root}");
      var orphaned = Map.of("child", child);
      var other = layout("other", "name: Other");
      ElementHierarchy.validateMutation(orphaned, Map.of("child", child, "other", other));
      assertThrows(IllegalArgumentException.class, () -> ElementHierarchy.validate(orphaned));
      var bad = layout("other", "parent: nonexistent");
      assertThrows(IllegalArgumentException.class, () -> ElementHierarchy.validateMutation(orphaned, Map.of("child", child, "other", bad)));
      var root = layout("root", "type: group");
      ElementHierarchy.validateMutation(orphaned, Map.of("child", child, "root", root));
   }
   static MenuLayout layout(String id, String yaml) throws Exception {
      var c = new YamlConfiguration(); c.loadFromString(yaml);
      var l = new MenuLayout(id, id, List.of(), false, c.getDouble("x"), c.getDouble("y"), c.getDouble("z"),
         false, false, null, false, List.of(), 0, 0, 0, "", false, "", "");
      l.loadConfig(c); return l;
   }
   @Test void resolvesNestedOffsetsRegardlessOfDefinitionOrder() throws Exception {
      var root = layout("window", "type: group\nx: 2\ny: 3\nz: 4\n");
      var child = layout("panel", "parent: window\nx: -1\ny: 2\nz: 0\n");
      var leaf = layout("button", "parent: panel\nx: 0.5\ny: -1\nz: 0.1\n");
      var tree = Map.of("button", leaf, "panel", child, "window", root);
      ElementHierarchy.validate(tree);
      assertEquals(new Position(1.5, 4, 4.1), ElementHierarchy.position(leaf, tree, l -> l.getDynamicPositionSpec().basePosition()));
      assertFalse(root.interaction().pointerEvents());
      assertFalse(ElementHierarchy.hasAncestors("button", Map.of("button", leaf, "panel", child)));
   }
   @Test void rejectsCyclesAndMissingParentsAndUnrelatedDragHandles() throws Exception {
      var a = layout("a", "parent: b"); var b = layout("b", "parent: a");
      assertThrows(IllegalArgumentException.class, () -> ElementHierarchy.validate(Map.of("a", a, "b", b)));
      assertThrows(IllegalArgumentException.class, () -> ElementHierarchy.validate(Map.of("a", a)));
      var handle = layout("handle", "drag: {enabled: true, target: unrelated}");
      var unrelated = layout("unrelated", "type: group");
      assertThrows(IllegalArgumentException.class, () -> ElementHierarchy.validate(Map.of("handle", handle, "unrelated", unrelated)));
   }
   @Test void locksAxisClampsAndQuantizesSliderValues() throws Exception {
      var spec = layout("slider", "slider: {state: volume, min: 0, max: 100, step: 5}\ndrag: {axis: x, bounds: {x: [-1, 1]}}").interaction();
      assertTrue(spec.draggable());
      assertEquals(1, spec.x(0, 100)); assertEquals(-1, spec.x(0, -100));
      assertEquals(.3, spec.y(.3, 100));
      assertTrue(spec.permitsDrop(0, .3, .5, .3));
      assertFalse(spec.permitsDrop(0, .3, 2, .3));
      assertFalse(spec.permitsDrop(0, .3, .5, .4));
      assertEquals(50, spec.sliderValue(.03, 0)); assertEquals(100, spec.sliderValue(3, 0));
   }
   @Test void rejectsMalformedInteractionConfiguration() {
      for (String yaml : List.of("parent: [x]", "drag: true", "drag: {enabled: yesplease}",
         "drag: {bounds: {x: [2, 1]}}", "slider: {state: value}", "pointer-events: maybe",
         "interaction-priority: 0.5", "drag: {drop-targets: target}", "on-drop: action", "on-drop: [true]", "on-drop: [' ']"))
         assertThrows(IllegalArgumentException.class, () -> layout("bad", yaml), yaml);
   }
   @Test void parentMovesPropagateWithoutChangingChildLocalCoordinates() throws Exception {
      var root = layout("root", "type: group"); var child = layout("child", "parent: root\nx: 1");
      var tree = Map.of("root", root, "child", child);
      assertEquals(new Position(11, 20, 3), ElementHierarchy.position(child, tree,
         l -> l == root ? new Position(10, 20, 3) : l.getDynamicPositionSpec().basePosition()));
      assertEquals(1, child.x);
   }
}
