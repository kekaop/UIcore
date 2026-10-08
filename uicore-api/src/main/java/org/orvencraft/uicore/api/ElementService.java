package org.orvencraft.uicore.api;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

/** Element mutations use the same runtime model as YAML layouts. */
public interface ElementService {
   List<String> ids(String menuId);
   Optional<MenuElementSnapshot> get(String menuId, String elementId);
   Optional<MenuElementSnapshot> get(Player player, String menuId, String elementId);
   MenuMutationResult add(String menuId, MenuElementSpec element);
   MenuMutationResult add(Player player, String menuId, MenuElementSpec element);
   MenuMutationResult remove(String menuId, String elementId);
   MenuMutationResult remove(Player player, String menuId, String elementId);
   MenuMutationResult edit(String menuId, String elementId, Consumer<MenuElementEditor> editor);
   MenuMutationResult edit(Player player, String menuId, String elementId, Consumer<MenuElementEditor> editor);
}
