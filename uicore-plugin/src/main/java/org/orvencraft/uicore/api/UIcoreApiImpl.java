package org.orvencraft.uicore.api;

import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.section.MenuConfigLoader;
import org.orvencraft.uicore.section.Section;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class UIcoreApiImpl implements UIcoreApi {
   private final UIcorePlugin plugin;

   public UIcoreApiImpl(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   public boolean openMenu(Player player, String menuId) {
      if (player == null || !player.isOnline() || !this.hasMenu(menuId)) {
         return false;
      }

      this.plugin.setupCursor(player, menuId);
      return true;
   }

   public boolean closeMenu(Player player) {
      return this.closeMenu(player, true);
   }

   public boolean closeMenu(Player player, boolean restoreLocation) {
      if (player == null || !player.isOnline() || !this.isMenuOpen(player)) {
         return false;
      }

      this.plugin.stopCursor(player, restoreLocation);
      return true;
   }

   public boolean isMenuOpen(Player player) {
      return player != null && this.plugin.getCurrentPlayerMenu(player) != null;
   }

   public Optional<String> getOpenMenu(Player player) {
      return Optional.ofNullable(player == null ? null : this.plugin.getCurrentPlayerMenu(player));
   }

   public boolean hasMenu(String menuId) {
      return menuId != null && UIcorePlugin.sectionManager.has(menuId);
   }

   public Set<String> getMenuIds() {
      return new LinkedHashSet<>(UIcorePlugin.sectionManager.keySet());
   }

   public List<String> getElementIds(String menuId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return List.of();
      }

      LinkedHashSet<String> ids = new LinkedHashSet<>(section.layouts.keySet());
      ids.addAll(section.slots.keySet());
      return new ArrayList<>(ids);
   }

   public List<String> getElementIds(Player player, String menuId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      return section == null ? List.of() : new ArrayList<>(this.plugin.getLayoutsForPlayer(player, menuId, section).keySet());
   }

   public Optional<MenuElementSnapshot> getElement(String menuId, String elementId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return Optional.empty();
      }

      MenuLayout layout = section.get(elementId);
      if (layout == null && section.hasSlot(elementId)) {
         String templateId = this.plugin.getResolvedMenuSlotTemplate(null, menuId, elementId);
         layout = section.materializeSlot(menuId, elementId, templateId);
      }
      return layout == null ? Optional.empty() : Optional.of(snapshot(menuId, layout));
   }

   public Optional<MenuElementSnapshot> getElement(Player player, String menuId, String elementId) {
      MenuLayout layout = this.plugin.getLayoutForPlayer(player, menuId, elementId);
      return layout == null ? Optional.empty() : Optional.of(snapshot(menuId, layout));
   }

   public Optional<String> getInput(Player player, String field) {
      if (player == null || field == null || field.isBlank()) {
         return Optional.empty();
      }

      String value = this.plugin.getPlayerInputValue(player, field);
      return value.isEmpty() ? Optional.empty() : Optional.of(value);
   }

   public Map<String, String> getInputs(Player player) {
      return this.plugin.getPlayerInputSnapshot(player);
   }

   public void setInput(Player player, String field, String value) {
      this.plugin.setPlayerInputData(player, field, value);
   }

   public void clearInput(Player player, String field) {
      this.plugin.clearPlayerInputData(player, field);
   }

   public void clearInputs(Player player) {
      this.plugin.clearPlayerInputData(player);
   }

   public Optional<String> getFocusedInputField(Player player) {
      return Optional.ofNullable(this.plugin.getCurrentPlayerInputField(player));
   }

   public void focusInputField(Player player, String field) {
      this.plugin.setCurrentPlayerInputField(player, field);
   }

   public void clearFocusedInputField(Player player) {
      this.plugin.setCurrentPlayerInputField(player, "");
   }

   public Optional<String> getState(Player player, String menuId, String key) {
      if (player == null || menuId == null || key == null || menuId.isBlank() || key.isBlank()) {
         return Optional.empty();
      }

      String value = this.plugin.getPlayerMenuState(player, menuId, key);
      return value.isEmpty() ? Optional.empty() : Optional.of(value);
   }

   public Map<String, String> getState(Player player, String menuId) {
      return this.plugin.getPlayerMenuStateSnapshot(player, menuId);
   }

   public void setState(Player player, String menuId, String key, String value) {
      this.plugin.setPlayerMenuState(player, menuId, key, value);
      this.refreshPlayerMenu(player, menuId);
   }

   public void clearState(Player player, String menuId, String key) {
      this.plugin.clearPlayerMenuState(player, menuId, key);
      this.refreshPlayerMenu(player, menuId);
   }

   public void clearState(Player player, String menuId) {
      this.plugin.clearPlayerMenuState(player, menuId);
      this.refreshPlayerMenu(player, menuId);
   }

   public Set<String> getSlotIds(String menuId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      return section == null ? Set.of() : new LinkedHashSet<>(section.slots.keySet());
   }

   public Set<String> getTemplateIds(String menuId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      return section == null ? Set.of() : new LinkedHashSet<>(section.templates.keySet());
   }

   public Optional<String> getSlot(String menuId, String slotId) {
      return optionalSlotValue(this.plugin.getResolvedMenuSlotTemplate(null, menuId, slotId));
   }

   public Optional<String> getSlot(Player player, String menuId, String slotId) {
      if (!this.isValidPlayer(player)) {
         return Optional.empty();
      }
      return optionalSlotValue(this.plugin.getResolvedMenuSlotTemplate(player, menuId, slotId));
   }

   public MenuMutationResult setSlot(String menuId, String slotId, String templateId) {
      MenuMutationResult validation = this.validateSlotAndTemplate(menuId, slotId, templateId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setGlobalMenuSlot(menuId, slotId, templateId);
      int refreshed = this.refreshMenu(menuId);
      return MenuMutationResult.success("Global slot set: " + menuId + ":" + slotId + " -> " + templateId, refreshed);
   }

   public MenuMutationResult setSlot(Player player, String menuId, String slotId, String templateId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      MenuMutationResult validation = this.validateSlotAndTemplate(menuId, slotId, templateId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setPlayerMenuSlot(player, menuId, slotId, templateId);
      int refreshed = this.refreshPlayerMenu(player, menuId);
      return MenuMutationResult.success("Player slot set: " + player.getName() + " " + menuId + ":" + slotId + " -> " + templateId, refreshed);
   }

   public MenuMutationResult clearSlot(String menuId, String slotId) {
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setGlobalMenuSlot(menuId, slotId, "");
      int refreshed = this.refreshMenu(menuId);
      return MenuMutationResult.success("Global slot cleared: " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult clearSlot(Player player, String menuId, String slotId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setPlayerMenuSlot(player, menuId, slotId, "");
      int refreshed = this.refreshPlayerMenu(player, menuId);
      return MenuMutationResult.success("Player slot cleared: " + player.getName() + " " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult resetSlot(String menuId, String slotId) {
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.resetGlobalMenuSlot(menuId, slotId);
      int refreshed = this.refreshMenu(menuId);
      return MenuMutationResult.success("Global slot reset: " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult resetSlot(Player player, String menuId, String slotId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.resetPlayerMenuSlot(player, menuId, slotId);
      int refreshed = this.refreshPlayerMenu(player, menuId);
      return MenuMutationResult.success("Player slot reset: " + player.getName() + " " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult addElement(String menuId, MenuElementSpec element) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }
      if (element == null || element.id() == null || element.id().isBlank()) {
         return MenuMutationResult.failure("Element id is empty");
      }
      if (section.contains(element.id())) {
         return MenuMutationResult.failure("Element already exists: " + menuId + ":" + element.id());
      }

      MenuLayout layout = MenuConfigLoader.loadLayout(menuId, element.id(), element.toConfiguration(), section.world);
      section.add(element.id(), layout);
      int refreshed = this.refreshMenu(menuId);
      return MenuMutationResult.success("Element added: " + menuId + ":" + element.id(), refreshed);
   }

   public MenuMutationResult addElement(Player player, String menuId, MenuElementSpec element) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }
      if (element == null || element.id() == null || element.id().isBlank()) {
         return MenuMutationResult.failure("Element id is empty");
      }
      if (this.plugin.getLayoutForPlayer(player, menuId, element.id()) != null) {
         return MenuMutationResult.failure("Element already exists for player: " + player.getName() + " " + menuId + ":" + element.id());
      }

      MenuLayout layout = MenuConfigLoader.loadLayout(menuId, element.id(), element.toConfiguration(), section.world);
      this.plugin.putPlayerMenuLayout(player, menuId, element.id(), layout);
      int refreshed = this.refreshPlayerMenu(player, menuId);
      return MenuMutationResult.success("Player element added: " + player.getName() + " " + menuId + ":" + element.id(), refreshed);
   }

   public MenuMutationResult removeElement(String menuId, String elementId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }
      if (section.remove(elementId) == null) {
         return MenuMutationResult.failure("Element not found: " + menuId + ":" + elementId);
      }

      int refreshed = this.refreshMenu(menuId);
      return MenuMutationResult.success("Element removed: " + menuId + ":" + elementId, refreshed);
   }

   public MenuMutationResult removeElement(Player player, String menuId, String elementId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (UIcorePlugin.sectionManager.get(menuId) == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }
      if (this.plugin.getLayoutForPlayer(player, menuId, elementId) == null) {
         return MenuMutationResult.failure("Element not found for player: " + player.getName() + " " + menuId + ":" + elementId);
      }

      this.plugin.hidePlayerMenuLayout(player, menuId, elementId);
      int refreshed = this.refreshPlayerMenu(player, menuId);
      return MenuMutationResult.success("Player element removed: " + player.getName() + " " + menuId + ":" + elementId, refreshed);
   }

   public MenuMutationResult editElement(String menuId, String elementId, Consumer<MenuElementEditor> editor) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }

      MenuLayout existing = section.get(elementId);
      if (existing == null) {
         return MenuMutationResult.failure("Element not found: " + menuId + ":" + elementId);
      }
      if (editor == null) {
         return MenuMutationResult.failure("Editor is null");
      }

      MenuElementEditor mutable = new MenuElementEditor(existing.copySourceConfig());
      editor.accept(mutable);
      YamlConfiguration updated = mutable.toConfiguration();
      if (!updated.contains("name")) {
         updated.set("name", existing.name);
      }

      MenuLayout replacement = MenuConfigLoader.loadLayout(menuId, elementId, updated, section.world);
      section.add(elementId, replacement);
      int refreshed = this.refreshMenu(menuId);
      return MenuMutationResult.success("Element edited: " + menuId + ":" + elementId, refreshed);
   }

   public MenuMutationResult editElement(Player player, String menuId, String elementId, Consumer<MenuElementEditor> editor) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }

      MenuLayout existing = this.plugin.getLayoutForPlayer(player, menuId, elementId);
      if (existing == null) {
         return MenuMutationResult.failure("Element not found for player: " + player.getName() + " " + menuId + ":" + elementId);
      }
      if (editor == null) {
         return MenuMutationResult.failure("Editor is null");
      }

      MenuElementEditor mutable = new MenuElementEditor(existing.copySourceConfig());
      editor.accept(mutable);
      YamlConfiguration updated = mutable.toConfiguration();
      if (!updated.contains("name")) {
         updated.set("name", existing.name);
      }

      MenuLayout replacement = MenuConfigLoader.loadLayout(menuId, elementId, updated, section.world);
      this.plugin.putPlayerMenuLayout(player, menuId, elementId, replacement);
      int refreshed = this.refreshPlayerMenu(player, menuId);
      return MenuMutationResult.success("Player element edited: " + player.getName() + " " + menuId + ":" + elementId, refreshed);
   }

   public void clearPlayerElements(Player player, String menuId) {
      this.plugin.clearPlayerMenuLayouts(player, menuId);
      this.refreshPlayerMenu(player, menuId);
   }

   public void clearPlayerElements(Player player) {
      this.plugin.clearPlayerMenuLayouts(player, null);
      this.plugin.refreshCurrentMenu(player);
   }

   public void refresh(Player player) {
      this.plugin.refreshCurrentMenu(player);
   }

   public int refreshMenu(String menuId) {
      int refreshed = 0;
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (menuId != null && menuId.equals(this.plugin.getCurrentPlayerMenu(player))) {
            this.plugin.refreshCurrentMenu(player);
            refreshed++;
         }
      }

      return refreshed;
   }

   private boolean isValidPlayer(Player player) {
      return player != null && player.isOnline();
   }

   private MenuMutationResult validateSlot(String menuId, String slotId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }
      if (slotId == null || slotId.isBlank() || !section.hasSlot(slotId)) {
         return MenuMutationResult.failure("Slot not found: " + menuId + ":" + slotId);
      }
      return null;
   }

   private MenuMutationResult validateSlotAndTemplate(String menuId, String slotId, String templateId) {
      MenuMutationResult slotValidation = this.validateSlot(menuId, slotId);
      if (slotValidation != null) {
         return slotValidation;
      }

      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (templateId == null || templateId.isBlank() || !section.hasTemplate(templateId)) {
         return MenuMutationResult.failure("Template not found: " + menuId + ":" + templateId);
      }
      return null;
   }

   private static Optional<String> optionalSlotValue(String value) {
      return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
   }

   public static MenuElementSnapshot snapshot(String menuId, MenuLayout layout) {
      if (layout == null) {
         return null;
      }

      return new MenuElementSnapshot(
         menuId,
         layout.key,
         layout.getLayoutType(),
         layout.name,
         layout.x,
         layout.y,
         layout.z,
         layout.getTextSize(),
         new LinkedHashMap<>(layout.copySourceConfig().getValues(true))
      );
   }

   private int refreshPlayerMenu(Player player, String menuId) {
      if (this.isValidPlayer(player) && menuId != null && menuId.equals(this.plugin.getCurrentPlayerMenu(player))) {
         this.plugin.refreshCurrentMenu(player);
         return 1;
      }

      return 0;
   }
}
