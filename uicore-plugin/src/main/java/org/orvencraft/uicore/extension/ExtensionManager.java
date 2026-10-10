package org.orvencraft.uicore.extension;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.extension.ActionService;
import org.orvencraft.uicore.api.extension.ConditionService;
import org.orvencraft.uicore.api.extension.ComponentContext;
import org.orvencraft.uicore.api.extension.ComponentService;
import org.orvencraft.uicore.api.extension.DataService;
import org.orvencraft.uicore.api.extension.ExtensionContext;
import org.orvencraft.uicore.api.extension.ExtensionRegistration;
import org.orvencraft.uicore.api.MenuElementSpec;
import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.section.Section;

/** Registrations are scoped to their owning plugin and are invoked on the player's region. */
public final class ExtensionManager implements Listener {
   private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*");
   private final UIcorePlugin plugin;
   private final java.util.function.Predicate<Player> ownsPlayer;
   private final java.util.function.LongSupplier serverTick;
   private final Map<String, Owned<ActionService.Handler>> actions = new ConcurrentHashMap<>();
   private final Map<String, Owned<DataService.Provider>> data = new ConcurrentHashMap<>();
   private final Map<String, Owned<ConditionService.Predicate>> conditions = new ConcurrentHashMap<>();
   private final Map<String, Owned<ComponentService.Factory>> components = new ConcurrentHashMap<>();
   private final Map<String, Long> lastDiagnostic = new ConcurrentHashMap<>();
   private final Map<DataCacheKey, DataCacheValue> dataCache = new ConcurrentHashMap<>();
   private final Map<ComponentCacheKey, ComponentCacheValue> componentCache = new ConcurrentHashMap<>();

   private record Owned<T>(Plugin owner, T handler, long minRefreshTicks) {}
   private record DataCacheKey(UUID playerId, String menuId, String elementId, String id) {}
   private record DataCacheValue(long tick, Object value) {}
   private record ComponentCacheKey(UUID playerId, String menuId, String elementId, String id, Map<String, Object> parameters) {}
   private record ComponentCacheValue(long tick, List<MenuElementSpec> children) {}

   public ExtensionManager(UIcorePlugin plugin) { this(plugin, Bukkit::isOwnedByCurrentRegion, Bukkit::getCurrentTick); }

   ExtensionManager(UIcorePlugin plugin, java.util.function.Predicate<Player> ownsPlayer,
                    java.util.function.LongSupplier serverTick) {
      this.plugin = plugin;
      this.ownsPlayer = ownsPlayer;
      this.serverTick = serverTick;
   }

   public ActionService actions() {
      return new ActionService() {
         public ExtensionRegistration register(Plugin owner, String id, Handler handler) {
            return ExtensionManager.this.register(actions, owner, id, handler);
         }
         public boolean isRegistered(String id) { return active(actions, id); }
      };
   }

   public DataService data() {
      return new DataService() {
         public ExtensionRegistration register(Plugin owner, String id, long minRefreshTicks, Provider provider) {
            if (minRefreshTicks < 1L || minRefreshTicks > 1200L) throw new IllegalArgumentException("minRefreshTicks must be 1..1200");
            return ExtensionManager.this.register(data, owner, id, provider, minRefreshTicks);
         }
         public boolean isRegistered(String id) { return active(data, id); }
      };
   }

   public ConditionService conditions() {
      return new ConditionService() {
         public ExtensionRegistration register(Plugin owner, String id, Predicate predicate) {
            return ExtensionManager.this.register(conditions, owner, id, predicate);
         }
         public boolean isRegistered(String id) { return active(conditions, id); }
      };
   }

   public ComponentService components() {
      return new ComponentService() {
         public ExtensionRegistration register(Plugin owner, String id, Factory factory) {
            return ExtensionManager.this.register(components, owner, id, factory);
         }
         public boolean isRegistered(String id) { return active(components, id); }
      };
   }

