package org.orvencraft.uicore.cutscene.config;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;
import org.orvencraft.uicore.cutscene.model.SpeedCurve;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import static org.orvencraft.uicore.cutscene.model.CutsceneDefinition.*;

/** Strict, transactional loader. No configuration or filesystem access is needed during playback. */
public final class CutsceneConfigLoader {
   private CutsceneConfigLoader() {}

   public static Map<String, CutsceneDefinition> loadDirectory(File directory) {
      if (!directory.isDirectory()) throw new IllegalArgumentException("Cutscene directory does not exist: " + directory);
      File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
      if (files == null) throw new IllegalArgumentException("Cannot list cutscene directory: " + directory);
      Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      Map<File, YamlConfiguration> parsed = new LinkedHashMap<>();
      for (File file : files) {
         parsed.put(file, loadFile(file));
      }
      Map<String, SpeedCurve> shared = new LinkedHashMap<>(SpeedCurve.builtins());
      for (var file : parsed.entrySet()) {
         if (!file.getValue().contains("id")) {
            if (!file.getValue().isConfigurationSection("curves")) throw error(file.getKey().getName(), null, "id", "Required scenario ID or curves collection");
            readCurves(file.getKey().getName(), file.getValue().getConfigurationSection("curves"), shared);
         }
      }
      Map<String, CutsceneDefinition> result = new LinkedHashMap<>();
      for (var file : parsed.entrySet()) {
         if (!file.getValue().contains("id")) continue;
         String id = requiredString(file.getKey().getName(), null, file.getValue(), "id");
         if (result.containsKey(id)) throw error(id, null, "id", "Duplicate scenario ID in " + file.getKey().getName());
         result.put(id, load(id, file.getValue(), shared));
      }
      return Map.copyOf(result);
   }

