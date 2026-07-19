package com.cmenu.ui;

import com.cmenu.ui.bstats.bukkit.Metrics;
import com.cmenu.ui.compat.CraftEngineCompat;
import com.cmenu.ui.api.CustomScreenMenuApi;
import com.cmenu.ui.api.CustomScreenMenuApiImpl;
import com.cmenu.ui.api.event.MenuClickEvent;
import com.cmenu.ui.api.event.MenuCloseEvent;
import com.cmenu.ui.api.event.MenuOpenEvent;
import com.cmenu.ui.api.event.MenuSwitchEvent;
import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.npc.NPCModule;
import com.cmenu.ui.scheduler.ChunkLoaderManager;
import com.cmenu.ui.section.Section;
import com.cmenu.ui.section.SectionManager;
import com.cmenu.ui.util.CommandUtils;
import com.cmenu.ui.wasd.WASDModule;
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
import java.net.InetAddress;
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

public class CursorMenuPlugin extends JavaPlugin {
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
   public static CursorMenuPlugin plugin;
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
   private static final Pattern INPUT_PLACEHOLDER_PATTERN = Pattern.compile("%(?:cursormenu_)?input_([A-Za-z0-9_.-]+)%|\\{(?:menu\\.)?input\\.([A-Za-z0-9_.-]+)\\}");
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
   private HoverEnlargeManager hoverEnlargeManager;
   private CustomScreenMenuApi menuApi;
   private ChunkLoaderManager chunkLoaderManager;
   private Map<Player, String> currentPlayerMenus = new ConcurrentHashMap<>();
   public Map<Player, MenuLayout> selectedLayouts = new ConcurrentHashMap<>();
   private UserDataManager userDataManager;
   private DatabaseManager databaseManager;
   private AdminIpValidator adminIpValidator;
   private Map<UUID, String> currentPlayerInputFields = new ConcurrentHashMap<>();
   private Map<UUID, Map<String, String>> userInputData = new ConcurrentHashMap<>();
   private Map<UUID, Boolean> passwordVisibility = new ConcurrentHashMap<>();
   private Set<UUID> loggedInPlayers = ConcurrentHashMap.newKeySet();
   private static boolean ipBindingEnabled = true;
   private static boolean ipBindingStrict = false;
   private static boolean allowSameIpLogin = true;
   private static int lockoutDuration = 5;
   private static int maxLoginAttempts = 5;
   private static boolean ipWhitelistEnabled = false;
   private static List<String> ipWhitelist = new ArrayList<>();
   private static boolean ipBlacklistEnabled = false;
   private static List<String> ipBlacklist = new ArrayList<>();
   private List<String> loginRequiredNameTags = new ArrayList<>();
   private List<String> loginRequiredKeyTags = new ArrayList<>();
   private List<String> noDuplicateRegNameTags = new ArrayList<>();
   private List<String> noDuplicateRegKeyTags = new ArrayList<>();
   private YamlConfiguration langConfig;
   private static final String HELMET_META_KEY = "cursor_original_helmet";

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

   public boolean isPlayerLoggedIn(Player var1) {
      return this.loggedInPlayers.contains(var1.getUniqueId());
   }

   public void setPlayerLoggedIn(Player var1, boolean var2) {
      UUID var3 = var1.getUniqueId();
      if (var2) {
         this.loggedInPlayers.add(var3);
      } else {
         this.loggedInPlayers.remove(var3);
      }
   }

   public boolean requiresLogin(String var1, String var2) {
      for (String var4 : this.loginRequiredNameTags) {
         if (var1 != null && var1.contains(var4)) {
            return true;
         }
      }

      for (String var6 : this.loginRequiredKeyTags) {
         if (var2 != null && var2.contains(var6)) {
            return true;
         }
      }

      return false;
   }

   public boolean requiresNotRegistered(String var1, String var2) {
      for (String var4 : this.noDuplicateRegNameTags) {
         if (var1 != null && var1.contains(var4)) {
            return true;
         }
      }

      for (String var6 : this.noDuplicateRegKeyTags) {
         if (var2 != null && var2.contains(var6)) {
            return true;
         }
      }

      return false;
   }

   public boolean isIpBindingEnabled() {
      return ipBindingEnabled;
   }

   public boolean isIpBindingStrict() {
      return ipBindingStrict;
   }

   public boolean isAllowSameIpLogin() {
      return allowSameIpLogin;
   }

   public int getLockoutDuration() {
      return lockoutDuration;
   }

   public int getMaxLoginAttempts() {
      return maxLoginAttempts;
   }

   public boolean isIpWhitelistEnabled() {
      return ipWhitelistEnabled;
   }

   public List<String> getIpWhitelist() {
      return ipWhitelist;
   }

   public boolean isIpBlacklistEnabled() {
      return ipBlacklistEnabled;
   }

   public List<String> getIpBlacklist() {
      return ipBlacklist;
   }

   public boolean isIpInWhitelist(String var1) {
      return ipWhitelistEnabled && !ipWhitelist.isEmpty() ? this.matchesIpList(var1, ipWhitelist) : false;
   }

   public boolean isIpInBlacklist(String var1) {
      return ipBlacklistEnabled && !ipBlacklist.isEmpty() ? this.matchesIpList(var1, ipBlacklist) : false;
   }

