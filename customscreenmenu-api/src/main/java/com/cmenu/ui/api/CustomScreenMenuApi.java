package com.cmenu.ui.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public interface CustomScreenMenuApi {
   default String apiVersion() {
      return "1.0";
   }

   default Set<String> capabilities() {
      return Set.of(
         "open-close",
         "global-elements",
         "player-elements",
         "input-fields",
         "player-menu-state",
         "refresh",
         "events",
         "hover-styles",
         "pressed-styles",
         "permission-styles",
         "visible-if",
         "toggle-content",
         "item-display"
      );
   }

   boolean openMenu(Player player, String menuId);

   boolean closeMenu(Player player);

   boolean closeMenu(Player player, boolean restoreLocation);

   boolean isMenuOpen(Player player);

   Optional<String> getOpenMenu(Player player);

   boolean hasMenu(String menuId);

   Set<String> getMenuIds();

   List<String> getElementIds(String menuId);

   List<String> getElementIds(Player player, String menuId);

   Optional<MenuElementSnapshot> getElement(String menuId, String elementId);

   Optional<MenuElementSnapshot> getElement(Player player, String menuId, String elementId);

   Optional<String> getInput(Player player, String field);

   Map<String, String> getInputs(Player player);

   void setInput(Player player, String field, String value);

   void clearInput(Player player, String field);

   void clearInputs(Player player);

   Optional<String> getFocusedInputField(Player player);

   void focusInputField(Player player, String field);

   void clearFocusedInputField(Player player);

   Optional<String> getState(Player player, String menuId, String key);

   Map<String, String> getState(Player player, String menuId);

   void setState(Player player, String menuId, String key, String value);

   void clearState(Player player, String menuId, String key);

   void clearState(Player player, String menuId);

   MenuMutationResult addElement(String menuId, MenuElementSpec element);

   MenuMutationResult addElement(Player player, String menuId, MenuElementSpec element);

   MenuMutationResult removeElement(String menuId, String elementId);

   MenuMutationResult removeElement(Player player, String menuId, String elementId);

   MenuMutationResult editElement(String menuId, String elementId, Consumer<MenuElementEditor> editor);

   MenuMutationResult editElement(Player player, String menuId, String elementId, Consumer<MenuElementEditor> editor);

   void clearPlayerElements(Player player, String menuId);

   void clearPlayerElements(Player player);

   void refresh(Player player);

   int refreshMenu(String menuId);
}
