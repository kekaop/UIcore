package org.orvencraft.uicore.session;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.section.MenuPrivacySettings;

class MenuVisibilityManagerTest {
   private final List<Player> online = new ArrayList<>();
   private final Map<UUID, Map<UUID, Set<Plugin>>> hides = new HashMap<>();
   private final ArrayDeque<Runnable> scheduled = new ArrayDeque<>();
   private final Set<UUID> privateByDefault = new HashSet<>();
   private UUID owner;
   private int calls;
   private final Plugin plugin = plugin("UIcore");
   private final MenuVisibilityManager manager = new MenuVisibilityManager(plugin, () -> online, (viewer, work) -> {
      scheduled.add(() -> { owner = viewer.getUniqueId(); try { work.run(); } finally { owner = null; } });
   });

   private Player player(UUID id) {
      hides.put(id, new HashMap<>());
      Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
         case "getUniqueId" -> id;
         case "isVisibleByDefault" -> !privateByDefault.contains(id);
         case "isOnline" -> online.contains(proxy);
         case "equals" -> proxy == args[0];
         case "hashCode" -> System.identityHashCode(proxy);
         case "toString" -> id.toString();
         case "canSee" -> hides.get(id).getOrDefault(((Player)args[0]).getUniqueId(), Set.of()).isEmpty();
         case "hidePlayer", "showPlayer" -> {
            assertEquals(id, owner, "Visibility must run on the viewer's scheduler");
            assertNotEquals(id, ((Player)args[1]).getUniqueId(), "Never hide self");
            Set<Plugin> requests = hides.get(id).computeIfAbsent(((Player)args[1]).getUniqueId(), ignored -> new HashSet<>());
            if (method.getName().equals("hidePlayer")) requests.add((Plugin)args[0]); else requests.remove(args[0]);
            calls++;
            yield null;
         }
         default -> throw new UnsupportedOperationException(method.getName());
      });
      online.add(player);
      return player;
   }

   private Player player() { return player(UUID.randomUUID()); }
   private void disconnect(Player player) {
      online.remove(player);
      // Paper's onEntityRemove clears visibility references to the disconnected entity.
      hides.values().forEach(targets -> targets.remove(player.getUniqueId()));
      manager.quit(player);
   }
   private void drain() { while (!scheduled.isEmpty()) scheduled.removeFirst().run(); }
   private boolean visible(Player viewer, Player target) {
      return !privateByDefault.contains(target.getUniqueId()) && hides.get(viewer.getUniqueId()).getOrDefault(target.getUniqueId(), Set.of()).isEmpty();
   }
   private static Plugin plugin(String name) {
      return (Plugin)Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) -> switch (method.getName()) {
         case "equals" -> proxy == args[0];
         case "hashCode" -> System.identityHashCode(proxy);
         case "getName", "toString" -> name;
         default -> throw new UnsupportedOperationException(method.getName());
      });
   }

   @Test void combinesBothDirectionsAndClosingOneMenuKeepsTheOtherRule() {
      Player alice = player(), bob = player(), carol = player();
      manager.open(alice, MenuPrivacySettings.DEFAULT);
      manager.open(bob, new MenuPrivacySettings(false, false, true, false));
      assertEquals(0, calls);
      drain();
      assertFalse(visible(bob, alice));
      assertFalse(visible(carol, alice));
      assertTrue(visible(alice, bob));
      assertFalse(visible(bob, carol));
      manager.close(alice); drain();
      assertTrue(visible(carol, alice));
      assertFalse(visible(bob, alice));
      manager.close(bob); drain();
      for (Player viewer : online) for (Player target : online) assertTrue(visible(viewer, target));
      int previous = calls;
      manager.close(bob); manager.refresh(); drain();
      assertEquals(previous, calls, "Closing is idempotent");
   }

   @Test void queuedWorkUsesTheLatestMenuPolicy() {
      Player alice = player(), bob = player();
      manager.open(alice, MenuPrivacySettings.DEFAULT);
      manager.close(alice);
      drain();
      assertEquals(0, calls, "Delayed open cannot hide a player after close");
      manager.open(alice, MenuPrivacySettings.DEFAULT); drain();
      assertFalse(visible(bob, alice));
      manager.open(alice, new MenuPrivacySettings(false, false, false, true)); drain();
      assertTrue(visible(bob, alice));
      assertFalse(manager.blocksPlayerDamage(alice.getUniqueId()));
   }

   @Test void newAndReconnectedPlayersReceiveBothVisibilityRules() {
      Player alice = player(), bob = player();
      manager.open(alice, new MenuPrivacySettings(true, true, true, false)); drain();
      Player carol = player(); manager.joined(carol); drain();
      assertFalse(visible(carol, alice));
      assertFalse(visible(alice, carol));
      disconnect(bob); drain();
      Player reconnected = player(bob.getUniqueId()); manager.joined(reconnected); drain();
      assertFalse(visible(reconnected, alice));
      assertFalse(visible(alice, reconnected));
      disconnect(alice); drain();
      assertFalse(manager.blocksPlayerDamage(alice.getUniqueId()));
      Player newAlice = player(alice.getUniqueId()); manager.joined(newAlice); drain();
      assertTrue(visible(carol, newAlice));
      assertTrue(visible(newAlice, carol));
   }

   @Test void closingOnlyRemovesUIcoresHideRequest() {
      Player alice = player(), bob = player();
      Plugin foreign = plugin("Vanish");
      hides.get(bob.getUniqueId()).put(alice.getUniqueId(), new HashSet<>(Set.of(foreign)));
      manager.open(alice, MenuPrivacySettings.DEFAULT); drain();
      manager.close(alice); drain();
      assertFalse(visible(bob, alice));
      assertEquals(Set.of(foreign), hides.get(bob.getUniqueId()).get(alice.getUniqueId()));
   }

   @Test void restoresVisibilityRulesAfterServerTrackingResets() {
      Player alice = player(), bob = player();
      manager.open(alice, MenuPrivacySettings.DEFAULT); drain();
      hides.get(bob.getUniqueId()).remove(alice.getUniqueId());
      assertTrue(visible(bob, alice));
      manager.refresh(); drain();
      assertFalse(visible(bob, alice));
      manager.close(alice); drain();
      assertTrue(visible(bob, alice));
   }

   @Test void closeDoesNotShowAPlayerHiddenByDefault() {
      Player alice = player(), bob = player();
      privateByDefault.add(alice.getUniqueId());
      manager.open(alice, MenuPrivacySettings.DEFAULT); drain();
      manager.close(alice); drain();
      assertEquals(0, calls);
      assertFalse(visible(bob, alice));
   }

   @Test void damageProtectionIsIncomingOnlyAndEndsWithTheVisit() {
      Player alice = player(), bob = player();
      assertFalse(manager.blocksPlayerDamage(alice.getUniqueId()));
      manager.open(alice, MenuPrivacySettings.DEFAULT);
      assertTrue(manager.blocksPlayerDamage(alice.getUniqueId()));
      assertFalse(manager.blocksPlayerDamage(bob.getUniqueId()));
      manager.open(alice, new MenuPrivacySettings(true, true, false, true));
      assertFalse(manager.blocksPlayerDamage(alice.getUniqueId()));
      manager.open(alice, MenuPrivacySettings.DEFAULT);
      manager.close(alice);
      assertFalse(manager.blocksPlayerDamage(alice.getUniqueId()));
   }

   @Test void shutdownClearsAllOwnedVisibilityRequests() {
      Player alice = player(), bob = player();
      manager.open(alice, new MenuPrivacySettings(true, true, true, false)); drain();
      assertFalse(visible(bob, alice));
      manager.shutdown(); drain();
      assertTrue(visible(bob, alice));
      assertTrue(visible(alice, bob));
      assertFalse(manager.blocksPlayerDamage(alice.getUniqueId()));
   }
}
