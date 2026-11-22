package org.orvencraft.uicore.layout;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.position.DynamicPositionSpec.Position;
import static org.junit.jupiter.api.Assertions.*;

class MenuZoomTest {
   @Test void projectsOnlyTheSelectedSubtreeAroundItsPivotAndPreservesDepth() throws Exception {
      MenuZoom zoom = parse("zoom: {enabled: true, group: map, pivot: {x: 0.5, y: -0.5}}");
      var map = ElementInteractionTest.layout("map", "type: group");
      var sub = ElementInteractionTest.layout("sub", "type: group\nparent: map");
      var leaf = ElementInteractionTest.layout("leaf", "parent: sub");
      var outside = ElementInteractionTest.layout("outside", "type: text");
      var layouts = Map.of("map", map, "sub", sub, "leaf", leaf, "outside", outside);

      assertTrue(zoom.affects(leaf, layouts));
      assertFalse(zoom.affects(outside, layouts));
      assertEquals(new Position(3.5, 6.5, 9), zoom.project(new Position(3, 5, 9), new Position(2, 4, 8), 2));
   }

   @Test void clampsStepsAndApproachesTheExactTarget() throws Exception {
      MenuZoom zoom = parse("zoom: {enabled: true, min: 0.6, max: 1.8, step: 0.1, sensitivity: 2, smoothing: 0.5}");
      assertEquals(.6, zoom.step(1, -9));
      assertEquals(1.8, zoom.step(1, 9));
      assertEquals(1.2, zoom.step(1, 1));
      assertEquals(1.4, zoom.approach(1, 1.8));
      assertEquals(1.8, zoom.approach(1.7999, 1.8));
   }

   @Test void treatsOnlyAdjacentSelectionsIncludingWraparoundAsWheelSteps() {
      assertEquals(1, MenuZoom.scrollDirection(0, 8));
      assertEquals(-1, MenuZoom.scrollDirection(8, 0));
      assertEquals(1, MenuZoom.scrollDirection(5, 4));
      assertEquals(-1, MenuZoom.scrollDirection(4, 5));
      assertEquals(0, MenuZoom.scrollDirection(4, 4));
      assertEquals(0, MenuZoom.scrollDirection(8, 3));
      assertEquals(0, MenuZoom.scrollDirection(8, 9));
   }

   @Test void validatesZoomInputAndKeepsMenusOptIn() throws Exception {
      assertNull(parse("zoom: {enabled: false}"));
      assertNull(parse(""));
      for (String yaml : List.of("zoom: true", "zoom: {min: .nan}", "zoom: {max: 0.8}",
         "zoom: {smoothing: 0}", "zoom: {pivot: 0.5}", "zoom: {enabled: 'yes'}", "zoom: {group: [map]}")) {
         assertThrows(IllegalArgumentException.class, () -> parse(yaml), yaml);
      }
   }

   private static MenuZoom parse(String yaml) throws Exception {
      var config = new YamlConfiguration();
      config.loadFromString(yaml);
      return MenuZoom.from(config);
   }
}
