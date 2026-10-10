package org.orvencraft.uicore.api;

import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.section.MenuConfigLoader;
import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.api.extension.ActionService;
import org.orvencraft.uicore.api.extension.DataService;
import org.orvencraft.uicore.api.extension.ConditionService;
import org.orvencraft.uicore.api.extension.ComponentService;
import org.orvencraft.uicore.api.state.MenuState;
import org.orvencraft.uicore.api.state.StateKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class UIcoreApiImpl implements UIcoreApi {
   private final UIcorePlugin plugin;

   public UIcoreApiImpl(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   @Override public MenuService menus() {
      return new MenuService() {
         public boolean open(Player player, String menuId) { return UIcoreApiImpl.this.openMenu(player, menuId); }
         public boolean close(Player player) { return UIcoreApiImpl.this.closeMenu(player); }
         public Optional<String> current(Player player) { return UIcoreApiImpl.this.getOpenMenu(player); }
         public boolean exists(String menuId) { return UIcoreApiImpl.this.hasMenu(menuId); }
         public Set<String> ids() { return UIcoreApiImpl.this.getMenuIds(); }
      };
   }

   @Override public ElementService elements() {
      return new ElementService() {
         public List<String> ids(String menuId) { return UIcoreApiImpl.this.getElementIds(menuId); }
         public Optional<MenuElementSnapshot> get(String menuId, String elementId) { return UIcoreApiImpl.this.getElement(menuId, elementId); }
         public Optional<MenuElementSnapshot> get(Player player, String menuId, String elementId) { return UIcoreApiImpl.this.getElement(player, menuId, elementId); }
         public MenuMutationResult add(String menuId, MenuElementSpec element) { return UIcoreApiImpl.this.addElement(menuId, element); }
         public MenuMutationResult add(Player player, String menuId, MenuElementSpec element) { return UIcoreApiImpl.this.addElement(player, menuId, element); }
         public MenuMutationResult remove(String menuId, String elementId) { return UIcoreApiImpl.this.removeElement(menuId, elementId); }
         public MenuMutationResult remove(Player player, String menuId, String elementId) { return UIcoreApiImpl.this.removeElement(player, menuId, elementId); }
         public MenuMutationResult edit(String menuId, String elementId, Consumer<MenuElementEditor> editor) { return UIcoreApiImpl.this.editElement(menuId, elementId, editor); }
         public MenuMutationResult edit(Player player, String menuId, String elementId, Consumer<MenuElementEditor> editor) { return UIcoreApiImpl.this.editElement(player, menuId, elementId, editor); }
      };
   }

   @Override public InputService inputs() {
      return new InputService() {
         public Optional<String> get(Player player, String field) { return UIcoreApiImpl.this.getInput(player, field); }
         public Map<String, String> all(Player player) { return UIcoreApiImpl.this.getInputs(player); }
         public void set(Player player, String field, String value) { UIcoreApiImpl.this.setInput(player, field, value); }
         public void clear(Player player, String field) { UIcoreApiImpl.this.clearInput(player, field); }
         public void clear(Player player) { UIcoreApiImpl.this.clearInputs(player); }
         public Optional<String> focused(Player player) { return UIcoreApiImpl.this.getFocusedInputField(player); }
         public void focus(Player player, String field) { UIcoreApiImpl.this.focusInputField(player, field); }
         public void clearFocus(Player player) { UIcoreApiImpl.this.clearFocusedInputField(player); }
      };
   }

   @Override public SlotService slots() {
      return new SlotService() {
         public Set<String> ids(String menuId) { return UIcoreApiImpl.this.getSlotIds(menuId); }
         public Set<String> templateIds(String menuId) { return UIcoreApiImpl.this.getTemplateIds(menuId); }
         public Optional<String> get(String menuId, String slotId) { return UIcoreApiImpl.this.getSlot(menuId, slotId); }
         public Optional<String> get(Player player, String menuId, String slotId) { return UIcoreApiImpl.this.getSlot(player, menuId, slotId); }
         public MenuMutationResult set(String menuId, String slotId, String templateId) { return UIcoreApiImpl.this.setSlot(menuId, slotId, templateId); }
         public MenuMutationResult set(Player player, String menuId, String slotId, String templateId) { return UIcoreApiImpl.this.setSlot(player, menuId, slotId, templateId); }
         public MenuMutationResult clear(String menuId, String slotId) { return UIcoreApiImpl.this.clearSlot(menuId, slotId); }
         public MenuMutationResult clear(Player player, String menuId, String slotId) { return UIcoreApiImpl.this.clearSlot(player, menuId, slotId); }
         public MenuMutationResult reset(String menuId, String slotId) { return UIcoreApiImpl.this.resetSlot(menuId, slotId); }
         public MenuMutationResult reset(Player player, String menuId, String slotId) { return UIcoreApiImpl.this.resetSlot(player, menuId, slotId); }
      };
   }

   @Override public ActionService actions() { return this.plugin.getExtensionManager().actions(); }
   @Override public DataService data() { return this.plugin.getExtensionManager().data(); }
   @Override public ConditionService conditions() { return this.plugin.getExtensionManager().conditions(); }
   @Override public ComponentService components() { return this.plugin.getExtensionManager().components(); }

   @Override public MenuState state(Player player, String menuId) {
      return new MenuState() {
         public <T> T get(StateKey<T> key) {
            String raw = UIcoreApiImpl.this.getState(player, menuId, key.name()).orElse(null);
            if (raw == null) return key.defaultValue();
            try { return key.parse(raw); }
            catch (IllegalArgumentException exception) { return key.defaultValue(); }
         }
         public <T> void set(StateKey<T> key, T value) {
            UIcoreApiImpl.this.setState(player, menuId, key.name(), key.format(value));
         }
         public void clear(StateKey<?> key) {
            UIcoreApiImpl.this.clearState(player, menuId, key.name());
         }
      };
   }

   @Override public String apiVersion() { return "1.2"; }

   @Override public Set<String> capabilities() {
      Set<String> capabilities = new LinkedHashSet<>(UIcoreApi.super.capabilities());
      capabilities.addAll(Set.of("extension-actions", "extension-data", "extension-conditions",
         "extension-components", "typed-state", "service-groups"));
      return Set.copyOf(capabilities);
   }

   public boolean openMenu(Player player, String menuId) {
      if (player == null || !player.isOnline() || !this.hasMenu(menuId)) {
         return false;
      }

      return this.runPlayerAction(player, () -> this.plugin.setupCursor(player, menuId));
   }

   public boolean closeMenu(Player player) {
      return this.closeMenu(player, true);
   }

   public boolean closeMenu(Player player, boolean restoreLocation) {
      if (player == null || !player.isOnline()) {
         return false;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.runPlayerAction(player, () -> {
            if (this.plugin.getCurrentPlayerMenu(player) != null) {
               this.plugin.stopCursor(player, restoreLocation);
            }
         });
      }
      if (!this.isMenuOpen(player)) {
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

      Section.Definition definition = section.definition();
      LinkedHashSet<String> ids = new LinkedHashSet<>(definition.layouts().keySet());
      ids.addAll(definition.slots().keySet());
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
      this.runPlayerAction(player, () -> this.plugin.submitMenuInput(player, field, value));
   }

   public void clearInput(Player player, String field) {
      this.runPlayerAction(player, () -> this.plugin.clearPlayerInputData(player, field));
   }

   public void clearInputs(Player player) {
      this.runPlayerAction(player, () -> this.plugin.clearPlayerInputData(player));
   }

   public Optional<String> getFocusedInputField(Player player) {
      return Optional.ofNullable(this.plugin.getCurrentPlayerInputField(player));
   }

   public void focusInputField(Player player, String field) {
      this.runPlayerAction(player, () -> this.plugin.setCurrentPlayerInputField(player, field));
   }

   public void clearFocusedInputField(Player player) {
      this.runPlayerAction(player, () -> this.plugin.setCurrentPlayerInputField(player, ""));
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
      this.runPlayerAction(player, () -> {
         this.plugin.setPlayerMenuState(player, menuId, key, value);
         this.refreshPlayerMenu(player, menuId);
      });
   }

   public void clearState(Player player, String menuId, String key) {
      this.runPlayerAction(player, () -> {
         this.plugin.clearPlayerMenuState(player, menuId, key);
         this.refreshPlayerMenu(player, menuId);
      });
   }

   public void clearState(Player player, String menuId) {
      this.runPlayerAction(player, () -> {
         this.plugin.clearPlayerMenuState(player, menuId);
         this.refreshPlayerMenu(player, menuId);
      });
   }

   public Set<String> getSlotIds(String menuId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      return section == null ? Set.of() : new LinkedHashSet<>(section.definition().slots().keySet());
   }

   public Set<String> getTemplateIds(String menuId) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      return section == null ? Set.of() : new LinkedHashSet<>(section.definition().templates().keySet());
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
      int refreshed = this.replaceMenuElement(menuId, slotId);
      return MenuMutationResult.success("Global slot set: " + menuId + ":" + slotId + " -> " + templateId, refreshed);
   }

   public MenuMutationResult setSlot(Player player, String menuId, String slotId, String templateId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.queuePlayerMutation(player, () -> this.setSlot(player, menuId, slotId, templateId));
      }
      MenuMutationResult validation = this.validateSlotAndTemplate(menuId, slotId, templateId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setPlayerMenuSlot(player, menuId, slotId, templateId);
      int refreshed = this.replacePlayerMenuElement(player, menuId, slotId);
      return MenuMutationResult.success("Player slot set: " + player.getName() + " " + menuId + ":" + slotId + " -> " + templateId, refreshed);
   }

   public MenuMutationResult clearSlot(String menuId, String slotId) {
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setGlobalMenuSlot(menuId, slotId, "");
      int refreshed = this.replaceMenuElement(menuId, slotId);
      return MenuMutationResult.success("Global slot cleared: " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult clearSlot(Player player, String menuId, String slotId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.queuePlayerMutation(player, () -> this.clearSlot(player, menuId, slotId));
      }
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.setPlayerMenuSlot(player, menuId, slotId, "");
      int refreshed = this.replacePlayerMenuElement(player, menuId, slotId);
      return MenuMutationResult.success("Player slot cleared: " + player.getName() + " " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult resetSlot(String menuId, String slotId) {
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.resetGlobalMenuSlot(menuId, slotId);
      int refreshed = this.replaceMenuElement(menuId, slotId);
      return MenuMutationResult.success("Global slot reset: " + menuId + ":" + slotId, refreshed);
   }

   public MenuMutationResult resetSlot(Player player, String menuId, String slotId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.queuePlayerMutation(player, () -> this.resetSlot(player, menuId, slotId));
      }
      MenuMutationResult validation = this.validateSlot(menuId, slotId);
      if (validation != null) {
         return validation;
      }

      this.plugin.resetPlayerMenuSlot(player, menuId, slotId);
      int refreshed = this.replacePlayerMenuElement(player, menuId, slotId);
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

      MenuLayout layout = MenuConfigLoader.loadLayout(menuId, element.id(), element.model(), section.world);
      if (!section.addIfAbsent(element.id(), layout)) {
         return MenuMutationResult.failure("Element already exists: " + menuId + ":" + element.id());
      }
      int refreshed = this.refreshMenuElement(menuId, element.id());
      return MenuMutationResult.success("Element added: " + menuId + ":" + element.id(), refreshed);
   }

   public MenuMutationResult addElement(Player player, String menuId, MenuElementSpec element) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.queuePlayerMutation(player, () -> this.addElement(player, menuId, element));
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

      MenuLayout layout = MenuConfigLoader.loadLayout(menuId, element.id(), element.model(), section.world);
      this.plugin.putPlayerMenuLayout(player, menuId, element.id(), layout);
      int refreshed = this.refreshPlayerMenuElement(player, menuId, element.id());
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

      int refreshed = this.replaceMenuElement(menuId, elementId);
      return MenuMutationResult.success("Element removed: " + menuId + ":" + elementId, refreshed);
   }

   public MenuMutationResult removeElement(Player player, String menuId, String elementId) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.queuePlayerMutation(player, () -> this.removeElement(player, menuId, elementId));
      }
      if (UIcorePlugin.sectionManager.get(menuId) == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }
      if (this.plugin.getLayoutForPlayer(player, menuId, elementId) == null) {
         return MenuMutationResult.failure("Element not found for player: " + player.getName() + " " + menuId + ":" + elementId);
      }

      this.plugin.hidePlayerMenuLayout(player, menuId, elementId);
      int refreshed = this.replacePlayerMenuElement(player, menuId, elementId);
      return MenuMutationResult.success("Player element removed: " + player.getName() + " " + menuId + ":" + elementId, refreshed);
   }

   public MenuMutationResult editElement(String menuId, String elementId, Consumer<MenuElementEditor> editor) {
      Section section = UIcorePlugin.sectionManager.get(menuId);
      if (section == null) {
         return MenuMutationResult.failure("Menu not found: " + menuId);
      }

      if (editor == null) {
         return MenuMutationResult.failure("Editor is null");
      }

      boolean changed = section.edit(elementId, existing -> {
         MenuElementEditor mutable = new MenuElementEditor(existing.copySourceConfig());
         editor.accept(mutable);
         org.orvencraft.uicore.api.model.UiElementModel updated = mutable.model();
         if (!updated.contains("name")) updated = updated.with("name", existing.name);
         return MenuConfigLoader.loadLayout(menuId, elementId, updated, section.world);
      });
      if (!changed) return MenuMutationResult.failure("Element not found: " + menuId + ":" + elementId);
      int refreshed = this.replaceMenuElement(menuId, elementId);
      return MenuMutationResult.success("Element edited: " + menuId + ":" + elementId, refreshed);
   }

   public MenuMutationResult editElement(Player player, String menuId, String elementId, Consumer<MenuElementEditor> editor) {
      if (!this.isValidPlayer(player)) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.queuePlayerMutation(player, () -> this.editElement(player, menuId, elementId, editor));
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
      org.orvencraft.uicore.api.model.UiElementModel updated = mutable.model();
      if (!updated.contains("name")) updated = updated.with("name", existing.name);

      MenuLayout replacement = MenuConfigLoader.loadLayout(menuId, elementId, updated, section.world);
      this.plugin.putPlayerMenuLayout(player, menuId, elementId, replacement);
      int refreshed = this.replacePlayerMenuElement(player, menuId, elementId);
      return MenuMutationResult.success("Player element edited: " + player.getName() + " " + menuId + ":" + elementId, refreshed);
   }

   public void clearPlayerElements(Player player, String menuId) {
      this.runPlayerAction(player, () -> {
         this.plugin.clearPlayerMenuLayouts(player, menuId);
         this.refreshPlayerMenu(player, menuId);
      });
   }

   public void clearPlayerElements(Player player) {
      this.runPlayerAction(player, () -> {
         this.plugin.clearPlayerMenuLayouts(player, null);
         this.plugin.refreshCurrentMenu(player);
      });
   }

   public void refresh(Player player) {
      this.runPlayerAction(player, () -> {
         this.plugin.getExtensionManager().invalidateData(player);
         this.plugin.refreshCurrentMenu(player);
      });
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
      if (!this.isValidPlayer(player) || menuId == null) {
         return 0;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.runPlayerAction(player, () -> this.refreshPlayerMenu(player, menuId)) ? 1 : 0;
      }
      if (menuId.equals(this.plugin.getCurrentPlayerMenu(player))) {
         this.plugin.refreshCurrentMenu(player);
         return 1;
      }

      return 0;
   }

   private int refreshPlayerMenuElement(Player player, String menuId, String elementId) {
      if (!this.isValidPlayer(player) || menuId == null) {
         return 0;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.runPlayerAction(player, () -> this.refreshPlayerMenuElement(player, menuId, elementId)) ? 1 : 0;
      }
      if (menuId.equals(this.plugin.getCurrentPlayerMenu(player)) && this.plugin.refreshMenuElement(player, menuId, elementId)) {
         return 1;
      }
      return 0;
   }

   private int replacePlayerMenuElement(Player player, String menuId, String elementId) {
      if (!this.isValidPlayer(player) || menuId == null) {
         return 0;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         return this.runPlayerAction(player, () -> this.replacePlayerMenuElement(player, menuId, elementId)) ? 1 : 0;
      }
      if (menuId.equals(this.plugin.getCurrentPlayerMenu(player)) && this.plugin.replaceMenuElement(player, menuId, elementId)) {
         return 1;
      }
      return 0;
   }

   private int refreshMenuElement(String menuId, String elementId) {
      int refreshed = 0;
      for (Player player : Bukkit.getOnlinePlayers()) {
         refreshed += this.refreshPlayerMenuElement(player, menuId, elementId);
      }
      return refreshed;
   }

   private int replaceMenuElement(String menuId, String elementId) {
      int refreshed = 0;
      for (Player player : Bukkit.getOnlinePlayers()) {
         refreshed += this.replacePlayerMenuElement(player, menuId, elementId);
      }
      return refreshed;
   }

   /**
    * UIcoreApi is safe to call from a global, async or foreign region thread.
    * Player-affecting mutations are queued to the player's entity scheduler;
    * a successful boolean/result means the work was accepted when it had to be
    * queued, while refresh counts include scheduled players.
    */
   private boolean runPlayerAction(Player player, Runnable action) {
      if (!this.isValidPlayer(player)) {
         return false;
      }
      if (Bukkit.isOwnedByCurrentRegion(player)) {
         action.run();
         return true;
      }
      return player.getScheduler().execute(this.plugin, action, null, 0L);
   }

   private MenuMutationResult queuePlayerMutation(Player player, Runnable action) {
      return this.runPlayerAction(player, action)
         ? MenuMutationResult.success("Player menu mutation scheduled: " + player.getUniqueId(), 0)
         : MenuMutationResult.failure("Player is not available for menu mutation");
   }
}