   private boolean matchesIpList(String var1, List<String> var2) {
      if (var1 != null && var2 != null) {
         for (String var4 : var2) {
            var4 = var4.trim();
            if (!var4.isEmpty()) {
               if (var4.equals(var1)) {
                  return true;
               }

               if (var4.contains("/") && this.matchesCidr(var1, var4)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean matchesCidr(String var1, String var2) {
      try {
         String[] var3 = var2.split("/");
         if (var3.length != 2) {
            return false;
         } else {
            String var4 = var3[0].trim();
            int var5 = Integer.parseInt(var3[1].trim());
            byte[] var6 = this.ipToBytes(var1);
            byte[] var7 = this.ipToBytes(var4);
            if (var6 != null && var7 != null && var6.length == var7.length) {
               int var8 = var6.length * 8;
               if (var5 >= 0 && var5 <= var8) {
                  int var9 = var5;

                  for (int var10 = 0; var10 < var6.length; var10++) {
                     int var11 = var6[var10] < 0 ? var6[var10] + 256 : var6[var10];
                     int var12 = var7[var10] < 0 ? var7[var10] + 256 : var7[var10];
                     if (var9 <= 0) {
                        break;
                     }

                     int var13 = Math.min(8, var9);
                     int var14 = 255 << 8 - var13 & 0xFF;
                     if ((var11 & var14) != (var12 & var14)) {
                        return false;
                     }

                     var9 -= var13;
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } catch (Exception var15) {
         return false;
      }
   }

   private byte[] ipToBytes(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            return InetAddress.getByName(var1.trim()).getAddress();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   public String getCurrentPlayerMenu(Player var1) {
      return this.currentPlayerMenus.get(var1);
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

   public CustomScreenMenuApi getMenuApi() {
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

   public UserDataManager getUserDataManager() {
      return this.userDataManager;
   }

   public DatabaseManager getDatabaseManager() {
      return this.databaseManager;
   }

   public AdminIpValidator getAdminIpValidator() {
      return this.adminIpValidator;
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
                        Objects.requireNonNull(CursorMenuPlugin.this);
                        this.tick = 0L;
                        this.angle = 0.0F;
                     }

                     public void run() {
                        if (var1.isOnline()
                           && var2.isValid()
                           && CursorMenuPlugin.this.playerDisplays.getOrDefault(var1, Collections.emptyList()).contains(var2)) {
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

   public boolean getPasswordVisibility(Player var1) {
      return this.passwordVisibility.getOrDefault(var1.getUniqueId(), false);
   }

   public void togglePasswordVisibility(Player var1) {
      UUID var2 = var1.getUniqueId();
      boolean var3 = this.passwordVisibility.getOrDefault(var2, false);
      this.passwordVisibility.put(var2, !var3);
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

      if (this.databaseManager != null) {
         this.databaseManager.closeConnection();
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
            this.getLogger().warning("无法加载默认配置 " + var1 + ": " + var8.getMessage());
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
            this.getLogger().warning("无法保存合并后的 " + var1 + ": " + var7.getMessage());
         }
      }
   }

   private void ensureDefaultsOnReload(String... var1) {
      for (String var5 : var1) {
         File var6 = new File(this.getDataFolder(), var5);
         if (!var6.exists()) {
            this.saveResource(var5, false);
            this.getLogger().info("[CustomScreenMenu] 检测到缺失文件，已重新生成默认配置: " + var5);
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
      this.menuApi = new CustomScreenMenuApiImpl(this);
      Bukkit.getServicesManager().register(CustomScreenMenuApi.class, this.menuApi, this, ServicePriority.Normal);
      this.getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
      this.getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
      itemDisplayManager = new ItemDisplayManager(this);
      this.registerUseEntityPacketListener();
      this.chunkLoaderManager = new ChunkLoaderManager(this);
      this.chunkLoaderManager.startChunkLoaderTask();
      this.getServer().getPluginManager().registerEvents(new CreatureSpawnListener(), this);
      this.getServer().getPluginManager().registerEvents(new CursorMenuPlugin.SessionCleanupListener(), this);
      this.getServer().getPluginManager().registerEvents(new MenuListener(this), this);
      this.getServer().getPluginManager().registerEvents(new CursorMenuPlugin.AttackBreakListener(), this);
      this.getServer().getPluginManager().registerEvents(new CursorMenuPlugin.PlayerJoinListener(), this);
      Bukkit.getPluginCommand("cursormenu").setExecutor(new Commands());
      textDisplayManager = new TextDisplayManager(this);
      this.hoverEnlargeManager = new HoverEnlargeManager(this);
      this.chunkLoaderManager = new ChunkLoaderManager(this);
      this.chunkLoaderManager.startChunkLoaderTask();
      this.databaseManager = new DatabaseManager(this);
      this.userDataManager = new UserDataManager(this, this.databaseManager);
      this.adminIpValidator = new AdminIpValidator(this);
      NPCModule.initialize(this);
      short var1 = 30148;
      new Metrics(this, var1);
      WASDModule.initialize(this);
      CraftEngineCompat.init();
      this.getLogger().info("====================================");
      this.getLogger().info("CustomScreenMenu 插件已启动");
      this.getLogger().info("版本: " + this.getDescription().getVersion());
      this.getLogger().info("作者:野比大雄 " + this.getDescription().getAuthors());
      this.getLogger().info("感谢使用本插件！");
      this.getLogger().info("====================================");
      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         new CursorMenuPlaceholder(this).register();
         this.getLogger().info("已注册PlaceholderAPI变量支持");
      }

      String[] var2 = new String[]{"lang.yml", "lang/zh_cn.yml", "lang/en_us.yml", "lang/ru_ru.yml"};

      for (String var6 : var2) {
         File var7 = new File(this.getDataFolder(), var6);
         if (!var7.exists()) {
            this.saveResource(var6, false);
         }
      }

      Bukkit.getScheduler().runTaskTimer(this, () -> {
         for (Player var2x : Bukkit.getOnlinePlayers()) {
            if (playerCursors.containsKey(var2x)) {
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
         Bukkit.getServicesManager().unregister(CustomScreenMenuApi.class, this.menuApi);
      }

      WASDModule.shutdown();
      NPCModule.shutdown();
      if (this.chunkLoaderManager != null) {
         this.chunkLoaderManager.stopChunkLoaderTask();
      }

      this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
      PacketEvents.getAPI().terminate();
      this.purgeAllEntities();
      this.getLogger().info("====================================");
      this.getLogger().info("CustomScreenMenu 插件已关闭");
      this.getLogger().info("版本: " + this.getDescription().getVersion());
      this.getLogger().info("感谢使用，下次再见！");
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
      this.reloadLangConfig();
      this.getLogger().info("正在重载插件配置...");
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
            this.passwordVisibility.remove(var3.getUniqueId());
            this.playerLayoutToggleStates.remove(var3.getUniqueId());
            this.playerLayoutClickTimes.remove(var3.getUniqueId());
            this.playerLayoutCooldownUntil.remove(var3.getUniqueId());
            this.playerMenuLayoutOverrides.remove(var3.getUniqueId());
            this.playerMenuHiddenLayouts.remove(var3.getUniqueId());
            this.playerMenuStates.remove(var3.getUniqueId());
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
      this.passwordVisibility.clear();
      this.loggedInPlayers.clear();
      this.loadConfig();
      Bukkit.getOnlinePlayers().forEach(var1x -> {
         if (var1x.hasMetadata("cursor_original_helmet")) {
            var1x.removeMetadata("cursor_original_helmet", this);
         }
      });
      if (textDisplayManager != null) {
         textDisplayManager.reloadConfig();
      }

      this.ensureDefaultsOnReload("config.yml", "items.yml", "lang.yml", "lang/zh_cn.yml", "lang/en_us.yml", "lang/ru_ru.yml", "commands.yml");
      cursorModelData = this.getConfig().getInt("cursor-item.custom-model-data", 0);
      this.getLogger().info("配置已成功重载，所有菜单元素已刷新");
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
      if (this.databaseManager != null) {
         this.databaseManager.closeConnection();
      }

      this.databaseManager = new DatabaseManager(this);
      if (this.userDataManager != null) {
      }

      this.userDataManager = new UserDataManager(this, this.databaseManager);
      if (this.adminIpValidator != null) {
         this.adminIpValidator.loadConfig();
      } else {
         this.adminIpValidator = new AdminIpValidator(this);
      }

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

      this.loadButtonAccessControlConfig();
      this.loadIpBindingConfig();
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
      runDelay = this.getConfig().getInt("join-run.delay", 0);
      joinRunCommands = this.getConfig().getStringList("join-run.commands");
      exitYaw = (float)this.getConfig().getDouble("exit-camera.yaw", 0.0);
      exitPitch = (float)this.getConfig().getDouble("exit-camera.pitch", 0.0);
      creatureSpawnLimitEnabled = this.getConfig().getBoolean("creature-spawn-limits.enabled", false);
      creatureSpawnLimitRadius = this.getConfig().getInt("creature-spawn-limits.radius", 10);
      com.cmenu.ui.section.PlayerLocationOverride.reload(this.getConfig().getBoolean("use-player-location", false));
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

   private void loadButtonAccessControlConfig() {
      this.loginRequiredNameTags = this.getConfig().getStringList("button-access-control.login-required.name-tags");
      this.loginRequiredKeyTags = this.getConfig().getStringList("button-access-control.login-required.key-tags");
      this.noDuplicateRegNameTags = this.getConfig().getStringList("button-access-control.no-duplicate-registration.name-tags");
      this.noDuplicateRegKeyTags = this.getConfig().getStringList("button-access-control.no-duplicate-registration.key-tags");
      if (this.loginRequiredNameTags.isEmpty()) {
         this.loginRequiredNameTags.add("[需要登录]");
         this.loginRequiredNameTags.add("[LOGIN_REQUIRED]");
      }

      if (this.loginRequiredKeyTags.isEmpty()) {
         this.loginRequiredKeyTags.add("login_required");
         this.loginRequiredKeyTags.add("需要登录");
      }

      if (this.noDuplicateRegNameTags.isEmpty()) {
         this.noDuplicateRegNameTags.add("[防止重复注册]");
         this.noDuplicateRegNameTags.add("[NO_DUPLICATE_REG]");
      }

      if (this.noDuplicateRegKeyTags.isEmpty()) {
         this.noDuplicateRegKeyTags.add("no_duplicate_reg");
         this.noDuplicateRegKeyTags.add("防止重复注册");
      }
   }

   private void loadIpBindingConfig() {
      ipBindingEnabled = this.getConfig().getBoolean("ip-binding.enabled", true);
      ipBindingStrict = this.getConfig().getBoolean("ip-binding.strict", false);
      allowSameIpLogin = this.getConfig().getBoolean("ip-binding.allow-same-ip-login", true);
      lockoutDuration = this.getConfig().getInt("ip-binding.lockout-duration", 5);
      maxLoginAttempts = this.getConfig().getInt("ip-binding.max-login-attempts", 5);
      ipWhitelistEnabled = this.getConfig().getBoolean("ip-binding.whitelist.enabled", false);
      ipWhitelist = this.getConfig().getStringList("ip-binding.whitelist.ips");
      ipBlacklistEnabled = this.getConfig().getBoolean("ip-binding.blacklist.enabled", false);
      ipBlacklist = this.getConfig().getStringList("ip-binding.blacklist.ips");
      this.getLogger().info("IP绑定安全配置已加载 - 启用: " + ipBindingEnabled + ", 严格模式: " + ipBindingStrict);
      this.getLogger().info("IP白名单: " + (ipWhitelistEnabled ? "启用" : "禁用") + ", 规则数: " + ipWhitelist.size());
      this.getLogger().info("IP黑名单: " + (ipBlacklistEnabled ? "启用" : "禁用") + ", 规则数: " + ipBlacklist.size());
   }

   public void setupCursor(Player var1, String var2) {
      Section var3 = sectionManager.get(var2);
      if (var3 == null) {
         this.getLogger().warning("Menu '" + var2 + "' not found! Please check your menu configuration.");
         var1.sendMessage(
            this.getLangMessage("command.menu_not_found", "&c[CursorMenu] иЏњеЌ• '%menu%' жњЄж‰ѕе€°пјЊиЇ·жЈЂжџҐй…ЌзЅ®ж–‡д»¶пјЃ").replace("%menu%", var2)
         );
      } else {
         String previousMenuForEvent = this.currentPlayerMenus.get(var1);
         Section var4 = com.cmenu.ui.section.PlayerLocationOverride.apply(var1, var3);
         if (playerCursors.containsKey(var1)) {
            String var5 = this.currentPlayerMenus.get(var1);
            Section var6 = var5 == null ? null : sectionManager.get(var5);
            Section var7 = var6 == null ? null : com.cmenu.ui.section.PlayerLocationOverride.apply(var1, var6);
            Pig var8 = playerSit.get(var1);
            boolean var9 = var8 != null && var8.isValid() && var8.getPassengers().contains(var1);
            if (var9 && var7 != null && this.isSameLocation(var7, var4)) {
               if (var5 != null && !var5.equals(var2)) {
                  Bukkit.getPluginManager().callEvent(new MenuSwitchEvent(var1, var5, var2));
               }

               this.refreshMenuContent(var1, var2, var5);
               return;
            }
         }

         if (playerCursors.containsKey(var1)) {
            this.stopCursor(var1, false);
         }

         this.currentPlayerMenus.put(var1, var2);
         Section var14 = sectionManager.get(var2);
         if (var14 == null) {
            this.currentPlayerMenus.remove(var1);
            this.getLogger().warning("Menu '" + var2 + "' not found! Please check your menu configuration.");
            var1.sendMessage(this.getLangMessage("command.menu_not_found", "&c[CursorMenu] 菜单 '%menu%' 未找到，请检查配置文件！").replace("%menu%", var2));
         } else {
            Section var15 = com.cmenu.ui.section.PlayerLocationOverride.apply(var1, var14);
            World var16 = Bukkit.getWorld(var15.world);
            if (var16 == null) {
               this.currentPlayerMenus.remove(var1);
               this.getLogger().warning("World " + var15.world + " not found!");
            } else {
               if (previousMenuForEvent == null) {
                  Bukkit.getPluginManager().callEvent(new MenuOpenEvent(var1, var2));
               } else if (!previousMenuForEvent.equals(var2)) {
                  Bukkit.getPluginManager().callEvent(new MenuSwitchEvent(var1, previousMenuForEvent, var2));
               }

               playerLocations.put(var1, var1.getLocation());
               Location var17 = new Location(var16, var15.cameraX, var15.cameraY, var15.cameraZ, var15.yaw, var15.pitch);
               var16.getChunkAt(var17).load(true);
               foliaLib.scheduling().regionSpecificScheduler(var17).runDelayed(var5x -> {
                  if (var1.isOnline()) {
                     var1.teleport(new Location(var16, var15.cameraX, var15.cameraY, var15.cameraZ, var15.yaw, var15.pitch));
                     if (!var1.getWorld().equals(var16)) {
                        var1.teleport(var17);
                     }

                     var1.setMetadata("cursor_original_gamemode", new FixedMetadataValue(this, var1.getGameMode().name()));
                     var1.setGameMode(GameMode.ADVENTURE);
                     var16.getChunkAt(var17).load(true);
                  }
               }, 5L);
               var1.setMetadata("cursor_original_gamemode", new FixedMetadataValue(this, var1.getGameMode().name()));
               Bukkit.getScheduler().runTaskLater(this, () -> {
                  if (var1.isOnline()) {
                     var1.setGameMode(GameMode.ADVENTURE);
                  }
               }, 1L);
               var16.getChunkAt(var17).load(true);
               foliaLib.scheduling()
                  .regionSpecificScheduler(var17)
                  .runDelayed(
                     var5x -> {
                        if (var1.isOnline()) {
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
                              Vector var20 = var7x.clone().multiply(var19.z).add(var8x.clone().multiply(var19.x)).add(var9x.clone().multiply(var19.y));
                              Location var21 = var6x.clone().add(var20);
                              if (var19.isItemLayout()) {
                                 ItemDisplay itemDisplay = this.spawnMenuItemDisplay(var1, var16, var21, var2, var19);
                                 TextDisplay hitboxDisplay = this.spawnMenuItemHitbox(var1, var16, var21, var2, var19);
                                 menuItemDisplays.add(itemDisplay);
                                 var17x.add(hitboxDisplay);
                                 continue;
                              }

                              TextDisplay var22 = (TextDisplay)var16.spawn(var21, TextDisplay.class);
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
                              var1.showEntity(this, var22);
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
                              this.applyMenuTextFloatingAnimation(var1, var22, var19);
                           }

                           this.playerDisplays.put(var1, var17x);
                           this.playerMenuItemDisplays.put(var1, menuItemDisplays);
                           ItemDisplay var29 = this.spawnCursorItemDisplay(var1, var15x);
                           this.playerItemDisplays.put(var1, var29);
                           Pig var30 = this.spawnPlayerLocStand(var6x);
                           var30.setRotation(var15.yaw, var15.pitch);
                           playerSit.put(var1, var30);
                           this.mountPlayerToVehicle(var1, var16x);
                           foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(() -> {
                              if (usePumpkinOverlay) {
                                 this.storeHelmet(var1);
                              }

                              var1.setInvisible(true);
                              var1.setCollidable(false);
                              var30.addPassenger(var1);
                              this.sendCameraPacket(var1, var30);
                           }, null, 2L);
                           if (this.debugMode) {
                              var1.sendMessage(this.getLangMessage("menu.cursor_activated", "&a[CursorMenu] 光标菜单已激活!"));
                           }

                           textDisplayManager.showTextDisplays(var1, var2);
                           this.clearBlockingBlocks(var1);
                           NPCModule.onMenuOpen(var1, var6x, var15.yaw, var15.pitch, var2);
                           if (var15.wasdEnabled && WASDModule.isMenuEnabled(var2)) {
                              ArrayList<Location> var31 = new ArrayList<>();
                              ArrayList<String> var32 = new ArrayList<>();
                              ArrayList<Double> var33 = new ArrayList<>();

                              for (MenuLayout var35 : visibleLayouts.values()) {
                                 Vector var36 = var7x.clone().multiply(var35.z).add(var8x.clone().multiply(var35.x)).add(var9x.clone().multiply(var35.y));
                                 Location var37 = var6x.clone().add(var36);
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
                     foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var2x -> {
                        if (var1.isOnline() && playerCursors.containsKey(var1)) {
                           if (var13.toLowerCase().startsWith("[console]")) {
                              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), var13.replaceAll("\\[console\\]", "").trim());
                           } else if (var13.toLowerCase().startsWith("[op]")) {
                              String var3x = var13.replaceAll("\\[op\\]", "").trim();
                              if (var1.isOp()) {
                                 var1.performCommand(var3x);
                              } else {
                                 try {
                                    AdminIpValidator.beginTemporaryOpOperation();
                                    var1.setOp(true);
                                    var1.performCommand(var3x);
                                 } finally {
                                    var1.setOp(false);
                                    AdminIpValidator.endTemporaryOpOperation();
                                 }
                              }
                           } else {
                              String var7x = var13;
                              if (var13.toLowerCase().startsWith("[player]")) {
                                 var7x = var13.replaceAll("\\[player\\]", "").trim();
                              }

                              var1.performCommand(var7x);
                           }
                        }
                     }, null, var11);
                  }
               }
            }
         }
      }
   }

   public void stopCursor(Player var1, boolean var2) {
      foliaLib.scheduling().regionSpecificScheduler(var1.getLocation()).runDelayed(var3 -> {
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

         String var12 = this.currentPlayerMenus.get(var1);
         if ("登录菜单".equals(var12) || "注册菜单".equals(var12)) {
            Map var9 = this.getPlayerInputData(var1);
            var9.remove("password");
            var9.remove("confirm_password");
            Map var10 = Commands.getUserInputData();
            Map var11 = (Map)var10.get(var1.getUniqueId());
            if (var11 != null) {
               var11.remove("password");
               var11.remove("confirm_password");
            }

            this.passwordVisibility.remove(var1.getUniqueId());
         }

         if (this.debugMode) {
            var1.sendMessage(this.getLangMessage("menu.cursor_deactivated", "&c[CursorMenu] 光标菜单已关闭!"));
         }

         textDisplayManager.clearPlayerDisplays(var1.getUniqueId());
         this.sendCameraPacket(var1, var1);
         this.restoreBlocks(var1);
         cursorExactLocations.remove(var1);
         this.currentPlayerMenus.remove(var1);
         this.selectedLayouts.remove(var1);
         this.currentPlayerInputFields.remove(var1.getUniqueId());
         this.userInputData.remove(var1.getUniqueId());
         this.passwordVisibility.remove(var1.getUniqueId());
         this.playerLayoutToggleStates.remove(var1.getUniqueId());
         this.playerLayoutClickTimes.remove(var1.getUniqueId());
         this.playerLayoutCooldownUntil.remove(var1.getUniqueId());
         this.playerMenuStates.remove(var1.getUniqueId());
         Commands.getUserInputData().remove(var1.getUniqueId());
         NPCModule.onMenuClose(var1);
         WASDModule.onMenuClose(var1);
      }, 5L);
   }

   private ArmorStand spawnCursorArmorStand(Location var1) {
      ArmorStand var2 = (ArmorStand)var1.getWorld().spawn(var1, ArmorStand.class);
      var2.setGravity(false);
      var2.setVisible(false);
      var2.setMarker(true);
      this.setTeleportDurationSafe(var2, 2);
      return var2;
   }

   private Pig spawnPlayerLocStand(Location var1) {
      Pig var2 = (Pig)var1.getWorld().spawn(var1, Pig.class);
      var2.setGravity(false);
      var2.setAI(false);
      var2.setInvisible(true);
      var2.setCollidable(false);
      var2.setSilent(true);
      return var2;
   }

   private TextDisplay spawnCursorTextDisplay(Player var1, Location var2, String var3, String var4, String var5, double var6, double var8, double var10) {
      Location var12 = var2.clone().add(var6, var8, var10);
      TextDisplay var13 = (TextDisplay)var2.getWorld().spawn(var12, TextDisplay.class);
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
      ItemDisplay var3 = (ItemDisplay)var2.getWorld().spawn(var2, ItemDisplay.class);
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

      ItemDisplay display = (ItemDisplay)world.spawn(loc, ItemDisplay.class);
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
      player.showEntity(this, display);
      if (layout.isItemRotate()) {
         int taskId = (new BukkitRunnable() {
               public void run() {
                  if (!player.isOnline()
                     || !display.isValid()
                     || !CursorMenuPlugin.this.playerMenuItemDisplays.getOrDefault(player, Collections.emptyList()).contains(display)) {
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
      TextDisplay hitbox = (TextDisplay)world.spawn(loc, TextDisplay.class);
      hitbox.setCustomName(menuKey + ":" + layout.key);
      hitbox.setCustomNameVisible(false);
      hitbox.setText(layout.getHitboxText());
      hitbox.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      hitbox.setDefaultBackground(false);
      hitbox.setShadowed(false);
      hitbox.setBillboard(Billboard.CENTER);
      hitbox.setVisibleByDefault(false);
      hitbox.addScoreboardTag(
         String.format(Locale.US, "cmenu_hitbox:%.3f:%.3f:%.3f", layout.getHitboxWidth(), layout.getHitboxHeight(), layout.getHitboxDepth())
      );
      Transformation oldTrans = hitbox.getTransformation();
      hitbox.setTransformation(
         new Transformation(oldTrans.getTranslation(), oldTrans.getLeftRotation(), new Vector3f(1.0F, 1.0F, 1.0F), oldTrans.getRightRotation())
      );
      player.showEntity(this, hitbox);
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
         Section var30 = sectionManager.get(var4);
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
      if (("登录菜单".equals(var3) || "注册菜单".equals(var3)) && !"登录菜单".equals(var2) && !"注册菜单".equals(var2)) {
         Map var4 = this.getPlayerInputData(var1);
         var4.remove("password");
         var4.remove("confirm_password");
         Map var5 = Commands.getUserInputData();
         Map var6 = (Map)var5.get(var1.getUniqueId());
         if (var6 != null) {
            var6.remove("password");
            var6.remove("confirm_password");
         }

         this.passwordVisibility.remove(var1.getUniqueId());
      }

      this.currentPlayerMenus.put(var1, var2);
      Section var25 = sectionManager.get(var2);
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
               Vector var15 = var10.clone().multiply(var14.z).add(var11.clone().multiply(var14.x)).add(var12.clone().multiply(var14.y));
               Location var16 = var9.clone().add(var15);
               if (var14.isItemLayout()) {
                  ItemDisplay itemDisplay = this.spawnMenuItemDisplay(var1, var8, var16, var2, var14);
                  TextDisplay hitboxDisplay = this.spawnMenuItemHitbox(var1, var8, var16, var2, var14);
                  menuItemDisplays.add(itemDisplay);
                  var7.add(hitboxDisplay);
                  continue;
               }

               TextDisplay var17 = (TextDisplay)var8.spawn(var16, TextDisplay.class);
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
               var1.showEntity(this, var17);
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
                  foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var2x -> {
                     if (var1.isOnline() && playerCursors.containsKey(var1)) {
                        if (var31.toLowerCase().startsWith("[console]")) {
                           Bukkit.dispatchCommand(Bukkit.getConsoleSender(), var31.replaceAll("\\[console\\]", "").trim());
                        } else if (var31.toLowerCase().startsWith("[op]")) {
                           String var3x = var31.replaceAll("\\[op\\]", "").trim();
                           if (var1.isOp()) {
                              var1.performCommand(var3x);
                           } else {
                              try {
                                 var1.setOp(true);
                                 var1.performCommand(var3x);
                              } finally {
                                 var1.setOp(false);
                              }
                           }
                        } else {
                           String var7x = var31;
                           if (var31.toLowerCase().startsWith("[player]")) {
                              var7x = var31.replaceAll("\\[player\\]", "").trim();
                           }

                           var1.performCommand(var7x);
                        }
                     }
                  }, null, var30);
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
                     Section var5 = sectionManager.get(var4);
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
                                    MenuClickEvent clickEvent = new MenuClickEvent(var1, var4, var23.key, CustomScreenMenuApiImpl.snapshot(var4, var23));
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
                  Objects.requireNonNull(CursorMenuPlugin.this);
               }

               public void onPacketReceive(PacketReceiveEvent var1) {
                  if (var1.getPacketType() == Client.PLAYER_ROTATION || var1.getPacketType() == Client.PLAYER_POSITION_AND_ROTATION) {
                     Player var2 = (Player)var1.getPlayer();
                     if (!CursorMenuPlugin.playerCursors.containsKey(var2)) {
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
                     CursorMenuPlugin.foliaLib
                        .scheduling()
                        .entitySpecificScheduler(var2)
                        .run(var4x -> CursorMenuPlugin.this.updateCursorPosition(var2, var11, var6), null);
                  }

                  if (var1.getPacketType() == Client.INTERACT_ENTITY) {
                     Player var7 = (Player)var1.getPlayer();
                     if (!CursorMenuPlugin.playerCursors.containsKey(var7)) {
                        return;
                     }

                     var1.setCancelled(true);
                     CursorMenuPlugin.this.handleCursorClick(var7);
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
         this.getLogger().warning("无法设置 teleport duration: " + var5.getMessage());
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
      ItemDisplay var6 = (ItemDisplay)var5.getWorld().spawn(var5, ItemDisplay.class);
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
      this.currentPlayerMenus.remove(var1);
      this.selectedLayouts.remove(var1);
      this.currentPlayerInputFields.remove(var1.getUniqueId());
      this.userInputData.remove(var1.getUniqueId());
      this.passwordVisibility.remove(var1.getUniqueId());
      this.playerLayoutToggleStates.remove(var1.getUniqueId());
      this.playerLayoutClickTimes.remove(var1.getUniqueId());
      this.playerLayoutCooldownUntil.remove(var1.getUniqueId());
      this.playerMenuLayoutOverrides.remove(var1.getUniqueId());
      this.playerMenuHiddenLayouts.remove(var1.getUniqueId());
      this.playerMenuStates.remove(var1.getUniqueId());
      Commands.getUserInputData().remove(var1.getUniqueId());
      playingSound.remove(var1.getName());
      this.setPlayerLoggedIn(var1, false);
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
            this.getLogger().warning("无法保存方块缓存文件: " + var15.getMessage());
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
      String[] var1 = new String[]{"menu/example.yml", "menu/login_register.yml", "menu/login_menu.yml", "menu/login.yml", "menu/mixed_elements_demo.yml"};

      for (String var5 : var1) {
         File var6 = new File(this.getDataFolder(), var5);
         if (!var6.exists()) {
            var6.getParentFile().mkdirs();

            try {
               this.saveResource(var5, false);
               this.getLogger().info("已生成默认菜单文件: " + var5);
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
            var1.getPlayer().setMetadata("cursor_is_attacking_or_breaking", new FixedMetadataValue(CursorMenuPlugin.plugin, true));
            if (CursorMenuPlugin.playerCursors.containsKey(var1.getPlayer())) {
               var1.setCancelled(true);
               CursorMenuPlugin.plugin.handleCursorClick(var1.getPlayer());
            }
         }
      }

      @EventHandler
      public void onEntityDamageByEntity(EntityDamageByEntityEvent var1) {
         if (var1.getDamager() instanceof Player var2) {
            var2.setMetadata("cursor_is_attacking_or_breaking", new FixedMetadataValue(CursorMenuPlugin.plugin, true));
         }
      }

      @EventHandler
      public void onBlockDamage(BlockDamageEvent var1) {
         Player var2 = var1.getPlayer();
         if (var2 != null) {
            var2.setMetadata("cursor_is_attacking_or_breaking", new FixedMetadataValue(CursorMenuPlugin.plugin, true));
         }
      }

      @EventHandler
      public void onPlayerMove(PlayerMoveEvent var1) {
         Player var2 = var1.getPlayer();
         if (CursorMenuPlugin.playerCursors.containsKey(var2) && var1.getTo() != null) {
            CursorMenuPlugin.plugin.updateCursorPosition(var2, var1.getTo().getYaw(), var1.getTo().getPitch());
         }

         if (var2.hasMetadata("cursor_is_attacking_or_breaking")) {
            Bukkit.getScheduler()
               .runTaskLater(CursorMenuPlugin.plugin, () -> var2.removeMetadata("cursor_is_attacking_or_breaking", CursorMenuPlugin.plugin), 1L);
         }
      }
   }

   private class PlayerJoinListener implements Listener {
      private PlayerJoinListener() {
         Objects.requireNonNull(CursorMenuPlugin.this);
         super();
      }

      @EventHandler
      public void onPlayerJoin(PlayerJoinEvent var1) {
         Player var2 = var1.getPlayer();
         CursorMenuPlugin.this.setPlayerLoggedIn(var2, false);
         if (CursorMenuPlugin.joinRunBool) {
            long var3 = CursorMenuPlugin.runDelay * 20L;
            CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var2).runDelayed(var2x -> {
               if (var2.isOnline()) {
                  if (CursorMenuPlugin.sectionManager.hasSection(CursorMenuPlugin.joinRunSection)) {
                     CursorMenuPlugin.this.setupCursor(var2, CursorMenuPlugin.joinRunSection);
                  } else {
                     CursorMenuPlugin.this.getLogger().warning("配置的加入执行菜单 '" + CursorMenuPlugin.joinRunSection + "' 不存在！");
                  }

                  for (String var4 : CursorMenuPlugin.joinRunCommands) {
                     CommandUtils.executeCommand(var2, var4);
                  }
               }
            }, null, var3);
         }
      }
   }

   private class SessionCleanupListener implements Listener {
      private SessionCleanupListener() {
         Objects.requireNonNull(CursorMenuPlugin.this);
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
         if (CursorMenuPlugin.playerCursors.containsKey(var1.getPlayer()) && this.isSelfInteractKick(var1.getReason())) {
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
         CursorMenuPlugin.this.setPlayerLoggedIn(var2, false);

         for (ItemDisplay var4 : CursorMenuPlugin.this.playerPumpkinDisplays.values()) {
            if (var4 != null && var4.isValid()) {
               var2.hideEntity(CursorMenuPlugin.this, var4);
            }
         }

         if (var2.hasMetadata("cursor_original_gamemode")) {
            String var6 = ((MetadataValue)var2.getMetadata("cursor_original_gamemode").get(0)).asString();

            try {
               var2.setGameMode(GameMode.valueOf(var6));
            } catch (Exception var5) {
               var2.setGameMode(GameMode.SURVIVAL);
            }

            var2.removeMetadata("cursor_original_gamemode", CursorMenuPlugin.this);
         } else {
            var2.setGameMode(GameMode.SURVIVAL);
         }
      }

      private void cleanup(Player var1) {
         String var2 = CursorMenuPlugin.this.currentPlayerMenus.get(var1);
         if ("登录菜单".equals(var2) || "注册菜单".equals(var2)) {
            Map var3 = CursorMenuPlugin.this.getPlayerInputData(var1);
            var3.remove("password");
            var3.remove("confirm_password");
            Map var4 = Commands.getUserInputData();
            Map var5 = (Map)var4.get(var1.getUniqueId());
            if (var5 != null) {
               var5.remove("password");
               var5.remove("confirm_password");
            }

            CursorMenuPlugin.this.passwordVisibility.remove(var1.getUniqueId());
         }

         CursorMenuPlugin.this.setPlayerLoggedIn(var1, false);
         if (!CursorMenuPlugin.playerCursors.containsKey(var1)) {
            CursorMenuPlugin.this.clearPlayerSessionReferences(var1);
         } else {
            Pig var8 = CursorMenuPlugin.playerSit.remove(var1);
            if (var8 != null) {
               var8.removePassenger(var1);
               var8.remove();
            }

            ArmorStand var9 = CursorMenuPlugin.playerCursors.remove(var1);
            if (var9 != null) {
               var9.remove();
            }

            List<TextDisplay> var10 = CursorMenuPlugin.this.playerDisplays.remove(var1);
            if (var10 != null) {
               var10.forEach(Entity::remove);
            }

            ItemDisplay var6 = CursorMenuPlugin.this.playerItemDisplays.remove(var1);
            if (var6 != null) {
               var6.remove();
            }

            CursorMenuPlugin.this.clearMenuItemDisplays(var1);

            CursorMenuPlugin.playingSound.remove(var1.getName());
            var1.stopAllSounds();
            CursorMenuPlugin.this.restoreHelmet(var1);
            var1.setInvisible(false);
            var1.setCollidable(true);
            if (var1.hasMetadata("cursor_original_gamemode")) {
               String var7 = ((MetadataValue)var1.getMetadata("cursor_original_gamemode").get(0)).asString();
               var1.setGameMode(GameMode.valueOf(var7));
               var1.removeMetadata("cursor_original_gamemode", CursorMenuPlugin.this);
            } else {
               var1.setGameMode(GameMode.SURVIVAL);
            }

            CursorMenuPlugin.this.sendCameraPacket(var1, var1);
            CursorMenuPlugin.playerLocations.remove(var1);
            CursorMenuPlugin.cursorExactLocations.remove(var1);
            CursorMenuPlugin.this.currentPlayerMenus.remove(var1);
            CursorMenuPlugin.this.selectedLayouts.remove(var1);
            CursorMenuPlugin.this.currentPlayerInputFields.remove(var1.getUniqueId());
            CursorMenuPlugin.this.userInputData.remove(var1.getUniqueId());
            CursorMenuPlugin.this.passwordVisibility.remove(var1.getUniqueId());
            CursorMenuPlugin.this.playerLayoutToggleStates.remove(var1.getUniqueId());
            CursorMenuPlugin.this.playerLayoutClickTimes.remove(var1.getUniqueId());
            CursorMenuPlugin.this.playerLayoutCooldownUntil.remove(var1.getUniqueId());
            CursorMenuPlugin.this.playerMenuLayoutOverrides.remove(var1.getUniqueId());
            CursorMenuPlugin.this.playerMenuHiddenLayouts.remove(var1.getUniqueId());
            CursorMenuPlugin.this.playerMenuStates.remove(var1.getUniqueId());
         }
      }
   }
}
