package org.orvencraft.uicore;

import org.orvencraft.uicore.animation.AnimationEasing;
import org.orvencraft.uicore.avatar.AvatarDisplayTransform;
import org.orvencraft.uicore.session.MenuSession;
import org.orvencraft.uicore.api.event.MenuInputEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.orvencraft.uicore.camera.CameraEdgePan;
import org.orvencraft.uicore.camera.CursorRotationTracker;
import org.orvencraft.uicore.camera.MenuCameraGeometry;
import org.orvencraft.uicore.camera.MenuCameraFrame;
import org.orvencraft.uicore.camera.MenuCursorInput;
import org.orvencraft.uicore.camera.MenuClientView;
import org.orvencraft.uicore.cutscene.CutsceneManager;
import org.orvencraft.uicore.cutscene.config.CutsceneConfigLoader;
import org.orvencraft.uicore.cutscene.model.CameraPose;
import org.orvencraft.uicore.cutscene.model.CutsceneDefinition;
import org.orvencraft.uicore.api.cutscene.CutsceneAbortReason;
import org.orvencraft.uicore.compat.CraftEngineCompat;
import org.orvencraft.uicore.api.UIcoreApi;
import org.orvencraft.uicore.api.UIcoreApiImpl;
import org.orvencraft.uicore.api.MenuMutationResult;
import org.orvencraft.uicore.api.MenuElementSpec;
import org.orvencraft.uicore.api.model.UiElementModel;
import org.orvencraft.uicore.api.extension.ComponentContext;
import org.orvencraft.uicore.api.event.MenuClickEvent;
import org.orvencraft.uicore.api.event.MenuCloseEvent;
import org.orvencraft.uicore.api.event.MenuDragEvent;
import org.orvencraft.uicore.api.event.MenuDropEvent;
import org.orvencraft.uicore.api.event.MenuOpenEvent;
import org.orvencraft.uicore.api.event.MenuSwitchEvent;
import org.orvencraft.uicore.extension.ExtensionManager;
import org.orvencraft.uicore.extension.ComponentLayoutCache;
import org.orvencraft.uicore.extension.builtin.BuiltInIntegrations;
import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.layout.MenuZoom;
import org.orvencraft.uicore.npc.NPCModule;
import org.orvencraft.uicore.position.DynamicPositionSpec;
import org.orvencraft.uicore.layout.ElementHierarchy;
import org.orvencraft.uicore.scheduler.ChunkLoaderManager;
import org.orvencraft.uicore.schematic.SchematicPreviewManager;
import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.section.SectionManager;
import org.orvencraft.uicore.section.MenuConfigLoader;
import org.orvencraft.uicore.section.MenuSlot;
import org.orvencraft.uicore.util.CommandUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectate;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectateEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.util.Vector3d;
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
import java.util.LinkedHashSet;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.ScheduledTask;

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
   private final Map<Player, List<ScheduledTask>> playerMenuTextAnimationTasks = new ConcurrentHashMap<>();
   private final Map<Player, List<ScheduledTask>> playerMenuItemAnimationTasks = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Boolean>> playerLayoutToggleStates = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Long>> playerLayoutClickTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Long>> playerLayoutCooldownUntil = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Map<String, MenuLayout>>> playerMenuLayoutOverrides = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Set<String>>> playerMenuHiddenLayouts = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Map<String, String>>> playerMenuStates = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Map<String, String>>> playerElementStates = new ConcurrentHashMap<>();
   private SchematicPreviewManager schematicPreviewManager;
   private final Map<UUID, Map<String, Map<String, String>>> playerMenuSlotAssignments = new ConcurrentHashMap<>();
   private final Map<String, Map<String, String>> globalMenuSlotAssignments = new ConcurrentHashMap<>();
   private final Map<Player, Section> playerRuntimeSections = new ConcurrentHashMap<>();
   public static Map<Player, Location> playerLocations = new ConcurrentHashMap<>();
   public static Map<Player, Pig> playerSit = new ConcurrentHashMap<>();
   private final Map<Player, Pig> playerMenuSeats = new ConcurrentHashMap<>();
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
   private static final Pattern EXTENSION_DATA_PATTERN = Pattern.compile("\\{([a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*)\\}");
   private static final Pattern SLOT_STATE_PLACEHOLDER_PATTERN = Pattern.compile("\\{state\\.([A-Za-z0-9_.-]+)\\}");
   public static double maxX;
   public static double maxY;
   public static double cursorScale;
   public static double cursorSensitivity;
   public static double cursorSmoothing;
   public static int runDelay;
   public static boolean hasPAPI;
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
   private ExtensionManager extensionManager;
   private BuiltInIntegrations builtInIntegrations;
   private final ComponentLayoutCache componentLayoutCache = new ComponentLayoutCache();
   private ChunkLoaderManager chunkLoaderManager;
   private Map<Player, String> currentPlayerMenus = new ConcurrentHashMap<>();
   private final org.orvencraft.uicore.session.MenuTeleportTracker internalMenuTeleports = new org.orvencraft.uicore.session.MenuTeleportTracker();
   private final Map<UUID, UUID> activeMenuTransitions = new ConcurrentHashMap<>();
   private final Map<UUID, UUID> menuSessionTokens = new ConcurrentHashMap<>();
   private final Map<UUID, CameraEdgePan.Offset> cameraEdgePanTargets = new ConcurrentHashMap<>();
   private final Map<UUID, CameraEdgePan.Offset> cameraEdgePanOffsets = new ConcurrentHashMap<>();
   private final Map<UUID, CursorRotationTracker> cursorRotationTrackers = new ConcurrentHashMap<>();
   private final Map<String, PlayerProfile> avatarProfileCache = new ConcurrentHashMap<>();
   private final Set<String> avatarProfileLoads = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Long> menuOpenedAtTicks = new ConcurrentHashMap<>();
   private final Set<String> activeSlotTransitions = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Map<String, DynamicElementPositionState>> dynamicElementPositions = new ConcurrentHashMap<>();
   private final Map<UUID, Long> playerMenuTicks = new ConcurrentHashMap<>();
   private final Map<UUID, space.arim.morepaperlib.scheduling.ScheduledTask> menuTickTasks = new ConcurrentHashMap<>();
   private final Map<UUID, MenuSession> menuSessions = new ConcurrentHashMap<>();
   private org.orvencraft.uicore.session.MenuVisibilityManager menuVisibility;
   private final Map<UUID, java.util.concurrent.CompletableFuture<Boolean>> closingSessions = new ConcurrentHashMap<>();
   private final Set<UUID> recoveringSessions = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Long> sessionGenerations = new ConcurrentHashMap<>();
   private final java.util.concurrent.atomic.AtomicLong generationCounter = new java.util.concurrent.atomic.AtomicLong();
   private record RotationInput(long generation, float yaw, float pitch) {}
   private final Map<UUID, RotationInput> pendingRotations = new ConcurrentHashMap<>();
   private final Map<UUID, RotationInput> lastMenuRotations = new ConcurrentHashMap<>();
   private final Map<UUID, Long> pendingClicks = new ConcurrentHashMap<>();
   private final Map<UUID, Location> respawnOrigins = new ConcurrentHashMap<>();
   private final Map<UUID, Set<Location>> clientHiddenBlocks = new ConcurrentHashMap<>();
   private PacketListenerAbstract menuPacketListener;
   private CutsceneManager cutsceneManager;
   private final Map<UUID, CutsceneView> cutsceneViews = new ConcurrentHashMap<>();
   private final Map<UUID, MenuClientView> menuClientViews = new ConcurrentHashMap<>();

   private static final class CutsceneView {
      final UUID lease = UUID.randomUUID();
      final Object menuToken;
      final CutsceneDefinition definition;
      final MenuCursorInput input;
      final double eyeHeight;
      final Vector menuOriginOffset;
      final double cursorDepth;
      volatile MenuCameraFrame frame;
      volatile CameraPose basePose;
      volatile boolean moving, dirty;
      volatile java.util.concurrent.CompletableFuture<Boolean> inFlight;
      Location carrierDestination;
      Runnable afterMove;
      long requestVersion;
      final Map<Entity, String> names = new ConcurrentHashMap<>();
      final Map<TextDisplay, TextDisplayHitBox.Snapshot> hitboxes = new ConcurrentHashMap<>();
      final Map<Entity, Location> locations = new ConcurrentHashMap<>();
      final Map<Entity, Vector> animationPositions = new ConcurrentHashMap<>();
      CutsceneView(Object token, CutsceneDefinition definition, MenuCameraFrame frame, MenuCursorInput input, double eyeHeight, Vector menuOriginOffset, double cursorDepth) {
         this.menuToken = token; this.definition = definition; this.frame = frame; this.input = input; this.eyeHeight = eyeHeight;
         this.basePose = frame.pose();
         this.menuOriginOffset = menuOriginOffset.clone(); this.cursorDepth = cursorDepth;
      }
   }

   public CutsceneManager getCutsceneManager() { return cutsceneManager; }

   public Map<String, CutsceneDefinition> validateCutscenes() {
      var definitions = CutsceneConfigLoader.loadDirectory(new File(getDataFolder(), "cutscenes"));
      validateCutsceneBindings(definitions, sectionManager.validateMenuConfigs().values());
      return definitions;
   }

   private void validateCutsceneBindings(Map<String, CutsceneDefinition> definitions, Iterable<Section> menus) {
      for (Section menu : menus) {
         if (!menu.cutsceneId.isBlank() && !definitions.containsKey(menu.cutsceneId))
            throw new IllegalArgumentException("Menu '" + menu.key + "' cutscene.id: unknown scenario '" + menu.cutsceneId + "'");
         CutsceneDefinition scene = definitions.get(menu.cutsceneId);
         if (scene != null) for (var step : scene.steps().values()) if (step instanceof CutsceneDefinition.Choice choice)
            for (var option : choice.options().values()) if (!menu.contains(option.element()) && !menu.hasSlot(option.element()))
               throw new IllegalArgumentException("Scenario '" + scene.id() + "' step '" + step.id() + "' options.element: menu '" + menu.key + "' has no '" + option.element() + "'");
      }
   }

   public boolean reloadCutscenes() {
      try {
         var definitions = validateCutscenes();
         cutsceneManager.replaceDefinitions(definitions);
         return true;
      } catch (RuntimeException exception) {
         getLogger().warning("Cutscene configuration rejected; current snapshots retained: " + exception.getMessage());
         return false;
      }
   }

   private final class CutsceneHost implements CutsceneManager.Host {
      public Object menuToken(Player player) {
         if (isBlockDiggingMenu(player)) return null;
         MenuSession session = menuSessions.get(player.getUniqueId());
         return session != null && session.stage() == MenuSession.Stage.ACTIVE ? menuSessionTokens.get(player.getUniqueId()) : null;
      }
      public CameraPose camera(Player player) {
         CutsceneView view = cutsceneViews.get(player.getUniqueId());
         if (view != null) return view.frame.pose();
         Pig camera = playerSit.get(player);
         Location eye = camera != null && Bukkit.isOwnedByCurrentRegion(camera) ? camera.getEyeLocation() : player.getEyeLocation();
         return new CameraPose(eye.getWorld().getName(), eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch());
      }
      public void execute(Player player, Runnable work) {
         if (Bukkit.isOwnedByCurrentRegion(player)) work.run();
         else if (isEnabled()) foliaLib.scheduling().entitySpecificScheduler(player).run(work, null);
      }
      public java.util.concurrent.CompletionStage<Boolean> prepare(Player player, CutsceneDefinition definition, CameraPose initialPose) {
         CutsceneView previous = cutsceneViews.get(player.getUniqueId());
         if (previous != null && previous.moving && previous.inFlight != null) {
            var waiting = new java.util.concurrent.CompletableFuture<Boolean>();
            UUID request = cutsceneManager.state(player).map(state -> state.sessionId()).orElse(null);
            previous.inFlight.whenComplete((ok, failure) -> execute(player, () -> {
               if (failure != null || menuToken(player) == null || !cutsceneManager.active(player)
                  || !java.util.Objects.equals(request, cutsceneManager.state(player).map(state -> state.sessionId()).orElse(null))) { waiting.complete(false); return; }
               prepare(player, definition, initialPose).whenComplete((ready, error) -> {
                  if (error != null) waiting.completeExceptionally(error); else waiting.complete(ready);
               });
            }));
            return waiting;
         }
         CameraPose currentPose = camera(player);
         cutsceneViews.remove(player.getUniqueId());
         if (previous != null && chunkLoaderManager != null) chunkLoaderManager.releaseRoute(previous.lease);
         Pig camera = playerSit.get(player);
         if (camera == null || previous == null && !Bukkit.isOwnedByCurrentRegion(camera)) return java.util.concurrent.CompletableFuture.completedFuture(false);
         World world = Bukkit.getWorld(initialPose.world());
         if (world == null || world != player.getWorld()) return java.util.concurrent.CompletableFuture.completedFuture(false);
         MenuCameraFrame frame = new MenuCameraFrame(currentPose);
         Section section = getRuntimeSection(player, getCurrentPlayerMenu(player));
         Vector menuOriginOffset = frame.worldToLocal(new Location(world, section.cameraX, section.cameraY, section.cameraZ));
         Location cursor = cursorExactLocations.get(player);
         Vector local = cursor == null ? new Vector(cursorX, -cursorY, section.distance + cursorZOffset) : frame.worldToLocal(cursor).subtract(menuOriginOffset);
         RotationInput rotation = pendingRotations.getOrDefault(player.getUniqueId(), lastMenuRotations.get(player.getUniqueId()));
         var input = new MenuCursorInput(previous != null ? previous.input.rawYaw() : rotation == null ? player.getLocation().getYaw() : rotation.yaw(),
            previous != null ? previous.input.rawPitch() : rotation == null ? player.getLocation().getPitch() : rotation.pitch(), local.getX(), -local.getY());
         var view = new CutsceneView(menuToken(player), definition, frame, input,
            previous == null ? camera.getEyeHeight() : previous.eyeHeight, menuOriginOffset, local.getZ());
         if (previous != null) {
            view.names.putAll(previous.names); view.locations.putAll(previous.locations); view.hitboxes.putAll(previous.hitboxes);
            view.animationPositions.putAll(previous.animationPositions);
         }
         cutsceneViews.put(player.getUniqueId(), view);
         MenuClientView clientView = menuClientViews.get(player.getUniqueId());
         if (clientView != null) {
            var ids = new ArrayList<Integer>();
            playerDisplays.getOrDefault(player, List.of()).forEach(display -> ids.add(display.getEntityId()));
            playerMenuItemDisplays.getOrDefault(player, List.of()).forEach(display -> ids.add(display.getEntityId()));
            ItemDisplay cursorItem = playerItemDisplays.get(player);
            if (cursorItem != null) ids.add(cursorItem.getEntityId());
            clientView.beginFrames(ids);
         }
         for (TextDisplay display : playerDisplays.getOrDefault(player, List.of())) if (Bukkit.isOwnedByCurrentRegion(display)) {
            if (display.getCustomName() != null) view.names.put(display, display.getCustomName());
            view.hitboxes.put(display, TextDisplayHitBox.snapshot(display));
            view.locations.put(display, display.getLocation());
         }
         resetCameraEdgePan(player);
         // A route lease is bounded, shared with ordinary menu tickets, and never edits blocks.
         Set<Long> chunks = new java.util.HashSet<>();
         var points = new ArrayList<>(definition.points().values());
         points.add(initialPose);
         double radius = Math.max(4, getRuntimeSection(player, getCurrentPlayerMenu(player)).distance + 4);
         for (CameraPose point : points) addRouteChunks(chunks, point.x(), point.z(), radius);
         long preparedSegments = 0;
         for (var step : definition.steps().values()) if (step instanceof CutsceneDefinition.Move move) {
            var route = new ArrayList<CameraPose>();
            // A current-origin move can be entered from any branch endpoint.
            var origins = move.from() == null ? points : List.of(definition.points().get(move.from()));
            for (CameraPose origin : origins) {
               preparedSegments += move.to().size();
               if (preparedSegments > 100000) throw new IllegalArgumentException("Scenario '" + definition.id() + "' steps: route preparation exceeds 100000 segments");
               route.clear(); route.add(origin);
               for (String id : move.to()) route.add(definition.points().get(id));
               var path = org.orvencraft.uicore.cutscene.model.MoveMotion.preparePath(definition, move, origin);
               int samples = Math.min(65536, Math.max(1, (int)Math.ceil(path.length() / 4)));
               for (int sample = 0; sample <= samples; sample++) {
                  CameraPose point = path.sample((double)sample / samples);
                  addRouteChunks(chunks, point.x(), point.z(), radius);
               }
            }
         }
         return chunkLoaderManager.prepareRoute(view.lease, world, chunks).thenApply(ready ->
            ready && cutsceneViews.get(player.getUniqueId()) == view && view.menuToken == menuToken(player));
      }
      public java.util.concurrent.CompletionStage<Boolean> apply(Player player, CameraPose pose) { return applyCutsceneFrame(player, pose); }
      public boolean eligible(Player player, String elementId) {
         MenuLayout layout = getLayoutForPlayer(player, getCurrentPlayerMenu(player), elementId);
         return layout != null && layout.isVisible(player) && layout.isInteractionStillAllowed(player);
      }
      public void actions(Player player, List<String> actions) {
         Object token = menuToken(player);
         for (String action : actions) {
            if (token == null || token != menuToken(player)) break;
            executeMenuAction(player, action);
         }
      }
      public void event(org.bukkit.event.Event event) {
         if (event instanceof org.orvencraft.uicore.api.event.CutsceneAbortEvent abort && !abort.getDiagnostic().isBlank())
            getLogger().warning("Cutscene '" + abort.getSnapshot().id() + "' step '" + abort.getSnapshot().stepId() + "': " + abort.getReason() + ": " + abort.getDiagnostic());
         Bukkit.getPluginManager().callEvent(event);
      }
      public void finished(Player player, boolean closeMenu) {
         CutsceneView view = cutsceneViews.get(player.getUniqueId());
         if (view != null) {
            view.requestVersion++;
            view.basePose = view.frame.pose();
            resetCameraEdgePan(player);
         }
         if (view != null && chunkLoaderManager != null) chunkLoaderManager.releaseRoute(view.lease);
         if (closeMenu) stopCursor(player, true);
      }
   }

   private static void addRouteChunks(Set<Long> chunks, double x, double z, double radius) {
      for (int cx = (int)Math.floor((x-radius)/16); cx <= (int)Math.floor((x+radius)/16); cx++)
         for (int cz = (int)Math.floor((z-radius)/16); cz <= (int)Math.floor((z+radius)/16); cz++) {
            chunks.add(((long)cx << 32) | (cz & 0xffffffffL));
            if (chunks.size() > 4096) throw new IllegalArgumentException("Route preparation exceeds 4096 chunks; split the scenario");
         }
   }

   private void releaseCutsceneView(Player player) {
      CutsceneView view = cutsceneViews.remove(player.getUniqueId());
      if (view != null && chunkLoaderManager != null) chunkLoaderManager.releaseRoute(view.lease);
      MenuClientView clientView = menuClientViews.get(player.getUniqueId());
      if (clientView != null) clientView.endFrames();
   }

   public boolean isCutsceneUiFrozen(Player player) {
      return cutsceneManager != null && cutsceneManager.policy(player).map(policy -> policy.freezeUi()).orElse(false)
         && cutsceneManager.state(player).map(state -> state.state() == org.orvencraft.uicore.api.cutscene.CutsceneState.PAUSED
            || state.state() == org.orvencraft.uicore.api.cutscene.CutsceneState.WAITING_CHOICE).orElse(false);
   }
   public boolean isCutsceneCameraMoving(Player player) {
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      return view != null && view.moving;
   }
   public boolean isMenuDisplayUiFrozen(TextDisplay display) {
      for (var entry : playerDisplays.entrySet()) if (entry.getValue().contains(display)) return isCutsceneUiFrozen(entry.getKey());
      return false;
   }

   public boolean isCutsceneCarrierTeleport(Player player, Location destination) {
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      if (view == null || !view.moving || view.carrierDestination == null || destination == null) return false;
      Location carrier = view.carrierDestination;
      // Passenger positioning adds the carrier's riding height to the requested entity position.
      return carrier.getWorld().equals(destination.getWorld()) && Math.abs(carrier.getX()-destination.getX()) < .01
         && Math.abs(carrier.getZ()-destination.getZ()) < .01 && Math.abs(carrier.getY()-destination.getY()) < 2;
   }

   private java.util.concurrent.CompletableFuture<Boolean> applyCutsceneFrame(Player player, CameraPose pose) {
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      if (view == null || view.menuToken != menuSessionTokens.get(player.getUniqueId()))
         return java.util.concurrent.CompletableFuture.completedFuture(false);
      if (view.moving && view.inFlight != null) {
         long version = view.requestVersion;
         var queued = new java.util.concurrent.CompletableFuture<Boolean>();
         view.inFlight.whenComplete((ok, failure) -> new CutsceneHost().execute(player, () -> {
            if (failure != null || !Boolean.TRUE.equals(ok) || cutsceneViews.get(player.getUniqueId()) != view || view.requestVersion != version) {
               queued.complete(false); return;
            }
            applyCutsceneFrame(player, pose).whenComplete((ready, error) -> {
               if (error == null) queued.complete(ready); else queued.completeExceptionally(error);
            });
         }));
         return queued;
      }
      World world = Bukkit.getWorld(pose.world());
      if (world == null) return java.util.concurrent.CompletableFuture.completedFuture(false);
      view.moving = true; view.dirty = false;
      var result = new java.util.concurrent.CompletableFuture<Boolean>();
      view.inFlight = result;
      CameraEdgePan.Offset pan = CameraEdgePan.Offset.ZERO;
      if (cutsceneManager.active(player) && view.definition.policy().edgePan()) {
         updateCameraEdgePanTarget(player, view.input.x(), view.input.y());
         pan = CameraEdgePan.interpolate(cameraEdgePanOffsets.get(player.getUniqueId()), cameraEdgePanTargets.get(player.getUniqueId()), cameraEdgePanSmoothing);
      }
      cameraEdgePanOffsets.put(player.getUniqueId(), pan);
      MenuCameraFrame base = new MenuCameraFrame(pose);
      Location origin = base.localToWorld(world, pan.right(), pan.up(), 0);
      MenuCameraFrame frame = new MenuCameraFrame(new CameraPose(pose.world(), origin.getX(), origin.getY(), origin.getZ(), pose.yaw(), pose.pitch()));
      Section section = getRuntimeSection(player, getCurrentPlayerMenu(player));
      Map<String, MenuLayout> layouts = getLayoutsForPlayer(player, getCurrentPlayerMenu(player), section);
      Map<String, Location> positions = new HashMap<>();
      for (MenuLayout layout : layouts.values()) {
         var position = getCurrentLayoutPosition(player, getCurrentPlayerMenu(player), layout);
         positions.put(layout.key, layout.isWorldAnchored() ? new Location(world, position.x(), position.y(), position.z())
            : frame.localToWorld(world, position.x() + view.menuOriginOffset.getX(), position.y() + view.menuOriginOffset.getY(), position.z() + view.menuOriginOffset.getZ()));
      }
      Location cursor = frame.localToWorld(world, view.input.x() + view.menuOriginOffset.getX(), -view.input.y() + view.menuOriginOffset.getY(), view.cursorDepth + view.menuOriginOffset.getZ());
      var operations = new ArrayList<java.util.concurrent.CompletableFuture<Boolean>>();
      var entities = new ArrayList<Entity>();
      entities.addAll(playerDisplays.getOrDefault(player, List.of()));
      entities.addAll(playerMenuItemDisplays.getOrDefault(player, List.of()));
      Entity camera = playerSit.get(player), seat = playerMenuSeats.get(player);
      view.carrierDestination = MenuCameraGeometry.seatLocation(frame.origin(world).subtract(0, view.eyeHeight, 0));
      if (camera != null) entities.add(camera);
      if (seat != null) entities.add(seat);
      Entity cursorStand = playerCursors.get(player), cursorDisplay = playerItemDisplays.get(player);
      if (cursorStand != null) entities.add(cursorStand);
      if (cursorDisplay != null) entities.add(cursorDisplay);
      Map<Entity, Location> committed = new ConcurrentHashMap<>();
      Map<Long, List<Runnable>> regionalBatches = new HashMap<>();
      for (Entity entity : entities) {
         MenuClientView clientView = menuClientViews.get(player.getUniqueId());
         if (clientView != null && entity instanceof Display) clientView.track(entity.getEntityId());
         var operation = new java.util.concurrent.CompletableFuture<Boolean>(); operations.add(operation);
         Runnable move = () -> {
            if (cutsceneViews.get(player.getUniqueId()) != view || !entity.isValid()) { operation.complete(false); return; }
            Location target;
            if (entity == camera) target = frame.origin(world).subtract(0, view.eyeHeight, 0);
            else if (entity == seat) target = MenuCameraGeometry.seatLocation(frame.origin(world).subtract(0, view.eyeHeight, 0));
            else if (entity == cursorStand || entity == cursorDisplay) target = cursor.clone();
            else {
               String name = entity.getCustomName();
               if (name == null) { operation.complete(true); return; }
               view.names.put(entity, name);
               String key = name.substring(name.indexOf(':') + 1);
               if (key.endsWith(":item")) key = key.substring(0, key.length()-5);
               target = positions.get(key);
               if (target == null) { operation.complete(true); return; }
               Vector animated = view.animationPositions.get(entity);
               MenuLayout layout = layouts.get(key);
               if (animated != null) target = layout != null && layout.isWorldAnchored()
                  ? new Location(world, animated.getX(), animated.getY(), animated.getZ())
                  : frame.localToWorld(world, animated.getX() + view.menuOriginOffset.getX(), animated.getY() + view.menuOriginOffset.getY(), animated.getZ() + view.menuOriginOffset.getZ());
               if (entity instanceof TextDisplay text) view.hitboxes.put(text, TextDisplayHitBox.snapshot(text));
            }
            if (entity instanceof org.bukkit.entity.Display display) display.setTeleportDuration(1);
            Location destination = target;
            try {
               Location current = entity.getLocation();
               if (current.getWorld().equals(destination.getWorld()) && current.distanceSquared(destination) < 1e-14
                  && current.getYaw() == destination.getYaw() && current.getPitch() == destination.getPitch()) {
                  committed.put(entity, destination); operation.complete(true); return;
               }
               java.util.concurrent.CompletableFuture<Boolean> moved;
               if (entity == seat) moved = entity.teleportAsync(destination,
                  org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN, io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS);
               else moved = entity.teleportAsync(destination);
               moved.whenComplete((success, error) -> {
                  if (Boolean.TRUE.equals(success)) committed.put(entity, destination);
                  if (error == null) operation.complete(Boolean.TRUE.equals(success)); else operation.completeExceptionally(error);
               });
            } catch (RuntimeException exception) { operation.completeExceptionally(exception); }
         };
         if (Bukkit.isOwnedByCurrentRegion(entity)) move.run();
         else {
            Location source = view.locations.get(entity);
            if (source == null) foliaLib.scheduling().entitySpecificScheduler(entity).run(move, () -> operation.complete(false));
            else {
               long chunk = ((long)(source.getBlockX() >> 4) << 32) | ((source.getBlockZ() >> 4) & 0xffffffffL);
               regionalBatches.computeIfAbsent(chunk, ignored -> new ArrayList<>()).add(() -> {
                  if (Bukkit.isOwnedByCurrentRegion(entity)) move.run();
                  else foliaLib.scheduling().entitySpecificScheduler(entity).run(move, () -> operation.complete(false));
               });
            }
         }
      }
      for (var batch : regionalBatches.entrySet()) {
         long key = batch.getKey();
         Location source = new Location(world, (int)(key >> 32) * 16.0, 0, (int)key * 16.0);
         foliaLib.scheduling().regionSpecificScheduler(source).run(() -> batch.getValue().forEach(Runnable::run));
      }
      java.util.concurrent.CompletableFuture.allOf(operations.toArray(java.util.concurrent.CompletableFuture[]::new)).whenComplete((ignored, error) ->
         new CutsceneHost().execute(player, () -> {
            view.moving = false;
            if (cutsceneViews.get(player.getUniqueId()) != view || view.menuToken != menuSessionTokens.get(player.getUniqueId())) { result.complete(false); return; }
            if (error != null || operations.stream().anyMatch(future -> !Boolean.TRUE.equals(future.getNow(false)))) {
               result.complete(false); return;
            }
            view.frame = frame; view.basePose = pose; view.locations.putAll(committed);
            cursorExactLocations.put(player, cursor.clone());
            MenuClientView clientView = menuClientViews.get(player.getUniqueId());
            if (clientView != null) {
               Map<Integer, CameraPose> visualPositions = new HashMap<>();
               committed.forEach((entity, location) -> {
                  if (entity instanceof Display) visualPositions.put(entity.getEntityId(), cameraPose(location));
               });
               clientView.render(frame.pose(), visualPositions);
            }
            // Runtime sections are private per viewer; canonical configuration is never mutated.
            Section runtime = getRuntimeSection(player, getCurrentPlayerMenu(player));
            Location anchor = frame.localToWorld(world, view.menuOriginOffset.getX(), view.menuOriginOffset.getY(), view.menuOriginOffset.getZ());
            playerRuntimeSections.put(player, runtime.copyWithCamera(pose.world(), anchor.getX(), anchor.getY(), anchor.getZ(), (float)pose.yaw(), (float)pose.pitch()));
            Runnable after = view.afterMove; view.afterMove = null;
            result.complete(true);
            if (after != null && cutsceneViews.get(player.getUniqueId()) == view
               && view.menuToken == menuSessionTokens.get(player.getUniqueId()) && !closingSessions.containsKey(player.getUniqueId())) after.run();
         }));
      return result;
   }

   public void executeMenuAction(Player player, String action) {
      executeMenuAction(player, action, "", Map.of());
   }

   public void executeMenuAction(Player player, String action, String elementId, Map<String, Object> arguments) {
      String menu = getCurrentPlayerMenu(player);
      if (menu == null) return;
      if (action.startsWith("[state] ")) {
         String[] parts = action.substring(8).trim().split("\\s+", 2);
         if (parts.length == 2) { setPlayerMenuState(player, menu, parts[0], parts[1]); refreshCurrentMenu(player); }
      } else if (action.startsWith("[element-state] ")) {
         String[] parts = action.substring(16).trim().split("\\s+", 2);
         if (parts.length == 2) applyElementStateChanges(player, menu, Map.of(parts[0], parts[1]));
      } else if (action.startsWith("[action] ")) {
         String id = action.substring(9).trim();
         if (!extensionManager.executeAction(id, player, menu, elementId, arguments)) throw new IllegalArgumentException("Unknown or failed action: " + id);
      } else if (action.startsWith("[cutscene] ")) {
         String[] parts = action.substring(11).trim().split("\\s+", 2);
         switch (parts[0]) {
            case "start" -> { if (parts.length == 2) cutsceneManager.start(player, parts[1]); }
            case "pause" -> cutsceneManager.pause(player);
            case "resume" -> cutsceneManager.resume(player);
            case "skip" -> cutsceneManager.skip(player);
            case "stop" -> cutsceneManager.stop(player);
            case "choose" -> { if (parts.length == 2) cutsceneManager.choose(player, parts[1]); }
            default -> throw new IllegalArgumentException("Unknown cutscene action: " + parts[0]);
         }
      } else CommandUtils.executeCommand(player, action);
   }
   public Map<Player, MenuLayout> selectedLayouts = new ConcurrentHashMap<>();
   private Map<UUID, String> currentPlayerInputFields = new ConcurrentHashMap<>();
   private Map<UUID, Map<String, String>> userInputData = new ConcurrentHashMap<>();
   private YamlConfiguration langConfig;
   private static final String MENU_ENTITY_TAG = "uicore_runtime";

   private static final class MenuViewSnapshot {
      private final Pig cameraMount;
      private final Pig playerSeat;
      private final ArmorStand cursorStand;
      private final List<TextDisplay> textDisplays;
      private final ItemDisplay cursorDisplay;
      private final List<ItemDisplay> menuItemDisplays;

      private MenuViewSnapshot(
         Pig cameraMount,
         Pig playerSeat,
         ArmorStand cursorStand,
         List<TextDisplay> textDisplays,
         ItemDisplay cursorDisplay,
         List<ItemDisplay> menuItemDisplays
      ) {
         this.cameraMount = cameraMount;
         this.playerSeat = playerSeat;
         this.cursorStand = cursorStand;
         this.textDisplays = textDisplays;
         this.cursorDisplay = cursorDisplay;
         this.menuItemDisplays = menuItemDisplays;
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
      Set<TextDisplay> matches = new LinkedHashSet<>();
      List<TextDisplay> displays = this.playerDisplays.get(player);
      if (displays != null) {
         for (TextDisplay display : displays) {
            if (display != null && (cutsceneViews.containsKey(player.getUniqueId()) || !Bukkit.isOwnedByCurrentRegion(display) || display.isValid())
               && displayName.equals(this.trackedMenuEntityName(player, display))) {
               matches.add(display);
            }
         }
      }

      return new ArrayList<>(matches);
   }

   String trackedMenuEntityName(Player player, Entity entity) {
      if (entity == null) return null;
      CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
      String name = view == null ? null : view.names.get(entity);
      return name != null ? name : Bukkit.isOwnedByCurrentRegion(entity) ? entity.getCustomName() : null;
   }

   public List<Player> getActiveMenuPlayers() { return new ArrayList<>(this.currentPlayerMenus.keySet()); }

   public List<Location> getMenuDisplayLocations(Player player) {
      List<Location> locations = new ArrayList<>();
      for (TextDisplay display : this.playerDisplays.getOrDefault(player, List.of())) {
         Location location = display == null ? null : this.getMenuDisplayLocation(player, display);
         if (location != null) locations.add(location);
      }
      for (ItemDisplay display : this.playerMenuItemDisplays.getOrDefault(player, List.of())) {
         Location location = display == null ? null : this.getMenuDisplayLocation(player, display);
         if (location != null) locations.add(location);
      }
      return locations;
   }

   public String getCurrentPlayerMenu(Player var1) {
      return this.currentPlayerMenus.get(var1);
   }

   public Section getRuntimeSection(Player player, String menuKey) {
      if (player != null && menuKey != null && menuKey.equals(this.currentPlayerMenus.get(player))) {
         Section runtime = this.playerRuntimeSections.get(player);
         if (runtime != null) {
            Section canonical = sectionManager.get(menuKey);
            if (canonical != null) runtime.syncDefinitionFrom(canonical);
            return runtime;
         }
      }

      return menuKey == null ? null : sectionManager.get(menuKey);
   }

   boolean consumeInternalMenuTeleport(Player player, Location destination) {
      return player != null && this.internalMenuTeleports.consume(player.getUniqueId(), destination);
   }

   java.util.concurrent.CompletableFuture<Boolean> teleportMenuInternalAsync(Player player, Location location) {
      if (player == null || location == null || location.getWorld() == null) {
         return java.util.concurrent.CompletableFuture.completedFuture(false);
      }
      UUID token = this.internalMenuTeleports.begin(player.getUniqueId(), location);
      try {
         return player.teleportAsync(location).whenComplete((result, error) -> this.internalMenuTeleports.complete(player.getUniqueId(), token));
      } catch (RuntimeException exception) {
         this.internalMenuTeleports.complete(player.getUniqueId(), token);
         throw exception;
      }
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

   public ExtensionManager getExtensionManager() {
      return this.extensionManager;
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

      long now = this.playerMenuTicks.getOrDefault(player.getUniqueId(), 0L);
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
         if (!menuKey.equals(this.getCurrentPlayerMenu(player))) return;
         var rendered = layout.renderStyledComponent(player, false, false);
         long generation = this.getMenuSessionGeneration(player);
         for (TextDisplay display : this.getTextDisplaysByKeys(player, menuKey, layout.key)) {
            org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
               if (!display.isValid() || this.getMenuSessionGeneration(player) != generation
                  || !menuKey.equals(this.getCurrentPlayerMenu(player))) return;
               display.text(rendered);
               CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
               if (view != null && view.menuToken == this.menuSessionTokens.get(player.getUniqueId())) {
                  view.hitboxes.put(display, TextDisplayHitBox.snapshot(display));
                  view.dirty = true;
               }
            });
         }
      }
   }

   public void updateLayoutDisplayText(Player player, String menuKey, MenuLayout layout, boolean hovered, boolean pressed) {
      if (player != null && menuKey != null && layout != null && !layout.isItemLayout()) {
         var rendered = layout.renderStyledComponent(player, hovered, pressed);
         long generation = getMenuSessionGeneration(player);
         for (TextDisplay display : this.getTextDisplaysByKeys(player, menuKey, layout.key)) {
            org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
               if (isMenuSessionCurrent(player, generation) && display.isValid()) display.text(rendered);
            });
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
      if (previous != null && now == previous) {
         return false;
      }

      times.put(key, now);
      return true;
   }

   public void refreshCurrentMenu(Player player) {
      if (player == null || !player.isOnline()) return;
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         foliaLib.scheduling().entitySpecificScheduler(player).run(() -> this.refreshCurrentMenu(player), null);
         return;
      }
      if (this.extensionManager != null) this.extensionManager.invalidateComponents(player);
      if (playerCursors.containsKey(player)) {
         String menuKey = this.getCurrentPlayerMenu(player);
         if (menuKey != null && sectionManager.get(menuKey) != null) {
            this.reconcileMenuContent(player, menuKey);
         }
      }
   }

   public boolean refreshMenuElement(Player player, String menuKey, String elementId) {
      return this.refreshMenuElement(player, menuKey, elementId, false);
   }

   public boolean replaceMenuElement(Player player, String menuKey, String elementId) {
      return this.refreshMenuElement(player, menuKey, elementId, true);
   }

   public Map<String, MenuLayout> getLayoutsForPlayer(Player player, String menuKey, Section section) {
      return getLayoutsForPlayer(player, menuKey, section, true);
   }

   private Map<String, MenuLayout> getLayoutsForPlayer(Player player, String menuKey, Section section, boolean visibleOnly) {
      LinkedHashMap<String, MenuLayout> layouts = new LinkedHashMap<>();
      if (section != null) {
         Section.Definition definition = section.definition();
         layouts.putAll(definition.layouts());
         for (MenuSlot slot : definition.slots().values()) {
            String templateId = this.resolveMenuSlotTemplate(player, menuKey, definition, slot);
            if (templateId.isEmpty()) {
               layouts.put(slot.id(), slot.materializeEmpty(menuKey, section.world));
            } else {
               MenuLayout materialized = section.materializeSlot(menuKey, slot.id(), templateId, definition);
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
         if (visibleOnly && hidden != null) {
            for (String elementId : hidden) {
               layouts.remove(elementId);
            }
         }

         Map<String, Map<String, MenuLayout>> overridesByMenu = this.playerMenuLayoutOverrides.get(playerId);
         Map<String, MenuLayout> overrides = overridesByMenu == null ? null : overridesByMenu.get(menuKey);
         if (overrides != null) {
            layouts.putAll(overrides);
         }

         if (!visibleOnly) return Collections.unmodifiableMap(layouts);
         layouts.entrySet().removeIf(var0 -> !var0.getValue().isVisible(player));
         LinkedHashMap<String, MenuLayout> expanded = new LinkedHashMap<>();
         int[] componentCalls = {0};
         Set<String> activeComponentChildren = new LinkedHashSet<>();
         for (Map.Entry<String, MenuLayout> entry : layouts.entrySet()) {
            this.expandComponentLayout(player, menuKey, section, entry.getKey(), entry.getValue(), expanded,
               0, componentCalls, activeComponentChildren);
         }
         this.componentLayoutCache.retain(playerId, menuKey, activeComponentChildren);
         layouts = expanded;
         Map<String, MenuLayout> hierarchySnapshot = new LinkedHashMap<>(layouts);
         layouts.entrySet().removeIf(entry -> !ElementHierarchy.hasAncestors(entry.getKey(), hierarchySnapshot));
      }

      return Collections.unmodifiableMap(layouts);
   }

   private void expandComponentLayout(Player player, String menuKey, Section section, String elementId,
                                      MenuLayout layout, Map<String, MenuLayout> result, int depth, int[] componentCalls,
                                      Set<String> activeComponentChildren) {
      String type = layout.getLayoutType();
      if (type == null || !type.matches("[a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*")) {
         result.put(elementId, layout);
         return;
      }
      if (!layout.hasPermission(player)) return;
      if (depth >= 4 || result.size() >= 256 || componentCalls[0]++ >= 256) {
         this.getLogger().warning("Component nesting or element limit reached at " + menuKey + ":" + elementId);
         return;
      }
      UiElementModel source = layout.sourceModel();
      for (String axis : List.of("x", "y", "z")) {
         if (source.contains(axis) && !(source.get(axis) instanceof Number)) {
            this.getLogger().warning("Component position must be numeric at " + menuKey + ":" + elementId);
            return;
         }
      }
      Map<String, Object> parameters = new LinkedHashMap<>();
      for (Map.Entry<String, Object> parameter : source.properties().entrySet()) {
         String key = parameter.getKey();
         Object value = parameter.getValue();
         if (!Set.of("type", "x", "y", "z", "name", "permission", "visible-if").contains(key)
            && (value instanceof String || value instanceof Number || value instanceof Boolean)) parameters.put(key, value);
      }
      List<MenuElementSpec> children = this.extensionManager.createComponent(type,
         new ComponentContext(player, menuKey, elementId, parameters)).orElse(List.of());
      Set<String> localIds = new LinkedHashSet<>();
      for (MenuElementSpec child : children) {
         if (result.size() >= 256) break;
         if (!child.id().matches("[a-zA-Z0-9_-]+") || !localIds.add(child.id())) {
            this.getLogger().warning("Invalid or duplicate component child id at " + menuKey + ":" + elementId);
            continue;
         }
         String childId = elementId + "/" + child.id();
         try {
            UiElementModel model = child.model();
            for (String axis : List.of("x", "y", "z")) {
               Object local = model.get(axis);
               if (!(local instanceof Number number)) throw new IllegalArgumentException("Component child position must be numeric");
               double origin = switch (axis) { case "x" -> layout.x; case "y" -> layout.y; default -> layout.z; };
               model = model.with(axis, origin + number.doubleValue());
            }
            UiElementModel childModel = model;
            MenuLayout materialized = this.componentLayoutCache.getOrCreate(player.getUniqueId(), menuKey,
               childId, section.world, childModel,
               () -> MenuConfigLoader.loadLayout(menuKey, childId, childModel, section.world));
            if (materialized.isVisible(player)) {
               activeComponentChildren.add(childId);
               this.expandComponentLayout(player, menuKey, section, childId, materialized, result,
                  depth + 1, componentCalls, activeComponentChildren);
            }
         } catch (RuntimeException exception) {
            this.getLogger().warning("Could not build component child " + menuKey + ":" + childId
               + " (" + exception.getClass().getSimpleName() + ")");
         }
      }
   }

   public String getResolvedMenuSlotTemplate(Player player, String menuKey, String slotId) {
      Section section = sectionManager.get(menuKey);
      if (section == null) {
         return "";
      }

      Section.Definition definition = section.definition();
      MenuSlot slot = definition.slots().get(slotId);
      return slot == null ? "" : this.resolveMenuSlotTemplate(player, menuKey, definition, slot);
   }

   private String resolveMenuSlotTemplate(Player player, String menuKey, Section.Definition definition, MenuSlot slot) {
      if (slot == null) return "";
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
      if (definition.templates().containsKey(resolved)) {
         return resolved;
      }

      return definition.templates().containsKey(slot.fallback()) ? slot.fallback() : "";
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

      Map<String, MenuLayout> previous = getLayoutsForPlayer(player, menuKey, getRuntimeSection(player, menuKey), false);
      Map<String, MenuLayout> candidate = new LinkedHashMap<>(previous);
      candidate.put(elementId, layout);
      ElementHierarchy.validateMutation(previous, candidate);
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
      this.activeSlotTransitions.removeIf(key -> key.startsWith(var1.getUniqueId() + ":"));
      List<ScheduledTask> var2 = this.playerMenuTextAnimationTasks.remove(var1);
      if (var2 != null) {
         var2.forEach(ScheduledTask::cancel);
      }
   }

   private void clearMenuItemAnimationTasks(Player var1) {
      this.activeSlotTransitions.removeIf(key -> key.startsWith(var1.getUniqueId() + ":"));
      List<ScheduledTask> var2 = this.playerMenuItemAnimationTasks.remove(var1);
      if (var2 != null) {
         var2.forEach(ScheduledTask::cancel);
      }
   }

   private void clearMenuItemDisplays(Player var1) {
      this.clearMenuItemAnimationTasks(var1);
      List<ItemDisplay> var2 = this.playerMenuItemDisplays.remove(var1);
      if (var2 != null) {
         var2.forEach(this::removeMenuEntity);
         var2.clear();
      }
   }

   private MenuViewSnapshot captureMenuView(Player player) {
      if (player == null) {
         return null;
      }

      return new MenuViewSnapshot(
         playerSit.get(player),
         this.playerMenuSeats.get(player),
         playerCursors.get(player),
         this.playerDisplays.get(player),
         this.playerItemDisplays.get(player),
         this.playerMenuItemDisplays.get(player)
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
         if (Bukkit.isOwnedByCurrentRegion(snapshot.cameraMount) && snapshot.cameraMount.getPassengers().contains(player)) {
            snapshot.cameraMount.removePassenger(player);
         }
      }

      if (snapshot.cursorStand != null) {
         playerCursors.remove(player, snapshot.cursorStand);
      }

      if (snapshot.playerSeat != null) {
         this.playerMenuSeats.remove(player, snapshot.playerSeat);
         if (Bukkit.isOwnedByCurrentRegion(snapshot.playerSeat) && snapshot.playerSeat.getPassengers().contains(player)) {
            snapshot.playerSeat.removePassenger(player);
         }
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

   private boolean isNearMenuCameraForMount(Player player, Location cameraLocation) {
      if (player == null || cameraLocation == null || !player.getWorld().equals(cameraLocation.getWorld())) return false;
      Location actual = player.getLocation();
      double dx = actual.getX() - cameraLocation.getX();
      double dz = actual.getZ() - cameraLocation.getZ();
      double dy = actual.getY() - cameraLocation.getY();
      // The player can fall while the asynchronous teleport settles, before the camera mount is ready.
      return dx * dx + dz * dz <= 1.0 && dy >= -3.0 && dy <= 1.0;
   }

   private void abortMenuTransition(Player player, String menuKey, UUID transitionToken, MenuViewSnapshot previousView, String reason) {
      if (player == null || !Objects.equals(transitionToken, this.activeMenuTransitions.get(player.getUniqueId()))) return;
      this.getLogger().warning("Could not open menu '" + menuKey + "': " + reason);
      this.cleanupMenuViewSnapshot(player, previousView);
      this.stopCursor(player, true);
   }

   private void storeOriginalGameMode(Player player) {
      if (!this.menuSessions.containsKey(player.getUniqueId())) throw new IllegalStateException("Menu session must be prepared before changing GameMode");
   }

   private void cleanupMenuViewSnapshot(Player player, MenuViewSnapshot snapshot) {
      if (player == null || snapshot == null) {
         return;
      }

      if (snapshot.cameraMount != null) {
         playerSit.remove(player, snapshot.cameraMount);
         if (Bukkit.isOwnedByCurrentRegion(snapshot.cameraMount) && snapshot.cameraMount.getPassengers().contains(player)) {
            snapshot.cameraMount.removePassenger(player);
         }
         this.closeSessionStep(() -> this.removeMenuEntity(snapshot.cameraMount));
      }

      if (snapshot.cursorStand != null) {
         playerCursors.remove(player, snapshot.cursorStand);
         this.closeSessionStep(() -> this.removeMenuEntity(snapshot.cursorStand));
      }

      if (snapshot.playerSeat != null) {
         this.playerMenuSeats.remove(player, snapshot.playerSeat);
         if (Bukkit.isOwnedByCurrentRegion(snapshot.playerSeat) && snapshot.playerSeat.getPassengers().contains(player)) {
            snapshot.playerSeat.removePassenger(player);
         }
         this.closeSessionStep(() -> this.removeMenuEntity(snapshot.playerSeat));
      }

      if (snapshot.textDisplays != null) {
         this.playerDisplays.remove(player, snapshot.textDisplays);
         snapshot.textDisplays.forEach(entity -> this.closeSessionStep(() -> this.removeMenuEntity(entity)));
         snapshot.textDisplays.clear();
      }

      if (snapshot.cursorDisplay != null) {
         this.playerItemDisplays.remove(player, snapshot.cursorDisplay);
         this.closeSessionStep(() -> this.removeMenuEntity(snapshot.cursorDisplay));
      }

      if (snapshot.menuItemDisplays != null) {
         this.playerMenuItemDisplays.remove(player, snapshot.menuItemDisplays);
         snapshot.menuItemDisplays.forEach(entity -> this.closeSessionStep(() -> this.removeMenuEntity(entity)));
         snapshot.menuItemDisplays.clear();
      }

   }

   private void removeMenuEntity(Entity entity) {
      if (entity == null) return;
      menuClientViews.values().forEach(view -> view.forget(entity.getEntityId()));
      if (!Bukkit.isOwnedByCurrentRegion(entity)) {
         org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, entity, () -> this.removeMenuEntity(entity));
         return;
      }
      if (entity.isDead()) return;
      try { if (entity instanceof TextDisplay display && textDisplayManager != null) textDisplayManager.getTypewriterEffect().cancelEffect(display); }
      finally { entity.remove(); }
   }

   <T extends Entity> T tagMenuEntity(T entity) {
      if (entity != null) {
         entity.addScoreboardTag(MENU_ENTITY_TAG);
         entity.getPersistentDataContainer().set(new NamespacedKey(this, "runtime_owner"), PersistentDataType.BYTE, (byte) 1);
         entity.setPersistent(false);
      }

      return entity;
   }

   public String getPlayerElementStateOverride(Player player, String menuKey, String elementId) {
      if (player == null || menuKey == null || elementId == null) return "";
      Map<String, Map<String, String>> byMenu = this.playerElementStates.get(player.getUniqueId());
      Map<String, String> states = byMenu == null ? null : byMenu.get(menuKey);
      return states == null ? "" : states.getOrDefault(elementId, "");
   }

   public void applyElementStateChanges(Player player, String menuKey, Map<String, String> changes) {
      if (player == null || menuKey == null || menuKey.isBlank() || changes == null || changes.isEmpty()) return;
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         foliaLib.scheduling().entitySpecificScheduler(player).run(() -> this.applyElementStateChanges(player, menuKey, changes), null);
         return;
      }
      if (!menuKey.equals(this.getCurrentPlayerMenu(player))) return;
      Section section = this.getRuntimeSection(player, menuKey);
      if (section == null) return;

      Map<String, String> states = this.playerElementStates
         .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
         .computeIfAbsent(menuKey, ignored -> new ConcurrentHashMap<>());
      Section.Definition definition = section.definition();
      boolean changed = false;
      for (Map.Entry<String, String> change : changes.entrySet()) {
         String elementId = change.getKey();
         String state = change.getValue();
         MenuLayout target = definition.layouts().get(elementId);
         if (target == null) {
            Map<String, Map<String, MenuLayout>> byMenu = this.playerMenuLayoutOverrides.get(player.getUniqueId());
            Map<String, MenuLayout> overrides = byMenu == null ? null : byMenu.get(menuKey);
            target = overrides == null ? null : overrides.get(elementId);
         }
         if (target == null && definition.slots().containsKey(elementId)) {
            String templateId = this.resolveMenuSlotTemplate(player, menuKey, definition, definition.slots().get(elementId));
            target = section.materializeSlot(menuKey, elementId, templateId, definition);
         }
         if (target == null || !target.hasElementState(state)) {
            this.getLogger().warning("Unknown element state " + menuKey + ":" + elementId + "=" + state);
            continue;
         }
         if (!state.equals(states.put(elementId, state))) changed = true;
      }
      if (changed) {
         foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(ignored -> {
            if (menuKey.equals(this.getCurrentPlayerMenu(player))) this.refreshCurrentMenu(player);
         }, null, 1L);
      }
   }

   static void moveMenuEntity(Entity entity, Location target) {
      if (foliaLib != null && foliaLib.scheduling().isUsingFolia()) {
         entity.teleportAsync(target);
      } else {
         entity.teleport(target);
      }
   }

   public boolean isMenuHiddenFromOtherPlayers(Player player) {
      return this.menuVisibility == null || this.menuVisibility.settings(player.getUniqueId()).hideMenuForOtherPlayer();
   }

   public boolean blocksIncomingPlayerDamage(Player player) {
      return this.menuVisibility != null && this.menuVisibility.blocksPlayerDamage(player.getUniqueId());
   }

   public void refreshMenuPlayerVisibility() {
      if (this.menuVisibility != null) this.menuVisibility.refresh();
   }

   /** Called on the menu entity's owning region, including configured temporary text. */
   public void configureMenuEntityVisibility(Player owner, Entity entity) {
      entity.setVisibleByDefault(!this.isMenuHiddenFromOtherPlayers(owner));
   }

   private void applyMenuPrivacy(Player player, Section section) {
      if (this.menuVisibility == null) return;
      this.menuVisibility.open(player, section.privacy);
      ItemDisplay cursor = this.playerItemDisplays.get(player);
      if (cursor != null) org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, cursor, () -> {
         if (cursor.isValid()) this.configureMenuEntityVisibility(player, cursor);
      });
   }

   private void showMenuDisplay(Player player, Display display, MenuLayout layout) {
      this.showMenuDisplay(player, display, layout, display.getLocation().clone(), false);
   }

   private void showMenuDisplay(Player player, Display display, MenuLayout layout, Location target, boolean stagedAtEntrance) {
      if (player == null || display == null || layout == null) {
         return;
      }
      MenuClientView clientView = menuClientViews.get(player.getUniqueId());
      if (clientView != null) clientView.track(display.getEntityId());
      if (display instanceof ItemDisplay || !layout.isItemLayout()) {
      }
      if (!layout.isEntranceAnimationEnabled()) {
         this.showDisplayToPlayer(player, display);
         return;
      }

      Vector offset = this.animationOffset(target, layout.getEntranceAnimationFrom(), layout.getEntranceAnimationDistance());
      Location entranceLocation = target.clone().add(offset);
      if (!Bukkit.isOwnedByCurrentRegion(entranceLocation)) {
         this.showDisplayToPlayer(player, display);
         return;
      }
      if (!stagedAtEntrance) moveMenuEntity(display, entranceLocation);

      Map<Player, List<ScheduledTask>> tasks = display instanceof ItemDisplay ? this.playerMenuItemAnimationTasks : this.playerMenuTextAnimationTasks;
      ScheduledTask entrance = foliaLib.scheduling().entitySpecificScheduler(display).runDelayed(() -> {
         if (!player.isOnline() || !display.isValid()) return;
         int duration = layout.getEntranceAnimationDuration();
         if (duration <= 0) {
            moveMenuEntity(display, target);
            this.showDisplayToPlayer(player, display);
         } else if (!"linear".equals(layout.getEntranceAnimationEasing())) {
            this.animateDisplay(player, display, display.getLocation().clone(), target, duration, layout.getEntranceAnimationEasing(), tasks, null);
         } else {
            this.setTeleportDurationSafe(display, duration);
            this.showDisplayToPlayer(player, display);
            ScheduledTask movement = foliaLib.scheduling().entitySpecificScheduler(display).runDelayed(() -> {
               if (display.isValid()) moveMenuEntity(display, target);
            }, null, 1L);
            if (movement != null) tasks.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(movement);
         }
      }, null, Math.max(1L, layout.getEntranceAnimationDelay()));
      if (entrance != null) tasks.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(entrance);
   }

   private void showDisplayToPlayer(Player player, Display display) {
      this.configureMenuEntityVisibility(player, display);
      foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
         if (player.isOnline() && display.isValid()) player.showEntity(this, display);
      }, null);
   }

   private Vector animationOffset(Location origin, String direction, double distance) {
      Vector forward = origin.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0, forward.getX()).normalize();
      Vector up = forward.getCrossProduct(right).multiply(-1);
      return switch (direction == null ? "top" : direction.toLowerCase(Locale.ROOT)) {
         case "bottom" -> up.multiply(-distance);
         case "left" -> right.multiply(-distance);
         case "right" -> right.multiply(distance);
         case "front" -> forward.multiply(-distance);
         case "back" -> forward.multiply(distance);
         default -> up.multiply(distance);
      };
   }

   private void animateDisplay(
      Player player,
      Display display,
      Location start,
      Location target,
      int duration,
      String easing,
      Map<Player, List<ScheduledTask>> tasks,
      Runnable completion
   ) {
      if (!Bukkit.isOwnedByCurrentRegion(target)) {
         if (completion != null) foliaLib.scheduling().entitySpecificScheduler(player).run(completion, null);
         return;
      }
      this.setTeleportDurationSafe(display, 1);
      this.showDisplayToPlayer(player, display);
      Vector delta = target.toVector().subtract(start.toVector());
      Section animationSection = getRuntimeSection(player, getCurrentPlayerMenu(player));
      MenuCameraFrame animationFrame = animationSection == null ? null : new MenuCameraFrame(new CameraPose(animationSection.world,
         animationSection.cameraX, animationSection.cameraY, animationSection.cameraZ, animationSection.yaw, animationSection.pitch));
      Vector localStart = animationFrame == null ? start.toVector() : animationFrame.worldToLocal(start);
      Vector localTarget = animationFrame == null ? target.toVector() : animationFrame.worldToLocal(target);
      CutsceneView startingView = cutsceneViews.get(player.getUniqueId());
      String animationName = startingView == null ? (Bukkit.isOwnedByCurrentRegion(display) ? display.getCustomName() : null) : startingView.names.get(display);
      if (animationName != null && animationName.endsWith(":item")) animationName = animationName.substring(0, animationName.length()-5);
      MenuLayout animationLayout = animationName == null ? null : getLayoutForPlayer(player, animationName);
      boolean worldAnimation = animationLayout != null && animationLayout.isWorldAnchored();
      final int[] elapsed = {0};
      final ScheduledTask[] animation = {null};
      animation[0] = foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(() -> {
            if (isCutsceneUiFrozen(player)) return;
            if (!player.isOnline() || !display.isValid()) {
               if (animation[0] != null) animation[0].cancel();
               if (completion != null) foliaLib.scheduling().entitySpecificScheduler(player).run(completion, null);
               return;
            }

            elapsed[0]++;
            double progress = Math.min(1.0, (double)elapsed[0] / Math.max(1, duration));
            double eased = AnimationEasing.apply(easing, progress);
            CutsceneView view = cutsceneViews.get(player.getUniqueId());
            if (view != null) {
               Vector animated = worldAnimation ? start.toVector().add(delta.clone().multiply(eased))
                  : localStart.clone().add(localTarget.clone().subtract(localStart).multiply(eased));
               view.animationPositions.put(display, animated); view.dirty = true;
            } else moveMenuEntity(display, start.clone().add(delta.clone().multiply(eased)));
            if (elapsed[0] >= duration) {
               if (view == null) moveMenuEntity(display, target);
               if (animation[0] != null) animation[0].cancel();
               if (completion != null) foliaLib.scheduling().entitySpecificScheduler(player).run(completion, null);
            }
      }, null, 1L, 1L);
      if (animation[0] != null) tasks.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(animation[0]);
   }

   public MenuMutationResult transitionPlayerMenuSlot(
      Player player,
      String menuId,
      String slotId,
      String templateId,
      String exitDirection,
      int exitDuration,
      double exitDistance,
      String exitEasing
   ) {
      if (player == null || !player.isOnline()) {
         return MenuMutationResult.failure("Player is not online");
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         foliaLib.scheduling().entitySpecificScheduler(player).run(() ->
            this.transitionPlayerMenuSlot(player, menuId, slotId, templateId, exitDirection, exitDuration, exitDistance, exitEasing), null);
         return MenuMutationResult.success("Slot transition queued for player region", 0);
      }
      Section section = sectionManager.get(menuId);
      if (section == null || !section.hasSlot(slotId)) {
         return MenuMutationResult.failure("Unknown slot: " + menuId + ":" + slotId);
      }
      if (!section.hasTemplate(templateId)) {
         return MenuMutationResult.failure("Unknown template: " + menuId + ":" + templateId);
      }
      if (!menuId.equals(this.getCurrentPlayerMenu(player))) {
         return MenuMutationResult.failure("Player does not have menu '" + menuId + "' open");
      }

      String transitionKey = player.getUniqueId() + ":" + menuId + ":" + slotId;
      if (!this.activeSlotTransitions.add(transitionKey)) {
         return MenuMutationResult.failure("Slot transition is already running");
      }

      long slotGeneration = this.getMenuSessionGeneration(player);
      Runnable replaceSlot = () -> {
         if (!this.isMenuSessionCurrent(player, slotGeneration)) return;
         this.activeSlotTransitions.remove(transitionKey);
         if (this.isMenuSessionCurrent(player, slotGeneration) && menuId.equals(this.getCurrentPlayerMenu(player))) {
            this.setPlayerMenuSlot(player, menuId, slotId, templateId);
            this.refreshMenuElement(player, menuId, slotId, true);
         }
      };
      String displayName = menuId + ":" + slotId + ":item";
      ItemDisplay current = this.playerMenuItemDisplays.getOrDefault(player, List.of()).stream()
         .filter(display -> display != null && display.isValid() && displayName.equals(display.getCustomName()))
         .findFirst()
         .orElse(null);
      if (current == null || exitDuration <= 0 || exitDistance <= 0.0) {
         replaceSlot.run();
         return MenuMutationResult.success("Slot changed without an outgoing animation", 1);
      }

      Location start = current.getLocation().clone();
      Location target = start.clone().add(this.animationOffset(start, exitDirection, exitDistance));
      this.animateDisplay(
         player,
         current,
         start,
         target,
         Math.min(59, Math.max(1, exitDuration)),
         exitEasing,
         this.playerMenuItemAnimationTasks,
         replaceSlot
      );
      return MenuMutationResult.success("Slot transition started", 0);
   }

   private void purgeOrphanedMenuEntities() {
      Set<String> scheduledChunks = ConcurrentHashMap.newKeySet();
      NamespacedKey ownerKey = new NamespacedKey(this, "runtime_owner");
      for (Section section : sectionManager.getAll().values()) {
          World world = Bukkit.getWorld(section.world);
          if (world == null) continue;
          Location cameraLocation = new Location(world, section.cameraX, section.cameraY, section.cameraZ);
          int chunkX = cameraLocation.getBlockX() >> 4;
          int chunkZ = cameraLocation.getBlockZ() >> 4;
          for (int x = chunkX - 1; x <= chunkX + 1; x++) {
             for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                final int scanX = x;
                final int scanZ = z;
                if (!scheduledChunks.add(world.getUID() + ":" + scanX + ":" + scanZ)) continue;
                Location chunkCenter = new Location(world, scanX * 16 + 8, cameraLocation.getY(), scanZ * 16 + 8);
                foliaLib.scheduling().regionSpecificScheduler(chunkCenter).run(() -> {
                   if (!world.isChunkLoaded(scanX, scanZ)) return;
                   for (Entity entity : world.getChunkAt(scanX, scanZ).getEntities()) {
                      if (entity.getPersistentDataContainer().has(ownerKey, PersistentDataType.BYTE)) entity.remove();
                   }
                });
             }
          }
      }
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
               final float initialZoom = appliedDisplayZoom(var2);
               final Vector3f var19 = new Vector3f(var2.getTransformation().getTranslation()).div(initialZoom);
               final Vector3f var20 = new Vector3f(var2.getTransformation().getScale()).div(initialZoom);
               final byte var21 = var2.getTextOpacity();
               final long[] tick = {0L};
               final float[] angle = {0.0F};
               final ScheduledTask[] floating = {null};
               floating[0] = foliaLib.scheduling().entitySpecificScheduler(var2).runAtFixedRate(() -> {
                        if (isCutsceneUiFrozen(var1)) return;
                        if (var1.isOnline()
                           && var2.isValid()
                           && UIcorePlugin.this.playerDisplays.getOrDefault(var1, Collections.emptyList()).contains(var2)) {
                            tick[0]++;
                            double wave = Math.sin(tick[0] * var5 * 0.05);
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
                           float zoom = appliedDisplayZoom(var2);
                           translation.mul(zoom);
                           scale.mul(zoom);

                           if (var12) {
                               angle[0] = (float)(angle[0] + var5 * 0.05);
                              var2.setTransformation(
                                 new Transformation(
                                    translation,
                                     current.getLeftRotation().rotationYXZ(var16 + angle[0], var17, var18),
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

                            if (floating[0] != null) floating[0].cancel();
                         }
                   }, null, 1L, 1L);
               if (floating[0] != null) this.playerMenuTextAnimationTasks.computeIfAbsent(var1, var0 -> new CopyOnWriteArrayList<>()).add(floating[0]);
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
      if (var1 == null) return Map.of();
      return Map.copyOf(this.userInputData.getOrDefault(var1.getUniqueId(), Map.of()));
   }

   public void setPlayerInputData(Player var1, String var2, String var3) {
      if (var1 != null && var2 != null && !var2.isBlank()) {
         UUID var4 = var1.getUniqueId();
         Map<String, String> inputs = this.userInputData.computeIfAbsent(var4, var0 -> new ConcurrentHashMap<>());
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
      this.playerMenuSeats.values().forEach(seat -> {
         if (seat != null && !seat.isDead()) seat.remove();
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
      this.playerMenuItemAnimationTasks.values().forEach(var0 -> var0.forEach(ScheduledTask::cancel));
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

   private void mergeYamlFile(String resource) {
      File destination = new File(this.getDataFolder(), resource);
      if (!destination.exists()) { this.saveResource(resource, false); return; }
      try (java.io.InputStream defaultsStream = this.getResource(resource)) {
         if (defaultsStream == null) throw new IOException("Bundled resource missing");
         YamlConfiguration defaults = new YamlConfiguration();
         defaults.load(new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
         YamlConfiguration existing = new YamlConfiguration();
         existing.load(destination);
         for (String key : defaults.getKeys(true)) if (!existing.contains(key)) existing.set(key, defaults.get(key));
         if (defaults.contains("version")) existing.set("version", defaults.get("version"));
         java.nio.file.Path temp = java.nio.file.Files.createTempFile(destination.toPath().getParent(), "uicore-", ".tmp");
         try {
            existing.save(temp.toFile());
            try { java.nio.file.Files.move(temp, destination.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { java.nio.file.Files.move(temp, destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
         } finally { java.nio.file.Files.deleteIfExists(temp); }
      } catch (Exception exception) {
         throw new IllegalStateException("Could not merge " + resource + "; original file preserved", exception);
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
      try {
      this.saveDefaultConfig();
      this.mergeYamlFile("config.yml");
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
      foliaLib = new MorePaperLib(this);
      this.schematicPreviewManager = new SchematicPreviewManager(this);
       for (Player player : Bukkit.getOnlinePlayers()) {
          foliaLib.scheduling().entitySpecificScheduler(player).run(() -> this.recoverSavedSession(player), null);
       }
      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         hasPAPI = true;
      }

      this.reloadLangConfig();
      this.loadConfig();
      if (!new File(getDataFolder(), "cutscenes/demo.yml").exists()) saveResource("cutscenes/demo.yml", false);
      this.cutsceneManager = new CutsceneManager(new CutsceneHost());
      if (!this.reloadCutscenes()) throw new IllegalStateException("Invalid cutscene configuration");
      this.menuVisibility = new org.orvencraft.uicore.session.MenuVisibilityManager(this, Bukkit::getOnlinePlayers,
         (viewer, work) -> org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, viewer, work));
      org.orvencraft.uicore.session.LegacyBlockCacheRecovery.recoverAll(this).whenComplete((result, failure) -> {
         if (failure != null) this.getLogger().warning("Legacy blockcache recovery incomplete; journals retained: " + failure.getMessage());
      });
      this.purgeOrphanedMenuEntities();
      this.menuApi = new UIcoreApiImpl(this);
      this.extensionManager = new ExtensionManager(this);
      this.getServer().getPluginManager().registerEvents(this.extensionManager, this);
      Bukkit.getServicesManager().register(UIcoreApi.class, this.menuApi, this, ServicePriority.Normal);
      this.builtInIntegrations = new BuiltInIntegrations(this, this.extensionManager);
      this.builtInIntegrations.start();
      Bukkit.getGlobalRegionScheduler().runDelayed(this,
         task -> this.extensionManager.validateMenuReferences(sectionManager.getAll()), 1L);
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
      NPCModule.initialize(this);
      CraftEngineCompat.init();
      this.getLogger().info("====================================");
      this.getLogger().info("UIcore enabled");
      this.getLogger().info("Version: " + this.getDescription().getVersion());
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

      // Menu state is owned by each player's entity region, not by a server-wide timer.
      } catch (RuntimeException exception) {
         this.getLogger().log(java.util.logging.Level.SEVERE, "UIcore initialization failed; disabling and releasing owned resources", exception);
         Bukkit.getPluginManager().disablePlugin(this);
      }
   }

   private void startMenuTick(Player player) {
      UUID playerId = player.getUniqueId();
      this.playerMenuTicks.putIfAbsent(playerId, 0L);
      this.menuTickTasks.computeIfAbsent(playerId, ignored -> foliaLib.scheduling().entitySpecificScheduler(player)
         .runAtFixedRate(() -> this.tickMenuPlayer(player), null, 1L, 1L));
   }

   private void tickMenuPlayer(Player player) {
      if (!player.isOnline()) { this.stopMenuTick(player); return; }
      UUID playerId = player.getUniqueId();
      long tick = this.playerMenuTicks.merge(playerId, 1L, Long::sum);
      if (isCutsceneUiFrozen(player)) menuOpenedAtTicks.computeIfPresent(playerId, (id, openedAt) -> openedAt + 1);
            MenuSession activeSession = this.menuSessions.get(player.getUniqueId());
            if (activeSession != null && activeSession.stage() == MenuSession.Stage.PREPARING && this.getMenuElapsedTicks(player) > 100L) {
               this.stopCursor(player, true); return;
            }
            if (activeSession != null && activeSession.stage() == MenuSession.Stage.ACTIVE) {
               Pig mount = this.playerMenuSeats.get(player);
               Pig camera = playerSit.get(player);
               ArmorStand cursor = playerCursors.get(player);
               CutsceneView view = cutsceneViews.get(playerId);
               if (player.isDead() || mount == null || camera == null || cursor == null || (view == null || !view.moving)
                  && ((Bukkit.isOwnedByCurrentRegion(mount) && (!mount.isValid() || !mount.getPassengers().contains(player)))
                  || (Bukkit.isOwnedByCurrentRegion(camera) && !camera.isValid()) || (Bukkit.isOwnedByCurrentRegion(cursor) && !cursor.isValid()))) {
                  this.stopCursor(player, !player.isDead());
                  return;
               }
            }
             if (playerCursors.containsKey(player)) {
                RotationInput input = this.pendingRotations.remove(player.getUniqueId());
                if (input != null && this.isMenuSessionCurrent(player, input.generation())) {
                   this.lastMenuRotations.put(playerId, input);
                   this.updateCursorPosition(player, input.yaw(), input.pitch());
                }
                if ((tick % 5) == 0 && hasHierarchy(player))
                   reconcileMenuContent(player, getCurrentPlayerMenu(player));
                tickMenuZoom(player);
                tickDiggingInput(player);
                tickDrag(player);
                if ((tick % 10) == 0) tickMenuDebug(player);
                if (!isCutsceneUiFrozen(player)) this.tickDynamicMenuPositions(player);
                Long clickGeneration = this.pendingClicks.remove(player.getUniqueId());
                if (clickGeneration != null && this.isMenuSessionCurrent(player, clickGeneration)) this.handleCursorClick(player);
                if (cutsceneManager != null) cutsceneManager.tick(player);
                CutsceneView view = cutsceneViews.get(playerId);
                if (view == null) this.tickCameraEdgePan(player);
                else if (view.dirty && !view.moving && !cutsceneManager.isPending(player))
                   this.applyCutsceneFrame(player, view.basePose).thenAccept(ok -> { if (!ok) new CutsceneHost().execute(player, () -> stopCursor(player, true)); });
            }
      if ((tick & 1L) == 0L && this.hoverEnlargeManager != null && !isCutsceneUiFrozen(player)) this.hoverEnlargeManager.tickPlayer(player);
   }

   private void stopMenuTick(Player player) {
      UUID playerId = player.getUniqueId();
      space.arim.morepaperlib.scheduling.ScheduledTask task = this.menuTickTasks.remove(playerId);
      if (task != null) task.cancel();
      this.playerMenuTicks.remove(playerId);
   }

   public void onDisable() {
      pendingMenuOpens.values().forEach(pending -> pending.cancelled = true);
      sessionJournalWriter.shutdown();
      if (cutsceneManager != null) cutsceneManager.shutdown();
      boolean folia = foliaLib != null && foliaLib.scheduling().isUsingFolia();
      List<java.util.concurrent.CompletableFuture<Boolean>> closures = new ArrayList<>();
      for (Player player : new ArrayList<>(this.currentPlayerMenus.keySet())) {
         // A disabled plugin cannot submit closure tasks to other Folia regions.
         // Their existing recovery journals are retained for the next login.
         if (folia && !Bukkit.isOwnedByCurrentRegion(player)) continue;
         closures.add(this.stopCursorAsync(player, true));
      }
      if (folia && !Bukkit.isStopping() && foliaLib.scheduling().isOnGlobalRegionThread() && !closures.isEmpty()) {
         try {
            java.util.concurrent.CompletableFuture.allOf(closures.toArray(new java.util.concurrent.CompletableFuture[0]))
               .get(5L, java.util.concurrent.TimeUnit.SECONDS);
         } catch (Exception exception) {
            this.getLogger().warning("Some player sessions could not close before disable; recovery journals will be used on their next login: " + exception.getMessage());
         }
      }
      if (this.menuVisibility != null) this.menuVisibility.shutdown();
      if (this.menuApi != null) {
         Bukkit.getServicesManager().unregister(UIcoreApi.class, this.menuApi);
      }
      if (this.builtInIntegrations != null) this.builtInIntegrations.stop();
      if (this.extensionManager != null) this.extensionManager.clear();
      this.componentLayoutCache.clear();

      this.closeSessionStep(NPCModule::shutdown);
      if (this.chunkLoaderManager != null) {
         this.closeSessionStep(this.chunkLoaderManager::stopChunkLoaderTask);
      }

      this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
      if (this.menuPacketListener != null) {
         PacketEvents.getAPI().getEventManager().unregisterListener(this.menuPacketListener);
         this.menuPacketListener = null;
      }
      if (!folia) {
         this.closeSessionStep(this::purgeAllEntities);
         playerCursors.clear(); playerSit.clear(); this.playerMenuSeats.clear(); this.playerDisplays.clear(); this.playerItemDisplays.clear();
         this.playerMenuItemDisplays.clear(); this.menuSessions.clear();
      } else if (!Bukkit.isStopping()) {
         if (textDisplayManager != null) this.closeSessionStep(textDisplayManager::cleanup);
         if (this.hoverEnlargeManager != null) this.closeSessionStep(this.hoverEnlargeManager::cleanup);
      }
      this.cursorRotationTrackers.clear();
      this.cutsceneViews.clear();
      this.avatarProfileCache.clear();
      this.avatarProfileLoads.clear();
      this.menuTickTasks.values().forEach(space.arim.morepaperlib.scheduling.ScheduledTask::cancel);
      this.menuTickTasks.clear(); this.playerMenuTicks.clear();
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
      this.langConfig = org.orvencraft.uicore.config.ConfigFiles.loadStrict(var1);
   }

   private final java.util.concurrent.atomic.AtomicBoolean reloadingConfiguration = new java.util.concurrent.atomic.AtomicBoolean();

   /** Completes with a diagnostic on failure or null after the configuration is applied. */
   public java.util.concurrent.CompletableFuture<String> reloadPluginConfig() {
      if (!reloadingConfiguration.compareAndSet(false, true))
         return java.util.concurrent.CompletableFuture.completedFuture("Another reload is already in progress.");
      var result = new java.util.concurrent.CompletableFuture<String>();
      result.whenComplete((ignored, failure) -> reloadingConfiguration.set(false));
      try {
         this.validateCoreConfiguration();
         sectionManager.validateMenuConfigs();
         validateCutscenes();
         for (File file : this.getDataFolder().listFiles() == null ? new File[0] : this.getDataFolder().listFiles()) {
            if (file.getName().endsWith(".yml")) { org.orvencraft.uicore.config.ConfigFiles.loadStrict(file); }
         }
      } catch (Exception exception) {
         this.getLogger().warning("Reload rejected; current configuration remains active: " + exception.getMessage());
         result.complete("Invalid configuration: " + exception.getMessage());
         return result;
      }
      Map<Player, String> openMenus = new HashMap<>(this.currentPlayerMenus);
      openMenus.keySet().removeIf(player -> cutsceneManager != null && cutsceneManager.active(player));
      var closes = openMenus.keySet().stream().map(player -> this.stopCursorAsync(player, true)).toList();
      java.util.concurrent.CompletableFuture.allOf(closes.toArray(java.util.concurrent.CompletableFuture<?>[]::new))
         .whenComplete((ignored, failure) -> {
            try {
               foliaLib.scheduling().globalRegionalScheduler().run(() -> {
                  try {
                     if (failure != null || closes.stream().anyMatch(close -> !Boolean.TRUE.equals(close.getNow(false)))) {
                        result.complete("Could not close all active menu sessions safely.");
                        return;
                     }
                     this.finishPluginConfigReload(openMenus);
                     result.complete(null);
                  } catch (RuntimeException exception) {
                     this.getLogger().warning("Reload failed: " + exception.getMessage());
                     result.complete("Could not apply configuration: " + exception.getMessage());
                  }
               });
            } catch (RuntimeException exception) { result.complete("Could not schedule configuration reload: " + exception.getMessage()); }
         });
      return result;
   }

   private void finishPluginConfigReload(Map<Player, String> openMenus) {
      this.loadConfig();
      this.reloadCutscenes();
      this.reloadLangConfig();
      if (textDisplayManager != null) textDisplayManager.reloadConfig();
      if (this.hoverEnlargeManager != null) this.hoverEnlargeManager.cleanup();
      this.hoverEnlargeManager = new HoverEnlargeManager(this);
      NPCModule.reload();
      for (Map.Entry<Player, String> entry : openMenus.entrySet()) if (entry.getKey().isOnline()) this.setupCursor(entry.getKey(), entry.getValue());
   }

   private void validateCoreConfiguration() throws Exception {
      YamlConfiguration config = new YamlConfiguration();
      config.load(new File(this.getDataFolder(), "config.yml"));
      for (String path : config.getKeys(true)) {
         Object value = config.get(path);
         if (value instanceof Number number && (!Double.isFinite(number.doubleValue()) || Math.abs(number.doubleValue()) > 1.0E6))
            throw new IllegalArgumentException("Invalid numeric value: " + path);
      }
      Material material = Material.matchMaterial(config.getString("cursor-item.material", "ARROW"));
      if (material == null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Invalid cursor-item.material");
      for (String path : List.of("cursor-item.scale", "cursor-item.size.x", "cursor-item.size.y", "cursor-item.size.z", "cursor-item.sensitivity")) {
         double value = config.getDouble(path, 1.0);
         if (!Double.isFinite(value) || value <= 0.0 || value > 100.0) throw new IllegalArgumentException("Out of bounds: " + path);
      }
      for (String path : List.of("sound.volume", "sound.pitch")) {
         double value = Double.parseDouble(config.getString(path, "1"));
         if (!Double.isFinite(value) || value < 0 || value > 100) throw new IllegalArgumentException("Invalid sound setting: " + path);
      }
      int radius = config.getInt("camera-block-check.radius", 1);
      if (radius < 0 || radius > 8) throw new IllegalArgumentException("camera-block-check.radius must be 0..8");
      if (config.getInt("sound.loop.duration", 30) < 1) throw new IllegalArgumentException("sound.loop.duration must be positive");
   }

   private void loadConfig() {
      try { this.validateCoreConfiguration(); }
      catch (Exception exception) { throw new IllegalArgumentException("Invalid config.yml", exception); }
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
      soundRate = this.getConfig().getInt("sound.loop.duration", 30);
      soundName = this.getConfig().getString("sound.name", "minecraft:ui.button.click");
      soundVolume = Float.parseFloat(this.getConfig().getString("sound.volume", "1"));
      soundPitch = Float.parseFloat(this.getConfig().getString("sound.pitch", "1"));
      this.debugMode = this.getConfig().getBoolean("Debug", false);
      joinRunBool = this.getConfig().getBoolean("join-run.enabled", false);
      joinRunSection = this.getConfig().getString("join-run.menu", "test");
      cursorItem = Material.matchMaterial(this.getConfig().getString("cursor-item.material", "ARROW")).name();
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
      if (this.extensionManager != null) this.extensionManager.validateMenuReferences(sectionManager.getAll());
      if (this.schematicPreviewManager != null) this.schematicPreviewManager.reload(sectionManager.getAll().values());
      File var2 = new File(this.getDataFolder(), "commands.yml");
      if (!var2.exists()) {
         this.saveResource("commands.yml", false);
      }

      YamlConfiguration var3 = org.orvencraft.uicore.config.ConfigFiles.loadStrict(var2);
      allowedCommands = var3.getStringList("allowed-commands");
      allowedCommands.replaceAll(String::toLowerCase);
      if (this.chunkLoaderManager != null) {
         this.chunkLoaderManager.startChunkLoaderTask();
      }
   }

   public void setupCursor(Player var1, String var2) {
      setupCursor(var1, var2, null);
   }

   private void setupCursor(Player var1, String var2, MenuSession preparedSession) {
       if (var1 == null) return;
       if (!Bukkit.isOwnedByCurrentRegion(var1)) {
          foliaLib.scheduling().entitySpecificScheduler(var1).run(() -> this.setupCursor(var1, var2, preparedSession), null);
          return;
       }
      if (this.closingSessions.containsKey(var1.getUniqueId())) return;
           Section var3 = sectionManager.get(var2);
      if (var3 == null) {
         this.getLogger().warning("Menu '" + var2 + "' not found! Please check your menu configuration.");
         var1.sendMessage(
            this.getLangMessage("command.menu_not_found", "&c[UIcore] Menu '%menu%' was not found. Check the configuration.").replace("%menu%", var2)
         );
       } else {
          if (var1 == null || !var1.isOnline()) return;
          CutsceneView pendingView = cutsceneViews.get(var1.getUniqueId());
          if (pendingView != null && pendingView.moving) {
             if (cutsceneManager != null) cutsceneManager.abort(var1, CutsceneAbortReason.MENU_CHANGED);
             pendingView.afterMove = () -> setupCursor(var1, var2);
             return;
          }
          if (var3.permission != null && !var3.permission.isBlank() && !var1.hasPermission(var3.permission)) return;
          Location relativeAnchor = playerLocations.getOrDefault(var1, var1.getLocation());
          Section var4 = org.orvencraft.uicore.section.PlayerLocationOverride.apply(var1, var3, relativeAnchor);
          if (cutsceneManager != null) {
             cutsceneManager.abort(var1, CutsceneAbortReason.MENU_CHANGED);
             releaseCutsceneView(var1);
             CutsceneDefinition scene = cutsceneManager.definition(var3.cutsceneId).orElse(null);
             if (scene != null && var3.cutsceneAutoStart && scene.policy().startMode() == CutsceneDefinition.StartMode.INITIAL) {
                CameraPose fallback = new CameraPose(var4.world, var4.cameraX, var4.cameraY, var4.cameraZ, var4.yaw, var4.pitch);
                CameraPose initial = org.orvencraft.uicore.cutscene.CutscenePlayback.initialPose(scene, fallback);
                var4 = var4.copyWithCamera(initial.world(), initial.x(), initial.y(), initial.z(), (float)initial.yaw(), (float)initial.pitch());
                var4.cameraViewpointCoordinates = true;
             }
          }
          if (Bukkit.getWorld(var4.world) == null) {
             this.getLogger().warning("Unknown menu world: " + var4.world);
             return;
          }
          if (!this.menuSessions.containsKey(var1.getUniqueId())) {
             if (preparedSession == null) {
                if (pendingMenuOpens.containsKey(var1.getUniqueId())) return;
                this.recoverSavedSession(var1);
                if (java.nio.file.Files.exists(this.sessionJournal(var1))) {
                   this.getLogger().warning("Cannot open menu while original session recovery is unresolved for " + var1.getName());
                   return;
                }
                this.prepareMenuOpen(var1, var2);
                return;
             }
             this.menuSessions.put(var1.getUniqueId(), preparedSession);
          }
          long beforeDragCancel = getMenuSessionGeneration(var1);
          finishDrag(var1, true);
          if (getMenuSessionGeneration(var1) != beforeDragCancel) return;
          this.startMenuTick(var1);
          this.sessionGenerations.put(var1.getUniqueId(), this.generationCounter.incrementAndGet());
          this.restoreBlocks(var1);
          if (this.schematicPreviewManager != null) this.schematicPreviewManager.clear(var1);
          NPCModule.onMenuClose(var1);
          if (textDisplayManager != null) textDisplayManager.clearPlayerDisplays(var1.getUniqueId());
          this.beginDynamicMenuSession(var1, var4.zoom);
          final UUID menuSessionToken = UUID.randomUUID();
          this.menuSessionTokens.put(var1.getUniqueId(), menuSessionToken);
          String previousMenuForEvent = this.currentPlayerMenus.get(var1);
         MenuViewSnapshot previousViewForSwitch = null;
         if (playerCursors.containsKey(var1)) {
            String var5 = this.currentPlayerMenus.get(var1);
            Section var7 = this.getRuntimeSection(var1, var5);
            Pig var8 = this.playerMenuSeats.get(var1);
            boolean var9 = var8 != null && var8.isValid() && var8.getPassengers().contains(var1);
            if (var9 && var7 != null && this.menuSessions.get(var1.getUniqueId()).stage() == MenuSession.Stage.ACTIVE && !this.isMenuTransitionActive(var1) && this.isSameLocation(var7, var4)) {
               this.activeMenuTransitions.remove(var1.getUniqueId());
               this.playerRuntimeSections.put(var1, var4);
               this.applyMenuPrivacy(var1, var4);
               if (var5 != null && !var5.equals(var2)) {
                  long eventGeneration = this.getMenuSessionGeneration(var1);
                  Bukkit.getPluginManager().callEvent(new MenuSwitchEvent(var1, var5, var2));
                  if (!this.isMenuSessionCurrent(var1, eventGeneration)) return;
               }

               this.refreshMenuContent(var1, var2, var5);
               if (var7.dragInput.equals("block-digging") || var4.dragInput.equals("block-digging")) this.sendCameraPacket(var1, playerSit.get(var1));
               this.rebuildMenuModules(var1, var2);
               this.runMenuAutoCommands(var1, var4);
               this.startBoundCutscene(var1, var4);
               return;
            }
         }

         if (playerCursors.containsKey(var1)) {
            previousViewForSwitch = this.captureMenuView(var1);
            this.prepareMenuViewSwitch(var1);
         }

         final MenuViewSnapshot previousViewSnapshot = previousViewForSwitch;
         final UUID menuTransitionToken = menuSessionToken;
         this.menuSessions.get(var1.getUniqueId()).prepare();
         this.activeMenuTransitions.put(var1.getUniqueId(), menuTransitionToken);
         this.currentPlayerMenus.put(var1, var2);
         this.playerRuntimeSections.put(var1, var4);
         this.applyMenuPrivacy(var1, var4);
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

               if (!this.isActiveMenuTransition(var1, var2, menuTransitionToken)) return;
               playerLocations.putIfAbsent(var1, var1.getLocation());
               Location var17 = new Location(var16, var15.cameraX, var15.cameraY, var15.cameraZ, var15.yaw, var15.pitch);
                this.storeOriginalGameMode(var1);
                foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(() -> {
                   if (this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                      var1.setGameMode(GameMode.ADVENTURE);
                   }
                }, null, 1L);
                foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(() -> {
                   if (!this.isActiveMenuTransition(var1, var2, menuTransitionToken)) return;
                   this.releasePlayerFromMenuCamera(var1, previousViewSnapshot);
                   this.teleportMenuInternalAsync(var1, var17).whenComplete((teleported, error) ->
                      foliaLib.scheduling().entitySpecificScheduler(var1).run(() -> {
                         if (!this.isActiveMenuTransition(var1, var2, menuTransitionToken)) return;
                         if (error != null || !Boolean.TRUE.equals(teleported) || !this.isAtMenuCamera(var1, var17)) {
                            this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "teleport to camera position was rejected");
                            return;
                         }
                         this.cleanupMenuViewSnapshot(var1, previousViewSnapshot);
                         var1.setGameMode(GameMode.ADVENTURE);
                         foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(() -> {
                          try {
                         if (this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                           if (!this.isNearMenuCameraForMount(var1, var17)) {
                              this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "player moved away from the camera position before mounting");
                              return;
                           }

                           this.currentPlayerMenus.put(var1, var2);
                           playingSound.add(var1.getName());
                           var1.stopAllSounds();
                           final long soundGeneration = this.getMenuSessionGeneration(var1);
                           if (soundLoop) {
                              foliaLib.scheduling().entitySpecificScheduler(var1).runAtFixedRate(var1xx -> {
                                 if (!this.isMenuSessionCurrent(var1, soundGeneration) || !playingSound.contains(var1.getName())) {
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
                            Map<String, MenuLayout> visibleLayouts = this.getLayoutsForPlayer(var1, var2, var15);
                            Location seatLocation = MenuCameraGeometry.seatLocation(var6x);
                            if (!Bukkit.isOwnedByCurrentRegion(var6x) || !Bukkit.isOwnedByCurrentRegion(var15x)
                               || !Bukkit.isOwnedByCurrentRegion(seatLocation)) {
                               this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "camera or cursor falls outside the player's Folia region");
                               return;
                            }
                            for (MenuLayout layout : visibleLayouts.values()) {
                               Location layoutLocation = this.toLayoutWorldLocation(var15, layout, this.resolveLayoutPositionForSpawn(var1, var2, layout));
                               if (layoutLocation != null && !Bukkit.isOwnedByCurrentRegion(layoutLocation)) {
                                  this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "layout '" + layout.key + "' falls outside the player's Folia region");
                                  return;
                               }
                            }
                            ArmorStand var16x = this.spawnCursorArmorStand(var15x);
                           playerCursors.put(var1, var16x);
                           this.cursorRotationTrackers.remove(var1.getUniqueId());
                           ArrayList<TextDisplay> var17x = new ArrayList<>();
                           ArrayList<ItemDisplay> menuItemDisplays = new ArrayList<>();
                           this.playerDisplays.put(var1, var17x);
                           this.playerMenuItemDisplays.put(var1, menuItemDisplays);

                            for (MenuLayout var19 : visibleLayouts.values()) {
                               if (var19.interaction().group()) continue;
                               DynamicPositionSpec.Position layoutPosition = this.resolveLayoutPositionForSpawn(var1, var2, var19);
                               Location var21 = this.toLayoutWorldLocation(var15, var19, layoutPosition);
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

                              TextDisplay var22 = this.tagMenuEntity((TextDisplay)var16.spawn(var21, TextDisplay.class, created -> created.setVisibleByDefault(false)));
                              var17x.add(var22);
                              String var23 = var19.renderStyledName(var1, false, false);
                              if (var19.isTypewriterEnabled()) {
                                 var22.setText(var19.getTypewriterCursorChar());
                                 textDisplayManager.getTypewriterEffect()
                                    .applyEffect(
                                       var22,
                                       var19.renderStyledComponent(var1, false, false),
                                       var19.getTypewriterSpeed(),
                                       var19.getTypewriterStartDelay(),
                                       var19.getTypewriterCursorChar()
                                    );
                              } else {
                                 var22.text(var19.renderStyledComponent(var1, false, false));
                              }

                              var22.setCustomName(var2 + ":" + var19.key);
                              var22.setCustomNameVisible(false);
                              var22.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                              var22.setDefaultBackground(false);
                              var22.setShadowed(true);
                              var22.setBillboard(Billboard.CENTER);
                              var22.setVisibleByDefault(false);
                              if (var19.hasCustomHitbox()) {
                                 var22.addScoreboardTag(
                                    String.format(
                                       Locale.US,
                                       "uicore_hitbox:%.3f:%.3f:%.3f",
                                       var19.getHitboxWidth(),
                                       var19.getHitboxHeight(),
                                       var19.getHitboxDepth()
                                    )
                                 );
                              }
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
                           Pig var30 = this.spawnPlayerLocStand(var1, var6x);
                           var30.setRotation(var15.yaw, var15.pitch);
                           var cameraReady = var15.cameraViewpointCoordinates
                              ? var30.teleportAsync(var6x.clone().subtract(0, var30.getEyeHeight(), 0))
                              : java.util.concurrent.CompletableFuture.completedFuture(true);
                           playerSit.put(var1, var30);
                           Pig playerSeat = this.spawnPlayerLocStand(var1, seatLocation);
                           this.playerMenuSeats.put(var1, playerSeat);
                           MenuViewSnapshot newViewSnapshot = new MenuViewSnapshot(
                              var30,
                              playerSeat,
                              var16x,
                              var17x,
                              var29,
                              menuItemDisplays
                           );
                           var1.setCollidable(false);
                           if (!playerSeat.addPassenger(var1)) {
                              this.cleanupMenuViewSnapshot(var1, newViewSnapshot);
                              this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "camera mount rejected the player passenger");
                              return;
                           }

                           cameraReady.whenComplete((ready, cameraError) -> foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(() -> {
                              if (!this.isActiveMenuTransition(var1, var2, menuTransitionToken)) {
                                 this.cleanupMenuViewSnapshot(var1, newViewSnapshot);
                                 return;
                              }

                              if (cameraError != null || !Boolean.TRUE.equals(ready) || !var30.isValid() || !playerSeat.isValid() || !playerSeat.getPassengers().contains(var1)) {
                                 this.cleanupMenuViewSnapshot(var1, newViewSnapshot);
                                 this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "camera mount was lost before activation");
                                 return;
                              }

                              this.sendCameraPacket(var1, var30);
                              this.activeMenuTransitions.remove(var1.getUniqueId(), menuTransitionToken);
                              MenuSession session = this.menuSessions.get(var1.getUniqueId());
                              if (session != null) session.activate();
                              this.runMenuAutoCommands(var1, var15);
                              this.startBoundCutscene(var1, var15);
                              if (previousMenuForEvent != null && !previousMenuForEvent.equals(var2)) {
                                 this.getLogger().info("Switched " + var1.getName() + " to menu '" + var2 + "'.");
                              }
                           }, null, 2L));
                           if (this.debugMode) {
                              var1.sendMessage(this.getLangMessage("menu.cursor_activated", "&a[UIcore] Menu cursor activated."));
                           }

                           textDisplayManager.showTextDisplays(var1, var2);
                           this.clearBlockingBlocks(var1);
                           NPCModule.onMenuOpen(var1, var6x, var15.yaw, var15.pitch, var2);
                           cursorExactLocations.put(var1, var15x.clone());
                        }
                        } catch (RuntimeException exception) {
                           this.abortMenuTransition(var1, var2, menuTransitionToken, previousViewSnapshot, "renderer failed: " + exception.getMessage());
                        }
                      }, null, 5L);
                      }, null));
                }, null, 5L);
            }
         }
      }
   }

   private void startBoundCutscene(Player player, Section section) {
      if (section.cutsceneAutoStart && cutsceneManager != null && !cutsceneManager.active(player)
         && section.key.equals(getCurrentPlayerMenu(player))) cutsceneManager.start(player, section.cutsceneId);
   }

   private void runMenuAutoCommands(Player player, Section section) {
      if (section == null || !Objects.equals(section.key, this.getCurrentPlayerMenu(player))) return;
      if (this.schematicPreviewManager != null) this.schematicPreviewManager.show(player, section);
      if (!section.autoCommandsEnabled) return;
      long generation = this.getMenuSessionGeneration(player);
      for (int index = 0; index < section.autoCommands.size(); index++) {
         if (!this.isMenuSessionCurrent(player, generation)) return;
         long delay = index < section.autoCommandDelays.size() ? section.autoCommandDelays.get(index) : 20L;
         CommandUtils.executeCommand(player, section.autoCommands.get(index), delay);
      }
   }

   public void stopCursor(Player player, boolean returnToOrigin) {
      this.stopCursorAsync(player, returnToOrigin);
   }

   public java.util.concurrent.CompletableFuture<Boolean> stopCursorAsync(Player player, boolean returnToOrigin) {
      java.util.concurrent.CompletableFuture<Boolean> completion = new java.util.concurrent.CompletableFuture<>();
      if (player == null) {
         completion.complete(false);
         return completion;
      }
      UUID playerId = player.getUniqueId();
      PendingMenuOpen pending = pendingMenuOpens.get(playerId);
      if (pending != null) pending.cancelled = true;
      java.util.concurrent.CompletableFuture<Boolean> existing = this.closingSessions.putIfAbsent(playerId, completion);
      if (existing != null) return existing;
      completion.whenComplete((result, error) -> this.closingSessions.remove(playerId, completion));
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         if (!this.isEnabled()) {
            completion.complete(false);
            return completion;
         }
         foliaLib.scheduling().entitySpecificScheduler(player).run(() ->
            this.stopCursorOnOwner(player, returnToOrigin, completion), () -> completion.complete(false));
         return completion;
      }
      this.stopCursorOnOwner(player, returnToOrigin, completion);
      return completion;
   }

   private void stopCursorOnOwner(Player player, boolean returnToOrigin, java.util.concurrent.CompletableFuture<Boolean> completion) {
      UUID playerId = player.getUniqueId();
      if (cutsceneManager != null) cutsceneManager.abort(player, CutsceneAbortReason.MENU_CLOSED);
      CutsceneView pendingView = cutsceneViews.get(playerId);
      if (pendingView != null && pendingView.moving && pendingView.inFlight != null && !pendingView.inFlight.isDone()) {
         pendingView.inFlight.copy().completeOnTimeout(false, 1, java.util.concurrent.TimeUnit.SECONDS).whenComplete((ok, error) -> {
            Runnable finish = () -> {
               if (completion.isDone()) return;
               if (cutsceneViews.get(playerId) == pendingView && pendingView.moving) releaseCutsceneView(player);
               stopCursorOnOwner(player, returnToOrigin, completion);
            };
            Runnable retired = () -> completion.complete(false);
            if (!org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, player, finish, retired)) retired.run();
         });
         return;
      }
      finishDrag(player, true);
      closeDiggingInput(player);
      MenuClientView clientView = menuClientViews.remove(playerId);
      if (clientView != null) clientView.close(player.getEntityId(), player.getGameMode().getValue());
      releaseCutsceneView(player);
      if (this.extensionManager != null) this.extensionManager.invalidateData(player);
      if (this.extensionManager != null) this.extensionManager.invalidateComponents(player);
      MenuSession session = this.menuSessions.remove(playerId);
      if (session != null && !session.beginClose()) {
         completion.complete(false);
         return;
      }
      this.stopMenuTick(player);
      String closingMenu = this.currentPlayerMenus.remove(player);
      if (this.menuVisibility != null) this.closeSessionStep(() -> this.menuVisibility.close(player));
      if (session == null && closingMenu == null) {
         this.clearPlayerSessionReferences(player);
         completion.complete(true);
         return;
      }
      this.sessionGenerations.remove(playerId);
      this.menuSessionTokens.remove(playerId);
      this.activeMenuTransitions.remove(playerId);
      this.pendingRotations.remove(playerId);
      this.lastMenuRotations.remove(playerId);
      this.pendingClicks.remove(playerId);
      try {
         this.closeSessionStep(() -> this.clearMenuTextAnimationTasks(player));
         this.closeSessionStep(() -> this.clearMenuItemAnimationTasks(player));
         this.closeSessionStep(() -> this.cleanupMenuViewSnapshot(player, this.captureMenuView(player)));
         if (textDisplayManager != null) this.closeSessionStep(() -> textDisplayManager.clearPlayerDisplays(playerId));
         if (itemDisplayManager != null) this.closeSessionStep(() -> itemDisplayManager.hideItem(player));
         if (this.hoverEnlargeManager != null) this.closeSessionStep(() -> this.hoverEnlargeManager.cleanupPlayer(player));
         this.closeSessionStep(() -> NPCModule.onMenuClose(player));
         this.closeSessionStep(() -> playingSound.remove(player.getName()));
         this.closeSessionStep(() -> player.stopAllSounds());
         this.closeSessionStep(() -> this.sendCameraPacket(player, player));
         this.closeSessionStep(() -> this.restoreBlocks(player));
         if (this.schematicPreviewManager != null) this.closeSessionStep(() -> this.schematicPreviewManager.clear(player));
      } finally {
         Location origin = session == null ? playerLocations.get(player) : session.originalLocation();
         boolean playerRestored = true;
         if (session != null) { try { session.restore(player); } catch (RuntimeException exception) { playerRestored = false; this.getLogger().warning("Player restoration failed; recovery journal retained: " + exception.getMessage()); } }
         // Bukkit may keep ADVENTURE unchanged, so explicitly undo the client-only spectator mode.
         this.closeSessionStep(() -> PacketEvents.getAPI().getPlayerManager().sendPacket(player,
            new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState(
               com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
               player.getGameMode().getValue())));
         this.closeSessionStep(() -> player.removeMetadata("cursor_original_gamemode", this));
         this.clearPlayerSessionReferences(player);
           if (!playerRestored || player.isDead()) {
              this.getLogger().warning("Original player state/location not fully restored; recovery journal retained for " + player.getName());
              completion.complete(false);
           } else if (!returnToOrigin && this.isEnabled()) {
              // A MONITOR teleport hook runs before the external teleport moves the player.
              player.getScheduler().run(this, ignored -> completion.complete(this.checkpointRestoredSession(player)),
                 () -> completion.complete(false));
           } else if (!returnToOrigin || origin == null) {
              completion.complete(this.checkpointRestoredSession(player));
           } else if (origin.getWorld() == null) {
              this.getLogger().warning("Original player world is unavailable; recovery journal retained for " + player.getName());
              completion.complete(false);
           } else {
              try {
                 if (!this.isEnabled()) {
                    boolean restored = false;
                    if (!foliaLib.scheduling().isUsingFolia()) {
                       // Paper onDisable runs on the main thread; no later tick is available.
                       UUID token = this.internalMenuTeleports.begin(player.getUniqueId(), origin);
                       try { restored = player.teleport(origin); }
                       finally { this.internalMenuTeleports.complete(player.getUniqueId(), token); }
                    }
                    completion.complete(restored && this.checkpointRestoredSession(player));
                 } else {
                    this.teleportMenuInternalAsync(player, origin).whenComplete((returned, error) -> {
                       boolean dispatched = org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, player, () -> {
                          boolean restored = error == null && Boolean.TRUE.equals(returned) && !player.isDead();
                          if (!restored) this.getLogger().warning("Return teleport failed; recovery journal retained for " + player.getName());
                          completion.complete(restored && this.checkpointRestoredSession(player));
                       }, () -> completion.complete(false));
                       if (!dispatched) completion.complete(false);
                    });
                 }
              } catch (RuntimeException exception) {
                 this.getLogger().warning("Return teleport failed; recovery journal retained: " + exception.getMessage());
                 completion.complete(false);
              }
           }
      }
      if (closingMenu != null) Bukkit.getPluginManager().callEvent(new MenuCloseEvent(player, closingMenu));
   }

   private void closeSessionStep(Runnable cleanup) {
      try { cleanup.run(); }
      catch (RuntimeException | LinkageError exception) {
         this.getLogger().warning("Menu cleanup step failed; continuing remaining cleanup: " + exception);
      }
   }

   private java.nio.file.Path sessionJournal(Player player) {
      return this.getDataFolder().toPath().resolve("sessions").resolve(player.getUniqueId() + ".yml");
   }

   private static final class PendingMenuOpen {
      final MenuSession snapshot;
      final java.util.concurrent.atomic.AtomicBoolean discarding = new java.util.concurrent.atomic.AtomicBoolean();
      volatile boolean cancelled;

      PendingMenuOpen(MenuSession snapshot) { this.snapshot = snapshot; }
   }

   private final Map<UUID, PendingMenuOpen> pendingMenuOpens = new ConcurrentHashMap<>();
   private final java.util.concurrent.ThreadPoolExecutor sessionJournalWriter = new java.util.concurrent.ThreadPoolExecutor(
      1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(32), work -> {
         Thread thread = new Thread(work, "UIcore session journal");
         thread.setDaemon(true);
         return thread;
      });

   private void prepareMenuOpen(Player player, String menu) {
      UUID id = player.getUniqueId();
      var journal = this.sessionJournal(player);
      PendingMenuOpen pending = new PendingMenuOpen(new MenuSession(player));
      if (pendingMenuOpens.putIfAbsent(id, pending) != null) return;
      try {
         sessionJournalWriter.execute(() -> {
            try {
               if (pending.cancelled || !isEnabled()) { pendingMenuOpens.remove(id, pending); return; }
               pending.snapshot.save(journal);
            } catch (IOException | RuntimeException exception) {
               pendingMenuOpens.remove(id, pending);
               getLogger().warning("Menu opening rejected: cannot save recovery snapshot: " + exception.getMessage());
               return;
            }
            Runnable discard = () -> {
               if (!pending.discarding.compareAndSet(false, true) || pendingMenuOpens.get(id) != pending) return;
               try { java.nio.file.Files.deleteIfExists(journal); }
               catch (IOException exception) { getLogger().warning("Could not remove unused menu recovery snapshot: " + exception.getMessage()); }
               finally { pendingMenuOpens.remove(id, pending); }
            };
            boolean dispatched = org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, player, () -> {
               try {
                  if (!pending.cancelled && isEnabled() && player.isOnline() && !player.isDead()
                     && player.getWorld().equals(pending.snapshot.originalLocation().getWorld())
                     && pending.snapshot.matchesPlayerFlags(player)) {
                     setupCursor(player, menu, pending.snapshot);
                  }
               } finally {
                  if (menuSessions.get(id) == pending.snapshot) pendingMenuOpens.remove(id, pending);
                  else discard.run();
               }
            }, discard);
            if (!dispatched) discard.run();
         });
      } catch (java.util.concurrent.RejectedExecutionException exception) {
         pendingMenuOpens.remove(id, pending);
         getLogger().warning("Menu opening rejected: session journal queue is full or shutting down.");
      }
   }

   private boolean checkpointRestoredSession(Player player) {
      var journal = this.sessionJournal(player);
      if (!java.nio.file.Files.exists(journal)) return true;
      try {
         player.saveData();
         java.nio.file.Files.deleteIfExists(journal);
         return true;
      } catch (IOException | RuntimeException exception) {
         this.getLogger().warning("Could not checkpoint restored player; session journal retained: " + exception.getMessage());
         return false;
      }
   }

   private void recoverSavedSession(Player player) {
      java.nio.file.Path journal = this.sessionJournal(player);
      UUID playerId = player.getUniqueId();
      if (!java.nio.file.Files.exists(journal) || this.menuSessions.containsKey(playerId)
         || this.pendingMenuOpens.containsKey(playerId) || !this.recoveringSessions.add(playerId)) return;
      try {
          MenuSession saved = MenuSession.load(journal, Bukkit::getWorld);
          saved.restore(player);
          this.sendCameraPacket(player, player);
          Location origin = saved.originalLocation();
          if (player.isDead()) { this.respawnOrigins.put(playerId, origin); this.recoveringSessions.remove(playerId); return; }
          if (origin.getWorld() == null) {
             this.getLogger().warning("Session origin unavailable; retained recovery journal for " + player.getName());
             this.recoveringSessions.remove(playerId);
             return;
          }
          this.teleportMenuInternalAsync(player, origin).whenComplete((returned, error) -> {
             foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
                this.recoveringSessions.remove(playerId);
                if (error == null && Boolean.TRUE.equals(returned)) this.checkpointRestoredSession(player);
                else this.getLogger().warning("Session return teleport failed; retained recovery journal for " + player.getName());
             }, () -> this.recoveringSessions.remove(playerId));
          });
          return;
      } catch (IOException | RuntimeException exception) {
         this.getLogger().warning("Cannot recover session for " + player.getName() + ": " + exception.getMessage());
      }
      this.recoveringSessions.remove(playerId);
   }

   private ArmorStand spawnCursorArmorStand(Location var1) {
      ArmorStand var2 = this.tagMenuEntity((ArmorStand)var1.getWorld().spawn(var1, ArmorStand.class));
      var2.setVisibleByDefault(false);
      var2.setGravity(false);
      var2.setVisible(false);
      var2.setMarker(true);
      this.setTeleportDurationSafe(var2, 2);
      return var2;
   }

   private Pig spawnPlayerLocStand(Player viewer, Location var1) {
      Pig var2 = this.tagMenuEntity(var1.getWorld().spawn(var1, Pig.class, pig -> pig.setVisibleByDefault(false)));
      var2.setGravity(false);
      var2.setInvulnerable(true);
      var2.setAI(false);
      var2.setInvisible(true);
      var2.setCollidable(false);
      var2.setSilent(true);
      viewer.showEntity(this, var2);
      return var2;
   }

   private TextDisplay spawnCursorTextDisplay(Player var1, Location var2, String var3, String var4, String var5, double var6, double var8, double var10) {
      Location var12 = var2.clone().add(var6, var8, var10);
      TextDisplay var13 = this.tagMenuEntity((TextDisplay)var2.getWorld().spawn(var12, TextDisplay.class, created -> created.setVisibleByDefault(false)));
      var13.setCustomName(var3 + ":" + var4);
      var13.setCustomNameVisible(false);
      var13.setText(var5);
      var13.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      var13.setDefaultBackground(false);
      var13.setBillboard(Billboard.CENTER);
      var13.setVisibleByDefault(false);
      this.configureMenuEntityVisibility(var1, var13);
      var1.showEntity(this, var13);
      return var13;
   }

   private ItemDisplay spawnCursorItemDisplay(Player var1, Location var2) {
      ItemDisplay var3 = this.tagMenuEntity((ItemDisplay)var2.getWorld().spawn(var2, ItemDisplay.class, created -> created.setVisibleByDefault(false)));
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
      this.configureMenuEntityVisibility(var1, var3);
      var1.showEntity(this, var3);
      this.setTeleportDurationSafe(var3, 2);
      return var3;
   }

   private ItemDisplay spawnMenuItemDisplay(Player player, World world, Location loc, String menuKey, MenuLayout layout) {
      ItemStack stack = this.createMenuItemStack(player, menuKey, layout);
      Location spawnLocation = loc;
      boolean stagedAtEntrance = false;
      if (layout.isEntranceAnimationEnabled()) {
         Location entrance = loc.clone().add(this.animationOffset(loc, layout.getEntranceAnimationFrom(), layout.getEntranceAnimationDistance()));
         if (Bukkit.isOwnedByCurrentRegion(entrance)) {
            spawnLocation = entrance;
            stagedAtEntrance = true;
         }
      }

      ItemDisplay display = this.tagMenuEntity((ItemDisplay)world.spawn(spawnLocation, ItemDisplay.class, created -> created.setVisibleByDefault(false)));
      display.setCustomName(menuKey + ":" + layout.key + ":item");
      display.setCustomNameVisible(false);
      display.setItemStack(stack);
      display.setGlowing(layout.resolveItemGlow(player));
      display.setViewRange(100.0F);
      display.setPersistent(false);
      display.setVisibleByDefault(false);
      display.setBillboard(this.parseBillboard(layout.getItemBillboard()));
      display.setItemDisplayTransform(this.resolveMenuItemTransform(layout));
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
      if (layout.isPlayerAvatarLayout() && stack.getType() == Material.PLAYER_HEAD) {
         display.setTransformation(AvatarDisplayTransform.create(
            layout.getItemScale(), layout.isAvatarFaceOnly(), layout.tiltX, layout.tiltY, layout.tiltZ,
            display.getItemDisplayTransform()
         ));
         display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
      }
      this.showMenuDisplay(player, display, layout, loc, stagedAtEntrance);
      if (layout.isItemRotate()) {
         final ScheduledTask[] rotation = {null};
         rotation[0] = foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(() -> {
                   if (isCutsceneUiFrozen(player)) return;
                   if (!player.isOnline()
                      || !display.isValid()
                      || !UIcorePlugin.this.playerMenuItemDisplays.getOrDefault(player, Collections.emptyList()).contains(display)) {
                      if (rotation[0] != null) rotation[0].cancel();
                   } else {
                     Transformation tr = display.getTransformation();
                     display.setTransformation(
                        new Transformation(
                           tr.getTranslation(),
                           tr.getLeftRotation().rotateAxis(
                              (float)Math.toRadians(layout.getItemRotateSpeed()),
                              0.0F, layout.isAvatarFaceOnly() ? 0.0F : 1.0F, layout.isAvatarFaceOnly() ? 1.0F : 0.0F
                           ),
                           tr.getScale(),
                           tr.getRightRotation()
                        )
                     );
                   }
             }, null, 1L, 1L);
         if (rotation[0] != null) this.playerMenuItemAnimationTasks.computeIfAbsent(player, var0 -> new CopyOnWriteArrayList<>()).add(rotation[0]);
      }

      return display;
   }

   private TextDisplay spawnMenuItemHitbox(Player player, World world, Location loc, String menuKey, MenuLayout layout) {
      TextDisplay hitbox = this.tagMenuEntity((TextDisplay)world.spawn(loc, TextDisplay.class, created -> created.setVisibleByDefault(false)));
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
      hitbox.addScoreboardTag(TextDisplayHitBox.ITEM_HITBOX_TAG);
      Transformation oldTrans = hitbox.getTransformation();
      hitbox.setTransformation(
         new Transformation(oldTrans.getTranslation(), oldTrans.getLeftRotation(), new Vector3f(1.0F, 1.0F, 1.0F), oldTrans.getRightRotation())
      );
      this.showMenuDisplay(player, hitbox, layout);
      return hitbox;
   }

   private void reconcileMenuContent(Player player, String menuKey) {
      Section section = this.getRuntimeSection(player, menuKey);
      if (section == null) {
         return;
      }

      Map<String, MenuLayout> desired = this.getLayoutsForPlayer(player, menuKey, section);
      Set<String> elementIds = new LinkedHashSet<>(desired.keySet());
      String textPrefix = menuKey + ":";
      List<TextDisplay> trackedTextDisplays = this.playerDisplays.get(player);
      if (trackedTextDisplays != null) {
         trackedTextDisplays.removeIf(display -> display == null || Bukkit.isOwnedByCurrentRegion(display) && !display.isValid());
      }
      for (TextDisplay display : this.playerDisplays.getOrDefault(player, Collections.emptyList())) {
         String customName = this.trackedMenuEntityName(player, display);
         if (customName != null && customName.startsWith(textPrefix)) {
            elementIds.add(customName.substring(textPrefix.length()));
         }
      }

      String itemSuffix = ":item";
      List<ItemDisplay> trackedItemDisplays = this.playerMenuItemDisplays.get(player);
      if (trackedItemDisplays != null) {
         trackedItemDisplays.removeIf(display -> display == null || Bukkit.isOwnedByCurrentRegion(display) && !display.isValid());
      }
      for (ItemDisplay display : this.playerMenuItemDisplays.getOrDefault(player, Collections.emptyList())) {
         String customName = this.trackedMenuEntityName(player, display);
         if (customName != null && customName.startsWith(textPrefix) && customName.endsWith(itemSuffix)) {
            elementIds.add(customName.substring(textPrefix.length(), customName.length() - itemSuffix.length()));
         }
      }

      for (String elementId : elementIds) {
         this.refreshMenuElement(player, menuKey, elementId, false, section, desired);
      }
   }

   private boolean refreshMenuElement(Player player, String menuKey, String elementId, boolean replaceExisting) {
      if (player == null || !player.isOnline()) return false;
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         foliaLib.scheduling().entitySpecificScheduler(player).run(() -> this.refreshMenuElement(player, menuKey, elementId, replaceExisting), null);
         return true;
      }
      if (player == null
         || !player.isOnline()
         || menuKey == null
         || elementId == null
         || !menuKey.equals(this.getCurrentPlayerMenu(player))) {
         return false;
      }

      Section section = this.getRuntimeSection(player, menuKey);
      World world = section == null ? null : Bukkit.getWorld(section.world);
      if (section == null || world == null) {
         return false;
      }

      return this.refreshMenuElement(player, menuKey, elementId, replaceExisting, section, this.getLayoutsForPlayer(player, menuKey, section));
   }

   private boolean refreshMenuElement(
      Player player,
      String menuKey,
      String elementId,
      boolean replaceExisting,
      Section section,
      Map<String, MenuLayout> layouts
   ) {
      this.playerDisplays.computeIfPresent(player, (viewer, displays) -> displays instanceof CopyOnWriteArrayList ? displays : new CopyOnWriteArrayList<>(displays));
      this.playerMenuItemDisplays.computeIfPresent(player, (viewer, displays) -> displays instanceof CopyOnWriteArrayList ? displays : new CopyOnWriteArrayList<>(displays));
      World world = Bukkit.getWorld(section.world);
      if (world == null) {
         return false;
      }
      MenuLayout layout = layouts.get(elementId);
      if (layout == null || layout.interaction().group()) {
         return this.removeRenderedMenuElement(player, menuKey, elementId);
      }

      DynamicPositionSpec.Position position = this.resolveLayoutPosition(player, menuKey, layout, layouts);
      Location location = this.toLayoutWorldLocation(section, layout, position);
      float zoom = menuZoomFactor(menuZooms.get(player.getUniqueId()), layout, layouts);
      if (location == null) {
         return false;
      }

      TextDisplay textDisplay = this.findTrackedTextDisplay(player, menuKey, elementId);
      ItemDisplay itemDisplay = this.findTrackedItemDisplay(player, menuKey, elementId);
      boolean wrongRepresentation = layout.isItemLayout() ? itemDisplay == null && textDisplay != null : itemDisplay != null;
      if (replaceExisting || wrongRepresentation) {
         this.removeRenderedMenuElement(player, menuKey, elementId);
         textDisplay = null;
         itemDisplay = null;
      }

      boolean textMissing = textDisplay == null || Bukkit.isOwnedByCurrentRegion(textDisplay) && !textDisplay.isValid();
      boolean itemMissing = itemDisplay == null || Bukkit.isOwnedByCurrentRegion(itemDisplay) && !itemDisplay.isValid();
      if (textMissing || layout.isItemLayout() && itemMissing) {
         this.spawnReconciledMenuDisplays(player, menuKey, layout, location, textMissing, layout.isItemLayout() && itemMissing, zoom);
      }

      if (layout.isItemLayout()) {
         if (!itemMissing) {
            this.updateMenuItemDisplay(player, itemDisplay, location, menuKey, layout, zoom);
         }
         if (!textMissing) {
            this.updateMenuItemHitbox(player, textDisplay, location, layout, zoom);
         }
         return true;
      }

      if (!textMissing) {
         this.updateMenuTextDisplay(player, textDisplay, location, layout, zoom);
      }
      return true;
   }

   private boolean removeRenderedMenuElement(Player player, String menuKey, String elementId) {
      boolean removed = false;
      String textName = menuKey + ":" + elementId;
      List<TextDisplay> textDisplays = this.playerDisplays.get(player);
      if (textDisplays != null) {
         for (TextDisplay display : new ArrayList<>(textDisplays)) {
            if (display != null && textName.equals(this.trackedMenuEntityName(player, display))) {
               textDisplays.remove(display);
               this.forgetCutsceneDisplay(player, display);
               this.removeMenuEntity(display);
               removed = true;
            }
         }
      }

      String itemName = textName + ":item";
      List<ItemDisplay> itemDisplays = this.playerMenuItemDisplays.get(player);
      if (itemDisplays != null) {
         for (ItemDisplay display : new ArrayList<>(itemDisplays)) {
            if (display != null && itemName.equals(this.trackedMenuEntityName(player, display))) {
               itemDisplays.remove(display);
               this.forgetCutsceneDisplay(player, display);
               this.removeMenuEntity(display);
               removed = true;
            }
         }
      }
      return removed;
   }

   private TextDisplay findTrackedTextDisplay(Player player, String menuKey, String elementId) {
      return this.getTextDisplayByKeys(player, menuKey, elementId);
   }

   private ItemDisplay findTrackedItemDisplay(Player player, String menuKey, String elementId) {
      String expectedName = menuKey + ":" + elementId + ":item";
      for (ItemDisplay display : this.playerMenuItemDisplays.getOrDefault(player, Collections.emptyList())) {
         if (display != null && (!Bukkit.isOwnedByCurrentRegion(display) || display.isValid())
            && expectedName.equals(this.trackedMenuEntityName(player, display))) {
            return display;
         }
      }
      return null;
   }

   private final Set<String> pendingMenuElementSpawns = ConcurrentHashMap.newKeySet();

   private void forgetCutsceneDisplay(Player player, Entity display) {
      MenuClientView clientView = menuClientViews.get(player.getUniqueId());
      if (clientView != null) clientView.forget(display.getEntityId());
      CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
      if (view != null) {
         view.names.remove(display); view.locations.remove(display); view.animationPositions.remove(display);
         if (display instanceof TextDisplay text) view.hitboxes.remove(text);
         view.dirty = true;
      }
   }

   /** Resolve viewer data on the viewer owner, then create missing displays on their destination owner. */
   private void spawnReconciledMenuDisplays(Player player, String menuKey, MenuLayout layout, Location location,
                                           boolean textMissing, boolean itemMissing, float zoom) {
      long generation = this.getMenuSessionGeneration(player);
      CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
      String pendingKey = player.getUniqueId() + ":" + generation + ":" + menuKey + ":" + layout.key;
      if (!this.pendingMenuElementSpawns.add(pendingKey)) return;
      if (view != null && view.moving) {
         foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(() -> {
            this.pendingMenuElementSpawns.remove(pendingKey);
            if (this.isMenuSessionCurrent(player, generation)) this.refreshMenuElement(player, menuKey, layout.key, false);
         }, () -> this.pendingMenuElementSpawns.remove(pendingKey), 1L);
         return;
      }
      if (Bukkit.isOwnedByCurrentRegion(location)) {
         try {
            if (itemMissing) {
               ItemDisplay item = this.spawnMenuItemDisplay(player, location.getWorld(), location, menuKey, layout);
               this.scaleMenuDisplay(item, zoom);
               this.playerMenuItemDisplays.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(item);
               if (view != null) { view.names.put(item, menuKey + ":" + layout.key + ":item"); view.locations.put(item, item.getLocation()); }
            }
            if (textMissing) {
               TextDisplay text = layout.isItemLayout() ? this.spawnMenuItemHitbox(player, location.getWorld(), location, menuKey, layout)
                  : this.spawnMenuTextDisplay(player, location.getWorld(), location, menuKey, layout);
               this.scaleMenuDisplay(text, zoom);
               this.playerDisplays.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(text);
               if (view != null) {
                  view.names.put(text, menuKey + ":" + layout.key); view.locations.put(text, text.getLocation());
                  view.hitboxes.put(text, TextDisplayHitBox.snapshot(text));
               }
            }
            if (view != null) view.dirty = true;
         } finally { this.pendingMenuElementSpawns.remove(pendingKey); }
         return;
      }

      try {
      net.kyori.adventure.text.Component rendered = textMissing && !layout.isItemLayout() ? layout.renderStyledComponent(player, false, false) : null;
      ItemStack stack = itemMissing ? this.createMenuItemStack(player, menuKey, layout) : null;
      boolean glow = itemMissing && layout.resolveItemGlow(player);
      boolean visibleByDefault = !this.isMenuHiddenFromOtherPlayers(player);
      Billboard billboard = this.parseBillboard(layout.getItemBillboard());
      ItemDisplayTransform transform = this.resolveMenuItemTransform(layout);
      String textName = menuKey + ":" + layout.key;
      Object token = this.menuSessionTokens.get(player.getUniqueId());
      location.getWorld().getChunkAtAsync(location.getBlockX() >> 4, location.getBlockZ() >> 4, true).whenComplete((chunk, loadingError) -> {
      if (loadingError != null || !isEnabled() || token != menuSessionTokens.get(player.getUniqueId())) {
         pendingMenuElementSpawns.remove(pendingKey); return;
      }
      boolean scheduled = org.orvencraft.uicore.scheduler.CleanupTasks.region(this, location, () -> {
         List<Display> created = new ArrayList<>();
         try {
            if (generation != this.getMenuSessionGeneration(player) || token != this.menuSessionTokens.get(player.getUniqueId())
               || this.cutsceneViews.get(player.getUniqueId()) != view) { this.pendingMenuElementSpawns.remove(pendingKey); return; }
            ItemDisplay item = null;
            TextDisplay text = null;
            if (itemMissing) {
               item = this.tagMenuEntity(location.getWorld().spawn(location, ItemDisplay.class, entity -> entity.setVisibleByDefault(false)));
               created.add(item);
               item.setCustomName(textName + ":item"); item.setCustomNameVisible(false);
               item.setItemStack(stack); item.setGlowing(glow); item.setViewRange(100F);
               item.setBillboard(billboard); item.setItemDisplayTransform(transform); this.setTeleportDurationSafe(item, 2);
               Transformation previous = item.getTransformation();
               item.setTransformation(new Transformation(previous.getTranslation(),
                  previous.getLeftRotation().rotationYXZ((float)Math.toRadians(layout.tiltY), (float)Math.toRadians(layout.tiltX), (float)Math.toRadians(layout.tiltZ)),
                  new Vector3f((float)layout.getItemScale()), previous.getRightRotation()));
               if (layout.isPlayerAvatarLayout() && stack.getType() == Material.PLAYER_HEAD) {
                  item.setTransformation(AvatarDisplayTransform.create(layout.getItemScale(), layout.isAvatarFaceOnly(), layout.tiltX, layout.tiltY, layout.tiltZ, transform));
                  item.setBrightness(new Display.Brightness(15, 15));
               }
            }
            if (textMissing) {
               text = this.tagMenuEntity(location.getWorld().spawn(location, TextDisplay.class, entity -> entity.setVisibleByDefault(false)));
               created.add(text);
               text.setCustomName(textName); text.setCustomNameVisible(false);
               text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); text.setDefaultBackground(false);
               text.setShadowed(!layout.isItemLayout()); text.setBillboard(Billboard.CENTER);
               this.updateMenuHitboxTag(text, layout);
               if (layout.isItemLayout()) {
                  text.setText(layout.getHitboxText()); text.addScoreboardTag(TextDisplayHitBox.ITEM_HITBOX_TAG);
               } else {
                  text.text(rendered);
                  Transformation previous = text.getTransformation();
                  text.setTransformation(new Transformation(previous.getTranslation(),
                     previous.getLeftRotation().rotationYXZ((float)Math.toRadians(layout.tiltY), (float)Math.toRadians(layout.tiltX), (float)Math.toRadians(layout.tiltZ)),
                     new Vector3f((float)layout.getTextSize()), previous.getRightRotation()));
                  if (layout.isTypewriterEnabled()) textDisplayManager.getTypewriterEffect().applyEffect(text, rendered,
                     layout.getTypewriterSpeed(), layout.getTypewriterStartDelay(), layout.getTypewriterCursorChar());
               }
            }
            for (Display display : created) {
               this.scaleMenuDisplay(display, zoom);
               display.setVisibleByDefault(visibleByDefault);
            }
            TextDisplay newText = text;
            ItemDisplay newItem = item;
            TextDisplayHitBox.Snapshot hitbox = text == null ? null : TextDisplayHitBox.snapshot(text);
            Runnable discard = () -> { this.pendingMenuElementSpawns.remove(pendingKey); created.forEach(this::removeMenuEntity); };
            boolean adopted = org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, player, () -> {
               this.pendingMenuElementSpawns.remove(pendingKey);
               if (!this.isMenuSessionCurrent(player, generation) || token != this.menuSessionTokens.get(player.getUniqueId())
                  || this.cutsceneViews.get(player.getUniqueId()) != view) { created.forEach(this::removeMenuEntity); return; }
               Section current = this.getRuntimeSection(player, menuKey);
               MenuLayout desired = current == null ? null : this.getLayoutsForPlayer(player, menuKey, current).get(layout.key);
               if (desired == null || desired.isItemLayout() != layout.isItemLayout()) { created.forEach(this::removeMenuEntity); return; }
               if (newText != null) {
                  if (this.findTrackedTextDisplay(player, menuKey, layout.key) != null) this.removeMenuEntity(newText);
                  else {
                     this.playerDisplays.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(newText);
                     if (view != null) { view.names.put(newText, textName); view.locations.put(newText, location.clone()); view.hitboxes.put(newText, hitbox); }
                     player.showEntity(this, newText);
                     if (!layout.isItemLayout()) org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, newText,
                        () -> { if (this.isMenuSessionCurrent(player, generation) && newText.isValid()) this.applyMenuTextFloatingAnimation(player, newText, layout); });
                  }
               }
               if (newItem != null) {
                  if (this.findTrackedItemDisplay(player, menuKey, layout.key) != null) this.removeMenuEntity(newItem);
                  else {
                     this.playerMenuItemDisplays.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(newItem);
                     if (view != null) { view.names.put(newItem, textName + ":item"); view.locations.put(newItem, location.clone()); }
                     player.showEntity(this, newItem);
                     if (layout.isItemRotate()) this.startReconciledItemRotation(player, newItem, layout, generation);
                  }
               }
               if (view != null) view.dirty = true;
               this.refreshMenuElement(player, menuKey, layout.key, false);
            }, discard);
            if (!adopted) discard.run();
         } catch (RuntimeException failure) {
            created.forEach(this::removeMenuEntity); this.pendingMenuElementSpawns.remove(pendingKey);
            this.getLogger().warning("Cannot refresh menu element '" + textName + "': " + failure.getMessage());
         }
      });
      if (!scheduled) this.pendingMenuElementSpawns.remove(pendingKey);
      });
      } catch (RuntimeException failure) {
         this.pendingMenuElementSpawns.remove(pendingKey);
         throw failure;
      }
   }

   private void startReconciledItemRotation(Player player, ItemDisplay display, MenuLayout layout, long generation) {
      org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
         if (!this.isMenuSessionCurrent(player, generation) || !display.isValid()) return;
         ScheduledTask[] task = {null};
         task[0] = foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(() -> {
            if (generation != this.getMenuSessionGeneration(player) || !display.isValid()
               || !this.playerMenuItemDisplays.getOrDefault(player, List.of()).contains(display)) {
               if (task[0] != null) task[0].cancel();
               return;
            }
            if (this.isCutsceneUiFrozen(player)) return;
            Transformation current = display.getTransformation();
            display.setTransformation(new Transformation(current.getTranslation(), current.getLeftRotation().rotateAxis(
               (float)Math.toRadians(layout.getItemRotateSpeed()), 0F, layout.isAvatarFaceOnly() ? 0F : 1F,
               layout.isAvatarFaceOnly() ? 1F : 0F), current.getScale(), current.getRightRotation()));
         }, null, 1L, 1L);
         if (task[0] != null) this.playerMenuItemAnimationTasks.computeIfAbsent(player, ignored -> new CopyOnWriteArrayList<>()).add(task[0]);
      });
   }

   private TextDisplay spawnMenuTextDisplay(Player player, World world, Location location, String menuKey, MenuLayout layout) {
      TextDisplay display = this.tagMenuEntity((TextDisplay)world.spawn(location, TextDisplay.class, created -> created.setVisibleByDefault(false)));
      display.setCustomName(menuKey + ":" + layout.key);
      display.setCustomNameVisible(false);
      display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      display.setDefaultBackground(false);
      display.setShadowed(true);
      display.setBillboard(Billboard.CENTER);
      display.setVisibleByDefault(false);
      this.updateMenuHitboxTag(display, layout);

      String rendered = layout.renderStyledName(player, false, false);
      if (layout.isTypewriterEnabled()) {
         display.setText(layout.getTypewriterCursorChar());
         textDisplayManager.getTypewriterEffect()
            .applyEffect(display, layout.renderStyledComponent(player, false, false), layout.getTypewriterSpeed(), layout.getTypewriterStartDelay(), layout.getTypewriterCursorChar());
      } else {
         display.text(layout.renderStyledComponent(player, false, false));
      }

      float tiltX = (float)Math.toRadians(layout.tiltX);
      float tiltY = (float)Math.toRadians(layout.tiltY);
      float tiltZ = (float)Math.toRadians(layout.tiltZ);
      Transformation oldTransformation = display.getTransformation();
      display.setTransformation(
         new Transformation(
            oldTransformation.getTranslation(),
            oldTransformation.getLeftRotation().rotationYXZ(tiltY, tiltX, tiltZ),
            new Vector3f((float)layout.getTextSize(), (float)layout.getTextSize(), (float)layout.getTextSize()),
            oldTransformation.getRightRotation()
         )
      );
      this.showMenuDisplay(player, display, layout);
      this.applyMenuTextFloatingAnimation(player, display, layout);
      return display;
   }

   private void updateMenuTextDisplay(Player player, TextDisplay display, Location location, MenuLayout layout, float zoom) {
      long generation = this.getMenuSessionGeneration(player);
      boolean hovered = this.hoverEnlargeManager != null && this.hoverEnlargeManager.isCurrentHovered(player, layout);
      net.kyori.adventure.text.Component rendered = layout.renderStyledComponent(player, hovered, false);
      CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
      if (view != null) view.dirty = true;
      org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
         if (generation != this.getMenuSessionGeneration(player) || !display.isValid()
            || !this.playerDisplays.getOrDefault(player, List.of()).contains(display)) return;
         CutsceneView current = this.cutsceneViews.get(player.getUniqueId());
         if (current == null && !display.getLocation().equals(location)) moveMenuEntity(display, location);
         this.updateMenuHitboxTag(display, layout);
         this.scaleMenuDisplay(display, zoom);
         if ((!layout.isTypewriterEnabled() || !textDisplayManager.getTypewriterEffect().isRunning(display)) && !Objects.equals(display.text(), rendered)) {
            display.text(rendered);
         }
         if (current != null) current.hitboxes.put(display, TextDisplayHitBox.snapshot(display));
      });
   }

   private void updateMenuItemDisplay(Player player, ItemDisplay display, Location location, String menuKey, MenuLayout layout, float zoom) {
      ItemStack stack = this.createMenuItemStack(player, menuKey, layout);
      boolean glow = layout.resolveItemGlow(player);
      Billboard billboard = this.parseBillboard(layout.getItemBillboard());
      ItemDisplayTransform transform = this.resolveMenuItemTransform(layout);
      long generation = this.getMenuSessionGeneration(player);
      CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
      if (view != null) view.dirty = true;
      org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
         if (generation != this.getMenuSessionGeneration(player) || !display.isValid()
            || !this.playerMenuItemDisplays.getOrDefault(player, List.of()).contains(display)) return;
         if (!stack.equals(display.getItemStack())) display.setItemStack(stack);
         if (display.isGlowing() != glow) display.setGlowing(glow);
         if (display.getBillboard() != billboard) display.setBillboard(billboard);
         if (display.getItemDisplayTransform() != transform) display.setItemDisplayTransform(transform);
         this.scaleMenuDisplay(display, zoom);
         if (!this.cutsceneViews.containsKey(player.getUniqueId()) && !display.getLocation().equals(location)) moveMenuEntity(display, location);
      });
   }

   private ItemStack createMenuItemStack(Player player, String menuKey, MenuLayout layout) {
      PlayerProfile avatarProfile = layout.isPlayerAvatarLayout()
         ? this.resolveAvatarProfile(player, layout.getAvatarPlayer(), menuKey, layout.key)
         : null;
      String materialName = layout.resolveItemMaterial(player);
      Material material = Material.matchMaterial(materialName);
      if (material == null) {
         material = Material.DIAMOND;
         this.getLogger().warning("Invalid item material for menu layout " + menuKey + ":" + layout.key);
      }
      ItemStack stack = new ItemStack(material);
      ItemMeta meta = stack.getItemMeta();
      if (meta != null && layout.resolveItemCustomModelData(player) > 0) {
         meta.setCustomModelData(layout.resolveItemCustomModelData(player));
      }

      if (layout.isPlayerAvatarLayout() && meta instanceof SkullMeta skullMeta) {
         if (avatarProfile != null) {
            skullMeta.setOwnerProfile(avatarProfile);
         }
      }
      if (meta != null) {
         stack.setItemMeta(meta);
      }
      return stack;
   }

   private ItemDisplayTransform resolveMenuItemTransform(MenuLayout layout) {
      return layout.isAvatarFaceOnly()
         ? ItemDisplayTransform.HEAD
         : this.parseItemDisplayTransform(layout.getItemDisplayTransform());
   }

   private void updateMenuItemHitbox(Player player, TextDisplay hitbox, Location location, MenuLayout layout, float zoom) {
      long generation = this.getMenuSessionGeneration(player);
      String text = layout.getHitboxText();
      CutsceneView view = this.cutsceneViews.get(player.getUniqueId());
      if (view != null) view.dirty = true;
      org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, hitbox, () -> {
         if (generation != this.getMenuSessionGeneration(player) || !hitbox.isValid()
            || !this.playerDisplays.getOrDefault(player, List.of()).contains(hitbox)) return;
         if (!hitbox.getScoreboardTags().contains(TextDisplayHitBox.ITEM_HITBOX_TAG)) hitbox.addScoreboardTag(TextDisplayHitBox.ITEM_HITBOX_TAG);
         if (!Objects.equals(hitbox.getText(), text)) hitbox.setText(text);
         CutsceneView current = this.cutsceneViews.get(player.getUniqueId());
         if (current == null && !hitbox.getLocation().equals(location)) moveMenuEntity(hitbox, location);
         this.updateMenuHitboxTag(hitbox, layout);
         this.scaleMenuDisplay(hitbox, zoom);
         if (current != null) current.hitboxes.put(hitbox, TextDisplayHitBox.snapshot(hitbox));
      });
   }

   private void updateMenuHitboxTag(TextDisplay display, MenuLayout layout) {
      String desiredTag = layout.hasCustomHitbox() || layout.isItemLayout()
         ? String.format(Locale.US, "uicore_hitbox:%.3f:%.3f:%.3f", layout.getHitboxWidth(), layout.getHitboxHeight(), layout.getHitboxDepth())
         : null;
      List<String> currentTags = display.getScoreboardTags().stream().filter(tag -> tag.startsWith("uicore_hitbox:")).toList();
      if ((desiredTag == null && currentTags.isEmpty()) || desiredTag != null && currentTags.size() == 1 && desiredTag.equals(currentTags.get(0))) {
         return;
      }
      for (String tag : currentTags) {
         display.removeScoreboardTag(tag);
      }
      if (desiredTag != null) {
         display.addScoreboardTag(desiredTag);
      }
   }

   private PlayerProfile resolveAvatarProfile(Player viewer, String configuredPlayer, String menuKey, String elementId) {
      if (viewer == null) {
         return null;
      }

      String owner = parsePlaceholders(viewer, configuredPlayer == null ? "self" : configuredPlayer).trim();
      if (owner.isEmpty()
         || owner.equalsIgnoreCase("self")
         || owner.equalsIgnoreCase("viewer")
         || owner.equalsIgnoreCase("@s")
         || owner.equalsIgnoreCase(viewer.getName())
         || owner.equalsIgnoreCase(viewer.getUniqueId().toString())) {
         return viewer.getPlayerProfile();
      }

      UUID ownerId = null;
      try {
         ownerId = UUID.fromString(owner);
      } catch (IllegalArgumentException ignored) {
      }

      Player onlineOwner = ownerId == null ? Bukkit.getPlayerExact(owner) : Bukkit.getPlayer(ownerId);
      if (onlineOwner != null) {
         return onlineOwner.getPlayerProfile();
      }

      String cacheKey = ownerId == null ? "name:" + owner.toLowerCase(Locale.ROOT) : "uuid:" + ownerId;
      PlayerProfile cachedProfile = this.avatarProfileCache.get(cacheKey);
      if (cachedProfile != null && cachedProfile.isComplete()) {
         return cachedProfile;
      }

      PlayerProfile profile = cachedProfile;
      if (profile == null) {
         try {
            if (ownerId != null) {
               OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerId);
               profile = offlinePlayer.getPlayerProfile();
            } else {
               OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(owner);
               profile = offlinePlayer == null ? Bukkit.createPlayerProfile(owner) : offlinePlayer.getPlayerProfile();
            }
         } catch (IllegalArgumentException exception) {
            if (this.debugMode) {
               this.getLogger().warning("Invalid avatar player for " + menuKey + ":" + elementId + ": " + owner);
            }
            return null;
         }
         this.avatarProfileCache.put(cacheKey, profile);
      }

      if (!profile.isComplete() && this.avatarProfileLoads.add(cacheKey)) {
         PlayerProfile requestedProfile = profile;
         requestedProfile.update().whenComplete((updatedProfile, error) -> {
            this.avatarProfileLoads.remove(cacheKey);
            if (error != null || updatedProfile == null) {
               if (this.debugMode && error != null) {
                  this.getLogger().warning("Could not load avatar skin for '" + owner + "': " + error.getMessage());
               }
               return;
            }

            this.avatarProfileCache.put(cacheKey, updatedProfile);
            if (this.isEnabled()) {
               long profileGeneration = this.getMenuSessionGeneration(viewer);
               foliaLib.scheduling().entitySpecificScheduler(viewer).run(() -> {
                  if (this.isMenuSessionCurrent(viewer, profileGeneration)) this.refreshMenuElement(viewer, menuKey, elementId, false);
               }, null);
            }
         });
      }
      return profile;
   }

   private ItemDisplayTransform parseItemDisplayTransform(String value) {
      if (value == null || value.isBlank()) {
         return ItemDisplayTransform.NONE;
      }

      try {
         return ItemDisplayTransform.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ignored) {
         return ItemDisplayTransform.NONE;
      }
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
      closeDiggingInput(var1);
      try {
         if (var2 != var1) {
            MenuClientView previous = menuClientViews.remove(var1.getUniqueId());
            if (previous != null) previous.close(var1.getEntityId(), var1.getGameMode().getValue());
            var user = PacketEvents.getAPI().getPlayerManager().getUser(var1);
            boolean digging = isBlockDiggingMenu(var1);
            byte playerFlags = digging ? MenuClientView.readPlayerFlags(
               io.github.retrooper.packetevents.util.SpigotConversionUtil.getEntityMetadata(var1)) : 0;
            var clientVersion = user.getClientVersion();
            var diggingAbilities = digging && clientVersion != null && clientVersion.isNewerThanOrEquals(
               com.github.retrooper.packetevents.protocol.player.ClientVersion.V_26_1)
               ? new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities(
                  false, false, false, false, var1.getFlySpeed() / 2f, var1.getWalkSpeed() / 2f)
               : null;
            var clientView = new MenuClientView(packets -> ChannelHelper.runInEventLoop(user.getChannel(), () -> {
               user.writePacketSilently(new WrapperPlayServerBundle());
               packets.forEach(user::writePacketSilently);
               user.writePacketSilently(new WrapperPlayServerBundle());
               user.flushPackets();
            }), digging, var1.getEntityId(), playerFlags, diggingAbilities);
            menuClientViews.put(var1.getUniqueId(), clientView);
            Location eye = ((org.bukkit.entity.LivingEntity)var2).getEyeLocation();
            // The physical camera carrier would intercept the survival client's block ray.
            if (digging) var1.hideEntity(this, var2);
            else var1.showEntity(this, var2);
            clientView.open(cameraPose(eye));
            openDiggingInput(var1, eye);
            return;
         }
         WrapperPlayServerCamera var3 = new WrapperPlayServerCamera(var2.getEntityId());
         PacketEvents.getAPI().getPlayerManager().sendPacket(var1, var3);
      } catch (Exception var4) {
         this.getLogger().warning("Failed to send camera packet: " + var4.getMessage());
         var4.printStackTrace();
         if (var2 != var1) this.stopCursor(var1, true);
      }
   }

   private static CameraPose cameraPose(Location location) {
      return new CameraPose(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
   }

   private double calculateCursor(double var1, double var3) {
      return var3 <= 0.0 ? var1 + Math.abs(var3) : var1 - Math.abs(var3);
   }

   private void updateCursorPosition(Player var1, float var2, float var3) {
      CutsceneView cutsceneView = cutsceneViews.get(var1.getUniqueId());
      if (cutsceneView != null) {
         boolean enabled = !cutsceneManager.active(var1) || cutsceneView.definition.policy().cursor();
         if (enabled) {
            double rangeX = Math.abs(maxX * cursorSensitivity), rangeY = Math.abs(maxY * cursorSensitivity);
            cutsceneView.input.update(var2, var3, maxX * cursorSensitivity / 90, maxY * cursorSensitivity / 90,
               cursorMovementRangeEnabled ? cursorMovementRangeXMin : cursorX - rangeX,
               cursorMovementRangeEnabled ? cursorMovementRangeXMax : cursorX + rangeX,
               cursorMovementRangeEnabled ? cursorMovementRangeYMin : cursorY - rangeY,
               cursorMovementRangeEnabled ? cursorMovementRangeYMax : cursorY + rangeY, Math.max(.01, Math.min(1, cursorSmoothing)));
            cutsceneView.dirty = true;
         } else cutsceneView.input.rebase(var2, var3);
         return;
      }
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
            Location var9 = var30.cameraViewpointCoordinates ? var8.getEyeLocation() : var8.getLocation();
            Vector var10 = var9.getDirection().normalize();
            Vector var11 = new Vector(-var10.getZ(), 0.0, var10.getX()).normalize();
            Vector var12 = var10.getCrossProduct(var11).multiply(-1);
            double var13 = cursorSensitivity;
            double horizontalScale = maxX * var13 / 90.0;
            double horizontalRange = Math.abs(maxX * var13);
            double verticalRange = Math.abs(maxY * var13);
            double minX = cursorMovementRangeEnabled ? Math.min(cursorMovementRangeXMin, cursorMovementRangeXMax) : cursorX - horizontalRange;
            double maxXValue = cursorMovementRangeEnabled ? Math.max(cursorMovementRangeXMin, cursorMovementRangeXMax) : cursorX + horizontalRange;
            double minY = cursorMovementRangeEnabled ? Math.min(cursorMovementRangeYMin, cursorMovementRangeYMax) : cursorY - verticalRange;
            double maxYValue = cursorMovementRangeEnabled ? Math.max(cursorMovementRangeYMin, cursorMovementRangeYMax) : cursorY + verticalRange;
            double minYaw = Math.abs(horizontalScale) < 1.0E-9 ? 0.0 : (minX - cursorX) / horizontalScale;
            double maxYaw = Math.abs(horizontalScale) < 1.0E-9 ? 0.0 : (maxXValue - cursorX) / horizontalScale;
            CursorRotationTracker rotationTracker = this.cursorRotationTrackers
               .computeIfAbsent(var1.getUniqueId(), ignored -> new CursorRotationTracker());
            double var15 = rotationTracker.update(var2, var9.getYaw(), minYaw, maxYaw);
            double var17 = var3 - var9.getPitch();
            double var19 = Math.max(minX, Math.min(maxXValue, var15 * horizontalScale + cursorX));
            double var21 = Math.max(minY, Math.min(maxYValue, var17 * maxY * var13 / 90.0 + cursorY));

            this.updateCameraEdgePanTarget(var1, var19, var21);

             CameraEdgePan.Offset edgePanOffset = this.cameraEdgePanOffsets
                .getOrDefault(var1.getUniqueId(), CameraEdgePan.Offset.ZERO);
            Vector var23 = var11.clone()
               .multiply(var19 + edgePanOffset.right())
               .add(var12.clone().multiply(-var21 + edgePanOffset.up()));
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

             if (!Bukkit.isOwnedByCurrentRegion(var27)) return;
             moveMenuEntity(var31, var27);
             moveMenuEntity(var7, var27);
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
      double minX = cursorMovementRangeEnabled ? Math.min(cursorMovementRangeXMin, cursorMovementRangeXMax) : cursorX - horizontalRange;
      double maxXValue = cursorMovementRangeEnabled ? Math.max(cursorMovementRangeXMin, cursorMovementRangeXMax) : cursorX + horizontalRange;
      double minY = cursorMovementRangeEnabled ? Math.min(cursorMovementRangeYMin, cursorMovementRangeYMax) : cursorY - verticalRange;
      double maxYValue = cursorMovementRangeEnabled ? Math.max(cursorMovementRangeYMin, cursorMovementRangeYMax) : cursorY + verticalRange;
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
      if (isBlockDiggingMenu(player) || dragGestures.containsKey(player.getUniqueId())) return;
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
       if (next.equals(current) && next.equals(target)) return;
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
       Vector previousOffset = right.clone().multiply(current.right()).add(up.clone().multiply(current.up()));
       Vector nextOffset = right.clone().multiply(next.right()).add(up.clone().multiply(next.up()));
       Vector delta = nextOffset.clone().subtract(previousOffset);
       Location desired = base.clone().add(nextOffset);
      camera.setVelocity(new Vector());
      this.sendCameraPositionPacket(player, camera, desired);
      if (delta.lengthSquared() > 1.0E-12) {
         this.translateCursorWithCamera(player, delta);
      }

      if (next.equals(CameraEdgePan.Offset.ZERO) && target.equals(CameraEdgePan.Offset.ZERO)) {
         this.cameraEdgePanOffsets.remove(playerId);
         this.cameraEdgePanTargets.remove(playerId);
      }
   }

   private void sendCameraPositionPacket(Player player, Entity camera, Location location) {
      if (player == null || camera == null || location == null) {
         return;
      }

      try {
         MenuClientView clientView = menuClientViews.get(player.getUniqueId());
         if (clientView != null) {
            Location eye = location.clone().add(0, ((org.bukkit.entity.LivingEntity)camera).getEyeHeight(), 0);
            clientView.render(cameraPose(eye), Map.of());
            return;
         }
         WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
            camera.getEntityId(),
            new Vector3d(location.getX(), location.getY(), location.getZ()),
            location.getYaw(),
            location.getPitch(),
            false
         );
         PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
      } catch (Exception exception) {
         if (this.debugMode) {
            this.getLogger().warning("Failed to move menu camera for edge pan: " + exception.getMessage());
         }
      }
   }

   private void translateCursorWithCamera(Player player, Vector delta) {
      ArmorStand cursorStand = playerCursors.get(player);
      if (cursorStand != null && cursorStand.isValid()) {
         Location target = cursorStand.getLocation().add(delta);
         if (Bukkit.isOwnedByCurrentRegion(target)) moveMenuEntity(cursorStand, target);
      }

      ItemDisplay cursorDisplay = this.playerItemDisplays.get(player);
      if (cursorDisplay != null && cursorDisplay.isValid()) {
         Location target = cursorDisplay.getLocation().add(delta);
         if (Bukkit.isOwnedByCurrentRegion(target)) moveMenuEntity(cursorDisplay, target);
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

   private void beginDynamicMenuSession(Player player, MenuZoom zoom) {
      closeDiggingInput(player);
      finishDrag(player, true);
      draggedPositions.remove(player.getUniqueId());
      closeMenuZoom(player);
      if (zoom != null) {
         int heldSlot = player.getInventory().getHeldItemSlot();
         menuZooms.put(player.getUniqueId(), new ZoomState(zoom, getMenuSessionGeneration(player), heldSlot));
      }
      if (player != null) {
         UUID playerId = player.getUniqueId();
         this.menuOpenedAtTicks.put(playerId, this.playerMenuTicks.getOrDefault(playerId, 0L));
         this.dynamicElementPositions.remove(playerId);
      }
   }

   private void clearDynamicMenuSession(Player player) {
      closeDiggingInput(player);
      finishDrag(player, true);
      draggedPositions.remove(player.getUniqueId());
      closeMenuZoom(player);
      debugMenus.remove(player.getUniqueId());
      if (player != null) {
         UUID playerId = player.getUniqueId();
         this.menuOpenedAtTicks.remove(playerId);
         this.dynamicElementPositions.remove(playerId);
      }
   }

   private long getMenuElapsedTicks(Player player) {
      Long openedAt = player == null ? null : this.menuOpenedAtTicks.get(player.getUniqueId());
      return openedAt == null ? 0L : Math.max(0L, this.playerMenuTicks.getOrDefault(player.getUniqueId(), 0L) - openedAt);
   }

   private static final NamespacedKey DISPLAY_ZOOM = new NamespacedKey("uicore", "display_zoom");
   private final Map<UUID, ZoomState> menuZooms = new ConcurrentHashMap<>();

   private static final class ZoomState {
      final MenuZoom config;
      final long generation;
      final java.util.concurrent.atomic.AtomicInteger clientSlot;
      final java.util.concurrent.ArrayBlockingQueue<Integer> input = new java.util.concurrent.ArrayBlockingQueue<>(16);
      double scale = 1;
      double target = 1;

      ZoomState(MenuZoom config, long generation, int heldSlot) {
         this.config = config;
         this.generation = generation;
         this.clientSlot = new java.util.concurrent.atomic.AtomicInteger(heldSlot);
      }
   }

   private void closeMenuZoom(Player player) {
      if (menuZooms.remove(player.getUniqueId()) != null && player.isOnline()) {
         PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerHeldItemChange(player.getInventory().getHeldItemSlot()));
      }
   }

   private void tickMenuZoom(Player player) {
      UUID id = player.getUniqueId();
      ZoomState zoom = menuZooms.get(id);
      if (zoom == null) return;
      if (!isMenuSessionCurrent(player, zoom.generation) || dragGestures.containsKey(id) || !dragAllowed(player)) {
         zoom.input.clear();
         return;
      }
      for (int i = 0; i < 16; i++) {
         Integer direction = zoom.input.poll();
         if (direction == null) break;
         zoom.target = zoom.config.step(zoom.target, direction);
      }
      if (zoom.scale == zoom.target) return;
      String menu = getCurrentPlayerMenu(player);
      Section section = getRuntimeSection(player, menu);
      if (section == null) return;
      zoom.scale = zoom.config.approach(zoom.scale, zoom.target);
      var layouts = getLayoutsForPlayer(player, menu, section);
      for (MenuLayout layout : layouts.values()) {
         if (!layout.interaction().group() && zoom.config.affects(layout, layouts))
            refreshMenuElement(player, menu, layout.key, false, section, layouts);
      }
   }

   private static float menuZoomFactor(ZoomState zoom, MenuLayout layout, Map<String, MenuLayout> layouts) {
      return zoom != null && zoom.scale != 1 && zoom.config.affects(layout, layouts) ? (float)zoom.scale : 1;
   }

   private static float appliedDisplayZoom(Display display) {
      return display.getPersistentDataContainer().getOrDefault(DISPLAY_ZOOM, PersistentDataType.FLOAT, 1F);
   }

   /** Apply on the entity owner and preserve its current hover, animation, rotation and avatar offsets. */
   private void scaleMenuDisplay(Display display, float scale) {
      float previous = appliedDisplayZoom(display);
      if (previous == scale) return;
      float ratio = scale / previous;
      Transformation transform = display.getTransformation();
      display.setTransformation(new Transformation(new Vector3f(transform.getTranslation()).mul(ratio),
         transform.getLeftRotation(), new Vector3f(transform.getScale()).mul(ratio), transform.getRightRotation()));
      if (scale == 1) display.getPersistentDataContainer().remove(DISPLAY_ZOOM);
      else display.getPersistentDataContainer().set(DISPLAY_ZOOM, PersistentDataType.FLOAT, scale);
      if (hoverEnlargeManager != null) hoverEnlargeManager.scaleHoverBaseline(display.getUniqueId(), ratio);
   }

   private double dragMotionScale(Player player, MenuLayout target) {
      ZoomState zoom = menuZooms.get(player.getUniqueId());
      // A dragged scope root also moves the pivot, so the whole scope follows at its original speed.
      if (zoom == null || zoom.scale == 1 || target.key.equals(zoom.config.group())) return 1;
      String menu = getCurrentPlayerMenu(player);
      return zoom.config.affects(target, getLayoutsForPlayer(player, menu, getRuntimeSection(player, menu))) ? zoom.scale : 1;
   }

   private DynamicPositionSpec.Position resolveLayoutPositionForSpawn(Player player, String menuKey, MenuLayout layout) {
      var layouts = getLayoutsForPlayer(player, menuKey, getRuntimeSection(player, menuKey));
      return resolveLayoutPosition(player, menuKey, layout, layouts);
   }

   private DynamicPositionSpec.Position resolveLayoutPosition(Player player, String menuKey, MenuLayout layout, Map<String, MenuLayout> layouts) {
      java.util.function.Function<MenuLayout, DynamicPositionSpec.Position> local = element -> localLayoutPosition(player, menuKey, element);
      var position = ElementHierarchy.position(layout, layouts, local);
      ZoomState zoom = menuZooms.get(player.getUniqueId());
      if (zoom == null || zoom.scale == 1 || !zoom.config.affects(layout, layouts)) return position;
      MenuLayout group = layouts.get(zoom.config.group());
      var origin = group == null ? new DynamicPositionSpec.Position(0, 0, 0) : ElementHierarchy.position(group, layouts, local);
      return zoom.config.project(position, origin, zoom.scale);
   }

   private DynamicPositionSpec.Position localLayoutPosition(Player player, String menuKey, MenuLayout layout) {
      DynamicPositionSpec.Position dragged = draggedPosition(player, layout.key);
      if (dragged != null) return dragged;
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
      return resolveLayoutPositionForSpawn(player, menuKey, layout);
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
      }
      Map<String, MenuLayout> layouts = getLayoutsForPlayer(player, menuKey, section);
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      for (MenuLayout layout : layouts.values()) {
         if (layout.interaction().group()) continue;
         boolean moving = layout.getDynamicPositionSpec().isDynamic() || !layout.interaction().parent().isEmpty()
            || hasDraggedPosition(player, layout.key);
         if (!moving) continue;
         if (view != null) { view.dirty = true; continue; }
         Location location = toLayoutWorldLocation(section, layout, resolveLayoutPosition(player, menuKey, layout, layouts));
         if (location != null && Bukkit.isOwnedByCurrentRegion(location)) {
            prepareDynamicLayoutMovement(player, menuKey, layout.key);
            moveLayoutEntities(player, menuKey, layout.key, location);
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
         reference -> this.resolveDynamicPositionValue(player, menuKey, layout.key, reference),
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

   private double resolveDynamicPositionValue(Player player, String menuKey, String elementId, String reference) {
      String value;
      if (reference.startsWith("{state.") && reference.endsWith("}")) {
         value = this.getPlayerMenuState(player, menuKey, reference.substring(7, reference.length() - 1));
      } else if (EXTENSION_DATA_PATTERN.matcher(reference).matches()) {
         value = this.extensionManager.resolveData(reference.substring(1, reference.length() - 1), player, menuKey, elementId)
            .map(String::valueOf).orElse("");
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
         throw new IllegalArgumentException("'" + reference + "' did not resolve to a finite number");
      }
   }

   private Location toLayoutWorldLocation(Section section, MenuLayout layout, DynamicPositionSpec.Position position) {
      if (layout.isWorldAnchored()) {
         World world = Bukkit.getWorld(section.world);
         return world == null ? null : new Location(world, position.x(), position.y(), position.z(), section.yaw, section.pitch);
      }
      return toMenuWorldLocation(section, position);
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
      if (!Bukkit.isOwnedByCurrentRegion(target)) return;
      String textName = menuKey + ":" + layoutKey;
      List<TextDisplay> textDisplays = this.playerDisplays.get(player);
      if (textDisplays != null) {
         for (TextDisplay display : textDisplays) {
            if (display != null && display.isValid() && textName.equals(display.getCustomName())) {
               moveMenuEntity(display, target);
            }
         }
      }

      String itemName = textName + ":item";
      List<ItemDisplay> itemDisplays = this.playerMenuItemDisplays.get(player);
      if (itemDisplays != null) {
         for (ItemDisplay display : itemDisplays) {
            if (display != null && display.isValid() && itemName.equals(display.getCustomName())) {
               moveMenuEntity(display, target);
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
      Section privacySection = this.getRuntimeSection(var1, var2);
      if (privacySection != null) this.applyMenuPrivacy(var1, privacySection);
      if (Objects.equals(var2, var3)) {
         this.reconcileMenuContent(var1, var2);
         for (Entity display : new ArrayList<>(this.playerDisplays.getOrDefault(var1, List.of()))) {
            org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
               if (display.isValid()) this.configureMenuEntityVisibility(var1, display);
            });
         }
         for (Entity display : new ArrayList<>(this.playerMenuItemDisplays.getOrDefault(var1, List.of()))) {
            org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, display, () -> {
               if (display.isValid()) this.configureMenuEntityVisibility(var1, display);
            });
         }
         return;
      }
      Section var25 = this.getRuntimeSection(var1, var2);
      if (var25 != null) {
         String var26 = this.getCurrentPlayerInputField(var1);
         List<TextDisplay> var27 = this.playerDisplays.get(var1);
         if (var27 != null) {
            this.clearMenuTextAnimationTasks(var1);
            foliaLib.scheduling().entitySpecificScheduler(var1).run(() -> {
               var27.forEach(var0x -> {
                  if (var0x != null && !var0x.isDead()) {
                     this.removeMenuEntity(var0x);
                  }
               });
               var27.clear();
            }, null);
         }

         this.clearMenuItemDisplays(var1);

         ArrayList var7 = new ArrayList();
         ArrayList<ItemDisplay> menuItemDisplays = new ArrayList<>();
         this.playerDisplays.put(var1, var7);
         this.playerMenuItemDisplays.put(var1, menuItemDisplays);
         World var8 = Bukkit.getWorld(var25.world);
         if (var8 != null) {
            Location var9 = new Location(var8, var25.cameraX, var25.cameraY, var25.cameraZ, var25.yaw, var25.pitch);
            Vector var10 = var9.getDirection().normalize();
            Vector var11 = new Vector(-var10.getZ(), 0.0, var10.getX()).normalize();
            Vector var12 = var10.getCrossProduct(var11).multiply(-1);

            for (MenuLayout var14 : this.getLayoutsForPlayer(var1, var2, var25).values()) {
               if (var14.interaction().group()) continue;
               DynamicPositionSpec.Position layoutPosition = this.resolveLayoutPositionForSpawn(var1, var2, var14);
                Location var16 = this.toLayoutWorldLocation(var25, var14, layoutPosition);
                if (var16 == null || !Bukkit.isOwnedByCurrentRegion(var16)) {
                   continue;
               }
               if (var14.isItemLayout()) {
                  ItemDisplay itemDisplay = this.spawnMenuItemDisplay(var1, var8, var16, var2, var14);
                  TextDisplay hitboxDisplay = this.spawnMenuItemHitbox(var1, var8, var16, var2, var14);
                  menuItemDisplays.add(itemDisplay);
                  var7.add(hitboxDisplay);
                  continue;
               }

               TextDisplay var17 = this.tagMenuEntity((TextDisplay)var8.spawn(var16, TextDisplay.class, created -> created.setVisibleByDefault(false)));
               var7.add(var17);
               String var18 = var14.renderStyledName(var1, false, false);
               String var19 = var18;
               if (var14.isTypewriterEnabled()) {
                  var17.setText(var14.getTypewriterCursorChar());
                  textDisplayManager.getTypewriterEffect()
                     .applyEffect(var17, var14.renderStyledComponent(var1, false, false), var14.getTypewriterSpeed(), var14.getTypewriterStartDelay(), var14.getTypewriterCursorChar());
               } else {
                  var17.text(var14.renderStyledComponent(var1, false, false));
               }

               var17.setCustomName(var2 + ":" + var14.key);
               var17.setCustomNameVisible(false);
               var17.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
               var17.setDefaultBackground(false);
               var17.setShadowed(true);
               var17.setBillboard(Billboard.CENTER);
               var17.setVisibleByDefault(false);
               if (var14.hasCustomHitbox()) {
                  var17.addScoreboardTag(
                     String.format(
                        Locale.US,
                        "uicore_hitbox:%.3f:%.3f:%.3f",
                        var14.getHitboxWidth(),
                        var14.getHitboxHeight(),
                        var14.getHitboxDepth()
                     )
                  );
               }
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
         }
      }
   }

   public void rebuildMenuModules(Player player, String menuKey) {
      if (!Objects.equals(menuKey, this.getCurrentPlayerMenu(player))) return;
      Section section = this.getRuntimeSection(player, menuKey);
      if (section == null) return;
      NPCModule.onMenuClose(player);
      Location camera = new Location(Bukkit.getWorld(section.world), section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
      NPCModule.onMenuOpen(player, camera, section.yaw, section.pitch, menuKey);
   }

   private void runLayout(Player var1, String var2) {
      MenuLayout var3 = this.getLayoutForPlayer(var1, var2);
      if (var3 != null) {
         var3.runCommand(var1);
      }
   }

   private void handleCursorClick(Player player) {
      if (!dragAllowed(player)) return;
      if (dragGestures.containsKey(player.getUniqueId())) { tickDrag(player); finishDrag(player, false); return; }
      MenuLayout layout = pickedLayout(player);
      if (layout == null) return;
      if (layout.interaction().draggable()) beginDrag(player, layout);
      else activateMenuElement(player, layout.key);
   }

   public TextDisplay pickMenuDisplay(Player player) {
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      Location cursor = cursorExactLocations.get(player);
      String menuKey = getCurrentPlayerMenu(player);
      Section section = getRuntimeSection(player, menuKey);
      if (cursor == null || section == null || isMenuTransitionActive(player)) return null;
      Map<String, MenuLayout> layouts = getLayoutsForPlayer(player, menuKey, section);
      String prefix = menuKey + ":";
      TextDisplay nearest = null;
      double distance = Double.MAX_VALUE;
      int priority = Integer.MIN_VALUE;
      for (TextDisplay display : playerDisplays.getOrDefault(player, Collections.emptyList())) {
         String name = view == null ? (display == null ? null : display.getCustomName()) : view.names.get(display);
         MenuLayout layout = name == null || !name.startsWith(prefix) ? null : layouts.get(name.substring(prefix.length()));
         if (layout == null || !layout.interaction().pointerEvents() || !isCursorInsideMenuDisplay(player, section, display, cursor)) continue;
         Location location = getMenuDisplayLocation(player, display);
         if (location == null) continue;
         double candidate = location.distanceSquared(cursor);
         if (layout.interaction().priority() > priority || layout.interaction().priority() == priority && candidate < distance) {
            nearest = display; distance = candidate; priority = layout.interaction().priority();
         }
      }
      return nearest;
   }

   private MenuLayout pickedLayout(Player player) {
      TextDisplay display = pickMenuDisplay(player);
      if (display == null) return null;
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      return getLayoutForPlayer(player, view == null ? display.getCustomName() : view.names.get(display));
   }

   private record DigInput(boolean start, int sequence) {}
   private record DiggingInput(Location location, com.github.retrooper.packetevents.util.Vector3i position,
      long generation, java.util.concurrent.ArrayBlockingQueue<DigInput> queue, java.util.concurrent.atomic.AtomicBoolean overflow) {}
   private final Map<UUID, DiggingInput> diggingInputs = new ConcurrentHashMap<>();
   private boolean hasHierarchy(Player player) {
      String menu = getCurrentPlayerMenu(player);
      return getLayoutsForPlayer(player, menu, getRuntimeSection(player, menu), false).values().stream()
         .anyMatch(layout -> !layout.interaction().parent().isEmpty());
   }
   private boolean isBlockDiggingMenu(Player player) {
      Section section = getRuntimeSection(player, getCurrentPlayerMenu(player));
      return section != null && section.dragInput.equals("block-digging");
   }
   // shortcut: fixed-block input supports stationary menus; extend targeting after vanilla camera tests.
   private void openDiggingInput(Player player, Location eye) {
      if (!isBlockDiggingMenu(player)) return;
      Location projected = eye.clone().add(eye.getDirection().multiply(2));
      Location location = new Location(eye.getWorld(), projected.getBlockX(), projected.getBlockY(), projected.getBlockZ());
      if (!Bukkit.isOwnedByCurrentRegion(location)) throw new IllegalStateException("Drag input block is outside the player's region");
      var position = new com.github.retrooper.packetevents.util.Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
      diggingInputs.put(player.getUniqueId(), new DiggingInput(location, position, getMenuSessionGeneration(player),
         new java.util.concurrent.ArrayBlockingQueue<>(32), new java.util.concurrent.atomic.AtomicBoolean()));
      player.sendBlockChange(location, Material.BARRIER.createBlockData());
   }
   private void closeDiggingInput(Player player) {
      DiggingInput input = diggingInputs.remove(player.getUniqueId());
      if (input == null || !player.isOnline() || !player.getWorld().equals(input.location().getWorld())) return;
      Runnable restore = () -> {
         var data = input.location().getBlock().getBlockData();
         org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, player, () -> {
            if (player.isOnline() && player.getWorld().equals(input.location().getWorld()))
               player.sendBlockChange(input.location(), resolveClientBlock(player.getUniqueId(), input.location(), data));
         });
      };
      org.orvencraft.uicore.scheduler.CleanupTasks.region(this, input.location(), restore);
   }
   private void tickDiggingInput(Player player) {
      DiggingInput input = diggingInputs.get(player.getUniqueId());
      if (input == null) return;
      if (!isMenuSessionCurrent(player, input.generation())) { closeDiggingInput(player); return; }
      if (input.overflow().getAndSet(false)) { input.queue().clear(); finishDrag(player, true); }
      for (int i = 0; i < 32; i++) {
         DigInput action = input.queue().poll();
         if (action == null) break;
         PacketEvents.getAPI().getPlayerManager().sendPacket(player,
            new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges(action.sequence()));
         if (action.start()) {
            if (!dragGestures.containsKey(player.getUniqueId())) handleCursorClick(player);
         } else { tickDrag(player); finishDrag(player, false); }
         if (!isMenuSessionCurrent(player, input.generation())) break;
      }
      if (diggingInputs.get(player.getUniqueId()) == input) player.sendBlockChange(input.location(), Material.BARRIER.createBlockData());
   }
   public void toggleMenuDebug(Player player) {
      if (!debugMenus.remove(player.getUniqueId())) debugMenus.add(player.getUniqueId());
      player.sendMessage("UIcore menu debug: " + (debugMenus.contains(player.getUniqueId()) ? "on" : "off"));
   }
   private void tickMenuDebug(Player player) {
      if (!debugMenus.contains(player.getUniqueId())) return;
      String menu = getCurrentPlayerMenu(player);
      Section section = getRuntimeSection(player, menu);
      MenuLayout picked = pickedLayout(player);
      player.sendActionBar(net.kyori.adventure.text.Component.text("UIcore " + menu + " | selected: " + (picked == null ? "none" : picked.key + " | parent: " + picked.interaction().parent())));
      if (section == null) return;
      for (MenuLayout layout : getLayoutsForPlayer(player, menu, section).values()) {
         TextDisplay display = findTrackedTextDisplay(player, menu, layout.key);
         Location center = display == null ? null : getMenuDisplayLocation(player, display);
         if (center == null) continue;
         CutsceneView view = cutsceneViews.get(player.getUniqueId());
         TextDisplayHitBox.Snapshot geometry = view == null ? TextDisplayHitBox.snapshot(display) : view.hitboxes.get(display);
         if (geometry == null) continue;
         Vector forward = view == null ? new Location(center.getWorld(), 0, 0, 0, section.yaw, section.pitch).getDirection() : view.frame.forward();
         Vector right = view == null ? new Vector(-forward.getZ(), 0, forward.getX()).normalize() : view.frame.right();
         Vector up = view == null ? forward.clone().crossProduct(right).multiply(-1).normalize() : view.frame.up();
         for (Vector point : geometry.outline()) {
            Location dot = center.clone().add(right.clone().multiply(point.getX())).add(up.clone().multiply(point.getY())).add(forward.clone().multiply(point.getZ()));
            player.spawnParticle(org.bukkit.Particle.DUST, dot, 1, new org.bukkit.Particle.DustOptions(layout == picked ? Color.LIME : Color.YELLOW, .5f));
         }
         if (!layout.interaction().parent().isEmpty()) {
            MenuLayout parent = getLayoutForPlayer(player, menu, layout.interaction().parent());
            if (parent != null) {
               var position = getCurrentLayoutPosition(player, menu, parent);
               Location parentLocation = view == null ? toLayoutWorldLocation(section, parent, position)
                  : view.frame.localToWorld(center.getWorld(), position.x() + view.menuOriginOffset.getX(), position.y() + view.menuOriginOffset.getY(), position.z() + view.menuOriginOffset.getZ());
               if (parentLocation != null) for (int i = 0; i <= 8; i++) {
                  Vector point = center.toVector().multiply(1 - i / 8.0).add(parentLocation.toVector().multiply(i / 8.0));
                  player.spawnParticle(org.bukkit.Particle.DUST, point.toLocation(center.getWorld()), 1, new org.bukkit.Particle.DustOptions(Color.AQUA, .4f));
               }
            }
         }
      }
   }

   private final Map<UUID, Map<String, DynamicPositionSpec.Position>> draggedPositions = new ConcurrentHashMap<>();
   private final Map<UUID, DragGesture> dragGestures = new ConcurrentHashMap<>();
   private final Set<UUID> debugMenus = ConcurrentHashMap.newKeySet();
   private record DragGesture(String menu, long generation, MenuLayout handle, MenuLayout target,
      DynamicPositionSpec.Position initial, DynamicPositionSpec.Position previous, Vector pointer,
      Location plane, Vector right, Vector up, Vector normal, Location origin, double motionScale, String previousSlider) {}

   private DynamicPositionSpec.Position draggedPosition(Player player, String id) {
      return draggedPositions.getOrDefault(player.getUniqueId(), Map.of()).get(id);
   }
   private boolean hasDraggedPosition(Player player, String id) { return draggedPosition(player, id) != null; }
   private void setDraggedPosition(Player player, String id, DynamicPositionSpec.Position position) {
      draggedPositions.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(id, position);
   }
   private boolean dragAllowed(Player player) {
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      return !isMenuTransitionActive(player) && !isCutsceneUiFrozen(player)
         && (cutsceneManager == null || !cutsceneManager.active(player) || view != null && view.definition.policy().clicks());
   }
   private void beginDrag(Player player, MenuLayout handle) {
      if (!dragAllowed(player) || !handle.prepareInteraction(player)) return;
      String menu = getCurrentPlayerMenu(player);
      long generation = getMenuSessionGeneration(player);
      MenuLayout target = handle.interaction().target().isEmpty() ? handle : getLayoutForPlayer(player, menu, handle.interaction().target());
      if (target == null || target.isWorldAnchored() || !target.isInteractionStillAllowed(player)) return;
      Section section = getRuntimeSection(player, menu);
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      Pig camera = playerSit.get(player);
      if (camera == null) return;
      Location origin = view == null ? MenuCameraGeometry.viewOrigin(camera, cameraEdgePanOffsets.getOrDefault(player.getUniqueId(), CameraEdgePan.Offset.ZERO)) : view.frame.origin(player.getWorld());
      Vector normal = origin.getDirection().normalize();
      Vector right = view == null ? new Vector(-normal.getZ(), 0, normal.getX()).normalize() : view.frame.right();
      Vector up = view == null ? normal.clone().crossProduct(right).multiply(-1).normalize() : view.frame.up();
      var absolute = getCurrentLayoutPosition(player, menu, target);
      Location plane = view == null ? toLayoutWorldLocation(section, target, absolute)
         : view.frame.localToWorld(player.getWorld(), absolute.x() + view.menuOriginOffset.getX(), absolute.y() + view.menuOriginOffset.getY(), absolute.z() + view.menuOriginOffset.getZ());
      Location pointer = TextDisplayHitBox.projectToPlane(origin, cursorExactLocations.get(player), plane, normal);
      if (pointer == null) return;
      var initial = localLayoutPosition(player, menu, target);
      DragGesture gesture = new DragGesture(menu, generation, handle, target, initial, draggedPosition(player, target.key), pointer.toVector(),
         plane, right, up, normal, origin, dragMotionScale(player, target),
         getPlayerMenuStateSnapshot(player, menu).get(handle.interaction().sliderState()));
      if (!fireDrag(player, gesture, MenuDragEvent.Phase.START, initial, null)
         || !isMenuSessionCurrent(player, generation) || getLayoutForPlayer(player, menu, handle.key) != handle
         || getLayoutForPlayer(player, menu, target.key) != target || !handle.isInteractionStillAllowed(player)
         || !target.isInteractionStillAllowed(player)) return;
      dragGestures.put(player.getUniqueId(), gesture);
   }
   private boolean fireDrag(Player player, DragGesture g, MenuDragEvent.Phase phase,
                            DynamicPositionSpec.Position p, String drop) {
      var event = new MenuDragEvent(player, g.menu(), g.handle().key, g.target().key, phase, p.x(), p.y(), p.z(), drop);
      Bukkit.getPluginManager().callEvent(event);
      return !event.isCancelled();
   }
   private void tickDrag(Player player) {
      DragGesture g = dragGestures.get(player.getUniqueId());
      if (g == null) return;
      if (!isMenuSessionCurrent(player, g.generation()) || !dragAllowed(player)
         || getLayoutForPlayer(player, g.menu(), g.handle().key) != g.handle()
         || getLayoutForPlayer(player, g.menu(), g.target().key) != g.target()
         || !g.handle().isInteractionStillAllowed(player) || !g.target().isInteractionStillAllowed(player)) { finishDrag(player, true); return; }
      CutsceneView activeView = cutsceneViews.get(player.getUniqueId());
      if (activeView != null && (activeView.moving || activeView.frame.origin(player.getWorld()).distanceSquared(g.origin()) > 1e-10)) { finishDrag(player, true); return; }
      Location cursor = cursorExactLocations.get(player);
      if (cursor == null || !cursor.getWorld().equals(g.origin().getWorld())) { finishDrag(player, true); return; }
      Location point = TextDisplayHitBox.projectToPlane(g.origin(), cursor, g.plane(), g.normal());
      if (point == null) return;
      Vector delta = point.toVector().subtract(g.pointer());
      var config = g.handle().interaction();
      var next = new DynamicPositionSpec.Position(config.x(g.initial().x(), delta.dot(g.right()) / g.motionScale()),
         config.y(g.initial().y(), delta.dot(g.up()) / g.motionScale()), g.initial().z());
      var current = draggedPosition(player, g.target().key);
      if (positionDistanceSquared(current == null ? g.initial() : current, next) < 1e-12) return;
      if (!fireDrag(player, g, MenuDragEvent.Phase.MOVE, next, null)) { finishDrag(player, true); return; }
      if (dragGestures.get(player.getUniqueId()) != g || !isMenuSessionCurrent(player, g.generation())) return;
      if (getLayoutForPlayer(player, g.menu(), g.handle().key) != g.handle()
         || getLayoutForPlayer(player, g.menu(), g.target().key) != g.target()
         || !g.handle().isInteractionStillAllowed(player) || !g.target().isInteractionStillAllowed(player)) { finishDrag(player, true); return; }
      applyDragPosition(player, g, next);
   }

   private void applyDragPosition(Player player, DragGesture g, DynamicPositionSpec.Position position) {
      setDraggedPosition(player, g.target().key, position);
      var config = g.handle().interaction();
      if (!config.sliderState().isEmpty()) {
         setPlayerMenuState(player, g.menu(), config.sliderState(), Double.toString(config.sliderValue(position.x(), position.y())));
      }
   }
   private void finishDrag(Player player, boolean cancel) {
      DragGesture g = dragGestures.remove(player.getUniqueId());
      if (g == null) return;
      var next = draggedPosition(player, g.target().key);
      if (next == null) next = g.initial();
      MenuLayout receiver = null;
      if (!cancel && !g.handle().interaction().dropTargets().isEmpty()) {
         Section section = getRuntimeSection(player, g.menu());
         int priority = Integer.MIN_VALUE;
         for (String id : g.handle().interaction().dropTargets()) {
            MenuLayout zone = getLayoutForPlayer(player, g.menu(), id);
            TextDisplay display = findTrackedTextDisplay(player, g.menu(), id);
            if (zone == null || display == null || !zone.isInteractionStillAllowed(player)
               || !zone.interaction().accepts().isEmpty() && !zone.interaction().accepts().contains(g.target().key)
               || !isCursorInsideMenuDisplay(player, section, display, cursorExactLocations.get(player))) continue;
            if (receiver == null || zone.interaction().priority() > priority) {
               receiver = zone;
               priority = zone.interaction().priority();
            }
         }
         if (receiver == null) cancel = true;
         else {
            var absolute = getCurrentLayoutPosition(player, g.menu(), receiver);
            var currentAbsolute = getCurrentLayoutPosition(player, g.menu(), g.target());
            var snapped = new DynamicPositionSpec.Position(next.x() + (absolute.x() - currentAbsolute.x()) / g.motionScale(),
               next.y() + (absolute.y() - currentAbsolute.y()) / g.motionScale(), next.z());
            cancel = !g.handle().interaction().permitsDrop(next.x(), next.y(), snapped.x(), snapped.y());
            next = snapped;
         }
      }
      String drop = receiver == null ? null : receiver.key;
      if (!cancel) cancel = !fireDrag(player, g, MenuDragEvent.Phase.DROP, next, drop);
      if (!isMenuSessionCurrent(player, g.generation())) return;
      if (!cancel && receiver != null) {
         cancel = getLayoutForPlayer(player, g.menu(), drop) != receiver || !receiver.isInteractionStillAllowed(player);
      }
      boolean sameTarget = getLayoutForPlayer(player, g.menu(), g.target().key) == g.target();
      cancel |= !dragAllowed(player) || getLayoutForPlayer(player, g.menu(), g.handle().key) != g.handle()
         || !sameTarget || !g.handle().isInteractionStillAllowed(player) || !g.target().isInteractionStillAllowed(player);
      if (cancel) {
         if (g.previous() == null || !sameTarget) {
            Map<String, DynamicPositionSpec.Position> positions = draggedPositions.get(player.getUniqueId());
            if (positions != null) positions.remove(g.target().key);
         } else setDraggedPosition(player, g.target().key, g.previous());
         if (!g.handle().interaction().sliderState().isEmpty()) setPlayerMenuState(player, g.menu(), g.handle().interaction().sliderState(), g.previousSlider());
         fireDrag(player, g, MenuDragEvent.Phase.CANCEL, g.initial(), null);
      } else {
         applyDragPosition(player, g, next);
         g.handle().startCooldown(player);
         if (receiver != null && receiver != g.handle()) receiver.startCooldown(player);
         Bukkit.getPluginManager().callEvent(new MenuDropEvent(player, g.menu(), g.handle().key, g.target().key,
            drop, next.x(), next.y(), next.z()));
         if (!g.handle().interaction().onDrop().isEmpty() || receiver != null && !receiver.interaction().onDrop().isEmpty()) {
            Map<String, Object> arguments = receiver == null
               ? Map.of("drag-handle", g.handle().key, "drag-target", g.target().key)
               : Map.of("drag-handle", g.handle().key, "drag-target", g.target().key, "drop-target", receiver.key);
            runDropActions(player, g, g.handle(), arguments);
            if (receiver != null && receiver != g.handle()) runDropActions(player, g, receiver, arguments);
         }
      }
      if (isMenuSessionCurrent(player, g.generation()) && !isMenuTransitionActive(player)) reconcileMenuContent(player, g.menu());
   }

   private void runDropActions(Player player, DragGesture gesture, MenuLayout owner, Map<String, Object> arguments) {
      for (String action : owner.interaction().onDrop()) {
         if (!isMenuSessionCurrent(player, gesture.generation()) || isMenuTransitionActive(player)) break;
         try {
            CommandUtils.executeCommand(player, action, 0L, owner.key, arguments);
         } catch (RuntimeException exception) {
            getLogger().log(java.util.logging.Level.WARNING,
               "on-drop action failed for menu '" + gesture.menu() + "', element '" + owner.key + "'", exception);
         }
      }
   }

   public long getMenuSessionGeneration(Player player) {
      return player == null ? -1L : this.sessionGenerations.getOrDefault(player.getUniqueId(), -1L);
   }

   public boolean isMenuSessionCurrent(Player player, long generation) {
      return player != null && player.isOnline() && generation >= 0L && generation == this.getMenuSessionGeneration(player)
         && this.currentPlayerMenus.containsKey(player);
   }

   public boolean activateMenuElement(Player player, String elementId) {
      String menuKey = this.getCurrentPlayerMenu(player);
      if (!Bukkit.isOwnedByCurrentRegion(player) || menuKey == null || this.isMenuTransitionActive(player) || elementId == null) return false;
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      if (view != null && cutsceneManager.active(player) && !view.definition.policy().clicks()) return false;
      String qualified = elementId.contains(":") ? elementId : menuKey + ":" + elementId;
      if (!qualified.startsWith(menuKey + ":")) return false;
      MenuLayout layout = this.getLayoutForPlayer(player, qualified);
      if (layout == null || !layout.isVisible(player) || !this.tryAcquireLayoutClick(player, menuKey, layout.key)
         || !this.authorizeMenuInteraction(player, menuKey, layout)) return false;
      if (cutsceneManager != null && cutsceneManager.chooseElement(player, layout.key)) return true;
      layout.runCommand(player);
      return true;
   }

   private boolean authorizeMenuInteraction(Player player, String menuKey, MenuLayout layout) {
      if (!layout.prepareInteraction(player)) return false;
      long generation = this.getMenuSessionGeneration(player);
      MenuClickEvent event = new MenuClickEvent(player, menuKey, layout.key, UIcoreApiImpl.snapshot(menuKey, layout));
      Bukkit.getPluginManager().callEvent(event);
      return !event.isCancelled() && this.isMenuSessionCurrent(player, generation)
         && this.getLayoutForPlayer(player, menuKey, layout.key) == layout && layout.isInteractionStillAllowed(player);
   }

   public boolean isMenuInputField(Player player, String field) {
      String menu = this.getCurrentPlayerMenu(player);
      if (menu == null || field == null || !field.matches("[A-Za-z0-9_.-]{1,64}")) return false;
      for (MenuLayout layout : this.getLayoutsForPlayer(player, menu, this.getRuntimeSection(player, menu)).values())
         if (layout.hasInputField() && field.equals(layout.getInputFieldName()) && layout.hasPermission(player)) return true;
      return false;
   }

   public boolean submitMenuInput(Player player, String field, String value) {
      if (player == null || !Bukkit.isOwnedByCurrentRegion(player) || !this.isMenuInputField(player, field) || value == null || value.length() > 4096) return false;
      Map<String, String> inputs = this.userInputData.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
      if (!inputs.containsKey(field) && inputs.size() >= 64) return false;
      long generation = this.getMenuSessionGeneration(player);
      MenuInputEvent event = new MenuInputEvent(player, this.getCurrentPlayerMenu(player), field, value);
      Bukkit.getPluginManager().callEvent(event);
      if (event.isCancelled() || !this.isMenuSessionCurrent(player, generation) || !this.isMenuInputField(player, field)
         || event.getValue() == null || event.getValue().length() > 4096) return false;
      inputs.put(field, event.getValue());
      Commands.getUserInputData().computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(field, event.getValue());
      return true;
   }

   private void registerUseEntityPacketListener() {
      this.menuPacketListener = new PacketListenerAbstract(PacketListenerPriority.LOWEST) {
         @Override public void onPacketSend(PacketSendEvent event) {
            if (!(event.getPlayer() instanceof Player player)) return;
            if (event.getPacketType() == Server.HELD_ITEM_CHANGE) {
               ZoomState zoom = menuZooms.get(player.getUniqueId());
               if (zoom != null) zoom.clientSlot.set(new WrapperPlayServerHeldItemChange(event).getSlot());
            }
            MenuClientView clientView = menuClientViews.get(player.getUniqueId());
            if (clientView != null) clientView.filter(event);
         }
         @Override public void onPacketReceive(PacketReceiveEvent event) {
            if (!(event.getPlayer() instanceof Player player) || !playerCursors.containsKey(player)) return;
            long generation = getMenuSessionGeneration(player);
            if (event.getPacketType() == Client.HELD_ITEM_CHANGE) {
               ZoomState zoom = menuZooms.get(player.getUniqueId());
               if (zoom != null && zoom.generation == generation) {
                  event.setCancelled(true);
                  int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
                  if (slot >= 0 && slot <= 8) {
                     // The invisible client slot rolls normally. The authoritative inventory never changes.
                     int direction = MenuZoom.scrollDirection(zoom.clientSlot.getAndSet(slot), slot);
                     if (direction != 0) zoom.input.offer(direction);
                  }
               }
               return;
            }
            DiggingInput digging = diggingInputs.get(player.getUniqueId());
            if (digging != null && event.getPacketType() == Client.PLAYER_DIGGING) {
               event.setCancelled(true);
               var packet = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
               if (packet.getBlockPosition().equals(digging.position())) {
                  var action = packet.getAction();
                  if (action == com.github.retrooper.packetevents.protocol.player.DiggingAction.START_DIGGING
                     || action == com.github.retrooper.packetevents.protocol.player.DiggingAction.CANCELLED_DIGGING
                     || action == com.github.retrooper.packetevents.protocol.player.DiggingAction.FINISHED_DIGGING) {
                     if (!digging.queue().offer(new DigInput(action == com.github.retrooper.packetevents.protocol.player.DiggingAction.START_DIGGING, packet.getSequence())))
                        digging.overflow().set(true);
                  }
               }
               return;
            }
            if (event.getPacketType() == Client.SPECTATE_ENTITY) {
               event.setCancelled(true);
               MenuClientView clientView = menuClientViews.get(player.getUniqueId());
               var packet = new WrapperPlayClientSpectateEntity(event);
               if (digging == null && clientView != null && packet.hasEntityId() && clientView.acceptsEntityClick())
                  pendingClicks.put(player.getUniqueId(), generation);
               return;
            }
            if (event.getPacketType() == Client.SPECTATE) {
               event.setCancelled(true);
               MenuClientView clientView = menuClientViews.get(player.getUniqueId());
               if (digging == null && clientView != null && clientView.acceptsClick(new WrapperPlayClientSpectate(event).getTargetUUID()))
                  pendingClicks.put(player.getUniqueId(), generation);
               return;
            }
            if (event.getPacketType() == Client.PLAYER_ROTATION || event.getPacketType() == Client.PLAYER_POSITION_AND_ROTATION) {
               float yaw, pitch;
               if (event.getPacketType() == Client.PLAYER_ROTATION) {
                  WrapperPlayClientPlayerRotation packet = new WrapperPlayClientPlayerRotation(event);
                  yaw = packet.getYaw(); pitch = packet.getPitch();
               } else {
                  WrapperPlayClientPlayerPositionAndRotation packet = new WrapperPlayClientPlayerPositionAndRotation(event);
                  yaw = packet.getYaw(); pitch = packet.getPitch();
               }
               if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) { event.setCancelled(true); return; }
               pendingRotations.put(player.getUniqueId(), new RotationInput(generation, yaw, pitch));
            }
             if (event.getPacketType() == Client.INTERACT_ENTITY || event.getPacketType() == Client.ANIMATION || event.getPacketType() == Client.USE_ITEM) {
                event.setCancelled(true);
                if (digging == null) pendingClicks.put(player.getUniqueId(), generation);
             }
         }
      };
      PacketEvents.getAPI().getEventManager().registerListener(this.menuPacketListener);
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

   public Location getMenuDisplayLocation(Player player, Entity entity) {
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      Location location = view == null ? null : view.locations.get(entity);
      return location != null ? location.clone() : Bukkit.isOwnedByCurrentRegion(entity) ? entity.getLocation() : null;
   }

   boolean isCursorInsideMenuDisplay(Player player, Section section, TextDisplay display, Location cursor) {
      if (player == null || section == null || display == null || cursor == null) {
         return false;
      }
      CutsceneView view = cutsceneViews.get(player.getUniqueId());
      if (view != null) {
         var geometry = view.hitboxes.get(display);
         Location origin = view.locations.get(display);
         if (geometry == null || origin == null || !origin.getWorld().equals(cursor.getWorld())) return false;
         return TextDisplayHitBox.hitsMenuRay(geometry, view.frame.origin(origin.getWorld()), cursor, origin,
            view.frame.right(), view.frame.up(), view.frame.forward());
      }

      World world = Bukkit.getWorld(section.world);
      if (world == null || !world.equals(display.getWorld()) || !world.equals(cursor.getWorld())) {
         return false;
      }

      Pig cameraMount = playerSit.get(player);
      if (cameraMount == null || !cameraMount.isValid() || !world.equals(cameraMount.getWorld())) {
         return false;
      }
      Location cameraOrigin = MenuCameraGeometry.viewOrigin(cameraMount,
         this.cameraEdgePanOffsets.getOrDefault(player.getUniqueId(), CameraEdgePan.Offset.ZERO));

      Vector forward = cameraOrigin.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0, forward.getX());
      if (right.lengthSquared() < 1.0E-8) {
         double yaw = Math.toRadians(section.yaw);
         right = new Vector(-Math.cos(yaw), 0.0, -Math.sin(yaw));
      }
       right.normalize();
       Vector up = forward.getCrossProduct(right).multiply(-1.0).normalize();
       return display.isValid() && TextDisplayHitBox.hitsMenuRay(TextDisplayHitBox.snapshot(display),
          cameraOrigin, cursor, display.getLocation(), right, up, forward);
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

   public static net.kyori.adventure.text.Component renderDisplayComponent(Player player, String template) {
      return renderDisplayComponent(player, plugin == null ? "" : plugin.getCurrentPlayerMenu(player), "", template);
   }

   public static net.kyori.adventure.text.Component renderDisplayComponent(Player player, String menuId, String elementId, String template) {
      if (template == null) return net.kyori.adventure.text.Component.empty();
      if (player == null || plugin == null) return ColorParser.parse(template);
      Matcher matcher = INPUT_PLACEHOLDER_PATTERN.matcher(template);
      StringBuffer trusted = new StringBuffer();
      Map<String, String> replacements = new LinkedHashMap<>();
      while (matcher.find()) {
         String field = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
         String value = plugin.getPlayerInputValue(player, field);
         if (plugin.isPasswordInputField(field)) value = "*".repeat(value.length());
         String marker = "UICORE_LITERAL_" + UUID.randomUUID().toString().replace("-", "");
         replacements.put(marker, value);
         matcher.appendReplacement(trusted, marker);
      }
      matcher.appendTail(trusted);
      String source = trusted.toString();
      Matcher dataMatcher = EXTENSION_DATA_PATTERN.matcher(source);
      StringBuffer withData = new StringBuffer();
      Map<String, String> resolvedData = new HashMap<>();
      int dataReferences = 0;
      while (dataMatcher.find() && dataReferences++ < 32) {
         String id = dataMatcher.group(1);
         String value = resolvedData.computeIfAbsent(id, key -> plugin.extensionManager.resolveData(
            key, player, menuId == null ? "" : menuId, elementId == null ? "" : elementId).map(String::valueOf).orElse(""));
         String marker = "UICORE_LITERAL_" + UUID.randomUUID().toString().replace("-", "");
         replacements.put(marker, value);
         dataMatcher.appendReplacement(withData, marker);
      }
      dataMatcher.appendTail(withData);
      source = withData.toString();
      if (hasPAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) source = PlaceholderAPI.setPlaceholders(player, source);
      net.kyori.adventure.text.Component component = ColorParser.parse(source);
      for (Map.Entry<String, String> replacement : replacements.entrySet()) component = component.replaceText(
         net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral(replacement.getKey())
            .replacement(net.kyori.adventure.text.Component.text(replacement.getValue())).build());
      return component;
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
      this.playerMenuSeats.remove(var1);
      this.lastMenuRotations.remove(var1.getUniqueId());
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
      this.componentLayoutCache.clearPlayer(var1.getUniqueId());
      this.playerElementStates.remove(var1.getUniqueId());
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

   private void clearBlockingBlocks(Player player) {
      this.restoreBlocks(player);
      if (!cameraBlockCheckEnabled) return;
      Location center = player.getLocation();
      World world = center.getWorld();
      int radius = Math.max(0, Math.min(8, cameraBlockCheckRadius));
      Set<Location> locations = ConcurrentHashMap.newKeySet();
      UUID playerId = player.getUniqueId();
      this.clientHiddenBlocks.put(playerId, locations);
      Map<Long, List<Location>> byChunk = new HashMap<>();
      for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
          Location point = center.clone().add(x, y, z);
          Location location = new Location(world, point.getBlockX(), point.getBlockY(), point.getBlockZ());
          if (location.getY() < world.getMinHeight() || location.getY() >= world.getMaxHeight()) continue;
          long chunkKey = ((long)(location.getBlockX() >> 4) << 32) ^ ((location.getBlockZ() >> 4) & 0xffffffffL);
          byChunk.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(location);
      }
      for (List<Location> chunkLocations : byChunk.values()) {
         foliaLib.scheduling().regionSpecificScheduler(chunkLocations.get(0)).run(() -> {
            List<Location> solid = chunkLocations.stream().filter(location -> !world.getBlockAt(location).getType().isAir()).toList();
            if (solid.isEmpty()) return;
            foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
               if (!player.isOnline() || this.clientHiddenBlocks.get(playerId) != locations) return;
               for (Location location : solid) {
                  locations.add(location);
                  player.sendBlockChange(location, resolveClientBlock(playerId, location, Material.AIR.createBlockData()));
               }
            }, null);
         });
      }
   }

   public static String resolveDataValue(Player player, String menuId, String elementId, String source) {
      if (source == null || plugin == null || player == null) return source == null ? "" : source;
      Matcher matcher = EXTENSION_DATA_PATTERN.matcher(source);
      if (!matcher.matches()) return parsePlaceholders(player, source);
      return plugin.extensionManager.resolveData(matcher.group(1), player, menuId, elementId)
         .map(String::valueOf).orElse("");
   }

   public boolean isClientHiddenBlock(UUID playerId, Location location) {
      Set<Location> hidden = this.clientHiddenBlocks.get(playerId);
      return hidden != null && hidden.contains(location);
   }

   /** Resolve current player-only layers on the player's scheduler just before sending a block packet. */
   public BlockData resolveClientBlock(UUID playerId, Location location, BlockData actual) {
      DiggingInput digging = diggingInputs.get(playerId);
      if (digging != null && digging.location().equals(location)) return Material.BARRIER.createBlockData();
      BlockData preview = schematicPreviewManager == null ? null : schematicPreviewManager.blockAt(playerId, location);
      if (preview != null) return preview;
      return isClientHiddenBlock(playerId, location) ? Material.AIR.createBlockData() : actual;
   }

   private void restoreBlocks(Player player) {
      Set<Location> locations = this.clientHiddenBlocks.remove(player.getUniqueId());
      if (locations == null || !player.isOnline()) return;
      Map<String, List<Location>> byChunk = new HashMap<>();
      for (Location location : locations) {
         World world = location.getWorld();
         if (world == null) continue;
         String key = world.getUID() + ":" + (location.getBlockX() >> 4) + ":" + (location.getBlockZ() >> 4);
         byChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(location);
      }
      for (List<Location> chunkLocations : byChunk.values()) {
         org.orvencraft.uicore.scheduler.CleanupTasks.region(this, chunkLocations.get(0), () -> {
            Map<Location, BlockData> blocks = new HashMap<>();
            for (Location location : chunkLocations) blocks.put(location, location.getBlock().getBlockData());
            org.orvencraft.uicore.scheduler.CleanupTasks.entity(this, player, () -> {
               if (!player.isOnline()) return;
               for (Map.Entry<Location, BlockData> entry : blocks.entrySet()) {
                  if (entry.getKey().getWorld().equals(player.getWorld()))
                     player.sendBlockChange(entry.getKey(), resolveClientBlock(player.getUniqueId(), entry.getKey(), entry.getValue()));
               }
            });
         });
      }
   }

   private void saveDefaultMenuFiles() {
      List<String> var1;
      try (java.io.InputStream stream = this.getResource("menu-resources.txt")) {
         if (stream == null) throw new IOException("menu-resources.txt missing");
         var1 = new java.io.BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
            .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
      } catch (IOException exception) { throw new IllegalStateException("Cannot install menu resources", exception); }

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
         if (UIcorePlugin.playerCursors.containsKey(var2)
            && !UIcorePlugin.plugin.cutsceneViews.containsKey(var2.getUniqueId())
            && var1.getTo() != null
            && CursorRotationTracker.rotationChanged(
               var1.getFrom().getYaw(),
               var1.getFrom().getPitch(),
               var1.getTo().getYaw(),
               var1.getTo().getPitch()
            )) {
            UIcorePlugin.plugin.pendingRotations.put(var2.getUniqueId(), new RotationInput(UIcorePlugin.plugin.getMenuSessionGeneration(var2), var1.getTo().getYaw(), var1.getTo().getPitch()));
         }

         if (var2.hasMetadata("cursor_is_attacking_or_breaking")) {
            foliaLib.scheduling().entitySpecificScheduler(var2)
               .runDelayed(() -> var2.removeMetadata("cursor_is_attacking_or_breaking", UIcorePlugin.plugin), null, 1L);
         }
      }
   }

   private class PlayerJoinListener implements Listener {
       private PlayerJoinListener() {
       }

      @EventHandler
      public void onPlayerJoin(PlayerJoinEvent var1) {
         Player var2 = var1.getPlayer();
         if (UIcorePlugin.this.menuVisibility != null) UIcorePlugin.this.menuVisibility.joined(var2);
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
       }

      @EventHandler(
         priority = EventPriority.HIGHEST
      )
      public void onQuit(PlayerQuitEvent var1) {
         this.cleanup(var1.getPlayer());
         if (UIcorePlugin.this.menuVisibility != null) UIcorePlugin.this.menuVisibility.quit(var1.getPlayer());
      }

      @EventHandler(
         priority = EventPriority.HIGHEST
      )
      public void onKick(PlayerKickEvent var1) {
         if (UIcorePlugin.playerCursors.containsKey(var1.getPlayer()) && this.isSelfInteractKick(var1.getReason())) {
            var1.setCancelled(true);
         } else if (!var1.isCancelled()) {
            this.cleanup(var1.getPlayer());
         }
      }

      @EventHandler
      public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
         Player player = event.getEntity();
         if (cutsceneManager != null) cutsceneManager.abort(player, CutsceneAbortReason.PLAYER_DIED);
         MenuSession session = UIcorePlugin.this.menuSessions.get(player.getUniqueId());
         if (session == null) return;
         UIcorePlugin.this.respawnOrigins.put(player.getUniqueId(), session.originalLocation());
         UIcorePlugin.this.stopCursor(player, false);
      }

      @EventHandler
      public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
         Player player = event.getPlayer();
         Location origin = UIcorePlugin.this.respawnOrigins.remove(player.getUniqueId());
         try {
            if (java.nio.file.Files.exists(UIcorePlugin.this.sessionJournal(player))) {
               MenuSession saved = MenuSession.load(UIcorePlugin.this.sessionJournal(player), Bukkit::getWorld);
               saved.restore(player);
               origin = saved.originalLocation();
            }
            if (origin != null && origin.getWorld() != null) {
               event.setRespawnLocation(origin);
               player.getScheduler().run(UIcorePlugin.this, ignored -> UIcorePlugin.this.checkpointRestoredSession(player), null);
            }
         } catch (IOException | RuntimeException exception) {
            UIcorePlugin.this.getLogger().warning("Respawn recovery failed; journal retained: " + exception.getMessage());
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
         UIcorePlugin.this.recoverSavedSession(var2);
      }

      private void cleanup(Player player) {
         if (cutsceneManager != null) cutsceneManager.abort(player, CutsceneAbortReason.PLAYER_QUIT);
         UIcorePlugin.this.stopCursor(player, true);
      }
   }
}