   /** Strict YAML parsing without requiring a complete scenario, suitable for point-editing drafts. */
   public static YamlConfiguration loadFile(File file) {
      YamlConfiguration yaml = new YamlConfiguration();
      try {
         String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
         yaml.loadFromString(source);
         // Bukkit accepts duplicate mapping keys. Inspect the YAML syntax tree before an ID can be overwritten.
         Node tree = new Yaml(new SafeConstructor(new LoaderOptions())).compose(new StringReader(source));
         rejectDuplicateKeys(yaml.getString("id", file.getName()), tree, "", java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
         return yaml;
      } catch (IOException | InvalidConfigurationException failure) {
         throw error(file.getName(), null, "file", "Cannot read YAML: " + failure.getMessage());
      }
   }

   private static void rejectDuplicateKeys(String id, Node node, String path, Set<Node> visited) {
      if (node == null || !visited.add(node)) return;
      if (node instanceof MappingNode mapping) {
         Set<String> keys = new HashSet<>();
         for (var tuple : mapping.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode key)) throw error(id, null, path, "Mapping keys must be scalar IDs");
            String full = path.isEmpty() ? key.getValue() : path + "." + key.getValue();
            if (!keys.add(key.getValue())) {
               String[] pieces = full.split("\\.", 3);
               String step = pieces.length >= 2 && pieces[0].equals("steps") ? pieces[1] : null;
               throw error(id, step, full, "Duplicate YAML key '" + key.getValue() + "'");
            }
            rejectDuplicateKeys(id, tuple.getValueNode(), full, visited);
         }
      } else if (node instanceof SequenceNode sequence) {
         for (int i = 0; i < sequence.getValue().size(); i++) rejectDuplicateKeys(id, sequence.getValue().get(i), path + "[" + i + "]", visited);
      }
   }

   public static CutsceneDefinition load(String id, ConfigurationSection section) {
      return load(id, section, SpeedCurve.builtins());
   }

   public static CutsceneDefinition load(String id, ConfigurationSection section, Map<String, SpeedCurve> sharedCurves) {
      if (id == null || id.isBlank()) throw error(String.valueOf(id), null, "id", "Scenario ID must not be blank");
      Map<String, SpeedCurve> curves = new LinkedHashMap<>(sharedCurves);
      if (section.contains("curves")) readCurves(id, requiredSection(id, null, section, "curves"), curves);
      Map<String, CameraPose> points = new LinkedHashMap<>();
      ConfigurationSection configuredPoints = requiredSection(id, null, section, "points");
      String world = null;
      for (String pointId : configuredPoints.getKeys(false)) {
         if (pointId.isBlank()) throw error(id, null, "points", "Point IDs must not be blank");
         ConfigurationSection p = requiredSection(id, null, configuredPoints, pointId);
         String path = "points." + pointId;
         String pointWorld = requiredString(id, null, p, "world");
         if (world != null && !world.equals(pointWorld)) throw error(id, null, path + ".world", "All points must use the same world");
         world = pointWorld;
         double pitch = number(id, null, p, "pitch", 0);
         if (Math.abs(pitch) > 90) throw error(id, null, path + ".pitch", "Pitch must be in [-90, 90]");
         try {
            points.put(pointId, new CameraPose(pointWorld, number(id, null, p, "x", null), number(id, null, p, "y", null),
               number(id, null, p, "z", null), number(id, null, p, "yaw", 0), pitch));
         } catch (IllegalArgumentException invalid) { throw error(id, null, path, invalid.getMessage()); }
      }
      if (points.isEmpty()) throw error(id, null, "points", "At least one camera point is required");
      Map<String, Step> steps = new LinkedHashMap<>();
      ConfigurationSection configuredSteps = requiredSection(id, null, section, "steps");
      for (String stepId : configuredSteps.getKeys(false)) {
         if (stepId.isBlank()) throw error(id, null, "steps", "Step IDs must not be blank");
         ConfigurationSection s = requiredSection(id, stepId, configuredSteps, stepId);
         List<String> enter = strings(id, stepId, s, "enter", false), exit = strings(id, stepId, s, "exit", false);
         String type = requiredString(id, stepId, s, "type");
         Step step = switch (type.toLowerCase(Locale.ROOT)) {
            case "move" -> {
               String from = optionalString(id, stepId, s, "from");
               if ("current".equalsIgnoreCase(from)) from = null;
               if (from != null) pointRef(id, stepId, "from", from, points);
               List<String> to = strings(id, stepId, s, "to", true);
               if (to.isEmpty()) throw error(id, stepId, "to", "At least one destination point is required");
               for (String dest : to) pointRef(id, stepId, "to", dest, points);
               long duration = ticks(id, stepId, s, "duration", null);
               if (duration <= 0) throw error(id, stepId, "duration", "Movement duration must be positive");
               PathType path = enumValue(id, stepId, s, "path", PathType.class, PathType.LINEAR);
               SpeedCurve speed = curveRef(id, stepId, s, "speed", curves);
               Rotation rotation = Rotation.defaults();
               if (s.contains("rotation")) {
                  ConfigurationSection r = requiredSection(id, stepId, s, "rotation");
                  RotationMode mode = enumValue(id, stepId, r, "mode", RotationMode.class, RotationMode.INTERPOLATE);
                  Double yaw = r.contains("yaw") ? number(id, stepId, r, "yaw", null) : null;
                  Double pitch = r.contains("pitch") ? number(id, stepId, r, "pitch", null) : null;
                  if (pitch != null && Math.abs(pitch) > 90) throw error(id, stepId, "rotation.pitch", "Pitch must be in [-90, 90]");
                  String target = optionalString(id, stepId, r, "target");
                  if (mode == RotationMode.LOOK_AT && target == null) throw error(id, stepId, "rotation.target", "look-at needs a point target");
                  if (target != null) pointRef(id, stepId, "rotation.target", target, points);
                  long turns = ticks(id, stepId, r, "turns", 0L);
                  if (turns < 0 || turns > Integer.MAX_VALUE) throw error(id, stepId, "rotation.turns", "Turns must be a nonnegative integer");
                  rotation = new Rotation(mode, yaw, pitch, target, curveRef(id, stepId, r, "curve", curves),
                     enumValue(id, stepId, r, "direction", TurnDirection.class, TurnDirection.SHORTEST), (int) turns);
               }
               String before = optionalString(id, stepId, s, "tangent-before"), after = optionalString(id, stepId, s, "tangent-after");
               if (before != null) pointRef(id, stepId, "tangent-before", before, points);
               if (after != null) pointRef(id, stepId, "tangent-after", after, points);
               yield new Move(stepId, enter, exit, from, to, duration, path, speed, rotation, requiredString(id, stepId, s, "next"), before, after);
            }
            case "wait" -> {
               long duration = "external".equals(s.get("duration")) ? -1 : ticks(id, stepId, s, "duration", null);
               if (duration < -1) throw error(id, stepId, "duration", "Wait must be >= 0 ticks or external");
               yield new Wait(stepId, enter, exit, duration, requiredString(id, stepId, s, "next"));
            }
            case "choice" -> {
               Map<String, ChoiceOption> options = new LinkedHashMap<>();
               Set<String> elements = new HashSet<>();
               ConfigurationSection opts = requiredSection(id, stepId, s, "options");
               for (String optionId : opts.getKeys(false)) {
                  if (optionId.isBlank()) throw error(id, stepId, "options", "Option IDs must not be blank");
                  ConfigurationSection option = requiredSection(id, stepId, opts, optionId);
                  String element = requiredString(id, stepId, option, "element");
                  if (!elements.add(element)) throw error(id, stepId, "options." + optionId + ".element", "Each choice option must use a distinct menu element");
                  options.put(optionId, new ChoiceOption(element, requiredString(id, stepId, option, "next")));
               }
               if (options.isEmpty()) throw error(id, stepId, "options", "Choice needs at least one option");
               long timeout = ticks(id, stepId, s, "timeout", 0L);
               if (timeout < 0) throw error(id, stepId, "timeout", "Timeout must be nonnegative");
               String timeoutNext = optionalString(id, stepId, s, "timeout-next");
               if (timeout > 0 && timeoutNext == null) throw error(id, stepId, "timeout-next", "A choice timeout requires an explicit transition");
               yield new Choice(stepId, enter, exit, options, timeout, timeoutNext, optionalString(id, stepId, s, "fallback-next"));
            }
            case "end" -> new End(stepId, enter, exit);
            default -> throw error(id, stepId, "type", "Unknown step type: " + type);
         };
         steps.put(stepId, step);
      }
      String start = requiredString(id, null, section, "start");
      requireStep(id, null, "start", start, steps);
      Policy policy = readPolicy(id, section);
      if (policy.skipTo() != null) requireStep(id, null, "policy.skip-to", policy.skipTo(), steps);
      for (Step step : steps.values()) {
         if (step instanceof Move move) requireStep(id, step.id(), "next", move.next(), steps);
         if (step instanceof Wait wait) requireStep(id, step.id(), "next", wait.next(), steps);
         if (step instanceof Choice choice) {
            for (var option : choice.options().entrySet()) requireStep(id, step.id(), "options." + option.getKey() + ".next", option.getValue().next(), steps);
            if (choice.timeoutNext() != null) requireStep(id, step.id(), "timeout-next", choice.timeoutNext(), steps);
            if (choice.fallbackNext() != null) requireStep(id, step.id(), "fallback-next", choice.fallbackNext(), steps);
         }
      }
      Set<String> checked = new HashSet<>();
      for (Step step : steps.values()) rejectInstantCycle(id, step.id(), steps, checked);
      return new CutsceneDefinition(id, points, steps, start, policy);
   }

   private static Policy readPolicy(String id, ConfigurationSection root) {
      Policy defaults = Policy.defaults();
      if (!root.contains("policy")) return defaults;
      ConfigurationSection p = requiredSection(id, null, root, "policy");
      long transition = ticks(id, null, p, "transition-ticks", defaults.transitionTicks());
      long chunkTimeout = ticks(id, null, p, "chunk-timeout-ticks", defaults.chunkTimeoutTicks());
      if (transition <= 0) throw error(id, null, "policy.transition-ticks", "Transition duration must be positive");
      if (chunkTimeout <= 0) throw error(id, null, "policy.chunk-timeout-ticks", "Chunk preparation timeout must be positive");
      return new Policy(enumValue(id, null, p, "start-mode", StartMode.class, defaults.startMode()), transition,
         enumValue(id, null, p, "completion", Completion.class, defaults.completion()),
         enumValue(id, null, p, "restart", Restart.class, defaults.restart()), bool(id, p, "allow-skip", false),
         optionalString(id, null, p, "skip-to"), bool(id, p, "cursor", true), bool(id, p, "clicks", true),
         bool(id, p, "edge-pan", false), bool(id, p, "freeze-ui", false), chunkTimeout);
   }

   private static void readCurves(String id, ConfigurationSection section, Map<String, SpeedCurve> result) {
      for (String curveId : section.getKeys(false)) {
         if (curveId.isBlank()) throw error(id, null, "curves", "Curve IDs must not be blank");
         if (result.containsKey(curveId)) throw error(id, null, "curves." + curveId, "Duplicate curve ID");
         ConfigurationSection curve = requiredSection(id, null, section, curveId);
         SpeedCurve.Interpolation interpolation = enumValue(id, null, curve, "interpolation", SpeedCurve.Interpolation.class, SpeedCurve.Interpolation.LINEAR);
         Object raw = curve.get("points");
         if (!(raw instanceof List<?> list)) throw error(id, null, "curves." + curveId + ".points", "Expected list of {time, speed}");
         List<SpeedCurve.Point> points = new ArrayList<>();
         try {
            for (Object item : list) {
               if (!(item instanceof Map<?, ?> map) || !(map.get("time") instanceof Number time) || !(map.get("speed") instanceof Number speed)) {
                  throw new IllegalArgumentException("Expected numeric time and speed for each control point");
               }
               points.add(new SpeedCurve.Point(time.doubleValue(), speed.doubleValue()));
            }
            result.put(curveId, new SpeedCurve(points, interpolation));
         } catch (IllegalArgumentException invalid) { throw error(id, null, "curves." + curveId + ".points", invalid.getMessage()); }
      }
   }

   private static void rejectInstantCycle(String id, String stepId, Map<String, Step> steps, Set<String> checked) {
      Set<String> visiting = new HashSet<>();
      String current = stepId;
      while (current != null && !checked.contains(current)) {
         Step step = steps.get(current);
         if (!visiting.add(current)) throw error(id, current, step instanceof Choice ? "fallback-next" : "next",
            "Instantaneous transition cycle has no movement, waiting, or input");
         // An unavailable choice follows fallback without receiving input, so include that route.
         current = step instanceof Wait wait && wait.durationTicks() == 0 ? wait.next()
            : step instanceof Choice choice ? choice.fallbackNext() : null;
      }
      checked.addAll(visiting);
   }

   private static SpeedCurve curveRef(String id, String step, ConfigurationSection s, String key, Map<String, SpeedCurve> curves) {
      String ref = s.contains(key) ? requiredString(id, step, s, key) : "uniform";
      SpeedCurve curve = curves.get(ref);
      if (curve == null) throw error(id, step, key, "Unknown speed curve: " + ref);
      return curve;
   }

   private static void pointRef(String id, String step, String path, String ref, Map<String, CameraPose> points) {
      if (!points.containsKey(ref)) throw error(id, step, path, "Unknown camera point: " + ref);
   }
   private static void requireStep(String id, String step, String path, String ref, Map<String, Step> steps) {
      if (!steps.containsKey(ref)) throw error(id, step, path, "Unknown target step: " + ref);
   }
   private static ConfigurationSection requiredSection(String id, String step, ConfigurationSection s, String path) {
      ConfigurationSection value = s.getConfigurationSection(path);
      if (value == null) throw error(id, step, fullPath(s, path), "Expected a configuration section");
      return value;
   }
   private static String requiredString(String id, String step, ConfigurationSection s, String path) {
      String value = optionalString(id, step, s, path);
      if (value == null) throw error(id, step, fullPath(s, path), "Required nonblank string");
      return value;
   }
   private static String optionalString(String id, String step, ConfigurationSection s, String path) {
      Object value = s.get(path);
      if (value == null) return null;
      if (!(value instanceof String text) || text.isBlank()) throw error(id, step, fullPath(s, path), "Expected nonblank string");
      return (String) value;
   }
   private static List<String> strings(String id, String step, ConfigurationSection s, String path, boolean allowScalar) {
      Object value = s.get(path);
      if (value == null) return List.of();
      if (allowScalar && value instanceof String text && !text.isBlank()) return List.of(text);
      if (!(value instanceof List<?> list)) throw error(id, step, fullPath(s, path), "Expected list of strings");
      List<String> result = new ArrayList<>();
      for (Object item : list) {
         if (!(item instanceof String text) || text.isBlank()) throw error(id, step, fullPath(s, path), "Expected nonblank string entries");
         result.add((String) item);
      }
      return result;
   }
   private static double number(String id, String step, ConfigurationSection s, String path, Number fallback) {
      Object raw = s.get(path, fallback);
      if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue())) throw error(id, step, fullPath(s, path), "Expected finite number");
      return number.doubleValue();
   }
   private static long ticks(String id, String step, ConfigurationSection s, String path, Long fallback) {
      Object raw = s.get(path, fallback);
      if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue() != Math.rint(number.doubleValue())
         || Math.abs(number.doubleValue()) > 9007199254740991D) throw error(id, step, fullPath(s, path), "Expected exact integer tick count with absolute value <= 9007199254740991");
      return number.longValue();
   }
   private static boolean bool(String id, ConfigurationSection s, String path, boolean fallback) {
      Object raw = s.get(path, fallback);
      if (!(raw instanceof Boolean value)) throw error(id, null, fullPath(s, path), "Expected boolean");
      return (Boolean) raw;
   }
   private static <E extends Enum<E>> E enumValue(String id, String step, ConfigurationSection s, String key, Class<E> type, E fallback) {
      if (!s.contains(key)) return fallback;
      String value = requiredString(id, step, s, key).toUpperCase(Locale.ROOT).replace('-', '_');
      try { return Enum.valueOf(type, value); }
      catch (IllegalArgumentException invalid) { throw error(id, step, fullPath(s, key), "Unknown value " + value + "; expected " + Arrays.toString(type.getEnumConstants())); }
   }
   private static String fullPath(ConfigurationSection section, String key) {
      String parent = section.getCurrentPath();
      return parent == null || parent.isEmpty() ? key : parent + "." + key;
   }
   private static IllegalArgumentException error(String id, String step, String path, String message) {
      if (step != null && !path.startsWith("steps.")) path = "steps." + step + "." + path;
      return new IllegalArgumentException("Cutscene '" + id + "', step '" + (step == null ? "<configuration>" : step) + "', path '" + path + "': " + message);
   }
}
