package org.orvencraft.uicore.api;

import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;

/** Menu operations exposed without expanding the legacy UIcoreApi interface further. */
public interface MenuService {
   boolean open(Player player, String menuId);
   boolean close(Player player);
   Optional<String> current(Player player);
   boolean exists(String menuId);
   Set<String> ids();
}