   public Optional<List<MenuElementSpec>> createComponent(String id, ComponentContext context) {
      Owned<ComponentService.Factory> entry = this.components.get(id);
      if (entry == null || !entry.owner().isEnabled()) {
         this.missing("component", id, context.menuId(), context.elementId());
         return Optional.empty();
      }
      if (context.player() == null || !Bukkit.isOwnedByCurrentRegion(context.player())) return Optional.empty();
      ComponentCacheKey cacheKey = new ComponentCacheKey(context.player().getUniqueId(), context.menuId(),
         context.elementId(), id, context.parameters());
      long tick = Bukkit.getCurrentTick();
      ComponentCacheValue cached = this.componentCache.get(cacheKey);
      if (cached != null && cached.tick() == tick) return Optional.of(cached.children());
      try {
         List<MenuElementSpec> result = entry.handler().create(context);
         if (result == null || result.size() > 64 || result.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Component must return at most 64 non-null elements");
         List<MenuElementSpec> children = List.copyOf(result);
         if (this.componentCache.size() > 4096) this.componentCache.clear();
         this.componentCache.put(cacheKey, new ComponentCacheValue(tick, children));
         return Optional.of(children);
      } catch (Exception | LinkageError exception) {
         this.failed("component", id, exception);
         return Optional.empty();
      }
   }

   private <T> ExtensionRegistration register(Map<String, Owned<T>> registry, Plugin owner, String id, T handler) {
      return this.register(registry, owner, id, handler, 0L);
   }

   private <T> ExtensionRegistration register(Map<String, Owned<T>> registry, Plugin owner, String id, T handler, long minRefreshTicks) {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(handler, "handler");
      if (!owner.isEnabled()) throw new IllegalStateException("Plugin is disabled: " + owner.getName());
      if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("Expected namespaced id such as quests:accept");
      Owned<T> registration = new Owned<>(owner, handler, minRefreshTicks);
      if (registry.putIfAbsent(id, registration) != null) throw new IllegalArgumentException("Extension id already registered: " + id);
      if ((Object)registry == this.data) this.dataCache.clear();
      if ((Object)registry == this.components) this.componentCache.clear();
      this.refreshOpenMenus();
      return () -> {
         if (registry.remove(id, registration)) {
            if ((Object)registry == this.data) this.dataCache.clear();
            if ((Object)registry == this.components) this.componentCache.clear();
            this.refreshOpenMenus();
         }
      };
   }

   private static <T> boolean active(Map<String, Owned<T>> registry, String id) {
      Owned<T> entry = id == null ? null : registry.get(id);
      return entry != null && entry.owner().isEnabled();
   }

   public boolean executeAction(String id, Player player, String menuId, String elementId, Map<String, Object> args) {
      Owned<ActionService.Handler> entry = this.actions.get(id);
      if (entry == null || !entry.owner().isEnabled()) { this.missing("action", id, menuId, elementId); return false; }
      if (player == null || !Bukkit.isOwnedByCurrentRegion(player)) return false;
      try {
         ExtensionContext context = new ExtensionContext(player, menuId, elementId, args);
         entry.handler().execute(context);
         return !context.isCancelled();
      } catch (Exception | LinkageError exception) {
         this.failed("action", id, exception);
         return false;
      }
   }

   public Optional<Object> resolveData(String id, Player player, String menuId, String elementId) {
      Owned<DataService.Provider> entry = this.data.get(id);
      if (entry == null || !entry.owner().isEnabled()) { this.missing("data", id, menuId, elementId); return Optional.empty(); }
      if (player == null || !ownsPlayer.test(player)) return Optional.empty();
      DataCacheKey cacheKey = new DataCacheKey(player.getUniqueId(), menuId, elementId, id);
      long tick = serverTick.getAsLong();
      DataCacheValue cached = this.dataCache.get(cacheKey);
      if (cached != null && tick - cached.tick() >= 0L && tick - cached.tick() < entry.minRefreshTicks())
         return Optional.ofNullable(cached.value());
      try {
         Object value = entry.handler().resolve(new ExtensionContext(player, menuId, elementId, Map.of()));
         if (value == null) { cacheData(cacheKey, tick, null); return Optional.empty(); }
         if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) throw new IllegalArgumentException("Unsupported data type");
         if (value instanceof Number number && !Double.isFinite(number.doubleValue())) throw new IllegalArgumentException("Non-finite data value");
         if (String.valueOf(value).length() > 4096) throw new IllegalArgumentException("Data value is too long");
         cacheData(cacheKey, tick, value);
         return Optional.of(value);
      } catch (Exception | LinkageError exception) {
         cacheData(cacheKey, tick, null);
         this.failed("data", id, exception);
         return Optional.empty();
      }
   }

   private void cacheData(DataCacheKey key, long tick, Object value) {
      if (this.dataCache.size() >= 4096) this.dataCache.clear();
      this.dataCache.put(key, new DataCacheValue(tick, value));
   }

