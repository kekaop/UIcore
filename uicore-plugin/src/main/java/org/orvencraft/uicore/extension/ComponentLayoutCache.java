package org.orvencraft.uicore.extension;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.orvencraft.uicore.api.model.UiElementModel;
import org.orvencraft.uicore.layout.MenuLayout;

/** Keeps component children stable across menu reads and refreshes. */
public final class ComponentLayoutCache {
   private record Key(UUID playerId, String menuId, String elementId) {}
   private record Entry(String world, UiElementModel model, MenuLayout layout) {}
   private final Map<Key, Entry> entries = new ConcurrentHashMap<>();

   public MenuLayout getOrCreate(UUID playerId, String menuId, String elementId, String world,
                                 UiElementModel model, Supplier<MenuLayout> factory) {
      Key key = new Key(playerId, menuId, elementId);
      Entry entry = this.entries.compute(key, (ignored, current) -> current != null
         && Objects.equals(current.world(), world) && current.model().equals(model)
         ? current : new Entry(world, model, factory.get()));
      return entry.layout();
   }

   public void retain(UUID playerId, String menuId, Set<String> activeChildIds) {
      this.entries.keySet().removeIf(key -> key.playerId().equals(playerId) && key.menuId().equals(menuId)
         && !activeChildIds.contains(key.elementId()));
   }

   public void clearPlayer(UUID playerId) {
      this.entries.keySet().removeIf(key -> key.playerId().equals(playerId));
   }

   public void clear() { this.entries.clear(); }
}
