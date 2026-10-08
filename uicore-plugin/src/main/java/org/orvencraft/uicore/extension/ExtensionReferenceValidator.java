package org.orvencraft.uicore.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.orvencraft.uicore.api.model.UiElementModel;

/** Validates extension references after dependent plugins have registered them. */
final class ExtensionReferenceValidator {
   private static final Pattern DATA = Pattern.compile("\\{([a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*)\\}");
   private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*");

   record Issue(String path, String kind, String id) {}

   static List<Issue> find(UiElementModel model, BiPredicate<String, String> registered) {
      List<Issue> issues = new ArrayList<>();
      Object action = model.get("action.type");
      check(issues, "action.type", "action", action, registered);
      Object type = model.get("type");
      if (type instanceof String id && ID.matcher(id).matches())
         check(issues, "type", "component", id, registered);
      conditions(model.get("visible-if"), "visible-if", registered, issues);
      data(model.properties(), "", registered, issues);
      return List.copyOf(issues);
   }

   private static void check(List<Issue> issues, String path, String kind, Object raw,
                             BiPredicate<String, String> registered) {
      if (raw instanceof String id && !id.isBlank() && !registered.test(kind, id))
         issues.add(new Issue(path, kind, id));
   }

   private static void conditions(Object node, String path, BiPredicate<String, String> registered, List<Issue> issues) {
      Map<?, ?> map = values(node);
      if (map != null) {
         Object custom = map.get("custom");
         Map<?, ?> customMap = values(custom);
         if (customMap != null) check(issues, path + ".custom.type", "condition", customMap.get("type"), registered);
         for (String key : List.of("all", "any", "not")) {
            if (map.containsKey(key)) conditions(map.get(key), path + "." + key, registered, issues);
         }
      } else if (node instanceof List<?> list) {
         for (int i = 0; i < list.size(); i++) conditions(list.get(i), path + "[" + i + "]", registered, issues);
      }
   }

   private static void data(Object node, String path, BiPredicate<String, String> registered, List<Issue> issues) {
      Map<?, ?> map = values(node);
      if (map != null) {
         for (Map.Entry<?, ?> entry : map.entrySet()) {
            String child = path.isEmpty() ? String.valueOf(entry.getKey()) : path + "." + entry.getKey();
            data(entry.getValue(), child, registered, issues);
         }
      } else if (node instanceof List<?> list) {
         for (int i = 0; i < list.size(); i++) data(list.get(i), path + "[" + i + "]", registered, issues);
      } else if (node instanceof String text) {
         Matcher matcher = DATA.matcher(text);
         while (matcher.find()) check(issues, path, "data", matcher.group(1), registered);
      }
   }

   private static Map<?, ?> values(Object node) {
      if (node instanceof UiElementModel.Section section) return section.values();
      return node instanceof Map<?, ?> map ? map : null;
   }
}
