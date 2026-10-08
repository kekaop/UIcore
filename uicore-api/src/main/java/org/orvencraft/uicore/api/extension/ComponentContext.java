package org.orvencraft.uicore.api.extension;

import java.util.Map;
import org.bukkit.entity.Player;

/** Player-specific parameters for a component instance. */
public record ComponentContext(Player player, String menuId, String elementId, Map<String, Object> parameters) {
   public ComponentContext {
      parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
   }

   public String parameter(String key) {
      Object value = parameters.get(key);
      return value == null ? "" : String.valueOf(value);
   }
}
