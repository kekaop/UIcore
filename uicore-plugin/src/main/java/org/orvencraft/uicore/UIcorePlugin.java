package org.orvencraft.uicore;

import org.orvencraft.uicore.camera.CameraEdgePan;
import org.orvencraft.uicore.compat.CraftEngineCompat;
import org.orvencraft.uicore.api.UIcoreApi;
import org.orvencraft.uicore.api.UIcoreApiImpl;
import org.orvencraft.uicore.api.event.MenuClickEvent;
import org.orvencraft.uicore.api.event.MenuCloseEvent;
import org.orvencraft.uicore.api.event.MenuOpenEvent;
import org.orvencraft.uicore.api.event.MenuSwitchEvent;
import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.npc.NPCModule;
import org.orvencraft.uicore.position.DynamicPositionSpec;
import org.orvencraft.uicore.scheduler.ChunkLoaderManager;
import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.section.SectionManager;
import org.orvencraft.uicore.section.MenuSlot;
import org.orvencraft.uicore.util.CommandUtils;
import org.orvencraft.uicore.wasd.WASDModule;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import space.arim.morepaperlib.MorePaperLib;

public class UIcorePlugin extends JavaPlugin {
   public static boolean creatureSpawnLimitEnabled;
   public static int creatureSpawnLimitRadius;
   public static List<String> joinRunCommands = new ArrayList<>();
   public static boolean cameraBlockCheckEnabled;
   public static int cameraBlockCheckRadius;
   public static float exitYaw;
   public static float exitPitch;
   public static List<String> allowedCommands = new ArrayList<>();
   public static TextDisplayManager textDisplayManager;
   public static double cursorZOffset;
   public static double cursorX;
   public static double cursorY;
   public static double cursorSizeX;
   public static double cursorSizeY;
   public static double cursorSizeZ;
   public static ItemDisplayManager itemDisplayManager;
   public static UIcorePlugin plugin;
   public static Map<Player, ArmorStand> playerCursors = new ConcurrentHashMap<>();
   private final Map<Player, List<TextDisplay>> playerDisplays = new ConcurrentHashMap<>();
   private final Map<Player, ItemDisplay> playerItemDisplays = new ConcurrentHashMap<>();
   private final Map<Player, List<ItemDisplay>> playerMenuItemDisplays = new ConcurrentHashMap<>();
   private final Map<Player, ItemDisplay> playerPumpkinDisplays = new ConcurrentHashMap<>();
   private final Map<Player, List<Integer>> playerMenuTextAnimationTasks = new ConcurrentHashMap<>();
   private final Map<Player, List<Integer>> playerMenuItemAnimationTasks = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Boolean>> playerLayoutToggleStates = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Long>> playerLayoutClickTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Long>> playerLayoutCooldownUntil = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Map<String, MenuLayout>>> playerMenuLayoutOverrides = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Set<String>>> playerMenuHiddenLayouts = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Map<String, String>>> playerMenuStates = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Map<String, String>>> playerMenuSlotAssignments = new ConcurrentHashMap<>();
   private final Map<String, Map<String, String>> globalMenuSlotAssignments = new ConcurrentHashMap<>();
   private final Map<Player, Section> playerRuntimeSections = new ConcurrentHashMap<>();
   public static Map<Player, Location> playerLocations = new ConcurrentHashMap<>();
   public static Map<Player, Pig> playerSit = new ConcurrentHashMap<>();
   public static Map<Player, Location> cursorExactLocations = new ConcurrentHashMap<>();
   public static Set<String> playingSound = ConcurrentHashMap.newKeySet();
   private boolean debugMode;
   public static MorePaperLib foliaLib;
   public static SectionManager sectionManager = new SectionManager();
   public static boolean soundLoop;
   public static int soundRate;
   public static String soundName;
   public static float soundVolume;
   public static float soundPitch;
   public static boolean joinRunBool;
   public static String joinRunSection;
   public static String cursorItem;
   public static int cursorModelData;
   private static final Pattern INPUT_PLACEHOLDER_PATTERN = Pattern.compile("%(?:uicore_)?input_([A-Za-z0-9_.-]+)%|\\{(?:menu\\.)?input\\.([A-Za-z0-9_.-]+)\\}");
   private static final Pattern SLOT_STATE_PLACEHOLDER_PATTERN = Pattern.compile("\\{state\\.([A-Za-z0-9_.-]+)\\}");
   public static double maxX;
   public static double maxY;
   public static double cursorScale;
   public static double cursorSensitivity;
   public static double cursorSmoothing;
   public static int runDelay;
   public static boolean hasPAPI;
   public static boolean usePumpkinOverlay;
   public static boolean cursorMovementRangeEnabled;
   public static double cursorMovementRangeXMin;
   public static double cursorMovementRangeXMax;
   public static double cursorMovementRangeYMin;
   public static double cursorMovementRangeYMax;
   public static boolean cursorDefaultPositionEnabled;
   public static double cursorDefaultPositionX;
   public static double cursorDefaultPositionY;
   public static boolean cameraEdgePanEnabled;
   public static double cameraEdgePanThreshold;
   public static double cameraEdgePanMaxOffsetX;
   public static double cameraEdgePanMaxOffsetY;
   public static double cameraEdgePanSmoothing;
   private HoverEnlargeManager hoverEnlargeManager;
   private UIcoreApi menuApi;
   private ChunkLoaderManager chunkLoaderManager;
   private Map<Player, String> currentPlayerMenus = new ConcurrentHashMap<>();
   private final Set<UUID> internalMenuTeleports = ConcurrentHashMap.newKeySet();
   private final Map<UUID, UUID> activeMenuTransitions = new ConcurrentHashMap<>();
   private final Map<UUID, UUID> menuSessionTokens = new ConcurrentHashMap<>();
   private final Map<UUID, CameraEdgePan.Offset> cameraEdgePanTargets = new ConcurrentHashMap<>();
   private final Map<UUID, CameraEdgePan.Offset> cameraEdgePanOffsets = new ConcurrentHashMap<>();
   private final Map<UUID, Long> menuOpenedAtTicks = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, DynamicElementPositionState>> dynamicElementPositions = new ConcurrentHashMap<>();
   private volatile long menuRuntimeTick;
   public Map<Player, MenuLayout> selectedLayouts = new ConcurrentHashMap<>();
   private Map<UUID, String> currentPlayerInputFields = new ConcurrentHashMap<>();
   private Map<UUID, Map<String, String>> userInputData = new ConcurrentHashMap<>();
   private YamlConfiguration langConfig;
   private static final String HELMET_META_KEY = "cursor_original_helmet";
   private static final String MENU_ENTITY_TAG = "uicore_runtime";

   private static final class MenuViewSnapshot {
      private final Pig cameraMount;
      private final ArmorStand cursorStand;
      private final List<TextDisplay> textDisplays;
      private final ItemDisplay cursorDisplay;
      private final List<ItemDisplay> menuItemDisplays;
      private final ItemDisplay pumpkinDisplay;

      private MenuViewSnapshot(
         Pig cameraMount,
         ArmorStand cursorStand,
         List<TextDisplay> textDisplays,
         ItemDisplay cursorDisplay,
         List<ItemDisplay> menuItemDisplays,
         ItemDisplay pumpkinDisplay
      ) {
         this.cameraMount = cameraMount;
         this.cursorStand = cursorStand;
         this.textDisplays = textDisplays;
         this.cursorDisplay = cursorDisplay;
         this.menuItemDisplays = menuItemDisplays;
         this.pumpkinDisplay = pumpkinDisplay;
      }
   }

   private static final class DynamicElementPositionState {
      private final DynamicPositionSpec spec;
      private DynamicPositionSpec.Position current;
      private DynamicPositionSpec.Position target;
      private long lastEvaluationTick = Long.MIN_VALUE;
      private boolean movementPrepared;
      private final Set<String> loggedFailures = ConcurrentHashMap.newKeySet();

      private DynamicElementPositionState(DynamicPositionSpec spec, DynamicPositionSpec.Position initial) {
         this.spec = spec;
         this.current = initial;
         this.target = initial;
      }
   }

   public List<TextDisplay> getPlayerTextDisplays(Player var1) {
      List var2 = this.playerDisplays.get(var1);
      return var2 != null ? var2 : Collections.emptyList();
   }

   public TextDisplay getTextDisplayByKeys(Player var1, String var2, String var3) {
      List<TextDisplay> displays = this.getTextDisplaysByKeys(var1, var2, var3);
      return displays.isEmpty() ? null : displays.get(0);
   }

   public List<TextDisplay> getTextDisplaysByKeys(Player player, String menuKey, String layoutKey) {
      if (player == null || menuKey == null || layoutKey == null) {
         return Collections.emptyList();
      }

      String displayName = menuKey + ":" + layoutKey;
      LinkedHashMap<UUID, TextDisplay> matches = new LinkedHashMap<>();
      List<TextDisplay> displays = this.playerDisplays.get(player);
      if (displays != null) {
         for (TextDisplay display : displays) {
            if (display != null && display.isValid() && displayName.equals(display.getCustomName())) {
               matches.put(display.getUniqueId(), display);
            }
         }
      }

      World world = player.getWorld();
      if (world != null) {
         for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
            if (display != null && display.isValid() && displayName.equals(display.getCustomName()) && player.canSee(display)) {
               matches.put(display.getUniqueId(), display);
            }
         }
      }

