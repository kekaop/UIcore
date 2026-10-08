package org.orvencraft.uicore.api;

import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;

/** Slot and template operations for menus. */
public interface SlotService {
   Set<String> ids(String menuId);
   Set<String> templateIds(String menuId);
   Optional<String> get(String menuId, String slotId);
   Optional<String> get(Player player, String menuId, String slotId);
   MenuMutationResult set(String menuId, String slotId, String templateId);
   MenuMutationResult set(Player player, String menuId, String slotId, String templateId);
   MenuMutationResult clear(String menuId, String slotId);
   MenuMutationResult clear(Player player, String menuId, String slotId);
   MenuMutationResult reset(String menuId, String slotId);
   MenuMutationResult reset(Player player, String menuId, String slotId);
}
