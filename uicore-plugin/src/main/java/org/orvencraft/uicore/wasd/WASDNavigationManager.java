package org.orvencraft.uicore.wasd;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import org.orvencraft.uicore.UIcorePlugin;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class WASDNavigationManager {
   private static WASDNavigationManager instance;
   private final JavaPlugin plugin;
   private WASDConfig config;
   private PacketListenerAbstract packetListener;
   private boolean registered = false;
   private WASDExpansion papiExpansion;
   private final Map<UUID, WASDSession> playerSessions = new ConcurrentHashMap<>();
   private final Map<UUID, Location[]> playerTextLocations = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> playerSelectedIndex = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> playerSelectionCooldown = new ConcurrentHashMap<>();
   private final Map<UUID, List<String>> playerElementIds = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> playerJumpPressed = new ConcurrentHashMap<>();
   private final Map<UUID, String> playerCurrentMenus = new ConcurrentHashMap<>();
   private record PendingInput(Player player, long generation, float forward, float sideways, boolean jump) {}
   private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
   private final Map<UUID, Long> selectionCooldownUntil = new ConcurrentHashMap<>();


   private WASDNavigationManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.config = new WASDConfig(plugin);
   }

   public static synchronized WASDNavigationManager getInstance(JavaPlugin plugin) {
      if (instance == null) {
         instance = new WASDNavigationManager(plugin);
      }

      return instance;
   }

   public static WASDNavigationManager getInstance() {
      return instance;
   }

   static synchronized void clearInstance() {
      instance = null;
   }

   public void initialize() {
      if (!this.registered) {
         this.registerPacketListener();
         this.registerPAPIExpansion();
         this.registered = true;
         this.plugin.getLogger().info("[WASDNavigation] WASD navigation module initialized.");
      }
   }

   /** Drain only this player's latest packet while on the owning entity region. */
   public void drainInput(Player player) {
      if (player == null) {
         return;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         player.getScheduler().execute(this.plugin, () -> this.drainInput(player), null, 0L);
         return;
      }
      UUID id = player.getUniqueId();
      PendingInput input = this.pendingInputs.remove(id);
      if (input != null && ((UIcorePlugin)this.plugin).isMenuSessionCurrent(player, input.generation()) && this.isWASDEnabledForPlayer(id))
         this.handlePlayerInput(player, input.forward(), input.sideways(), input.jump());
   }

   private void registerPAPIExpansion() {
      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         try {
            this.papiExpansion = new WASDExpansion(this);
            if (this.papiExpansion.register()) {
               this.plugin.getLogger().info("[WASDNavigation] PlaceholderAPI expansion registered.");
            }
         } catch (Exception var2) {
            this.plugin.getLogger().warning("[WASDNavigation] Could not register PlaceholderAPI expansion: " + var2.getMessage());
         }
      }
   }

   public boolean isRegistered() {
      return this.registered;
   }

   private void registerPacketListener() {
      this.packetListener = new PacketListenerAbstract() {
         public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != Client.PLAYER_INPUT || !(event.getPlayer() instanceof Player player)) return;
            UUID playerId = player.getUniqueId();
            if (!WASDNavigationManager.this.isWASDEnabledForPlayer(playerId)) return;
            WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);
            float forward = (packet.isForward() ? 1F : 0F) - (packet.isBackward() ? 1F : 0F);
            float sideways = (packet.isLeft() ? 1F : 0F) - (packet.isRight() ? 1F : 0F);
            pendingInputs.put(playerId, new PendingInput(player, ((UIcorePlugin)plugin).getMenuSessionGeneration(player), forward, sideways, packet.isJump()));
         }
      };
      PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);
   }

   private void handlePlayerInput(Player player, float forward, float sideways, boolean jump) {
      UUID playerId = player.getUniqueId();
      if (this.playerSessions.containsKey(playerId)) {
         boolean wasJumpPressed = this.playerJumpPressed.getOrDefault(playerId, false);
         this.playerJumpPressed.put(playerId, jump);
         if (jump && !wasJumpPressed) {
            this.executeSelectedCommand(player);
         }

         if (forward != 0.0F || sideways != 0.0F) {
            this.handleNavigationInput(player, forward, sideways);
         }
      }
   }

   private void handleNavigationInput(Player player, float forward, float sideways) {
      UUID playerId = player.getUniqueId();
      if (!this.isOnCooldown(playerId)) {
         int currentIndex = this.playerSelectedIndex.getOrDefault(playerId, 0);
         int newIndex = -1;
         if (forward > 0.0F) {
            newIndex = this.findPrevText(playerId, currentIndex);
         } else if (forward < 0.0F) {
            newIndex = this.findNextText(playerId, currentIndex);
         } else if (sideways > 0.0F) {
            newIndex = this.findLeftText(player, playerId, currentIndex);
         } else if (sideways < 0.0F) {
            newIndex = this.findRightText(player, playerId, currentIndex);
         }

         if (newIndex != -1 && newIndex != currentIndex) {
            this.setSelectedIndex(player, newIndex);
            this.startCooldown(playerId);
         }
      }
   }

   private int findPrevText(UUID playerId, int currentIndex) {
      Location[] locations = this.playerTextLocations.get(playerId);
      if (locations != null && currentIndex >= 0 && currentIndex < locations.length) {
         Location currentLoc = locations[currentIndex];
         if (currentLoc == null) {
            return -1;
         } else {
            double currentY = currentLoc.getY();
            double currentX = currentLoc.getX();
            double currentZ = currentLoc.getZ();
            int bestIndex = -1;
            double smallestYDiff = Double.MAX_VALUE;

            for (int i = 0; i < locations.length; i++) {
               if (i != currentIndex && locations[i] != null) {
                  Location targetLoc = locations[i];
                  double targetY = targetLoc.getY();
                  double targetX = targetLoc.getX();
                  double targetZ = targetLoc.getZ();
                  if (!(Math.abs(targetX - currentX) > this.config.getHorizontalThreshold())
                     && !(Math.abs(targetZ - currentZ) > this.config.getHorizontalThreshold())) {
                     double yDiff = targetY - currentY;
                     if (yDiff > 0.0 && yDiff < smallestYDiff) {
                        smallestYDiff = yDiff;
                        bestIndex = i;
                     }
                  }
               }
            }

            return bestIndex;
         }
      } else {
         return -1;
      }
   }

   private int findNextText(UUID playerId, int currentIndex) {
      Location[] locations = this.playerTextLocations.get(playerId);
      if (locations != null && currentIndex >= 0 && currentIndex < locations.length) {
         Location currentLoc = locations[currentIndex];
         if (currentLoc == null) {
            return -1;
         } else {
            double currentY = currentLoc.getY();
            double currentX = currentLoc.getX();
            double currentZ = currentLoc.getZ();
            int bestIndex = -1;
            double smallestYDiff = Double.MAX_VALUE;

            for (int i = 0; i < locations.length; i++) {
               if (i != currentIndex && locations[i] != null) {
                  Location targetLoc = locations[i];
                  double targetY = targetLoc.getY();
                  double targetX = targetLoc.getX();
                  double targetZ = targetLoc.getZ();
                  if (!(Math.abs(targetX - currentX) > this.config.getHorizontalThreshold())
                     && !(Math.abs(targetZ - currentZ) > this.config.getHorizontalThreshold())) {
                     double yDiff = currentY - targetY;
                     if (yDiff > 0.0 && yDiff < smallestYDiff) {
                        smallestYDiff = yDiff;
                        bestIndex = i;
                     }
                  }
               }
            }

            return bestIndex;
         }
      } else {
         return -1;
      }
   }

   private int findLeftText(Player player, UUID playerId, int currentIndex) {
      Location[] locations = this.playerTextLocations.get(playerId);
      if (locations != null && currentIndex >= 0 && currentIndex < locations.length) {
         Location currentLoc = locations[currentIndex];
         if (currentLoc == null) {
            return -1;
         } else {
            Vector playerDirection = player.getLocation().getDirection().setY(0).normalize();
            Vector rightVector = playerDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            Vector leftVector = rightVector.clone().multiply(-1);
            int bestIndex = -1;
            double bestDotProduct = -1.0;

            for (int i = 0; i < locations.length; i++) {
               if (i != currentIndex && locations[i] != null) {
                  Location targetLoc = locations[i];
                  Vector offset = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
                  double dotProduct = offset.dot(leftVector);
                  if (dotProduct > this.config.getDotProductThreshold() && dotProduct > bestDotProduct) {
                     bestDotProduct = dotProduct;
                     bestIndex = i;
                  }
               }
            }

            return bestIndex;
         }
      } else {
         return -1;
      }
   }

   private int findRightText(Player player, UUID playerId, int currentIndex) {
      Location[] locations = this.playerTextLocations.get(playerId);
      if (locations != null && currentIndex >= 0 && currentIndex < locations.length) {
         Location currentLoc = locations[currentIndex];
         if (currentLoc == null) {
            return -1;
         } else {
            Vector playerDirection = player.getLocation().getDirection().setY(0).normalize();
            Vector rightVector = playerDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            int bestIndex = -1;
            double bestDotProduct = -1.0;

            for (int i = 0; i < locations.length; i++) {
               if (i != currentIndex && locations[i] != null) {
                  Location targetLoc = locations[i];
                  Vector offset = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
                  double dotProduct = offset.dot(rightVector);
                  if (dotProduct > this.config.getDotProductThreshold() && dotProduct > bestDotProduct) {
                     bestDotProduct = dotProduct;
                     bestIndex = i;
                  }
               }
            }

            return bestIndex;
         }
      } else {
         return -1;
      }
   }

   private void setSelectedIndex(Player player, int index) {
      UUID playerId = player.getUniqueId();
      int oldIndex = this.playerSelectedIndex.getOrDefault(playerId, 0);
      this.playerSelectedIndex.put(playerId, index);
      WASDSession session = this.playerSessions.get(playerId);
      if (session != null) {
         session.onSelectionChanged(player, oldIndex, index);
      }

      if (this.papiExpansion != null) {
         Location[] locations = this.playerTextLocations.get(playerId);
         if (locations != null && index >= 0 && index < locations.length && locations[index] != null) {
            String menuKey = this.playerCurrentMenus.get(playerId);
            if (menuKey != null) {
               this.papiExpansion.updatePlayerMenu(playerId, menuKey, locations[index]);
            }
         }
      }

      if (this.config.isDebugMode()) {
         this.plugin.getLogger().info("[WASDNavigation] " + player.getName() + " selected index " + index + ".");
      }
   }

   private void executeSelectedCommand(Player player) {
      UUID playerId = player.getUniqueId();
      int selectedIndex = this.playerSelectedIndex.getOrDefault(playerId, -1);
      if (selectedIndex >= 0) {
         List<String> elementIds = this.playerElementIds.get(playerId);
         if (elementIds != null && selectedIndex < elementIds.size()) {
            String elementId = elementIds.get(selectedIndex);
            if (elementId != null && !elementId.isBlank() && this.playerSessions.containsKey(playerId) && UIcorePlugin.plugin != null) {
               if (this.config.isDebugMode()) {
                  this.plugin.getLogger().info("[WASDNavigation] Player " + player.getName() + " activates element " + elementId + ".");
               }
               UIcorePlugin.plugin.activateMenuElement(player, elementId);
            }
         }
      }
   }

   private boolean isOnCooldown(UUID playerId) {
      return this.selectionCooldownUntil.getOrDefault(playerId, 0L) > System.nanoTime();
   }

   private void startCooldown(UUID playerId) {
      this.selectionCooldownUntil.put(playerId, System.nanoTime() + Math.max(1L, this.config.getSelectionCooldown()) * 1_000_000L);
   }

   private boolean isWASDEnabledForPlayer(UUID playerId) {
      WASDSession session = this.playerSessions.get(playerId);
      return session != null && session.isEnabled();
   }

   public WASDSession getPlayerSession(UUID playerId) {
      return this.playerSessions.get(playerId);
   }

   public String getPlayerCurrentMenu(UUID playerId) {
      return this.playerCurrentMenus.get(playerId);
   }

   public Location getPlayerCurrentLocation(UUID playerId) {
      String menuKey = this.playerCurrentMenus.get(playerId);
      if (menuKey == null) {
         return null;
      } else {
         int index = this.playerSelectedIndex.getOrDefault(playerId, -1);
         if (index < 0) {
            return null;
         } else {
            Location[] locations = this.playerTextLocations.get(playerId);
            return locations != null && index < locations.length ? locations[index] : null;
         }
      }
   }

   public void startSession(Player player, Location[] textLocations, List<String> elementIds, int initialIndex) {
      this.startSession(player, null, textLocations, elementIds, null, null, initialIndex);
   }

   public void startSession(Player player, String menuKey, Location[] textLocations, List<String> elementIds, TextDisplay[] textDisplays, List<Double> scales, int initialIndex) {
      final int requestedInitialIndex = initialIndex;
      if (!this.runOnPlayerRegion(player, () -> this.startSession(player, menuKey, textLocations, elementIds, textDisplays, scales, requestedInitialIndex))) {
         return;
      }
      UUID playerId = player.getUniqueId();
      if (menuKey != null && menuKey.equals(this.playerCurrentMenus.get(playerId))) {
         List<String> oldIds = this.playerElementIds.getOrDefault(playerId, List.of());
         int oldIndex = this.playerSelectedIndex.getOrDefault(playerId, -1);
         if (oldIndex >= 0 && oldIndex < oldIds.size() && elementIds != null) {
            int matched = elementIds.indexOf(oldIds.get(oldIndex));
            if (matched >= 0) initialIndex = matched;
         }
      }
      initialIndex = textLocations == null || textLocations.length == 0 ? -1 : Math.max(0, Math.min(initialIndex, textLocations.length - 1));
      WASDSession previous = this.playerSessions.get(playerId);
      if (previous != null) previous.restoreDisplays();
      WASDSession session = new WASDSession(this.plugin, player, true);
      session.setTextDisplays(textDisplays == null ? new TextDisplay[0] : textDisplays, scales);
      this.playerSessions.put(playerId, session);
      this.playerTextLocations.put(playerId, textLocations);
      this.playerElementIds.put(playerId, elementIds == null ? List.of() : List.copyOf(elementIds));
      this.playerSelectedIndex.put(playerId, initialIndex);
      this.playerSelectionCooldown.put(playerId, false);
      this.playerJumpPressed.put(playerId, false);
      if (menuKey != null) {
         this.playerCurrentMenus.put(playerId, menuKey);
         if (this.papiExpansion != null && textLocations != null && textLocations.length > 0) {
            this.papiExpansion.updatePlayerMenu(playerId, menuKey, textLocations[initialIndex >= 0 && initialIndex < textLocations.length ? initialIndex : 0]);
         }
      }

      if (this.config.isDebugMode()) {
         this.plugin.getLogger().info("[WASDNavigation] Started session for " + player.getName() + ", menu: " + (menuKey != null ? menuKey : "unknown"));
      }
   }

   public void updateTextLocations(Player player, Location[] textLocations, List<String> elementIds) {
      if (!this.runOnPlayerRegion(player, () -> this.updateTextLocations(player, textLocations, elementIds))) {
         return;
      }
      UUID playerId = player.getUniqueId();
      this.playerTextLocations.put(playerId, textLocations);
      this.playerElementIds.put(playerId, elementIds == null ? List.of() : List.copyOf(elementIds));
   }

   public void stopSession(Player player) {
      if (!this.runOnPlayerRegion(player, () -> this.stopSession(player))) {
         return;
      }
      UUID playerId = player.getUniqueId();
      WASDSession session = this.playerSessions.remove(playerId);
      if (session != null) {
         session.restoreDisplays();
      }
      this.playerTextLocations.remove(playerId);
      this.playerSelectedIndex.remove(playerId);
      this.playerSelectionCooldown.remove(playerId);
      this.selectionCooldownUntil.remove(playerId);
      this.pendingInputs.remove(playerId);
      this.playerElementIds.remove(playerId);
      this.playerJumpPressed.remove(playerId);
      this.playerCurrentMenus.remove(playerId);
      if (this.papiExpansion != null) {
         this.papiExpansion.clearPlayerMenu(playerId);
      }

      if (this.config.isDebugMode()) {
         this.plugin.getLogger().info("[WASDNavigation] Stopped session for " + player.getName() + ".");
      }
   }

   public int getSelectedIndex(Player player) {
      return this.playerSelectedIndex.getOrDefault(player.getUniqueId(), -1);
   }

   public WASDConfig getConfig() {
      return this.config;
   }

   public void reloadConfig() {
      this.config.reload();
   }

   public void shutdown() {

      this.pendingInputs.clear();
      this.selectionCooldownUntil.clear();
      if (this.packetListener != null && this.registered) {
         PacketEvents.getAPI().getEventManager().unregisterListener(this.packetListener);
      }

      if (this.papiExpansion != null) {
         try {
            this.papiExpansion.unregister();
         } catch (Exception var2) {
         }
      }

      this.playerSessions.values().forEach(WASDSession::restoreDisplays);
      this.playerSessions.clear();
      this.playerTextLocations.clear();
      this.playerSelectedIndex.clear();
      this.playerSelectionCooldown.clear();
      this.playerElementIds.clear();
      this.playerJumpPressed.clear();
      this.playerCurrentMenus.clear();
      this.registered = false;
   }

   /** Returns true only when the caller already owns the player's region. */
   private boolean runOnPlayerRegion(Player player, Runnable action) {
      if (player == null) {
         return false;
      }
      if (Bukkit.isOwnedByCurrentRegion(player)) {
         return true;
      }
      player.getScheduler().execute(this.plugin, action, null, 0L);
      return false;
   }
}