   public Optional<Boolean> evaluateCondition(String id, Player player, String menuId, String elementId, Map<String, Object> args) {
      Owned<ConditionService.Predicate> entry = this.conditions.get(id);
      if (entry == null || !entry.owner().isEnabled()) { this.missing("condition", id, menuId, elementId); return Optional.empty(); }
      if (player == null || !Bukkit.isOwnedByCurrentRegion(player)) return Optional.empty();
      try {
         return Optional.of(entry.handler().test(new ExtensionContext(player, menuId, elementId, args)));
      } catch (Exception | LinkageError exception) {
         this.failed("condition", id, exception);
         return Optional.empty();
      }
   }

   private void missing(String kind, String id, String menuId, String elementId) {
      String key = kind + ":" + id + ":" + menuId + ":" + elementId;
      if (this.shouldLog(key)) this.plugin.getLogger().warning("Unknown " + kind + " '" + id + "' at menu " + menuId + ", element " + elementId);
   }

   private void failed(String kind, String id, Throwable exception) {
      if (this.plugin != null && this.shouldLog(kind + ":" + id + ":failure"))
         this.plugin.getLogger().warning("Extension " + kind + " '" + id + "' failed (" + exception.getClass().getSimpleName() + ")");
   }

   private boolean shouldLog(String key) {
      long now = System.currentTimeMillis();
      Long previous = this.lastDiagnostic.putIfAbsent(key, now);
      if (previous == null) return true;
      if (now - previous < 30_000L || !this.lastDiagnostic.replace(key, previous, now)) return false;
      if (this.lastDiagnostic.size() > 512) this.lastDiagnostic.clear();
      return true;
   }

   @EventHandler
   public void onPluginDisable(PluginDisableEvent event) {
      this.removeOwner(event.getPlugin());
   }

   public void removeOwner(Plugin owner) {
      boolean removed = this.actions.values().removeIf(entry -> entry.owner() == owner);
      removed |= this.data.values().removeIf(entry -> entry.owner() == owner);
      removed |= this.conditions.values().removeIf(entry -> entry.owner() == owner);
      removed |= this.components.values().removeIf(entry -> entry.owner() == owner);
      if (removed) {
         this.dataCache.clear();
         this.componentCache.clear();
         this.refreshOpenMenus();
      }
   }

   public void invalidateData(Player player) {
      if (player != null) this.dataCache.keySet().removeIf(key -> key.playerId().equals(player.getUniqueId()));
   }

   public void invalidateComponents(Player player) {
      if (player != null) this.componentCache.keySet().removeIf(key -> key.playerId().equals(player.getUniqueId()));
   }

   public void validateMenuReferences(Map<String, Section> menus) {
      int diagnostics = 0;
      for (Map.Entry<String, Section> menu : menus.entrySet()) {
         Section.Definition definition = menu.getValue().definition();
         for (Map.Entry<String, MenuLayout> element : definition.layouts().entrySet()) {
            diagnostics += this.validateElement(menu.getKey(), element.getKey(), element.getValue());
            if (diagnostics >= 200) return;
         }
         for (Map.Entry<String, MenuLayout> template : definition.templates().entrySet()) {
            diagnostics += this.validateElement(menu.getKey(), "template/" + template.getKey(), template.getValue());
            if (diagnostics >= 200) return;
         }
      }
   }

   private int validateElement(String menuId, String elementId, MenuLayout layout) {
      int found = 0;
      for (ExtensionReferenceValidator.Issue issue : ExtensionReferenceValidator.find(layout.sourceModel(), this::isRegistered)) {
         found++;
         String key = "config:" + menuId + ":" + elementId + ":" + issue.path() + ":" + issue.id();
         if (this.shouldLog(key)) this.plugin.getLogger().warning("Extension reference is unavailable: menu=" + menuId
            + ", element=" + elementId + ", path=" + issue.path() + ", type=" + issue.kind() + ", id=" + issue.id());
      }
      return found;
   }

   private boolean isRegistered(String kind, String id) {
      return switch (kind) {
         case "action" -> active(this.actions, id);
         case "data" -> active(this.data, id);
         case "condition" -> active(this.conditions, id);
         case "component" -> active(this.components, id);
         default -> false;
      };
   }

   private void refreshOpenMenus() {
      if (this.plugin == null || !this.plugin.isEnabled() || Bukkit.getServer() == null) return;
      try {
         Bukkit.getGlobalRegionScheduler().run(this.plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) this.plugin.refreshCurrentMenu(player);
         });
      } catch (IllegalStateException ignored) {
         // Shutdown may already have cancelled UIcore's scheduler.
      }
   }

   public void clear() {
      this.actions.clear();
      this.data.clear();
      this.conditions.clear();
      this.components.clear();
      this.lastDiagnostic.clear();
      this.dataCache.clear();
      this.componentCache.clear();
   }
}