      return new ArrayList<>(matches.values());
   }

   public String getCurrentPlayerMenu(Player var1) {
      return this.currentPlayerMenus.get(var1);
   }

   public Section getRuntimeSection(Player player, String menuKey) {
      if (player != null && menuKey != null && menuKey.equals(this.currentPlayerMenus.get(player))) {
         Section runtime = this.playerRuntimeSections.get(player);
         if (runtime != null) {
            return runtime;
         }
      }

      return menuKey == null ? null : sectionManager.get(menuKey);
   }

   void markInternalMenuTeleport(Player player) {
      if (player != null) {
         this.internalMenuTeleports.add(player.getUniqueId());
      }
   }

   boolean consumeInternalMenuTeleport(Player player) {
      return player != null && this.internalMenuTeleports.remove(player.getUniqueId());
   }

   boolean teleportMenuInternal(Player player, Location location) {
      if (player == null || location == null) {
         return false;
      }

      this.markInternalMenuTeleport(player);
      boolean teleported = player.teleport(location);
      if (!teleported) {
         this.consumeInternalMenuTeleport(player);
      }

      return teleported;
   }

   boolean isMenuTransitionActive(Player player) {
      return player != null && this.activeMenuTransitions.containsKey(player.getUniqueId());
   }

   boolean isCurrentMenuCameraLocation(Player player, Location location) {
      if (player == null || location == null) {
         return false;
      }

      String menuKey = this.currentPlayerMenus.get(player);
      Section section = this.getRuntimeSection(player, menuKey);
      if (section == null) {
         return false;
      }

      Section resolved = org.orvencraft.uicore.section.PlayerLocationOverride.apply(player, section);
      World world = Bukkit.getWorld(resolved.world);
      if (world == null || !world.equals(location.getWorld())) {
         return false;
      }

      Location cameraLocation = new Location(world, resolved.cameraX, resolved.cameraY, resolved.cameraZ);
      return location.distanceSquared(cameraLocation) <= 1.0;
   }

   public MenuLayout getSelectedLayout(Player var1) {
      return this.selectedLayouts.get(var1);
   }

   public ItemDisplayManager getItemDisplayManager() {
      return itemDisplayManager;
   }

   public HoverEnlargeManager getHoverEnlargeManager() {
      return this.hoverEnlargeManager;
   }

   public UIcoreApi getMenuApi() {
      return this.menuApi;
   }

   public ItemDisplay getCursorItemDisplay(Player player) {
      return this.playerItemDisplays.get(player);
   }

   public boolean getLayoutToggleState(Player player, String layoutKey) {
      return this.getLayoutToggleState(player, this.getCurrentPlayerMenu(player), layoutKey);
   }

   public boolean getLayoutToggleState(Player player, String menuKey, String layoutKey) {
      if (player == null || layoutKey == null) {
         return false;
      }

      if (menuKey == null || menuKey.isBlank()) {
         menuKey = this.getCurrentPlayerMenu(player);
         if (menuKey == null || menuKey.isBlank()) {
            return false;
         }
      }

      Map<String, Boolean> states = this.playerLayoutToggleStates.get(player.getUniqueId());
      return states != null && states.getOrDefault(menuKey + ":" + layoutKey, false);
   }

   public boolean toggleLayoutContent(Player player, MenuLayout layout) {
      if (player != null && layout != null) {
         String menuKey = layout.getOwnerMenuKey();
         if (menuKey == null || menuKey.isBlank()) {
            menuKey = this.getCurrentPlayerMenu(player);
         }

         if (menuKey != null) {
            String stateKey = menuKey + ":" + layout.key;
            Map<String, Boolean> states = this.playerLayoutToggleStates.computeIfAbsent(player.getUniqueId(), var0 -> new ConcurrentHashMap<>());
            boolean oldState = states.getOrDefault(stateKey, false);
            boolean newState = !oldState;
            states.put(stateKey, newState);
            String resolvedMenuKey = menuKey;
            this.refreshToggleDisplayText(player, resolvedMenuKey, layout);
            long refreshDelay = layout.hasPressedStyle() ? Math.max(1, layout.getPressedStyleDuration() + 1L) : 3L;
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(var1 -> this.refreshToggleDisplayText(player, resolvedMenuKey, layout), null, refreshDelay);

            return newState;
         }
      }

      return false;
   }

   public long getLayoutCooldownLeftMillis(Player player, String cooldownKey) {
      if (player == null || cooldownKey == null || cooldownKey.isBlank()) {
         return 0L;
      }

      Map<String, Long> playerCooldowns = this.playerLayoutCooldownUntil.get(player.getUniqueId());
      if (playerCooldowns == null) {
         return 0L;
      }

      long now = System.currentTimeMillis();
      Long until = playerCooldowns.get(cooldownKey);
      if (until == null || until <= now) {
         playerCooldowns.remove(cooldownKey);
         return 0L;
      }

      return until - now;
   }

   public void startLayoutCooldown(Player player, String cooldownKey, long durationMillis) {
      if (player != null && cooldownKey != null && !cooldownKey.isBlank() && durationMillis > 0L) {
         Map<String, Long> playerCooldowns = this.playerLayoutCooldownUntil.computeIfAbsent(player.getUniqueId(), var0 -> new ConcurrentHashMap<>());
         playerCooldowns.put(cooldownKey, System.currentTimeMillis() + durationMillis);
      }
   }

   private void refreshToggleDisplayText(Player player, String menuKey, MenuLayout layout) {
      if (player != null && menuKey != null && layout != null && !layout.isItemLayout()) {
         String rendered = layout.renderStyledName(player, false, false);
         for (TextDisplay display : this.getTextDisplaysByKeys(player, menuKey, layout.key)) {
            if (display != null && display.isValid()) {
               display.setText(rendered);
               display.text(ColorParser.parse(rendered));
            }
         }
      }
   }

   public void updateLayoutDisplayText(Player player, String menuKey, MenuLayout layout, boolean hovered, boolean pressed) {
      if (player != null && menuKey != null && layout != null && !layout.isItemLayout()) {
         String rendered = layout.renderStyledName(player, hovered, pressed);
         for (TextDisplay display : this.getTextDisplaysByKeys(player, menuKey, layout.key)) {
            display.setText(rendered);
            display.text(ColorParser.parse(rendered));
         }
      }
   }

   private boolean tryAcquireLayoutClick(Player player, String menuKey, String layoutKey) {
      if (player == null || menuKey == null || layoutKey == null) {
         return false;
      }

      long now = System.currentTimeMillis();
      String key = menuKey + ":" + layoutKey;
      Map<String, Long> times = this.playerLayoutClickTimes.computeIfAbsent(player.getUniqueId(), var0 -> new ConcurrentHashMap<>());
      Long previous = times.get(key);
      if (previous != null && now - previous < 1200L) {
         return false;
      }

      times.put(key, now);
      return true;
   }

   public void refreshCurrentMenu(Player player) {
      if (player != null && player.isOnline() && playerCursors.containsKey(player)) {
         String menuKey = this.getCurrentPlayerMenu(player);
         if (menuKey != null && sectionManager.get(menuKey) != null) {
            this.refreshMenuContent(player, menuKey, menuKey);
         }
      }
   }

   public Map<String, MenuLayout> getLayoutsForPlayer(Player player, String menuKey, Section section) {
      LinkedHashMap<String, MenuLayout> layouts = new LinkedHashMap<>();
      if (section != null) {
         layouts.putAll(section.layouts);
         for (MenuSlot slot : section.slots.values()) {
            String templateId = this.resolveMenuSlotTemplate(player, menuKey, section, slot);
            if (!templateId.isEmpty()) {
               MenuLayout materialized = section.materializeSlot(menuKey, slot.id(), templateId);
               if (materialized != null) {
                  layouts.put(slot.id(), materialized);
               }
            }
         }
      }

      if (player != null && menuKey != null && !menuKey.isBlank()) {
         UUID playerId = player.getUniqueId();
         Map<String, Set<String>> hiddenByMenu = this.playerMenuHiddenLayouts.get(playerId);
         Set<String> hidden = hiddenByMenu == null ? null : hiddenByMenu.get(menuKey);
         if (hidden != null) {
            for (String elementId : hidden) {
               layouts.remove(elementId);
            }
         }

         Map<String, Map<String, MenuLayout>> overridesByMenu = this.playerMenuLayoutOverrides.get(playerId);
         Map<String, MenuLayout> overrides = overridesByMenu == null ? null : overridesByMenu.get(menuKey);
         if (overrides != null) {
            layouts.putAll(overrides);
         }

         layouts.entrySet().removeIf(var0 -> !var0.getValue().isVisible(player));
      }

      return Collections.unmodifiableMap(layouts);
   }

   public String getResolvedMenuSlotTemplate(Player player, String menuKey, String slotId) {
      Section section = sectionManager.get(menuKey);
      if (section == null) {
         return "";
      }

      MenuSlot slot = section.slots.get(slotId);
      return slot == null ? "" : this.resolveMenuSlotTemplate(player, menuKey, section, slot);
   }

   private String resolveMenuSlotTemplate(Player player, String menuKey, Section section, MenuSlot slot) {
      String selected = null;
      boolean explicitlyAssigned = false;
      if (player != null) {
         Map<String, Map<String, String>> byMenu = this.playerMenuSlotAssignments.get(player.getUniqueId());
         Map<String, String> assignments = byMenu == null ? null : byMenu.get(menuKey);
         if (assignments != null && assignments.containsKey(slot.id())) {
            selected = assignments.get(slot.id());
            explicitlyAssigned = true;
         }
      }

      if (!explicitlyAssigned) {
         Map<String, String> assignments = this.globalMenuSlotAssignments.get(menuKey);
         if (assignments != null && assignments.containsKey(slot.id())) {
            selected = assignments.get(slot.id());
            explicitlyAssigned = true;
         }
      }

      if (explicitlyAssigned && (selected == null || selected.isBlank())) {
         return "";
      }

      String resolved = explicitlyAssigned ? selected.trim() : this.resolveSlotExpression(player, menuKey, slot.content());
      if (section.hasTemplate(resolved)) {
         return resolved;
      }

      return section.hasTemplate(slot.fallback()) ? slot.fallback() : "";
   }

   private String resolveSlotExpression(Player player, String menuKey, String expression) {
      if (expression == null || expression.isBlank()) {
         return "";
      }

      Matcher matcher = SLOT_STATE_PLACEHOLDER_PATTERN.matcher(expression);
      StringBuffer resolved = new StringBuffer();
      while (matcher.find()) {
         matcher.appendReplacement(resolved, Matcher.quoteReplacement(this.getPlayerMenuState(player, menuKey, matcher.group(1))));
      }
      matcher.appendTail(resolved);

      String value = resolved.toString();
      if (player != null) {
         value = parsePlaceholders(player, value);
      }
      return value.trim();
   }

   public void setPlayerMenuSlot(Player player, String menuKey, String slotId, String templateId) {
      if (player == null) {
         return;
      }
      this.playerMenuSlotAssignments
         .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
         .computeIfAbsent(menuKey, ignored -> new ConcurrentHashMap<>())
         .put(slotId, templateId == null ? "" : templateId);
   }

   public void resetPlayerMenuSlot(Player player, String menuKey, String slotId) {
      if (player != null) {
         removeNestedAssignment(this.playerMenuSlotAssignments.get(player.getUniqueId()), menuKey, slotId);
      }
   }

   public void setGlobalMenuSlot(String menuKey, String slotId, String templateId) {
      this.globalMenuSlotAssignments
         .computeIfAbsent(menuKey, ignored -> new ConcurrentHashMap<>())
         .put(slotId, templateId == null ? "" : templateId);
   }

   public void resetGlobalMenuSlot(String menuKey, String slotId) {
      removeNestedAssignment(this.globalMenuSlotAssignments, menuKey, slotId);
   }

   private static void removeNestedAssignment(Map<String, Map<String, String>> assignments, String menuKey, String slotId) {
      if (assignments == null) {
         return;
      }

      Map<String, String> bySlot = assignments.get(menuKey);
      if (bySlot != null) {
         bySlot.remove(slotId);
         if (bySlot.isEmpty()) {
            assignments.remove(menuKey);
         }
      }
   }

   public MenuLayout getLayoutForPlayer(Player player, String menuKey, String elementId) {
      if (menuKey == null || elementId == null || elementId.isBlank()) {
         return null;
      }

      Section section = sectionManager.get(menuKey);
      if (section == null) {
         return null;
      }

      return this.getLayoutsForPlayer(player, menuKey, section).get(elementId);
   }

   public MenuLayout getLayoutForPlayer(Player player, String fullName) {
      if (fullName != null && fullName.contains(":")) {
         String[] parts = fullName.split(":", 2);
         return this.getLayoutForPlayer(player, parts[0], parts[1]);
      }

      return null;
   }

   public boolean putPlayerMenuLayout(Player player, String menuKey, String elementId, MenuLayout layout) {
      if (player == null || menuKey == null || menuKey.isBlank() || elementId == null || elementId.isBlank() || layout == null) {
         return false;
      }

      layout.setOwnerMenuKey(menuKey);
      UUID playerId = player.getUniqueId();
      this.playerMenuLayoutOverrides
         .computeIfAbsent(playerId, var0 -> new ConcurrentHashMap<>())
         .computeIfAbsent(menuKey, var0 -> new ConcurrentHashMap<>())
         .put(elementId, layout);
      Map<String, Set<String>> hiddenByMenu = this.playerMenuHiddenLayouts.get(playerId);
      if (hiddenByMenu != null) {
         Set<String> hidden = hiddenByMenu.get(menuKey);
         if (hidden != null) {
            hidden.remove(elementId);
         }
      }

      return true;
   }

   public boolean hidePlayerMenuLayout(Player player, String menuKey, String elementId) {
      if (player == null || menuKey == null || menuKey.isBlank() || elementId == null || elementId.isBlank()) {
         return false;
      }

      UUID playerId = player.getUniqueId();
      Map<String, Map<String, MenuLayout>> overridesByMenu = this.playerMenuLayoutOverrides.get(playerId);
      if (overridesByMenu != null) {
         Map<String, MenuLayout> overrides = overridesByMenu.get(menuKey);
         if (overrides != null) {
            overrides.remove(elementId);
         }
      }

      this.playerMenuHiddenLayouts
         .computeIfAbsent(playerId, var0 -> new ConcurrentHashMap<>())
         .computeIfAbsent(menuKey, var0 -> ConcurrentHashMap.newKeySet())
         .add(elementId);
      return true;
   }

   public void clearPlayerMenuLayouts(Player player, String menuKey) {
      if (player == null) {
         return;
      }

      UUID playerId = player.getUniqueId();
      if (menuKey == null || menuKey.isBlank()) {
         this.playerMenuLayoutOverrides.remove(playerId);
         this.playerMenuHiddenLayouts.remove(playerId);
      } else {
         Map<String, Map<String, MenuLayout>> overridesByMenu = this.playerMenuLayoutOverrides.get(playerId);
         if (overridesByMenu != null) {
            overridesByMenu.remove(menuKey);
         }

         Map<String, Set<String>> hiddenByMenu = this.playerMenuHiddenLayouts.get(playerId);
         if (hiddenByMenu != null) {
            hiddenByMenu.remove(menuKey);
         }
      }
   }

   public String getPlayerMenuState(Player player, String menuKey, String key) {
      if (player == null || menuKey == null || key == null || menuKey.isBlank() || key.isBlank()) {
         return "";
      }

      Map<String, Map<String, String>> statesByMenu = this.playerMenuStates.get(player.getUniqueId());
      Map<String, String> states = statesByMenu == null ? null : statesByMenu.get(menuKey);
      String value = states == null ? null : states.get(key);
      return value == null ? "" : value;
   }

   public Map<String, String> getPlayerMenuStateSnapshot(Player player, String menuKey) {
      if (player == null || menuKey == null || menuKey.isBlank()) {
         return Collections.emptyMap();
      }

      Map<String, Map<String, String>> statesByMenu = this.playerMenuStates.get(player.getUniqueId());
      Map<String, String> states = statesByMenu == null ? null : statesByMenu.get(menuKey);
      return states == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(states));
   }

   public void setPlayerMenuState(Player player, String menuKey, String key, String value) {
      if (player == null || menuKey == null || key == null || menuKey.isBlank() || key.isBlank()) {
         return;
      }

      UUID playerId = player.getUniqueId();
      if (value == null) {
         Map<String, Map<String, String>> statesByMenu = this.playerMenuStates.get(playerId);
         if (statesByMenu != null) {
            Map<String, String> states = statesByMenu.get(menuKey);
            if (states != null) {
               states.remove(key);
               if (states.isEmpty()) {
                  statesByMenu.remove(menuKey);
               }
            }

            if (statesByMenu.isEmpty()) {
               this.playerMenuStates.remove(playerId);
            }
         }
      } else {
         this.playerMenuStates
            .computeIfAbsent(playerId, var0 -> new ConcurrentHashMap<>())
            .computeIfAbsent(menuKey, var0 -> new ConcurrentHashMap<>())
            .put(key, value);
      }
   }

   public void clearPlayerMenuState(Player player, String menuKey, String key) {
      this.setPlayerMenuState(player, menuKey, key, null);
   }

   public void clearPlayerMenuState(Player player, String menuKey) {
      if (player == null) {
         return;
      }

      UUID playerId = player.getUniqueId();
      if (menuKey == null || menuKey.isBlank()) {
         this.playerMenuStates.remove(playerId);
      } else {
         Map<String, Map<String, String>> statesByMenu = this.playerMenuStates.get(playerId);
         if (statesByMenu != null) {
            statesByMenu.remove(menuKey);
            if (statesByMenu.isEmpty()) {
               this.playerMenuStates.remove(playerId);
            }
         }
      }
   }

   private void clearMenuTextAnimationTasks(Player var1) {
      List<Integer> var2 = this.playerMenuTextAnimationTasks.remove(var1);
      if (var2 != null) {
         var2.forEach(Bukkit.getScheduler()::cancelTask);
      }
   }

   private void clearMenuItemAnimationTasks(Player var1) {
      List<Integer> var2 = this.playerMenuItemAnimationTasks.remove(var1);
      if (var2 != null) {
         var2.forEach(Bukkit.getScheduler()::cancelTask);
      }
   }

   private void clearMenuItemDisplays(Player var1) {
      this.clearMenuItemAnimationTasks(var1);
      List<ItemDisplay> var2 = this.playerMenuItemDisplays.remove(var1);
      if (var2 != null) {
         var2.forEach(var0 -> {
            if (var0 != null && !var0.isDead()) {
               var0.remove();
            }
         });
         var2.clear();
      }
   }

   private MenuViewSnapshot captureMenuView(Player player) {
      if (player == null) {
         return null;
      }

      return new MenuViewSnapshot(
         playerSit.get(player),
         playerCursors.get(player),
         this.playerDisplays.get(player),
         this.playerItemDisplays.get(player),
         this.playerMenuItemDisplays.get(player),
         this.playerPumpkinDisplays.get(player)
      );
   }

   private void prepareMenuViewSwitch(Player player) {
      if (player == null) {
         return;
      }

      this.clearMenuTextAnimationTasks(player);
      this.clearMenuItemAnimationTasks(player);
      playingSound.remove(player.getName());
      player.stopAllSounds();
   }

   private void releasePlayerFromMenuCamera(Player player, MenuViewSnapshot snapshot) {
      if (player == null || snapshot == null) {
         return;
      }

      if (snapshot.cameraMount != null) {
         playerSit.remove(player, snapshot.cameraMount);
         if (snapshot.cameraMount.getPassengers().contains(player)) {
            snapshot.cameraMount.removePassenger(player);
         }
      }

      if (snapshot.cursorStand != null) {
         playerCursors.remove(player, snapshot.cursorStand);
      }

      this.sendCameraPacket(player, player);
   }

   private boolean isActiveMenuTransition(Player player, String menuKey, UUID transitionToken) {
      return player != null
         && player.isOnline()
         && transitionToken != null
         && transitionToken.equals(this.activeMenuTransitions.get(player.getUniqueId()))
         && menuKey.equals(this.currentPlayerMenus.get(player));
   }

   private boolean isAtMenuCamera(Player player, Location cameraLocation) {
      return player != null
         && cameraLocation != null
         && player.getWorld().equals(cameraLocation.getWorld())
         && player.getLocation().distanceSquared(cameraLocation) <= 1.0;
   }

   private void abortMenuTransition(Player player, String menuKey, UUID transitionToken, MenuViewSnapshot previousView, String reason) {
      if (player == null || !this.activeMenuTransitions.remove(player.getUniqueId(), transitionToken)) {
         return;
      }

      this.getLogger().warning("Could not switch " + player.getName() + " to menu '" + menuKey + "': " + reason);
      this.cleanupMenuViewSnapshot(player, previousView);
      this.menuSessionTokens.remove(player.getUniqueId(), transitionToken);
      this.currentPlayerMenus.remove(player, menuKey);
      this.playerRuntimeSections.remove(player);
      this.clearDynamicMenuSession(player);
      cursorExactLocations.remove(player);
      this.sendCameraPacket(player, player);
      player.setInvisible(false);
      player.setCollidable(true);
   }

   private void storeOriginalGameMode(Player player) {
      if (player != null && !player.hasMetadata("cursor_original_gamemode")) {
         player.setMetadata("cursor_original_gamemode", new FixedMetadataValue(this, player.getGameMode().name()));
      }
   }

   private void cleanupMenuViewSnapshot(Player player, MenuViewSnapshot snapshot) {
      if (player == null || snapshot == null) {
         return;
      }

      if (snapshot.cameraMount != null) {
         playerSit.remove(player, snapshot.cameraMount);
         if (snapshot.cameraMount.getPassengers().contains(player)) {
            snapshot.cameraMount.removePassenger(player);
         }
         this.removeMenuEntity(snapshot.cameraMount);
      }

      if (snapshot.cursorStand != null) {
         playerCursors.remove(player, snapshot.cursorStand);
         this.removeMenuEntity(snapshot.cursorStand);
      }

      if (snapshot.textDisplays != null) {
         this.playerDisplays.remove(player, snapshot.textDisplays);
         snapshot.textDisplays.forEach(this::removeMenuEntity);
         snapshot.textDisplays.clear();
      }

      if (snapshot.cursorDisplay != null) {
         this.playerItemDisplays.remove(player, snapshot.cursorDisplay);
         this.removeMenuEntity(snapshot.cursorDisplay);
      }

      if (snapshot.menuItemDisplays != null) {
         this.playerMenuItemDisplays.remove(player, snapshot.menuItemDisplays);
         snapshot.menuItemDisplays.forEach(this::removeMenuEntity);
         snapshot.menuItemDisplays.clear();
      }

      if (snapshot.pumpkinDisplay != null) {
         this.playerPumpkinDisplays.remove(player, snapshot.pumpkinDisplay);
         this.removeMenuEntity(snapshot.pumpkinDisplay);
      }
   }

   private void removeMenuEntity(Entity entity) {
      if (entity != null && !entity.isDead()) {
         entity.remove();
      }
   }

   private <T extends Entity> T tagMenuEntity(T entity) {
      if (entity != null) {
         entity.addScoreboardTag(MENU_ENTITY_TAG);
         entity.setPersistent(false);
      }

      return entity;
   }

   private void showMenuDisplay(Player player, Display display, MenuLayout layout) {
      if (player == null || display == null || layout == null) {
         return;
      }
      if (!layout.isEntranceAnimationEnabled()) {
         player.showEntity(this, display);
         return;
      }

      Location target = display.getLocation().clone();
      Vector forward = target.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0, forward.getX()).normalize();
      Vector up = forward.getCrossProduct(right).multiply(-1);
      double distance = layout.getEntranceAnimationDistance();
      Vector offset = switch (layout.getEntranceAnimationFrom()) {
         case "bottom" -> up.clone().multiply(-distance);
         case "left" -> right.clone().multiply(-distance);
         case "right" -> right.clone().multiply(distance);
         case "front" -> forward.clone().multiply(-distance);
         case "back" -> forward.clone().multiply(distance);
         default -> up.clone().multiply(distance);
      };
      display.teleport(target.clone().add(offset));

      Map<Player, List<Integer>> tasks = display instanceof ItemDisplay ? this.playerMenuItemAnimationTasks : this.playerMenuTextAnimationTasks;
      int taskId = new BukkitRunnable() {
         @Override
         public void run() {
            if (!player.isOnline() || !display.isValid()) {
               return;
            }

            int duration = layout.getEntranceAnimationDuration();
            if (duration <= 0) {
               display.teleport(target);
               player.showEntity(UIcorePlugin.this, display);
               return;
            }

            UIcorePlugin.this.setTeleportDurationSafe(display, duration);
            player.showEntity(UIcorePlugin.this, display);

            int movementTaskId = new BukkitRunnable() {
               @Override
               public void run() {
                  if (player.isOnline() && display.isValid()) {
                     display.teleport(target);
                  }
               }
            }.runTaskLater(UIcorePlugin.this, 1L).getTaskId();
            tasks.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(movementTaskId);
         }
      }.runTaskLater(this, layout.getEntranceAnimationDelay()).getTaskId();

      tasks.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(taskId);
   }

   private void purgeOrphanedMenuEntities() {
      Set<UUID> checkedEntities = ConcurrentHashMap.newKeySet();
      int removed = 0;

      for (Section section : sectionManager.getAll().values()) {
         World world = Bukkit.getWorld(section.world);
         if (world == null) {
            continue;
         }

         Location cameraLocation = new Location(world, section.cameraX, section.cameraY, section.cameraZ);
         world.getChunkAt(cameraLocation).load(true);
         for (Entity entity : world.getNearbyEntities(cameraLocation, 12.0, 12.0, 12.0)) {
            if (!checkedEntities.add(entity.getUniqueId())) {
               continue;
            }

            if (entity.getScoreboardTags().contains(MENU_ENTITY_TAG) || this.isLegacyMenuEntity(entity)) {
               entity.remove();
               removed++;
            }
         }
      }

      if (removed > 0) {
         this.getLogger().info("Removed " + removed + " orphaned menu runtime entities.");
      }
   }

   private boolean isLegacyMenuEntity(Entity entity) {
      if (entity instanceof ArmorStand armorStand) {
         return armorStand.isMarker() && !armorStand.isVisible() && !armorStand.hasGravity();
      }

      if (entity instanceof Pig pig) {
         return !pig.hasAI() && pig.isInvisible() && !pig.hasGravity() && !pig.isCollidable() && pig.isSilent();
      }

      String customName = entity.getCustomName();
      if (customName != null) {
         for (String menuKey : sectionManager.getAll().keySet()) {
            if (customName.startsWith(menuKey + ":")) {
               return true;
            }
         }
      }

      if (entity instanceof ItemDisplay itemDisplay && customName == null && !itemDisplay.isVisibleByDefault()) {
         ItemStack stack = itemDisplay.getItemStack();
         Material configuredCursor = Material.matchMaterial(cursorItem);
         return stack != null && configuredCursor != null && stack.getType() == configuredCursor;
      }

      return false;
   }

   private void applyMenuTextFloatingAnimation(final Player var1, final TextDisplay var2, MenuLayout var3) {
      if (var1 != null && var2 != null && var3 != null && var3.isFloatingAnimationEnabled()) {
         String var4 = var3.getFloatingAnimationType();
         if (var4 != null && !"none".equalsIgnoreCase(var4)) {
            final double var5 = Math.max(0.01, var3.getFloatingAnimationSpeed());
            final double var7 = Math.max(0.0, var3.getFloatingAnimationAmplitude());
            final String var9 = var4.toLowerCase(Locale.ROOT);
            final boolean var10 = "left-right".equals(var9) || "float-x".equals(var9);
            final boolean var11 = "up-down".equals(var9) || "float-y".equals(var9);
            final boolean var12 = "rotate".equals(var9);
            final boolean var13 = "scale".equals(var9) || "pulse".equals(var9);
            final boolean var14 = "fade".equals(var9);
            final boolean var15 = "bounce".equals(var9);
            if (var10 || var11 || var12 || var13 || var14 || var15) {
               final float var16 = (float)Math.toRadians(var3.tiltY);
               final float var17 = (float)Math.toRadians(var3.tiltX);
               final float var18 = (float)Math.toRadians(var3.tiltZ);
               final Vector3f var19 = new Vector3f(var2.getTransformation().getTranslation());
               final Vector3f var20 = new Vector3f(var2.getTransformation().getScale());
               final byte var21 = var2.getTextOpacity();
               int var22 = (new BukkitRunnable() {
                     long tick;
                     float angle;

                     {
                        Objects.requireNonNull(UIcorePlugin.this);
                        this.tick = 0L;
                        this.angle = 0.0F;
                     }

                     public void run() {
                        if (var1.isOnline()
                           && var2.isValid()
                           && UIcorePlugin.this.playerDisplays.getOrDefault(var1, Collections.emptyList()).contains(var2)) {
                           this.tick++;
                           double wave = Math.sin(this.tick * var5 * 0.05);
                           float offset = (float)(wave * var7);
                           Transformation current = var2.getTransformation();
                           Vector3f translation = new Vector3f(var19);
                           Vector3f scale = new Vector3f(var20);
                           if (var10) {
                              translation.x += offset;
                           } else if (var11) {
                              translation.y += offset;
                           } else if (var15) {
                              translation.y += (float)(Math.abs(wave) * var7);
                           }

                           if (var13) {
                              float factor = (float)Math.max(0.05, 1.0 + wave * var7);
                              scale.mul(factor);
                           }

                           if (var12) {
                              this.angle = (float)(this.angle + var5 * 0.05);
                              var2.setTransformation(
                                 new Transformation(
                                    translation,
                                    current.getLeftRotation().rotationYXZ(var16 + this.angle, var17, var18),
                                    scale,
                                    current.getRightRotation()
                                 )
                              );
                           } else {
                              var2.setTransformation(new Transformation(translation, current.getLeftRotation(), scale, current.getRightRotation()));
                           }

                           if (var14) {
                              int alpha = (int)Math.max(30.0, Math.min(255.0, 142.0 + wave * 113.0));
                              var2.setTextOpacity((byte)alpha);
                           }
                        } else {
                           if (var2.isValid()) {
                              var2.setTextOpacity(var21);
                           }

                           this.cancel();
                        }
                     }
                  })
                  .runTaskTimer(this, 0L, 1L)
                  .getTaskId();
               this.playerMenuTextAnimationTasks.computeIfAbsent(var1, var0 -> new CopyOnWriteArrayList<>()).add(var22);
            }
         }
      }
   }

   public String getCurrentPlayerInputField(Player var1) {
      return var1 == null ? null : this.currentPlayerInputFields.get(var1.getUniqueId());
   }

   public void setCurrentPlayerInputField(Player var1, String var2) {
      if (var1 != null) {
         if (var2 == null || var2.isBlank()) {
            this.currentPlayerInputFields.remove(var1.getUniqueId());
         } else {
            this.currentPlayerInputFields.put(var1.getUniqueId(), var2.trim());
         }
      }
   }

   public Map<String, String> getPlayerInputData(Player var1) {
      UUID var2 = var1.getUniqueId();
      return this.userInputData.computeIfAbsent(var2, var0 -> new HashMap<>());
   }

   public void setPlayerInputData(Player var1, String var2, String var3) {
      if (var1 != null && var2 != null && !var2.isBlank()) {
         UUID var4 = var1.getUniqueId();
         Map<String, String> inputs = this.userInputData.computeIfAbsent(var4, var0 -> new HashMap<>());
         if (var3 == null) {
            inputs.remove(var2);
            if (inputs.isEmpty()) {
               this.userInputData.remove(var4);
            }
         } else {
            inputs.put(var2, var3);
         }
      }
   }

   public String getPlayerInputValue(Player player, String field) {
      if (player == null || field == null || field.isBlank()) {
         return "";
      }

      Map<String, String> inputs = this.userInputData.get(player.getUniqueId());
      String value = inputs == null ? null : inputs.get(field);
      if (value == null) {
         Map<String, String> legacyInputs = Commands.getUserInputData().get(player.getUniqueId());
         value = legacyInputs == null ? null : legacyInputs.get(field);
      }

      return value == null ? "" : value;
   }

   public Map<String, String> getPlayerInputSnapshot(Player player) {
      if (player == null) {
         return Collections.emptyMap();
      }

      Map<String, String> merged = new HashMap<>();
      Map<String, String> legacyInputs = Commands.getUserInputData().get(player.getUniqueId());
      if (legacyInputs != null) {
         merged.putAll(legacyInputs);
      }

      Map<String, String> inputs = this.userInputData.get(player.getUniqueId());
      if (inputs != null) {
         merged.putAll(inputs);
      }

      return Collections.unmodifiableMap(merged);
   }

   public void clearPlayerInputData(Player player, String field) {
      this.setPlayerInputData(player, field, null);
      if (player != null && field != null) {
         Map<String, String> legacyInputs = Commands.getUserInputData().get(player.getUniqueId());
         if (legacyInputs != null) {
            legacyInputs.remove(field);
         }
      }
   }

   public void clearPlayerInputData(Player player) {
      if (player != null) {
         this.userInputData.remove(player.getUniqueId());
         Commands.getUserInputData().remove(player.getUniqueId());
      }
   }

   public String applyInputPlaceholders(Player player, String text, boolean maskPasswords) {
      if (text == null || player == null) {
         return text;
      }

      Matcher matcher = INPUT_PLACEHOLDER_PATTERN.matcher(text);
      StringBuffer out = new StringBuffer();
      while (matcher.find()) {
         String field = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
         String value = this.getPlayerInputValue(player, field);
         if (maskPasswords && this.isPasswordInputField(field)) {
            value = "*".repeat(Math.max(0, value.length()));
         }

         matcher.appendReplacement(out, Matcher.quoteReplacement(value));
      }

      matcher.appendTail(out);
      return out.toString();
   }

   public boolean isPasswordInputField(String field) {
      return field != null && ("password".equalsIgnoreCase(field) || "confirm_password".equalsIgnoreCase(field) || field.toLowerCase(Locale.ROOT).contains("password"));
   }

   private void purgeAllEntities() {
      playerCursors.values().forEach(var0 -> {
         if (var0 != null && !var0.isDead()) {
            var0.remove();
         }
      });
      playerSit.values().forEach(var0 -> {
         if (var0 != null && !var0.isDead()) {
            var0.remove();
         }
      });
      this.playerDisplays.values().forEach(var0 -> var0.forEach(var0x -> {
         if (var0x != null && !var0x.isDead()) {
            var0x.remove();
         }
      }));
      this.playerItemDisplays.values().forEach(var0 -> {
         if (var0 != null && !var0.isDead()) {
            var0.remove();
         }
      });
      this.playerMenuItemDisplays.values().forEach(var0 -> var0.forEach(var0x -> {
         if (var0x != null && !var0x.isDead()) {
            var0x.remove();
         }
      }));
      this.playerMenuItemAnimationTasks.values().forEach(var0 -> var0.forEach(Bukkit.getScheduler()::cancelTask));
      this.playerMenuItemDisplays.clear();
      this.playerMenuItemAnimationTasks.clear();
      if (itemDisplayManager != null) {
         Bukkit.getOnlinePlayers().forEach(itemDisplayManager::hideItem);
      }

      if (textDisplayManager != null) {
         textDisplayManager.cleanup();
      }

      if (this.hoverEnlargeManager != null) {
         this.hoverEnlargeManager.cleanup();
      }

   }

   private void mergeYamlFile(String var1) {
      File var2 = new File(this.getDataFolder(), var1);
      if (!var2.exists()) {
         this.saveResource(var1, false);
      } else {
         YamlConfiguration var3;
         try {
            var3 = YamlConfiguration.loadConfiguration(new InputStreamReader(this.getResource(var1), StandardCharsets.UTF_8));
         } catch (Exception var8) {
            this.getLogger().warning("Could not load default configuration " + var1 + ": " + var8.getMessage());
            var3 = new YamlConfiguration();
         }

         YamlConfiguration var4 = YamlConfiguration.loadConfiguration(var2);

         for (String var6 : var3.getKeys(true)) {
            if (!var4.contains(var6)) {
               var4.set(var6, var3.get(var6));
            }
         }

         if (var3.contains("version") && !var4.getString("version").equals(var3.getString("version"))) {
            var4.set("version", var3.getString("version"));
         }

         try {
            var4.save(var2);
         } catch (IOException var7) {
            this.getLogger().warning("Could not save merged configuration " + var1 + ": " + var7.getMessage());
         }
      }
   }

   private void ensureDefaultsOnReload(String... var1) {
      for (String var5 : var1) {
         File var6 = new File(this.getDataFolder(), var5);
         if (!var6.exists()) {
            this.saveResource(var5, false);
            this.getLogger().info("[UIcore] Regenerated missing default resource: " + var5);
         }
      }
   }

   public void onEnable() {
      plugin = this;
      this.saveDefaultConfig();
      this.mergeYamlFile("config.yml");
      PacketEvents.getAPI().getSettings().reEncodeByDefault(false).checkForUpdates(false).bStats(true);
      PacketEvents.getAPI().load();
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
      foliaLib = new MorePaperLib(this);
      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         hasPAPI = true;
      }

      this.reloadLangConfig();
      this.loadConfig();
      this.purgeOrphanedMenuEntities();
      this.menuApi = new UIcoreApiImpl(this);
      Bukkit.getServicesManager().register(UIcoreApi.class, this.menuApi, this, ServicePriority.Normal);
      this.getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
      this.getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
      itemDisplayManager = new ItemDisplayManager(this);
      this.registerUseEntityPacketListener();
      this.chunkLoaderManager = new ChunkLoaderManager(this);
      this.chunkLoaderManager.startChunkLoaderTask();
      this.getServer().getPluginManager().registerEvents(new CreatureSpawnListener(), this);
      this.getServer().getPluginManager().registerEvents(new UIcorePlugin.SessionCleanupListener(), this);
      this.getServer().getPluginManager().registerEvents(new MenuListener(this), this);
      this.getServer().getPluginManager().registerEvents(new UIcorePlugin.AttackBreakListener(), this);
      this.getServer().getPluginManager().registerEvents(new UIcorePlugin.PlayerJoinListener(), this);
      Bukkit.getPluginCommand("uicore").setExecutor(new Commands());
      textDisplayManager = new TextDisplayManager(this);
      this.hoverEnlargeManager = new HoverEnlargeManager(this);
      this.chunkLoaderManager = new ChunkLoaderManager(this);
      this.chunkLoaderManager.startChunkLoaderTask();
      NPCModule.initialize(this);
      WASDModule.initialize(this);
      CraftEngineCompat.init();
      this.getLogger().info("====================================");
      this.getLogger().info("UIcore enabled");
      this.getLogger().info("Version: " + this.getDescription().getVersion());
      this.getLogger().info("Authors: " + this.getDescription().getAuthors());
      this.getLogger().info("====================================");
      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         new UIcorePlaceholder(this).register();
         this.getLogger().info("PlaceholderAPI expansion registered");
      }

      String[] var2 = new String[]{"lang.yml", "lang/en_us.yml", "lang/ru_ru.yml"};

      for (String var6 : var2) {
         File var7 = new File(this.getDataFolder(), var6);
         if (!var7.exists()) {
            this.saveResource(var6, false);
         }
      }

      Bukkit.getScheduler().runTaskTimer(this, () -> {
         this.menuRuntimeTick++;
         for (Player var2x : Bukkit.getOnlinePlayers()) {
            if (playerCursors.containsKey(var2x)) {
               this.tickCameraEdgePan(var2x);
               this.tickDynamicMenuPositions(var2x);
               Location var3 = var2x.getLocation();
               ItemDisplay var4 = this.playerPumpkinDisplays.get(var2x);
               if (var4 != null && var4.isValid()) {
                  Location var5 = var3.clone().add(0.0, 1.7, 0.0);
                  var4.teleport(var5);
                  var4.setRotation(var3.getYaw(), var3.getPitch());
               }
            }
         }
      }, 0L, 1L);
   }

   public void onDisable() {
      if (this.menuApi != null) {
         Bukkit.getServicesManager().unregister(UIcoreApi.class, this.menuApi);
      }

      WASDModule.shutdown();
      NPCModule.shutdown();
      if (this.chunkLoaderManager != null) {
         this.chunkLoaderManager.stopChunkLoaderTask();
      }

      this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
      PacketEvents.getAPI().terminate();
      this.purgeAllEntities();
      this.menuOpenedAtTicks.clear();
      this.dynamicElementPositions.clear();
      this.getLogger().info("====================================");
      this.getLogger().info("UIcore disabled");
      this.getLogger().info("Version: " + this.getDescription().getVersion());
      this.getLogger().info("====================================");
   }

   public synchronized String getLangMessage(String var1, String var2) {
      if (this.langConfig == null) {
         this.reloadLangConfig();
      }

      return ChatColor.translateAlternateColorCodes('&', this.langConfig.getString(var1, var2));
   }

   public synchronized void reloadLangConfig() {
      File var1 = new File(this.getDataFolder(), "lang.yml");
      if (!var1.exists()) {
         this.saveResource("lang.yml", false);
      }

      this.mergeYamlFile("lang.yml");
      this.langConfig = YamlConfiguration.loadConfiguration(var1);
   }

   public void reloadPluginConfig() {
      HashMap var1 = new HashMap<>(this.currentPlayerMenus);
      this.activeMenuTransitions.clear();
      this.reloadLangConfig();
      this.getLogger().info("Reloading UIcore configuration...");
      this.reloadConfig();
      if (itemDisplayManager != null) {
         itemDisplayManager.reloadConfig();
      }

      for (Player var3 : Bukkit.getOnlinePlayers()) {
         if (playerCursors.containsKey(var3)) {
            ArmorStand var4 = playerCursors.remove(var3);
            if (var4 != null && !var4.isDead()) {
               var4.remove();
            }

            this.clearMenuTextAnimationTasks(var3);
            List<TextDisplay> var5 = this.playerDisplays.remove(var3);
            if (var5 != null) {
               var5.forEach(var0 -> {
                  if (var0 != null && !var0.isDead()) {
                     textDisplayManager.getTypewriterEffect().cancelEffect(var0);
                     var0.remove();
                  }
               });
               var5.clear();
            }

            ItemDisplay var6 = this.playerItemDisplays.remove(var3);
            if (var6 != null && !var6.isDead()) {
               var6.remove();
            }

            this.clearMenuItemDisplays(var3);

            Pig var7 = playerSit.remove(var3);
            if (var7 != null) {
               if (var7.getPassengers().contains(var3)) {
                  var7.removePassenger(var3);
               }

               var7.remove();
            }

            playingSound.remove(var3.getName());
            var3.stopAllSounds();
            this.sendCameraPacket(var3, var3);
            var3.setInvisible(false);
            var3.setCollidable(true);
            Location var8 = playerLocations.remove(var3);
            if (var8 != null) {
               var3.teleport(var8);
            }

            if (this.hoverEnlargeManager != null) {
               this.hoverEnlargeManager.cleanupPlayer(var3);
            }

            this.currentPlayerInputFields.remove(var3.getUniqueId());
            this.userInputData.remove(var3.getUniqueId());
            this.playerLayoutToggleStates.remove(var3.getUniqueId());
            this.playerLayoutClickTimes.remove(var3.getUniqueId());
            this.playerLayoutCooldownUntil.remove(var3.getUniqueId());
            this.playerMenuLayoutOverrides.remove(var3.getUniqueId());
            this.playerMenuHiddenLayouts.remove(var3.getUniqueId());
            this.playerMenuStates.remove(var3.getUniqueId());
            this.clearDynamicMenuSession(var3);
            this.restoreBlocks(var3);
         }

         itemDisplayManager.hideItem(var3);
      }

      for (List<TextDisplay> var11 : this.playerDisplays.values()) {
         var11.forEach(var0 -> {
            if (var0 != null && var0.isValid()) {
               var0.remove();
            }
         });
      }

      if (textDisplayManager != null) {
         textDisplayManager.cleanup();
      }

      if (this.hoverEnlargeManager != null) {
         this.hoverEnlargeManager.cleanup();
      }

      this.purgeAllEntities();
      playerCursors.clear();
      this.playerDisplays.clear();
      this.playerItemDisplays.clear();
      this.playerMenuItemDisplays.clear();
      this.playerMenuItemAnimationTasks.clear();
      playerLocations.clear();
      playerSit.clear();
      cursorExactLocations.clear();
      this.currentPlayerMenus.clear();
      this.playerRuntimeSections.clear();
      this.selectedLayouts.clear();
      playingSound.clear();
      sectionManager.clear();
      this.currentPlayerInputFields.clear();
      this.userInputData.clear();
      this.playerLayoutToggleStates.clear();
      this.playerLayoutClickTimes.clear();
      this.playerLayoutCooldownUntil.clear();
      this.playerMenuLayoutOverrides.clear();
      this.playerMenuHiddenLayouts.clear();
      this.playerMenuStates.clear();
      this.playerMenuSlotAssignments.clear();
      this.globalMenuSlotAssignments.clear();
      this.cameraEdgePanTargets.clear();
      this.cameraEdgePanOffsets.clear();
      this.menuOpenedAtTicks.clear();
      this.dynamicElementPositions.clear();
      this.loadConfig();
      this.purgeOrphanedMenuEntities();
      Bukkit.getOnlinePlayers().forEach(var1x -> {
         if (var1x.hasMetadata("cursor_original_helmet")) {
            var1x.removeMetadata("cursor_original_helmet", this);
         }
      });
      if (textDisplayManager != null) {
         textDisplayManager.reloadConfig();
      }

      this.ensureDefaultsOnReload("config.yml", "items.yml", "lang.yml", "lang/en_us.yml", "lang/ru_ru.yml", "commands.yml");
      cursorModelData = this.getConfig().getInt("cursor-item.custom-model-data", 0);
      this.getLogger().info("Configuration reloaded and menu elements refreshed");
      File var10 = new File(this.getDataFolder(), "commands.yml");
      YamlConfiguration var12 = YamlConfiguration.loadConfiguration(var10);
      allowedCommands = var12.getStringList("allowed-commands");
      allowedCommands.replaceAll(String::toLowerCase);
      if (this.chunkLoaderManager != null) {
         this.chunkLoaderManager.startChunkLoaderTask();
      }

      creatureSpawnLimitEnabled = this.getConfig().getBoolean("creature-spawn-limits.enabled", false);
      creatureSpawnLimitRadius = this.getConfig().getInt("creature-spawn-limits.radius", 10);
      if (this.hoverEnlargeManager != null) {
         this.hoverEnlargeManager.cleanup();
      }

      this.hoverEnlargeManager = new HoverEnlargeManager(this);
      NPCModule.reload();
      WASDModule.reload();

      for (Player var14 : Bukkit.getOnlinePlayers()) {
         String var15 = (String)var1.get(var14);
         if (var15 != null) {
            this.setupCursor(var14, var15);
         }
      }
   }

   private void loadConfig() {
      this.reloadConfig();
      File var1 = new File(this.getDataFolder(), "config.yml");
      if (!var1.exists()) {
         this.saveDefaultConfig();
      }

      cameraBlockCheckEnabled = this.getConfig().getBoolean("camera-block-check.enabled", false);
      cameraBlockCheckRadius = this.getConfig().getInt("camera-block-check.radius", 5);
      cursorZOffset = this.getConfig().getDouble("cursor-item.z-offset", 0.0);
      cursorX = this.getConfig().getDouble("cursor-item.x", 0.0);
      cursorY = this.getConfig().getDouble("cursor-item.y", 0.0);
      cursorSmoothing = this.getConfig().getDouble("cursor-item.smoothing", 1.0);
      soundLoop = this.getConfig().getBoolean("sound.loop.enabled");
      soundRate = this.getConfig().getInt("sound.loop.duration");
      soundName = this.getConfig().getString("sound.name");
      soundVolume = Float.parseFloat(this.getConfig().getString("sound.volume"));
      soundPitch = Float.parseFloat(this.getConfig().getString("sound.pitch"));
      this.debugMode = this.getConfig().getBoolean("Debug", false);
      usePumpkinOverlay = this.getConfig().getBoolean("use-pumpkin-overlay", false);
      joinRunBool = this.getConfig().getBoolean("join-run.enabled", false);
      joinRunSection = this.getConfig().getString("join-run.menu", "test");
      cursorItem = this.getConfig().getString("cursor-item.material", "ARROW");
      cursorScale = this.getConfig().getDouble("cursor-item.scale", 1.0);
      cursorSizeX = this.getConfig().getDouble("cursor-item.size.x", 1.0);
      cursorSizeY = this.getConfig().getDouble("cursor-item.size.y", 1.0);
      cursorSizeZ = this.getConfig().getDouble("cursor-item.size.z", 1.0);
      cursorSensitivity = this.getConfig().getDouble("cursor-item.sensitivity", 1.0);
      cursorModelData = this.getConfig().getInt("cursor-item.custom-model-data", 0);
      maxX = this.getConfig().getDouble("cursor-item.max-x");
      maxY = this.getConfig().getDouble("cursor-item.max-y");
      cursorMovementRangeEnabled = this.getConfig().getBoolean("cursor-item.movement-range.enabled", false);
      cursorMovementRangeXMin = this.getConfig().getDouble("cursor-item.movement-range.x.min", -2.0);
      cursorMovementRangeXMax = this.getConfig().getDouble("cursor-item.movement-range.x.max", 2.0);
      cursorMovementRangeYMin = this.getConfig().getDouble("cursor-item.movement-range.y.min", -3.0);
      cursorMovementRangeYMax = this.getConfig().getDouble("cursor-item.movement-range.y.max", 3.0);
      cursorDefaultPositionEnabled = this.getConfig().getBoolean("cursor-item.default-position.enabled", false);
      cursorDefaultPositionX = this.getConfig().getDouble("cursor-item.default-position.x", 0.0);
      cursorDefaultPositionY = this.getConfig().getDouble("cursor-item.default-position.y", 0.0);
      cameraEdgePanEnabled = this.getConfig().getBoolean("cursor-item.edge-pan.enabled", true);
      cameraEdgePanThreshold = this.getConfig().getDouble("cursor-item.edge-pan.threshold", 0.78);
      cameraEdgePanMaxOffsetX = this.getConfig().getDouble("cursor-item.edge-pan.max-offset.x", 0.1);
      cameraEdgePanMaxOffsetY = this.getConfig().getDouble("cursor-item.edge-pan.max-offset.y", 0.06);
      cameraEdgePanSmoothing = this.getConfig().getDouble("cursor-item.edge-pan.smoothing", 0.08);
      runDelay = this.getConfig().getInt("join-run.delay", 0);
      joinRunCommands = this.getConfig().getStringList("join-run.commands");
      exitYaw = (float)this.getConfig().getDouble("exit-camera.yaw", 0.0);
      exitPitch = (float)this.getConfig().getDouble("exit-camera.pitch", 0.0);
      creatureSpawnLimitEnabled = this.getConfig().getBoolean("creature-spawn-limits.enabled", false);
      creatureSpawnLimitRadius = this.getConfig().getInt("creature-spawn-limits.radius", 10);
      org.orvencraft.uicore.section.PlayerLocationOverride.reload(this.getConfig().getBoolean("use-player-location", false));
      this.saveDefaultMenuFiles();
      sectionManager.loadAllMenuConfigs();
      File var2 = new File(this.getDataFolder(), "commands.yml");
      if (!var2.exists()) {
         this.saveResource("commands.yml", false);
      }

      YamlConfiguration var3 = YamlConfiguration.loadConfiguration(var2);
      allowedCommands = var3.getStringList("allowed-commands");
      allowedCommands.replaceAll(String::toLowerCase);
      if (this.chunkLoaderManager != null) {
         this.chunkLoaderManager.startChunkLoaderTask();
      }
   }

   public void setupCursor(Player var1, String var2) {
          Section var3 = sectionManager.get(var2);
      if (var3 == null) {
         this.getLogger().warning("Menu '" + var2 + "' not found! Please check your menu configuration.");
         var1.sendMessage(
            this.getLangMessage("command.menu_not_found", "&c[UIcore] Menu '%menu%' was not found. Check the configuration.").replace("%menu%", var2)
         );
       } else {
          this.beginDynamicMenuSession(var1);
          final UUID menuSessionToken = UUID.randomUUID();
         this.menuSessionTokens.put(var1.getUniqueId(), menuSessionToken);
         String previousMenuForEvent = this.currentPlayerMenus.get(var1);
         Location relativeAnchor = playerLocations.getOrDefault(var1, var1.getLocation());
         Section var4 = org.orvencraft.uicore.section.PlayerLocationOverride.apply(var1, var3, relativeAnchor);
         MenuViewSnapshot previousViewForSwitch = null;
         if (playerCursors.containsKey(var1)) {
            String var5 = this.currentPlayerMenus.get(var1);
            Section var7 = this.getRuntimeSection(var1, var5);
            Pig var8 = playerSit.get(var1);
            boolean var9 = var8 != null && var8.isValid() && var8.getPassengers().contains(var1);
            if (var9 && var7 != null && this.isSameLocation(var7, var4)) {
               this.activeMenuTransitions.remove(var1.getUniqueId());
               this.playerRuntimeSections.put(var1, var4);
               if (var5 != null && !var5.equals(var2)) {
                  Bukkit.getPluginManager().callEvent(new MenuSwitchEvent(var1, var5, var2));
               }

               this.refreshMenuContent(var1, var2, var5);
               return;
            }
         }

         if (playerCursors.containsKey(var1)) {
            previousViewForSwitch = this.captureMenuView(var1);
            this.prepareMenuViewSwitch(var1);
         }

         final MenuViewSnapshot previousViewSnapshot = previousViewForSwitch;
         final UUID menuTransitionToken = menuSessionToken;
         this.activeMenuTransitions.put(var1.getUniqueId(), menuTransitionToken);
         this.currentPlayerMenus.put(var1, var2);
         this.playerRuntimeSections.put(var1, var4);
         if (previousMenuForEvent != null && !previousMenuForEvent.equals(var2)) {
            this.getLogger().info("Switching " + var1.getName() + " from menu '" + previousMenuForEvent + "' to '" + var2 + "'.");
         }
         Section var14 = sectionManager.get(var2);
         if (var14 == null) {
            this.activeMenuTransitions.remove(var1.getUniqueId(), menuTransitionToken);
            this.menuSessionTokens.remove(var1.getUniqueId(), menuSessionToken);
             this.currentPlayerMenus.remove(var1);
             this.playerRuntimeSections.remove(var1);
             this.clearDynamicMenuSession(var1);
            this.getLogger().warning("Menu '" + var2 + "' not found! Please check your menu configuration.");
            var1.sendMessage(this.getLangMessage("command.menu_not_found", "&c[UIcore] Menu '%menu%' was not found. Check the configuration.").replace("%menu%", var2));
         } else {
            Section var15 = var4;
            World var16 = Bukkit.getWorld(var15.world);
            if (var16 == null) {
               this.activeMenuTransitions.remove(var1.getUniqueId(), menuTransitionToken);
               this.menuSessionTokens.remove(var1.getUniqueId(), menuSessionToken);
                this.currentPlayerMenus.remove(var1);
                this.playerRuntimeSections.remove(var1);
                this.clearDynamicMenuSession(var1);
               this.getLogger().warning("World " + var15.world + " not found!");
            } else {
               if (previousMenuForEvent == null) {
                  Bukkit.getPluginManager().callEvent(new MenuOpenEvent(var1, var2));
               } else if (!previousMenuForEvent.equals(var2)) {
                  Bukkit.getPluginManager().callEvent(new MenuSwitchEvent(var1, previousMenuForEvent, var2));
               }

               playerLocations.putIfAbsent(var1, var1.getLocation());
               Location var17 = new Location(var16, var15.cameraX, var15.cameraY, var15.cameraZ, var15.yaw, var15.pitch);
               var16.getChunkAt(var17).load(true);
               foliaLib.scheduling().regionSpecificScheduler(var17).runDelayed(var5x -> {
                  if (this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                     this.releasePlayerFromMenuCamera(var1, previousViewSnapshot);
                     boolean teleported = this.teleportMenuInternal(var1, var17);
                     if (!teleported || !this.isAtMenuCamera(var1, var17)) {
                        this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "teleport to camera position was rejected");
                        return;
                     }

                     this.cleanupMenuViewSnapshot(var1, previousViewSnapshot);
                     this.storeOriginalGameMode(var1);
                     var1.setGameMode(GameMode.ADVENTURE);
                     var16.getChunkAt(var17).load(true);
                  }
               }, 5L);
               this.storeOriginalGameMode(var1);
               Bukkit.getScheduler().runTaskLater(this, () -> {
                  if (this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                     var1.setGameMode(GameMode.ADVENTURE);
                  }
               }, 1L);
               var16.getChunkAt(var17).load(true);
               foliaLib.scheduling()
                  .regionSpecificScheduler(var17)
                  .runDelayed(
                         var5x -> {
                         if (this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                           if (!this.isAtMenuCamera(var1, var17)) {
                              this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "player could not be fixed at the camera position");
                              return;
                           }

                           this.currentPlayerMenus.put(var1, var2);
                           playingSound.add(var1.getName());
                           var1.stopAllSounds();
                           if (soundLoop) {
                              foliaLib.scheduling().entitySpecificScheduler(var1).runAtFixedRate(var1xx -> {
                                 if (!playingSound.contains(var1.getName())) {
                                    var1xx.cancel();
                                 } else {
                                    var1.stopAllSounds();
                                    var1.playSound(var1.getLocation(), soundName, soundVolume, soundPitch);
                                 }
                              }, null, 1L, (long)(20 * soundRate));
                           } else {
                              var1.playSound(var1.getLocation(), soundName, soundVolume, soundPitch);
                           }

                           Location var6x = new Location(var16, var15.cameraX, var15.cameraY, var15.cameraZ, var15.yaw, var15.pitch);
                           Vector var7x = var6x.getDirection().normalize();
                           Vector var8x = new Vector(-var7x.getZ(), 0.0, var7x.getX()).normalize();
                           Vector var9x = var7x.getCrossProduct(var8x).multiply(-1);
                           double var10x = cursorX;
                           double var12 = cursorY;
                           if (cursorDefaultPositionEnabled) {
                              var10x = cursorDefaultPositionX;
                              var12 = cursorDefaultPositionY;
                           }

                           if (cursorMovementRangeEnabled) {
                              var10x = Math.max(cursorMovementRangeXMin, Math.min(cursorMovementRangeXMax, var10x));
                              var12 = Math.max(cursorMovementRangeYMin, Math.min(cursorMovementRangeYMax, var12));
                           }

                           Vector var14x = var7x.clone()
                              .multiply(var15.distance + cursorZOffset)
                              .add(var8x.clone().multiply(var10x))
                              .add(var9x.clone().multiply(-var12));
                           Location var15x = var6x.clone().add(var14x);
                           ArmorStand var16x = this.spawnCursorArmorStand(var15x);
                           playerCursors.put(var1, var16x);
                           ArrayList<TextDisplay> var17x = new ArrayList<>();
                           ArrayList<ItemDisplay> menuItemDisplays = new ArrayList<>();

                           Map<String, MenuLayout> visibleLayouts = this.getLayoutsForPlayer(var1, var2, var15);
                            for (MenuLayout var19 : visibleLayouts.values()) {
                               DynamicPositionSpec.Position layoutPosition = this.resolveLayoutPositionForSpawn(var1, var2, var19);
                               Location var21 = this.toMenuWorldLocation(var15, layoutPosition);
                               if (var21 == null) {
                                  continue;
                               }
                               if (var19.isItemLayout()) {
                                 ItemDisplay itemDisplay = this.spawnMenuItemDisplay(var1, var16, var21, var2, var19);
                                 TextDisplay hitboxDisplay = this.spawnMenuItemHitbox(var1, var16, var21, var2, var19);
                                 menuItemDisplays.add(itemDisplay);
                                 var17x.add(hitboxDisplay);
                                 continue;
                              }

                              TextDisplay var22 = this.tagMenuEntity((TextDisplay)var16.spawn(var21, TextDisplay.class));
                              String var23 = var19.renderStyledName(var1, false, false);
                              if (var19.isTypewriterEnabled()) {
                                 var22.setText(var19.getTypewriterCursorChar());
                                 textDisplayManager.getTypewriterEffect()
                                    .applyEffect(
                                       var22,
                                       var23,
                                       var19.getTypewriterSpeed(),
                                       var19.getTypewriterStartDelay(),
                                       var19.getTypewriterCursorChar()
                                    );
                              } else {
                                 var22.setText(var23);
                              }

                              var22.setCustomName(var2 + ":" + var19.key);
                              var22.setCustomNameVisible(false);
                              var22.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                              var22.setDefaultBackground(false);
                              var22.setShadowed(true);
                              var22.setBillboard(Billboard.CENTER);
                              var22.setVisibleByDefault(false);
                              var17x.add(var22);
                              float var24 = (float)Math.toRadians(var19.tiltX);
                              float var25 = (float)Math.toRadians(var19.tiltY);
                              float var26 = (float)Math.toRadians(var19.tiltZ);
                              Transformation var27 = var22.getTransformation();
                              Transformation var28 = new Transformation(
                                 var27.getTranslation(),
                                 var27.getLeftRotation().rotationYXZ(var25, var24, var26),
                                 new Vector3f((float)var19.getTextSize(), (float)var19.getTextSize(), (float)var19.getTextSize()),
                                 var27.getRightRotation()
                              );
                               var22.setTransformation(var28);
                               this.showMenuDisplay(var1, var22, var19);
                               this.applyMenuTextFloatingAnimation(var1, var22, var19);
                           }

                           this.playerDisplays.put(var1, var17x);
                           this.playerMenuItemDisplays.put(var1, menuItemDisplays);
                           ItemDisplay var29 = this.spawnCursorItemDisplay(var1, var15x);
                           this.playerItemDisplays.put(var1, var29);
                           this.resetCameraEdgePan(var1);
                           Pig var30 = this.spawnPlayerLocStand(var6x);
                           var30.setRotation(var15.yaw, var15.pitch);
                           playerSit.put(var1, var30);
                           this.mountPlayerToVehicle(var1, var16x);
                           if (usePumpkinOverlay) {
                              this.storeHelmet(var1);
                           }

                           MenuViewSnapshot newViewSnapshot = new MenuViewSnapshot(
                              var30,
                              var16x,
                              var17x,
                              var29,
                              menuItemDisplays,
                              this.playerPumpkinDisplays.get(var1)
                           );
                           var1.setInvisible(true);
                           var1.setCollidable(false);
                           if (!var30.addPassenger(var1)) {
                              this.cleanupMenuViewSnapshot(var1, newViewSnapshot);
                              this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "camera mount rejected the player passenger");
                              return;
                           }

                           foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(() -> {
                              if (!this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                                 this.cleanupMenuViewSnapshot(var1, newViewSnapshot);
                                 return;
                              }

                              if (!var30.isValid() || !var30.getPassengers().contains(var1)) {
                                 this.cleanupMenuViewSnapshot(var1, newViewSnapshot);
                                 this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "camera mount was lost before activation");
                                 return;
                              }

                              this.sendCameraPacket(var1, var30);
                              this.activeMenuTransitions.remove(var1.getUniqueId(), menuTransitionToken);
                              if (previousMenuForEvent != null && !previousMenuForEvent.equals(var2)) {
                                 this.getLogger().info("Switched " + var1.getName() + " to menu '" + var2 + "'.");
                              }
                           }, null, 2L);
                           if (this.debugMode) {
                              var1.sendMessage(this.getLangMessage("menu.cursor_activated", "&a[UIcore] Menu cursor activated."));
                           }

                           textDisplayManager.showTextDisplays(var1, var2);
                           this.clearBlockingBlocks(var1);
                           NPCModule.onMenuOpen(var1, var6x, var15.yaw, var15.pitch, var2);
                           if (var15.wasdEnabled && WASDModule.isMenuEnabled(var2)) {
                              ArrayList<Location> var31 = new ArrayList<>();
                              ArrayList<String> var32 = new ArrayList<>();
                              ArrayList<Double> var33 = new ArrayList<>();

                               for (MenuLayout var35 : visibleLayouts.values()) {
                                  DynamicPositionSpec.Position layoutPosition = this.getCurrentLayoutPosition(var1, var2, var35);
                                  Location var37 = this.toMenuWorldLocation(var15, layoutPosition);
                                  if (var37 == null) {
                                     continue;
                                  }
                                  var31.add(var37);
                                 List var38 = var35.getCommands();
                                 if (var38 != null && !var38.isEmpty()) {
                                    var32.add((String)var38.get(0));
                                 } else {
                                    var32.add("");
                                 }

                                 var33.add(var35.getTextSize());
                              }

                              WASDModule.onMenuOpen(var1, var2, var31.toArray(new Location[0]), var32, var17x.toArray(new TextDisplay[0]), var33);
                           }

                           cursorExactLocations.put(var1, var15x.clone());
                        }
                     },
                     10L
                  );
               if (var15.autoCommandsEnabled && !var15.autoCommands.isEmpty()) {
                  for (int var18 = 0; var18 < var15.autoCommands.size(); var18++) {
                     String var10 = var15.autoCommands.get(var18);
                     long var11 = var18 < var15.autoCommandDelays.size() ? var15.autoCommandDelays.get(var18) : 20L;
                     String var13 = parsePlaceholders(var1, var10);
                     CommandUtils.executeCommand(var1, var13, var11);
                  }
               }
            }
         }
      }
   }

   public void stopCursor(Player var1, boolean var2) {
      final UUID requestedSessionToken = this.menuSessionTokens.get(var1.getUniqueId());
      foliaLib.scheduling().regionSpecificScheduler(var1.getLocation()).runDelayed(var3 -> {
         UUID currentSessionToken = this.menuSessionTokens.get(var1.getUniqueId());
         if (!Objects.equals(requestedSessionToken, currentSessionToken)) {
            if (this.debugMode) {
               this.getLogger().info("Ignored stale menu close request for " + var1.getName() + ".");
            }
            return;
         }

         String closingMenu = this.currentPlayerMenus.get(var1);
         if (closingMenu != null) {
            Bukkit.getPluginManager().callEvent(new MenuCloseEvent(var1, closingMenu));
         }

         Pig var4 = playerSit.remove(var1);
         if (var4 != null) {
            if (var4.getPassengers().contains(var1)) {
               var4.removePassenger(var1);
            }

            var4.remove();
         }

         ArmorStand var5 = playerCursors.remove(var1);
         if (var5 != null && !var5.isDead()) {
            var5.remove();
         }

         this.clearMenuTextAnimationTasks(var1);
         List<TextDisplay> var6 = this.playerDisplays.remove(var1);
         if (var6 != null) {
            var6.forEach(var0 -> {
               if (var0 != null && !var0.isDead()) {
                  var0.remove();
               }
            });
            var6.clear();
         }

         ItemDisplay var7 = this.playerItemDisplays.remove(var1);
         if (var7 != null && !var7.isDead()) {
            var7.remove();
         }

         this.clearMenuItemDisplays(var1);

         playingSound.remove(var1.getName());
         var1.stopAllSounds();
         this.sendCameraPacket(var1, var1);
         var1.setInvisible(false);
         var1.setCollidable(true);
         if (var2) {
            Location var8 = playerLocations.remove(var1);
            if (var8 != null) {
               var1.teleport(var8);
            }

            if (this.hoverEnlargeManager != null) {
               this.hoverEnlargeManager.cleanupPlayer(var1);
            }
         }

         if (this.debugMode) {
            var1.sendMessage(this.getLangMessage("menu.cursor_deactivated", "&c[UIcore] Menu closed."));
         }

         textDisplayManager.clearPlayerDisplays(var1.getUniqueId());
         this.sendCameraPacket(var1, var1);
         this.restoreBlocks(var1);
         cursorExactLocations.remove(var1);
         this.internalMenuTeleports.remove(var1.getUniqueId());
         this.activeMenuTransitions.remove(var1.getUniqueId());
         if (requestedSessionToken != null) {
            this.menuSessionTokens.remove(var1.getUniqueId(), requestedSessionToken);
         }
         this.currentPlayerMenus.remove(var1);
         this.playerRuntimeSections.remove(var1);
         this.selectedLayouts.remove(var1);
         this.currentPlayerInputFields.remove(var1.getUniqueId());
         this.userInputData.remove(var1.getUniqueId());
         this.playerLayoutToggleStates.remove(var1.getUniqueId());
         this.playerLayoutClickTimes.remove(var1.getUniqueId());
         this.playerLayoutCooldownUntil.remove(var1.getUniqueId());
          this.playerMenuStates.remove(var1.getUniqueId());
          this.resetCameraEdgePan(var1);
          this.clearDynamicMenuSession(var1);
         Commands.getUserInputData().remove(var1.getUniqueId());
         NPCModule.onMenuClose(var1);
         WASDModule.onMenuClose(var1);
      }, 5L);
   }

   private ArmorStand spawnCursorArmorStand(Location var1) {
      ArmorStand var2 = this.tagMenuEntity((ArmorStand)var1.getWorld().spawn(var1, ArmorStand.class));
      var2.setGravity(false);
      var2.setVisible(false);
      var2.setMarker(true);
      this.setTeleportDurationSafe(var2, 2);
      return var2;
   }

   private Pig spawnPlayerLocStand(Location var1) {
      Pig var2 = this.tagMenuEntity((Pig)var1.getWorld().spawn(var1, Pig.class));
      var2.setGravity(false);
      var2.setAI(false);
      var2.setInvisible(true);
      var2.setCollidable(false);
      var2.setSilent(true);
      return var2;
   }

   private TextDisplay spawnCursorTextDisplay(Player var1, Location var2, String var3, String var4, String var5, double var6, double var8, double var10) {
      Location var12 = var2.clone().add(var6, var8, var10);
      TextDisplay var13 = this.tagMenuEntity((TextDisplay)var2.getWorld().spawn(var12, TextDisplay.class));
      var13.setCustomName(var3 + ":" + var4);
      var13.setCustomNameVisible(false);
      var13.setText(var5);
      var13.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      var13.setDefaultBackground(false);
      var13.setBillboard(Billboard.CENTER);
      var13.setVisibleByDefault(false);
      var1.showEntity(this, var13);
      return var13;
   }

   private ItemDisplay spawnCursorItemDisplay(Player var1, Location var2) {
      ItemDisplay var3 = this.tagMenuEntity((ItemDisplay)var2.getWorld().spawn(var2, ItemDisplay.class));
      ItemStack var4 = new ItemStack(Material.valueOf(cursorItem));
      ItemMeta var5 = var4.getItemMeta();
      var5.setCustomModelData(cursorModelData);
      var4.setItemMeta(var5);
      var3.setItemStack(var4);
      var3.setBillboard(Billboard.CENTER);
      var3.setRotation(var2.getYaw(), var2.getPitch());
      var3.setVisibleByDefault(false);
      Transformation var6 = var3.getTransformation();
      Transformation var7 = new Transformation(
         var6.getTranslation(),
         var6.getLeftRotation(),
         new Vector3f((float)(cursorScale * cursorSizeX), (float)(cursorScale * cursorSizeY), (float)(cursorScale * cursorSizeZ)),
         var6.getRightRotation()
      );
      var3.setTransformation(var7);
      var1.showEntity(this, var3);
      this.setTeleportDurationSafe(var3, 2);
      return var3;
   }

   private ItemDisplay spawnMenuItemDisplay(Player player, World world, Location loc, String menuKey, MenuLayout layout) {
      Material material = Material.matchMaterial(layout.getItemMaterial());
      if (material == null) {
         material = Material.DIAMOND;
         this.getLogger().warning("Invalid item material for menu layout " + menuKey + ":" + layout.key + ": " + layout.getItemMaterial());
      }

      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null && layout.getItemCustomModelData() > 0) {
         meta.setCustomModelData(layout.getItemCustomModelData());
         stack.setItemMeta(meta);
      }

      ItemDisplay display = this.tagMenuEntity((ItemDisplay)world.spawn(loc, ItemDisplay.class));
      display.setCustomName(menuKey + ":" + layout.key + ":item");
      display.setCustomNameVisible(false);
      display.setItemStack(stack);
      display.setGlowing(layout.isItemGlow());
      display.setViewRange(100.0F);
      display.setPersistent(false);
      display.setVisibleByDefault(false);
      display.setBillboard(this.parseBillboard(layout.getItemBillboard()));
      this.setTeleportDurationSafe(display, 2);
      float tiltX = (float)Math.toRadians(layout.tiltX);
      float tiltY = (float)Math.toRadians(layout.tiltY);
      float tiltZ = (float)Math.toRadians(layout.tiltZ);
      Transformation oldTrans = display.getTransformation();
      display.setTransformation(
         new Transformation(
            oldTrans.getTranslation(),
            oldTrans.getLeftRotation().rotationYXZ(tiltY, tiltX, tiltZ),
            new Vector3f((float)layout.getItemScale(), (float)layout.getItemScale(), (float)layout.getItemScale()),
            oldTrans.getRightRotation()
         )
      );
      this.showMenuDisplay(player, display, layout);
      if (layout.isItemRotate()) {
         int taskId = (new BukkitRunnable() {
               public void run() {
                  if (!player.isOnline()
                     || !display.isValid()
                     || !UIcorePlugin.this.playerMenuItemDisplays.getOrDefault(player, Collections.emptyList()).contains(display)) {
                     this.cancel();
                  } else {
                     Transformation tr = display.getTransformation();
                     display.setTransformation(
                        new Transformation(
                           tr.getTranslation(),
                           tr.getLeftRotation().rotateAxis((float)Math.toRadians(layout.getItemRotateSpeed()), 0.0F, 1.0F, 0.0F),
                           tr.getScale(),
                           tr.getRightRotation()
                        )
                     );
                  }
               }
            })
            .runTaskTimer(this, 0L, 1L)
            .getTaskId();
         this.playerMenuItemAnimationTasks.computeIfAbsent(player, var0 -> new CopyOnWriteArrayList<>()).add(taskId);
      }

      return display;
   }

   private TextDisplay spawnMenuItemHitbox(Player player, World world, Location loc, String menuKey, MenuLayout layout) {
      TextDisplay hitbox = this.tagMenuEntity((TextDisplay)world.spawn(loc, TextDisplay.class));
      hitbox.setCustomName(menuKey + ":" + layout.key);
      hitbox.setCustomNameVisible(false);
      hitbox.setText(layout.getHitboxText());
      hitbox.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      hitbox.setDefaultBackground(false);
      hitbox.setShadowed(false);
      hitbox.setBillboard(Billboard.CENTER);
      hitbox.setVisibleByDefault(false);
      hitbox.addScoreboardTag(
         String.format(Locale.US, "uicore_hitbox:%.3f:%.3f:%.3f", layout.getHitboxWidth(), layout.getHitboxHeight(), layout.getHitboxDepth())
      );
      Transformation oldTrans = hitbox.getTransformation();
      hitbox.setTransformation(
         new Transformation(oldTrans.getTranslation(), oldTrans.getLeftRotation(), new Vector3f(1.0F, 1.0F, 1.0F), oldTrans.getRightRotation())
      );
      this.showMenuDisplay(player, hitbox, layout);
      return hitbox;
   }

   private Billboard parseBillboard(String value) {
      if (value == null) {
         return Billboard.CENTER;
      }

      try {
         return Billboard.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var3) {
         return Billboard.CENTER;
      }
   }

   private void sendCameraPacket(Player var1, Entity var2) {
      try {
         WrapperPlayServerCamera var3 = new WrapperPlayServerCamera(var2.getEntityId());
         PacketEvents.getAPI().getPlayerManager().sendPacket(var1, var3);
      } catch (Exception var4) {
         this.getLogger().warning("Failed to send camera packet: " + var4.getMessage());
         var4.printStackTrace();
      }
   }

   private void mountPlayerToVehicle(Player var1, Entity var2) {
   }

   private double calculateCursor(double var1, double var3) {
      return var3 <= 0.0 ? var1 + Math.abs(var3) : var1 - Math.abs(var3);
   }

   private void updateCursorPosition(Player var1, float var2, float var3) {
      String var4 = this.currentPlayerMenus.get(var1);
      if (var4 == null) {
         for (Entry var6 : sectionManager.getAll().entrySet()) {
            if (playerSit.containsKey(var1) && playerSit.get(var1).getWorld().getName().equals(((Section)var6.getValue()).world)) {
               var4 = (String)var6.getKey();
               break;
            }
         }
      }

      if (var4 != null) {
         Section var30 = this.getRuntimeSection(var1, var4);
         ArmorStand var31 = playerCursors.get(var1);
         ItemDisplay var7 = this.playerItemDisplays.get(var1);
         Pig var8 = playerSit.get(var1);
         if (var31 != null && var7 != null && var8 != null) {
            Location var9 = var8.getLocation();
            Vector var10 = var9.getDirection().normalize();
            Vector var11 = new Vector(-var10.getZ(), 0.0, var10.getX()).normalize();
            Vector var12 = var10.getCrossProduct(var11).multiply(-1);
            double var13 = cursorSensitivity;
            double var15 = var2 - var9.getYaw();

            while (var15 > 180.0) {
               var15 -= 360.0;
            }

            while (var15 < -180.0) {
               var15 += 360.0;
            }

            double var17 = var3 - var9.getPitch();
            double var19 = var15 / (90.0 / maxX) * var13 + cursorX;
            double var21 = var17 / (90.0 / maxY) * var13 + cursorY;
            if (cursorMovementRangeEnabled) {
               var19 = Math.max(cursorMovementRangeXMin, Math.min(cursorMovementRangeXMax, var19));
               var21 = Math.max(cursorMovementRangeYMin, Math.min(cursorMovementRangeYMax, var21));
            }

            this.updateCameraEdgePanTarget(var1, var19, var21);

            Vector var23 = var11.multiply(var19).add(var12.multiply(-var21));
            Location var24 = var9.clone().add(var10.multiply(var30.distance + cursorZOffset)).add(var23);
            double var25 = Math.max(0.01, Math.min(1.0, cursorSmoothing));
            Location var27 = var24;
            if (var25 < 1.0 && var31.isValid()) {
               Vector var28 = var31.getLocation().toVector();
               Vector var29 = var24.toVector();
               var28.add(var29.subtract(var28).multiply(var25));
               var27 = var24.clone();
               var27.setX(var28.getX());
               var27.setY(var28.getY());
               var27.setZ(var28.getZ());
            }

            var31.teleport(var27);
            var7.teleport(var27);
            cursorExactLocations.put(var1, var27.clone());
         }
      }
   }

   private void updateCameraEdgePanTarget(Player player, double cursorPlaneX, double cursorPlaneY) {
      if (!cameraEdgePanEnabled || player == null) {
         if (player != null) {
            this.cameraEdgePanTargets.put(player.getUniqueId(), CameraEdgePan.Offset.ZERO);
         }
         return;
      }

      double horizontalRange = Math.max(1.0E-6, Math.abs(maxX * cursorSensitivity));
      double verticalRange = Math.max(1.0E-6, Math.abs(maxY * cursorSensitivity));
      double minX = cursorMovementRangeEnabled ? cursorMovementRangeXMin : cursorX - horizontalRange;
      double maxXValue = cursorMovementRangeEnabled ? cursorMovementRangeXMax : cursorX + horizontalRange;
      double minY = cursorMovementRangeEnabled ? cursorMovementRangeYMin : cursorY - verticalRange;
      double maxYValue = cursorMovementRangeEnabled ? cursorMovementRangeYMax : cursorY + verticalRange;
      CameraEdgePan.Offset target = CameraEdgePan.target(
         cursorPlaneX,
         cursorPlaneY,
         minX,
         maxXValue,
         minY,
         maxYValue,
         cameraEdgePanThreshold,
         cameraEdgePanMaxOffsetX,
         cameraEdgePanMaxOffsetY
      );
      this.cameraEdgePanTargets.put(player.getUniqueId(), target);
   }

   private void tickCameraEdgePan(Player player) {
      if (player == null) {
         return;
      }

      Pig camera = playerSit.get(player);
      if (this.activeMenuTransitions.containsKey(player.getUniqueId())) {
         if (camera != null && camera.isValid()) {
            camera.setVelocity(new Vector());
         }
         return;
      }

      String menuKey = this.currentPlayerMenus.get(player);
      Section section = this.getRuntimeSection(player, menuKey);
      if (camera == null || !camera.isValid() || section == null) {
         this.resetCameraEdgePan(player);
         return;
      }

      UUID playerId = player.getUniqueId();
      CameraEdgePan.Offset target = cameraEdgePanEnabled
         ? this.cameraEdgePanTargets.getOrDefault(playerId, CameraEdgePan.Offset.ZERO)
         : CameraEdgePan.Offset.ZERO;
      CameraEdgePan.Offset current = this.cameraEdgePanOffsets.getOrDefault(playerId, CameraEdgePan.Offset.ZERO);
      CameraEdgePan.Offset next = CameraEdgePan.interpolate(current, target, cameraEdgePanSmoothing);
      this.cameraEdgePanOffsets.put(playerId, next);

      World world = Bukkit.getWorld(section.world);
      if (world == null || !world.equals(camera.getWorld())) {
         return;
      }

      Location base = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
      Vector forward = base.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0, forward.getX());
      if (right.lengthSquared() < 1.0E-8) {
         double yaw = Math.toRadians(section.yaw);
         right = new Vector(-Math.cos(yaw), 0.0, -Math.sin(yaw));
      }
      right.normalize();
      Vector up = forward.getCrossProduct(right).multiply(-1.0).normalize();
      Location desired = base.clone().add(right.multiply(next.right())).add(up.multiply(next.up()));
      Location previous = camera.getLocation();
      Vector delta = desired.toVector().subtract(previous.toVector());
      if (delta.lengthSquared() > 1.0E-12) {
         if (delta.lengthSquared() > 0.25) {
            camera.setVelocity(new Vector());
            this.resetCameraEdgePan(player);
            return;
         }
         camera.setVelocity(delta);
         this.translateCursorWithCamera(player, delta);
      } else {
         camera.setVelocity(new Vector());
      }

      if (next.equals(CameraEdgePan.Offset.ZERO) && target.equals(CameraEdgePan.Offset.ZERO)) {
         this.cameraEdgePanOffsets.remove(playerId);
         this.cameraEdgePanTargets.remove(playerId);
      }
   }

   private void translateCursorWithCamera(Player player, Vector delta) {
      ArmorStand cursorStand = playerCursors.get(player);
      if (cursorStand != null && cursorStand.isValid()) {
         cursorStand.teleport(cursorStand.getLocation().add(delta));
      }

      ItemDisplay cursorDisplay = this.playerItemDisplays.get(player);
      if (cursorDisplay != null && cursorDisplay.isValid()) {
         cursorDisplay.teleport(cursorDisplay.getLocation().add(delta));
      }

      Location exact = cursorExactLocations.get(player);
      if (exact != null) {
         cursorExactLocations.put(player, exact.clone().add(delta));
      }
   }

   private void resetCameraEdgePan(Player player) {
      if (player != null) {
         UUID playerId = player.getUniqueId();
         this.cameraEdgePanTargets.remove(playerId);
         this.cameraEdgePanOffsets.remove(playerId);
      }
   }

   private void beginDynamicMenuSession(Player player) {
      if (player != null) {
         UUID playerId = player.getUniqueId();
         this.menuOpenedAtTicks.put(playerId, this.menuRuntimeTick);
         this.dynamicElementPositions.remove(playerId);
      }
   }

   private void clearDynamicMenuSession(Player player) {
      if (player != null) {
         UUID playerId = player.getUniqueId();
         this.menuOpenedAtTicks.remove(playerId);
         this.dynamicElementPositions.remove(playerId);
      }
   }

   private long getMenuElapsedTicks(Player player) {
      Long openedAt = player == null ? null : this.menuOpenedAtTicks.get(player.getUniqueId());
      return openedAt == null ? 0L : Math.max(0L, this.menuRuntimeTick - openedAt);
   }

   private DynamicPositionSpec.Position resolveLayoutPositionForSpawn(Player player, String menuKey, MenuLayout layout) {
      DynamicPositionSpec spec = layout.getDynamicPositionSpec();
      if (!spec.isDynamic()) {
         return spec.basePosition();
      }

      long elapsedTicks = this.getMenuElapsedTicks(player);
      Map<String, DynamicElementPositionState> byLayout = this.dynamicElementPositions
         .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
      DynamicElementPositionState state = byLayout.get(layout.key);
      if (state == null || state.spec != spec) {
         DynamicPositionSpec.Position initial = spec.basePosition();
         state = new DynamicElementPositionState(spec, initial);
         DynamicPositionSpec.Evaluation evaluation = this.evaluateDynamicPosition(player, menuKey, layout, spec, elapsedTicks, initial, state);
         state.current = evaluation.position();
         state.target = evaluation.position();
         state.lastEvaluationTick = elapsedTicks;
         byLayout.put(layout.key, state);
      }
      return state.current;
   }

   private DynamicPositionSpec.Position getCurrentLayoutPosition(Player player, String menuKey, MenuLayout layout) {
      Map<String, DynamicElementPositionState> byLayout = this.dynamicElementPositions.get(player.getUniqueId());
      DynamicElementPositionState state = byLayout == null ? null : byLayout.get(layout.key);
      return state == null ? this.resolveLayoutPositionForSpawn(player, menuKey, layout) : state.current;
   }

   private void tickDynamicMenuPositions(Player player) {
      if (player == null || this.activeMenuTransitions.containsKey(player.getUniqueId())) {
         return;
      }

      String menuKey = this.currentPlayerMenus.get(player);
      Section section = this.getRuntimeSection(player, menuKey);
      if (menuKey == null || section == null || !this.menuOpenedAtTicks.containsKey(player.getUniqueId())) {
         return;
      }

      long elapsedTicks = this.getMenuElapsedTicks(player);
      Map<String, DynamicElementPositionState> byLayout = this.dynamicElementPositions
         .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
      for (MenuLayout layout : this.getLayoutsForPlayer(player, menuKey, section).values()) {
         DynamicPositionSpec spec = layout.getDynamicPositionSpec();
         if (!spec.isDynamic()) {
            continue;
         }

         DynamicElementPositionState state = byLayout.get(layout.key);
         if (state == null || state.spec != spec) {
            state = new DynamicElementPositionState(spec, spec.basePosition());
            byLayout.put(layout.key, state);
         }
         if (state.lastEvaluationTick == Long.MIN_VALUE || elapsedTicks - state.lastEvaluationTick >= spec.intervalTicks()) {
            DynamicPositionSpec.Evaluation evaluation = this.evaluateDynamicPosition(
               player,
               menuKey,
               layout,
               spec,
               elapsedTicks,
               state.target,
               state
            );
            state.target = evaluation.position();
            state.lastEvaluationTick = elapsedTicks;
         }

         if (elapsedTicks < layout.getDynamicPositionStartTick()) {
            continue;
         }

         DynamicPositionSpec.Position next = state.current.interpolate(state.target, spec.smoothing());
         if (positionDistanceSquared(next, state.target) < 1.0E-12) {
            next = state.target;
         }
         state.current = next;
         Location worldLocation = this.toMenuWorldLocation(section, next);
         if (worldLocation != null) {
            if (!state.movementPrepared) {
               this.prepareDynamicLayoutMovement(player, menuKey, layout.key);
               state.movementPrepared = true;
            }
            this.moveLayoutEntities(player, menuKey, layout.key, worldLocation);
         }
      }
   }

   private DynamicPositionSpec.Evaluation evaluateDynamicPosition(
      Player player,
      String menuKey,
      MenuLayout layout,
      DynamicPositionSpec spec,
      long elapsedTicks,
      DynamicPositionSpec.Position previous,
      DynamicElementPositionState state
   ) {
      DynamicPositionSpec.Evaluation evaluation = spec.evaluate(
         elapsedTicks,
         reference -> this.resolveDynamicPositionValue(player, menuKey, reference),
         previous
      );
      for (String failure : evaluation.failures()) {
         int separator = failure.indexOf(':');
         String failureKey = separator < 0 ? failure : failure.substring(0, separator);
         if (state.loggedFailures.add(failureKey)) {
            this.getLogger().warning(
               "Could not update dynamic position for " + menuKey + ":" + layout.key + " (" + failure + "). Last valid coordinate is kept."
            );
         }
      }
      return evaluation;
   }

   private double resolveDynamicPositionValue(Player player, String menuKey, String reference) {
      String value;
      if (reference.startsWith("{state.") && reference.endsWith("}")) {
         value = this.getPlayerMenuState(player, menuKey, reference.substring(7, reference.length() - 1));
      } else if (reference.startsWith("%") && reference.endsWith("%")) {
         value = parsePlaceholders(player, reference);
      } else {
         throw new IllegalArgumentException("Unsupported dynamic value: " + reference);
      }

      String normalized = ChatColor.stripColor(value == null ? "" : value.trim());
      try {
         double result = Double.parseDouble(normalized);
         if (!Double.isFinite(result)) {
            throw new NumberFormatException("non-finite value");
         }
         return result;
      } catch (NumberFormatException exception) {
         throw new IllegalArgumentException("'" + reference + "' resolved to non-numeric value '" + normalized + "'");
      }
   }

   private Location toMenuWorldLocation(Section section, DynamicPositionSpec.Position position) {
      if (section == null || position == null) {
         return null;
      }
      World world = Bukkit.getWorld(section.world);
      if (world == null) {
         return null;
      }

      Location base = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
      Vector forward = base.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0, forward.getX());
      if (right.lengthSquared() < 1.0E-8) {
         double yaw = Math.toRadians(section.yaw);
         right = new Vector(-Math.cos(yaw), 0.0, -Math.sin(yaw));
      }
      right.normalize();
      Vector up = forward.getCrossProduct(right).multiply(-1.0).normalize();
      return base.add(forward.multiply(position.z())).add(right.multiply(position.x())).add(up.multiply(position.y()));
   }

   private void moveLayoutEntities(Player player, String menuKey, String layoutKey, Location target) {
      String textName = menuKey + ":" + layoutKey;
      List<TextDisplay> textDisplays = this.playerDisplays.get(player);
      if (textDisplays != null) {
         for (TextDisplay display : textDisplays) {
            if (display != null && display.isValid() && textName.equals(display.getCustomName())) {
               display.teleport(target);
            }
         }
      }

      String itemName = textName + ":item";
      List<ItemDisplay> itemDisplays = this.playerMenuItemDisplays.get(player);
      if (itemDisplays != null) {
         for (ItemDisplay display : itemDisplays) {
            if (display != null && display.isValid() && itemName.equals(display.getCustomName())) {
               display.teleport(target);
            }
         }
      }
   }

   private void prepareDynamicLayoutMovement(Player player, String menuKey, String layoutKey) {
      String textName = menuKey + ":" + layoutKey;
      List<TextDisplay> textDisplays = this.playerDisplays.get(player);
      if (textDisplays != null) {
         for (TextDisplay display : textDisplays) {
            if (display != null && display.isValid() && textName.equals(display.getCustomName())) {
               this.setTeleportDurationSafe(display, 1);
            }
         }
      }

      String itemName = textName + ":item";
      List<ItemDisplay> itemDisplays = this.playerMenuItemDisplays.get(player);
      if (itemDisplays != null) {
         for (ItemDisplay display : itemDisplays) {
            if (display != null && display.isValid() && itemName.equals(display.getCustomName())) {
               this.setTeleportDurationSafe(display, 1);
            }
         }
      }
   }

   private static double positionDistanceSquared(DynamicPositionSpec.Position first, DynamicPositionSpec.Position second) {
      double x = first.x() - second.x();
      double y = first.y() - second.y();
      double z = first.z() - second.z();
      return x * x + y * y + z * z;
   }

   private float normalizeYaw(float var1) {
      if (var1 < -90.0F) {
         var1 = -90.0F;
      }

      if (var1 > 90.0F) {
         var1 = 90.0F;
      }

      return var1;
   }

   private float clampPitch(float var1, float var2, float var3) {
      return Math.max(var2, Math.min(var3, var1));
   }

   private boolean isSameLocation(Section var1, Section var2) {
      if (!var1.world.equals(var2.world)) {
         return false;
      } else {
         double var3 = 1.0E-6;
         return var1.cameraX == var2.cameraX
            && var1.cameraY == var2.cameraY
            && var1.cameraZ == var2.cameraZ
            && Math.abs(var1.yaw - var2.yaw) < var3
            && Math.abs(var1.pitch - var2.pitch) < var3;
      }
   }

   private void refreshMenuContent(Player var1, String var2, String var3) {
      this.currentPlayerMenus.put(var1, var2);
      Section var25 = this.getRuntimeSection(var1, var2);
      if (var25 != null) {
         String var26 = this.getCurrentPlayerInputField(var1);
         List<TextDisplay> var27 = this.playerDisplays.get(var1);
         if (var27 != null) {
            this.clearMenuTextAnimationTasks(var1);
            foliaLib.scheduling().entitySpecificScheduler(var1).run(() -> {
               var27.forEach(var0x -> {
                  if (var0x != null && !var0x.isDead()) {
                     var0x.remove();
                  }
               });
               var27.clear();
            }, null);
         }

         this.clearMenuItemDisplays(var1);

         ArrayList var7 = new ArrayList();
         ArrayList<ItemDisplay> menuItemDisplays = new ArrayList<>();
         World var8 = Bukkit.getWorld(var25.world);
         if (var8 != null) {
            Location var9 = new Location(var8, var25.cameraX, var25.cameraY, var25.cameraZ, var25.yaw, var25.pitch);
            Vector var10 = var9.getDirection().normalize();
            Vector var11 = new Vector(-var10.getZ(), 0.0, var10.getX()).normalize();
            Vector var12 = var10.getCrossProduct(var11).multiply(-1);

            for (MenuLayout var14 : this.getLayoutsForPlayer(var1, var2, var25).values()) {
               DynamicPositionSpec.Position layoutPosition = this.resolveLayoutPositionForSpawn(var1, var2, var14);
               Location var16 = this.toMenuWorldLocation(var25, layoutPosition);
               if (var16 == null) {
                  continue;
               }
               if (var14.isItemLayout()) {
                  ItemDisplay itemDisplay = this.spawnMenuItemDisplay(var1, var8, var16, var2, var14);
                  TextDisplay hitboxDisplay = this.spawnMenuItemHitbox(var1, var8, var16, var2, var14);
                  menuItemDisplays.add(itemDisplay);
                  var7.add(hitboxDisplay);
                  continue;
               }

               TextDisplay var17 = this.tagMenuEntity((TextDisplay)var8.spawn(var16, TextDisplay.class));
               String var18 = var14.renderStyledName(var1, false, false);
               String var19 = var18;
               if (var14.isTypewriterEnabled()) {
                  var17.setText(var14.getTypewriterCursorChar());
                  textDisplayManager.getTypewriterEffect()
                     .applyEffect(var17, var19, var14.getTypewriterSpeed(), var14.getTypewriterStartDelay(), var14.getTypewriterCursorChar());
               } else {
                  var17.setText(var18);
               }

               var17.setCustomName(var2 + ":" + var14.key);
               var17.setCustomNameVisible(false);
               var17.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
               var17.setDefaultBackground(false);
               var17.setShadowed(true);
               var17.setBillboard(Billboard.CENTER);
               var17.setVisibleByDefault(false);
               var7.add(var17);
               float var20 = (float)Math.toRadians(var14.tiltX);
               float var21 = (float)Math.toRadians(var14.tiltY);
               float var22 = (float)Math.toRadians(var14.tiltZ);
               Transformation var23 = var17.getTransformation();
               Transformation var24 = new Transformation(
                  var23.getTranslation(),
                  var23.getLeftRotation().rotationYXZ(var21, var20, var22),
                  new Vector3f((float)var14.getTextSize(), (float)var14.getTextSize(), (float)var14.getTextSize()),
                  var23.getRightRotation()
               );
                var17.setTransformation(var24);
                this.showMenuDisplay(var1, var17, var14);
                this.applyMenuTextFloatingAnimation(var1, var17, var14);
            }

            this.playerDisplays.put(var1, var7);
            this.playerMenuItemDisplays.put(var1, menuItemDisplays);
            if (var26 != null) {
               this.setCurrentPlayerInputField(var1, var26);
            }

            textDisplayManager.showTextDisplays(var1, var2);
            if (var25.autoCommandsEnabled && !var25.autoCommands.isEmpty()) {
               for (int var28 = 0; var28 < var25.autoCommands.size(); var28++) {
                  String var29 = var25.autoCommands.get(var28);
                  long var30 = var28 < var25.autoCommandDelays.size() ? var25.autoCommandDelays.get(var28) : 20L;
                  String var31 = parsePlaceholders(var1, var29);
                  CommandUtils.executeCommand(var1, var31, var30);
               }
            }
         }
      }
   }

   private void runLayout(Player var1, String var2) {
      MenuLayout var3 = this.getLayoutForPlayer(var1, var2);
      if (var3 != null) {
         var3.runCommand(var1);
      }
   }

   private void handleCursorClick(Player var1) {
      foliaLib.scheduling()
         .regionSpecificScheduler(var1.getLocation())
         .run(
            var2 -> {
               Location var3 = cursorExactLocations.get(var1);
               if (var3 != null) {
                  String var4 = this.getCurrentPlayerMenu(var1);
                  if (var4 != null) {
                     Section var5 = this.getRuntimeSection(var1, var4);
                     if (var5 != null) {
                        World var6 = Bukkit.getWorld(var5.world);
                        if (var6 != null) {
                           TextDisplay var7 = null;
                           double var8 = Double.MAX_VALUE;

                           for (MenuLayout var11 : this.getLayoutsForPlayer(var1, var4, var5).values()) {
                              Location var12 = new Location(var6, var5.cameraX, var5.cameraY, var5.cameraZ, var5.yaw, var5.pitch);
                              Vector var13 = var12.getDirection().normalize();
                              Vector var14 = new Vector(-var13.getZ(), 0.0, var13.getX()).normalize();
                              Vector var15 = var13.getCrossProduct(var14).multiply(-1);
                              Vector var16 = var13.multiply(var11.z).add(var14.multiply(var11.x)).add(var15.multiply(var11.y));
                              Location var17 = var12.clone().add(var16);
                              String var18 = var4 + ":" + var11.key;
                              TextDisplay var19 = var6.getEntitiesByClass(TextDisplay.class)
                                 .stream()
                                 .filter(var1xx -> var18.equals(var1xx.getCustomName()))
                                 .findFirst()
                                 .orElse(null);
                              if (var19 != null && TextDisplayHitBox.isInside(var19, var3)) {
                                 double var20 = var19.getLocation().distanceSquared(var3);
                                 if (var20 < var8) {
                                    var8 = var20;
                                    var7 = var19;
                                 }
                              }
                           }

                           if (var7 != null) {
                              String var22 = var7.getCustomName();
                              MenuLayout var23 = this.getLayoutForPlayer(var1, var22);
                              if (var23 != null) {
                                 if (this.tryAcquireLayoutClick(var1, var4, var23.key)) {
                                    MenuClickEvent clickEvent = new MenuClickEvent(var1, var4, var23.key, UIcoreApiImpl.snapshot(var4, var23));
                                    Bukkit.getPluginManager().callEvent(clickEvent);
                                    if (!clickEvent.isCancelled()) {
                                       this.selectedLayouts.put(var1, var23);
                                       var23.runCommand(var1);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         );
   }

   private void registerUseEntityPacketListener() {
      PacketEvents.getAPI()
         .getEventManager()
         .registerListener(
            new PacketListenerAbstract(PacketListenerPriority.LOWEST) {
               {
                  Objects.requireNonNull(UIcorePlugin.this);
               }

               public void onPacketReceive(PacketReceiveEvent var1) {
                  if (var1.getPacketType() == Client.PLAYER_ROTATION || var1.getPacketType() == Client.PLAYER_POSITION_AND_ROTATION) {
                     Player var2 = (Player)var1.getPlayer();
                     if (!UIcorePlugin.playerCursors.containsKey(var2)) {
                        return;
                     }

                     float var3 = 0.0F;
                     float var4 = 0.0F;
                     if (var1.getPacketType() == Client.PLAYER_ROTATION) {
                        WrapperPlayClientPlayerRotation var5 = new WrapperPlayClientPlayerRotation(var1);
                        var3 = var5.getYaw();
                        var4 = var5.getPitch();
                     } else {
                        WrapperPlayClientPlayerPositionAndRotation var10 = new WrapperPlayClientPlayerPositionAndRotation(var1);
                        var3 = var10.getYaw();
                        var4 = var10.getPitch();
                     }

                     float var11 = var3;
                     float var6 = var4;
                     UIcorePlugin.foliaLib
                        .scheduling()
                        .entitySpecificScheduler(var2)
                        .run(var4x -> UIcorePlugin.this.updateCursorPosition(var2, var11, var6), null);
                  }

                  if (var1.getPacketType() == Client.INTERACT_ENTITY) {
                     Player var7 = (Player)var1.getPlayer();
                     if (!UIcorePlugin.playerCursors.containsKey(var7)) {
                        return;
                     }

                     var1.setCancelled(true);
                     UIcorePlugin.this.handleCursorClick(var7);
                  }
               }
            }
         );
   }

   public void handleTextDisplayHover(Player var1, TextDisplay var2) {
      String var3 = this.getCurrentPlayerMenu(var1);
      if (var3 != null && var2.getScoreboardTags().contains("hover_enlarge_processed")) {
         MenuLayout var4 = this.getLayoutForPlayer(var1, var2.getName());
         if (var4 != null && var4.isHoverEnlargeEnabled()) {
            Transformation var5 = var2.getTransformation();
            Vector3f var6 = var5.getScale();
            var6.mul((float)var4.getHoverEnlargeScale());
            var2.setTransformation(var5);
            var2.addScoreboardTag("hover_enlarge_processed");
         }
      }
   }

   public void resetTextDisplaySize(Player var1, TextDisplay var2) {
      if (var2.getScoreboardTags().contains("hover_enlarge_processed")) {
         String var3 = this.getCurrentPlayerMenu(var1);
         if (var3 != null) {
            MenuLayout var4 = this.getLayoutForPlayer(var1, var2.getName());
            if (var4 != null && var4.isHoverEnlargeEnabled()) {
               Transformation var5 = var2.getTransformation();
               Vector3f var6 = var5.getScale();
               var6.div((float)var4.getHoverEnlargeScale());
               var2.setTransformation(var5);
               var2.removeScoreboardTag("hover_enlarge_processed");
            }
         }
      }
   }

   private void setTeleportDurationSafe(Entity var1, int var2) {
      try {
         Method var3 = var1.getClass().getMethod("setTeleportDuration", int.class);
         var3.invoke(var1, var2);
      } catch (NoSuchMethodException var4) {
      } catch (Exception var5) {
         this.getLogger().warning("Could not set teleport duration: " + var5.getMessage());
      }
   }

   public void storeHelmet(Player var1) {
      ItemStack var2 = var1.getInventory().getHelmet();
      var1.setMetadata("cursor_original_helmet", new FixedMetadataValue(this, var2));
      ItemStack var3 = new ItemStack(Material.CARVED_PUMPKIN);
      ItemMeta var4 = var3.getItemMeta();
      var4.addEnchant(Enchantment.BINDING_CURSE, 1, true);
      var4.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      var3.setItemMeta(var4);
      Location var5 = var1.getLocation().clone().add(0.0, 1.7, 0.0);
      ItemDisplay var6 = this.tagMenuEntity((ItemDisplay)var5.getWorld().spawn(var5, ItemDisplay.class));
      var6.setItemStack(var3);
      var6.setBillboard(Billboard.FIXED);
      var6.setRotation(var5.getYaw(), var5.getPitch());
      var6.setVisibleByDefault(false);
      var6.setPersistent(false);
      var6.setInvulnerable(true);
      var6.setGravity(false);
      var1.showEntity(this, var6);

      for (Player var8 : Bukkit.getOnlinePlayers()) {
         if (var8 != var1) {
            var8.hideEntity(this, var6);
         }
      }

      this.playerPumpkinDisplays.put(var1, var6);
   }

   private void restoreHelmet(Player var1) {
      ItemDisplay var2 = this.playerPumpkinDisplays.remove(var1);
      if (var2 != null && var2.isValid()) {
         var2.remove();
      }

      if (var1.hasMetadata("cursor_original_helmet")) {
         for (MetadataValue var4 : var1.getMetadata("cursor_original_helmet")) {
            if (var4.getOwningPlugin() == this) {
               ItemStack var5 = (ItemStack)var4.value();
               var1.getInventory().setHelmet(var5);
               break;
            }
         }

         var1.removeMetadata("cursor_original_helmet", this);
      }
   }

   public static String parsePlaceholders(Player var0, String var1) {
      if (var1 == null) {
         return "";
      } else {
         String parsed = hasPAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? PlaceholderAPI.setPlaceholders(var0, var1) : var1;
         return plugin == null ? parsed : plugin.applyInputPlaceholders(var0, parsed, true);
      }
   }

   private void clearPlayerSessionReferences(Player var1) {
      playerLocations.remove(var1);
      cursorExactLocations.remove(var1);
      this.activeMenuTransitions.remove(var1.getUniqueId());
      this.menuSessionTokens.remove(var1.getUniqueId());
      this.currentPlayerMenus.remove(var1);
      this.playerRuntimeSections.remove(var1);
      this.selectedLayouts.remove(var1);
      this.currentPlayerInputFields.remove(var1.getUniqueId());
      this.userInputData.remove(var1.getUniqueId());
      this.playerLayoutToggleStates.remove(var1.getUniqueId());
      this.playerLayoutClickTimes.remove(var1.getUniqueId());
      this.playerLayoutCooldownUntil.remove(var1.getUniqueId());
      this.playerMenuLayoutOverrides.remove(var1.getUniqueId());
      this.playerMenuHiddenLayouts.remove(var1.getUniqueId());
      this.playerMenuStates.remove(var1.getUniqueId());
      this.playerMenuSlotAssignments.remove(var1.getUniqueId());
      this.resetCameraEdgePan(var1);
      this.clearDynamicMenuSession(var1);
      Commands.getUserInputData().remove(var1.getUniqueId());
      playingSound.remove(var1.getName());
   }

   private File getPlayerBlockFile(Player var1) {
      File var2 = new File(this.getDataFolder(), "blockcache");
      if (!var2.exists()) {
         var2.mkdirs();
      }

      return new File(var2, var1.getUniqueId() + ".yml");
   }

   private void clearBlockingBlocks(Player var1) {
      if (cameraBlockCheckEnabled) {
         Location var2 = var1.getLocation();
         World var3 = var2.getWorld();
         File var4 = this.getPlayerBlockFile(var1);
         YamlConfiguration var5 = new YamlConfiguration();
         EnumSet var6 = EnumSet.of(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.ENDER_CHEST,
            Material.BARREL,
            Material.HOPPER,
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.BREWING_STAND,
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
         );
         int var7 = cameraBlockCheckRadius;

         for (int var8 = -var7; var8 <= var7; var8++) {
            for (int var9 = -var7; var9 <= var7; var9++) {
               for (int var10 = -var7; var10 <= var7; var10++) {
                  Location var11 = var2.clone().add(var8, var9, var10);
                  Block var12 = var3.getBlockAt(var11);
                  Material var13 = var12.getType();
                  if (!var13.isAir() && !var6.contains(var13)) {
                     String var14 = var11.getBlockX() + "," + var11.getBlockY() + "," + var11.getBlockZ();
                     var5.set(var14 + ".world", var11.getWorld().getName());
                     var5.set(var14 + ".type", var12.getBlockData().getAsString());
                     var12.setType(Material.AIR, false);
                  }
               }
            }
         }

         try {
            var5.save(var4);
         } catch (IOException var15) {
            this.getLogger().warning("Could not save the block cache: " + var15.getMessage());
         }
      }
   }

   private void restoreBlocks(Player var1) {
      if (cameraBlockCheckEnabled) {
         File var2 = this.getPlayerBlockFile(var1);
         if (var2.exists()) {
            YamlConfiguration var3 = YamlConfiguration.loadConfiguration(var2);

            for (String var5 : var3.getKeys(false)) {
               String[] var6 = var5.split(",");
               if (var6.length == 3) {
                  World var7 = Bukkit.getWorld(var3.getString(var5 + ".world"));
                  if (var7 != null) {
                     int var8 = Integer.parseInt(var6[0]);
                     int var9 = Integer.parseInt(var6[1]);
                     int var10 = Integer.parseInt(var6[2]);
                     BlockData var11 = Bukkit.createBlockData(var3.getString(var5 + ".type"));
                     var7.getBlockAt(var8, var9, var10).setBlockData(var11, false);
                  }
               }
            }

            var2.delete();
         }
      }
   }

   private void saveDefaultMenuFiles() {
      String[] var1 = new String[]{
         "menu/example.yml",
         "menu/demo_main.yml",
         "menu/demo_entrance_npc.yml",
         "menu/demo_player_menu.yml",
         "menu/demo_slots.yml",
         "menu/demo_dynamic_position.yml",
         "menu/mixed_elements_demo.yml"
      };

      for (String var5 : var1) {
         File var6 = new File(this.getDataFolder(), var5);
         if (!var6.exists()) {
            var6.getParentFile().mkdirs();

            try {
               this.saveResource(var5, false);
               this.getLogger().info("Generated default menu file: " + var5);
            } catch (IllegalArgumentException var8) {
            }
         }
      }
   }

   public void sendActionBarMessage(Player var1, String var2) {
      var1.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(var2));
   }

   public static class AttackBreakListener implements Listener {
      @EventHandler
      public void onPlayerAnimation(PlayerAnimationEvent var1) {
         if (var1.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            var1.getPlayer().setMetadata("cursor_is_attacking_or_breaking", new FixedMetadataValue(UIcorePlugin.plugin, true));
            if (UIcorePlugin.playerCursors.containsKey(var1.getPlayer())) {
               var1.setCancelled(true);
               UIcorePlugin.plugin.handleCursorClick(var1.getPlayer());
            }
         }
      }

      @EventHandler
      public void onEntityDamageByEntity(EntityDamageByEntityEvent var1) {
         if (var1.getDamager() instanceof Player var2) {
            var2.setMetadata("cursor_is_attacking_or_breaking", new FixedMetadataValue(UIcorePlugin.plugin, true));
         }
      }

      @EventHandler
      public void onBlockDamage(BlockDamageEvent var1) {
         Player var2 = var1.getPlayer();
         if (var2 != null) {
            var2.setMetadata("cursor_is_attacking_or_breaking", new FixedMetadataValue(UIcorePlugin.plugin, true));
         }
      }

      @EventHandler
      public void onPlayerMove(PlayerMoveEvent var1) {
         Player var2 = var1.getPlayer();
         if (UIcorePlugin.playerCursors.containsKey(var2) && var1.getTo() != null) {
            UIcorePlugin.plugin.updateCursorPosition(var2, var1.getTo().getYaw(), var1.getTo().getPitch());
         }

         if (var2.hasMetadata("cursor_is_attacking_or_breaking")) {
            Bukkit.getScheduler()
               .runTaskLater(UIcorePlugin.plugin, () -> var2.removeMetadata("cursor_is_attacking_or_breaking", UIcorePlugin.plugin), 1L);
         }
      }
   }

   private class PlayerJoinListener implements Listener {
      private PlayerJoinListener() {
         Objects.requireNonNull(UIcorePlugin.this);
         super();
      }

      @EventHandler
      public void onPlayerJoin(PlayerJoinEvent var1) {
         Player var2 = var1.getPlayer();
         if (UIcorePlugin.joinRunBool) {
            long var3 = UIcorePlugin.runDelay * 20L;
            UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(var2).runDelayed(var2x -> {
               if (var2.isOnline()) {
                  if (UIcorePlugin.sectionManager.hasSection(UIcorePlugin.joinRunSection)) {
                     UIcorePlugin.this.setupCursor(var2, UIcorePlugin.joinRunSection);
                  } else {
                     UIcorePlugin.this.getLogger().warning("Configured join menu '" + UIcorePlugin.joinRunSection + "' does not exist.");
                  }

                  for (String var4 : UIcorePlugin.joinRunCommands) {
                     CommandUtils.executeCommand(var2, var4);
                  }
               }
            }, null, var3);
         }
      }
   }

   private class SessionCleanupListener implements Listener {
      private SessionCleanupListener() {
         Objects.requireNonNull(UIcorePlugin.this);
         super();
      }

      @EventHandler(
         priority = EventPriority.HIGHEST
      )
      public void onQuit(PlayerQuitEvent var1) {
         this.cleanup(var1.getPlayer());
      }

      @EventHandler(
         priority = EventPriority.HIGHEST
      )
      public void onKick(PlayerKickEvent var1) {
         if (UIcorePlugin.playerCursors.containsKey(var1.getPlayer()) && this.isSelfInteractKick(var1.getReason())) {
            var1.setCancelled(true);
         } else {
            this.cleanup(var1.getPlayer());
         }
      }

      private boolean isSelfInteractKick(String var1) {
         return var1 != null && var1.toLowerCase(Locale.ROOT).contains("cannot interact with self");
      }

      @EventHandler(
         priority = EventPriority.HIGHEST
      )
      public void onJoin(PlayerJoinEvent var1) {
         Player var2 = var1.getPlayer();

         for (ItemDisplay var4 : UIcorePlugin.this.playerPumpkinDisplays.values()) {
            if (var4 != null && var4.isValid()) {
               var2.hideEntity(UIcorePlugin.this, var4);
            }
         }

         if (var2.hasMetadata("cursor_original_gamemode")) {
            String var6 = ((MetadataValue)var2.getMetadata("cursor_original_gamemode").get(0)).asString();

            try {
               var2.setGameMode(GameMode.valueOf(var6));
            } catch (Exception var5) {
               var2.setGameMode(GameMode.SURVIVAL);
            }

            var2.removeMetadata("cursor_original_gamemode", UIcorePlugin.this);
         } else {
            var2.setGameMode(GameMode.SURVIVAL);
         }
      }

      private void cleanup(Player var1) {
         if (!UIcorePlugin.playerCursors.containsKey(var1)) {
            UIcorePlugin.this.clearPlayerSessionReferences(var1);
         } else {
            Pig var8 = UIcorePlugin.playerSit.remove(var1);
            if (var8 != null) {
               var8.removePassenger(var1);
               var8.remove();
            }

            ArmorStand var9 = UIcorePlugin.playerCursors.remove(var1);
            if (var9 != null) {
               var9.remove();
            }

            List<TextDisplay> var10 = UIcorePlugin.this.playerDisplays.remove(var1);
            if (var10 != null) {
               var10.forEach(Entity::remove);
            }

            ItemDisplay var6 = UIcorePlugin.this.playerItemDisplays.remove(var1);
            if (var6 != null) {
               var6.remove();
            }

            UIcorePlugin.this.clearMenuItemDisplays(var1);

            UIcorePlugin.playingSound.remove(var1.getName());
            var1.stopAllSounds();
            UIcorePlugin.this.restoreHelmet(var1);
            var1.setInvisible(false);
            var1.setCollidable(true);
            if (var1.hasMetadata("cursor_original_gamemode")) {
               String var7 = ((MetadataValue)var1.getMetadata("cursor_original_gamemode").get(0)).asString();
               var1.setGameMode(GameMode.valueOf(var7));
               var1.removeMetadata("cursor_original_gamemode", UIcorePlugin.this);
            } else {
               var1.setGameMode(GameMode.SURVIVAL);
            }

            UIcorePlugin.this.sendCameraPacket(var1, var1);
            UIcorePlugin.playerLocations.remove(var1);
            UIcorePlugin.cursorExactLocations.remove(var1);
            UIcorePlugin.this.currentPlayerMenus.remove(var1);
            UIcorePlugin.this.playerRuntimeSections.remove(var1);
            UIcorePlugin.this.selectedLayouts.remove(var1);
            UIcorePlugin.this.currentPlayerInputFields.remove(var1.getUniqueId());
            UIcorePlugin.this.userInputData.remove(var1.getUniqueId());
            UIcorePlugin.this.playerLayoutToggleStates.remove(var1.getUniqueId());
            UIcorePlugin.this.playerLayoutClickTimes.remove(var1.getUniqueId());
            UIcorePlugin.this.playerLayoutCooldownUntil.remove(var1.getUniqueId());
            UIcorePlugin.this.playerMenuLayoutOverrides.remove(var1.getUniqueId());
            UIcorePlugin.this.playerMenuHiddenLayouts.remove(var1.getUniqueId());
             UIcorePlugin.this.playerMenuStates.remove(var1.getUniqueId());
             UIcorePlugin.this.playerMenuSlotAssignments.remove(var1.getUniqueId());
             UIcorePlugin.this.resetCameraEdgePan(var1);
             UIcorePlugin.this.clearDynamicMenuSession(var1);
         }
      }
   }
}
