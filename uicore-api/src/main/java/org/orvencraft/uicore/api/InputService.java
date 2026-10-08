package org.orvencraft.uicore.api;

import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;

/** Input field values and focus for one player. */
public interface InputService {
   Optional<String> get(Player player, String field);
   Map<String, String> all(Player player);
   void set(Player player, String field, String value);
   void clear(Player player, String field);
   void clear(Player player);
   Optional<String> focused(Player player);
   void focus(Player player, String field);
   void clearFocus(Player player);
}
