package org.orvencraft.uicore.layout;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.orvencraft.uicore.position.DynamicPositionSpec.Position;

public final class ElementHierarchy {
   private ElementHierarchy() {}
   public static void validate(Map<String, MenuLayout> layouts) {
      validateMutation(Map.of(), layouts);
   }
   /** Runtime removal may leave a hidden subtree; unrelated edits must not introduce new dangling links. */
   public static void validateMutation(Map<String, MenuLayout> previous, Map<String, MenuLayout> layouts) {
      for (MenuLayout layout : layouts.values()) {
         Set<String> seen = new HashSet<>();
         MenuLayout current = layout;
         while (current != null) {
            if (!seen.add(current.key)) throw new IllegalArgumentException("Parent cycle at " + layout.key);
            String parent = current.interaction().parent();
            if (parent.isEmpty()) break;
            MenuLayout next = layouts.get(parent);
            if (next == null) {
               MenuLayout old = previous.get(current.key);
               if (old != null && old.interaction().parent().equals(parent) && !previous.containsKey(parent)) break;
               throw new IllegalArgumentException("Missing parent '" + parent + "' for " + current.key);
            }
            if (next.isWorldAnchored() != current.isWorldAnchored()) throw new IllegalArgumentException("Parent and child must use the same anchor: " + current.key);
            current = next;
         }
         String target = layout.interaction().target();
         if (!target.isEmpty() && !seen.contains(target)
            && !(previous.containsKey(layout.key) && previous.get(layout.key).interaction().target().equals(target)
               && !hasAncestors(layout.key, layouts)))
            throw new IllegalArgumentException("drag.target must be self or an ancestor: " + layout.key);
         for (String id : layout.interaction().dropTargets())
            if (!layouts.containsKey(id) && !(previous.containsKey(layout.key)
               && previous.get(layout.key).interaction().dropTargets().contains(id) && !previous.containsKey(id)))
               throw new IllegalArgumentException("Missing drop target '" + id + "' for " + layout.key);
      }
   }
   public static boolean hasAncestors(String id, Map<String, MenuLayout> layouts) {
      Set<String> seen = new HashSet<>();
      while (!id.isEmpty()) {
         MenuLayout layout = layouts.get(id);
         if (layout == null || !seen.add(id)) return false;
         id = layout.interaction().parent();
      }
      return true;
   }
   public static Position position(MenuLayout layout, Map<String, MenuLayout> layouts, Function<MenuLayout, Position> local) {
      Position own = local.apply(layout);
      double x = own.x(), y = own.y(), z = own.z();
      Set<String> seen = new HashSet<>();
      seen.add(layout.key);
      String parent = layout.interaction().parent();
      while (!parent.isEmpty()) {
         MenuLayout next = layouts.get(parent);
         if (next == null || !seen.add(parent)) throw new IllegalArgumentException("Invalid parent chain at " + layout.key);
         Position p = local.apply(next);
         x += p.x(); y += p.y(); z += p.z();
         parent = next.interaction().parent();
      }
      return new Position(x, y, z);
   }
}
