package org.orvencraft.uicore.cutscene.config;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;

class CutsceneConfigLoaderTest {
   @TempDir Path directory;

   private static final String SCENE = """
      id: tour
      start: flight
      points:
        origin: {world: world, x: 0, y: 64, z: 0, yaw: 179, pitch: 0}
        destination: {world: world, x: 20, y: 66, z: 10, yaw: -179, pitch: 20}
      policy: {start-mode: initial, completion: keep-open, allow-skip: true, skip-to: done}
      steps:
        flight:
          type: move
          from: origin
          to: [destination]
          duration: 100
          speed: uniform
          rotation: {mode: interpolate, curve: ease-in-out}
          enter: ['[state] scene flying']
          next: decision
        decision:
          type: choice
          options:
            left: {element: left_button, next: pause}
            right: {element: right_button, next: done}
          timeout: 100
          timeout-next: done
          fallback-next: done
        pause: {type: wait, duration: external, next: done}
        done: {type: end}
      """;

   @Test void loadsImmutableScenarioAndExternalWait() throws Exception {
      CutsceneDefinition definition = load(SCENE);
      assertEquals("flight", definition.startStep());
      assertEquals(CutsceneDefinition.StartMode.INITIAL, definition.policy().startMode());
      assertEquals(-1, ((CutsceneDefinition.Wait) definition.steps().get("pause")).durationTicks());
      assertThrows(UnsupportedOperationException.class, () -> definition.steps().clear());
      assertThrows(UnsupportedOperationException.class, () -> definition.steps().get("flight").enterActions().clear());
   }

   @Test void loadsReusableCurvesAndDetectsDuplicateScenarioIds() throws Exception {
      Files.writeString(directory.resolve("curves.yml"), "curves:\n  cruise:\n    interpolation: smooth\n    points: [{time: 0, speed: 0}, {time: 1, speed: 3}]\n");
      Files.writeString(directory.resolve("tour.yml"), SCENE.replace("speed: uniform", "speed: cruise"));
      assertEquals(1, CutsceneConfigLoader.loadDirectory(directory.toFile()).size());
      Files.writeString(directory.resolve("duplicate.yml"), SCENE);
      assertTrue(assertThrows(IllegalArgumentException.class, () -> CutsceneConfigLoader.loadDirectory(directory.toFile())).getMessage().contains("Duplicate scenario"));
   }

   @Test void errorsIdentifyScenarioStepAndParameter() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("to: [destination]", "to: [missing]")));
      assertTrue(exception.getMessage().contains("tour"));
      assertTrue(exception.getMessage().contains("flight"));
      assertTrue(exception.getMessage().contains("to"));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("next: decision", "next: missing")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("duration: 100", "duration: 0")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("x: 20", "x: .NaN")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("pitch: 20", "pitch: 91")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("x: 20", "x: twenty")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("destination: {world: world", "destination: {world: nether")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("speed: uniform", "speed: missing")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("timeout-next: done", "unexpected: done")));
   }

   @Test void rejectsMalformedCurvesAndInstantCycles() {
      String curves = "curves:\n  invalid:\n    points: [{time: 0, speed: 0}, {time: 1, speed: 0}]\n";
      assertThrows(IllegalArgumentException.class, () -> load(SCENE + curves));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE + curves.replace("speed: 0", "speed: -1")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("pause: {type: wait, duration: external, next: done}", "pause: {type: wait, duration: 0, next: pause}")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("fallback-next: done", "fallback-next: decision")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("right_button", "left_button")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("duration: 100", "duration: 2.5")));
      assertThrows(IllegalArgumentException.class, () -> load(SCENE.replace("duration: 100", "duration: 9007199254740993")));
   }

   @Test void malformedYamlFailsInsteadOfSilentlyLoadingNothing() throws Exception {
      Files.writeString(directory.resolve("invalid.yml"), "points: [\n");
      assertThrows(IllegalArgumentException.class, () -> CutsceneConfigLoader.loadDirectory(directory.toFile()));
   }

   @Test void duplicateYamlIdsAreRejectedBeforeOneDefinitionCanOverwriteAnother() throws Exception {
      Files.writeString(directory.resolve("duplicate-steps.yml"), SCENE + "  done: {type: wait, duration: 20, next: flight}\n");
      assertThrows(IllegalArgumentException.class, () -> CutsceneConfigLoader.loadDirectory(directory.toFile()));
   }

   private static CutsceneDefinition load(String text) throws Exception {
      YamlConfiguration section = new YamlConfiguration();
      section.loadFromString(text);
      return CutsceneConfigLoader.load("tour", section);
   }
}
