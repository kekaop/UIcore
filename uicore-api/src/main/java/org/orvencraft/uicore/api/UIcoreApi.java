package org.orvencraft.uicore.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.orvencraft.uicore.api.extension.ActionService;
import org.orvencraft.uicore.api.extension.DataService;
import org.orvencraft.uicore.api.extension.ConditionService;
import org.orvencraft.uicore.api.state.MenuState;
import org.bukkit.entity.Player;

public interface UIcoreApi {
   default MenuService menus() { throw new UnsupportedOperationException("Menu service is unavailable"); }

   default ElementService elements() { throw new UnsupportedOperationException("Element service is unavailable"); }

   default InputService inputs() { throw new UnsupportedOperationException("Input service is unavailable"); }

   default SlotService slots() { throw new UnsupportedOperationException("Slot service is unavailable"); }

   default ActionService actions() { throw new UnsupportedOperationException("Action extensions are unavailable"); }

   default DataService data() { throw new UnsupportedOperationException("Data extensions are unavailable"); }

   default ConditionService conditions() { throw new UnsupportedOperationException("Condition extensions are unavailable"); }

   default org.orvencraft.uicore.api.extension.ComponentService components() {
      throw new UnsupportedOperationException("Component extensions are unavailable");
   }

   default MenuState state(Player player, String menuId) { throw new UnsupportedOperationException("Typed state is unavailable"); }

   default String apiVersion() {
      return "1.1";
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
         "item-display",
         "slot-templates"
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

   Set<String> getSlotIds(String menuId);

   Set<String> getTemplateIds(String menuId);

   Optional<String> getSlot(String menuId, String slotId);

   Optional<String> getSlot(Player player, String menuId, String slotId);

   MenuMutationResult setSlot(String menuId, String slotId, String templateId);

   MenuMutationResult setSlot(Player player, String menuId, String slotId, String templateId);

   MenuMutationResult clearSlot(String menuId, String slotId);

   MenuMutationResult clearSlot(Player player, String menuId, String slotId);

   MenuMutationResult resetSlot(String menuId, String slotId);

   MenuMutationResult resetSlot(Player player, String menuId, String slotId);

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
