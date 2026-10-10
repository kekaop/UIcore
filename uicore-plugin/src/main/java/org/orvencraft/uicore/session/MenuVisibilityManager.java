package org.orvencraft.uicore.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.orvencraft.uicore.section.MenuPrivacySettings;

/** Combines both sides' visibility rules; Bukkit calls run on the viewer's scheduler. */
public final class MenuVisibilityManager {
   private final Plugin plugin;
   private final Supplier<? extends Collection<? extends Player>> players;
   private final BiConsumer<Player, Runnable> dispatch;
   private final Map<UUID, MenuPrivacySettings> active = new ConcurrentHashMap<>();
   private final Map<UUID, Set<UUID>> hidden = new ConcurrentHashMap<>();

   public MenuVisibilityManager(Plugin plugin, Supplier<? extends Collection<? extends Player>> players,
                                BiConsumer<Player, Runnable> dispatch) {
      this.plugin = plugin;
      this.players = players;
      this.dispatch = dispatch;
   }

   public void open(Player player, MenuPrivacySettings settings) {
      active.put(player.getUniqueId(), settings);
      refresh();
   }

   public void close(Player player) {
      if (active.remove(player.getUniqueId()) != null) refresh();
   }

   public MenuPrivacySettings settings(UUID player) {
      return active.getOrDefault(player, MenuPrivacySettings.DEFAULT);
   }

   public boolean blocksPlayerDamage(UUID victim) {
      MenuPrivacySettings settings = active.get(victim);
      return settings != null && !settings.canDamaged();
   }

   public void joined(Player player) {
      // A reconnect is a new client with no previous hide requests.
      hidden.remove(player.getUniqueId());
      refresh();
   }

   public void quit(Player player) {
      active.remove(player.getUniqueId());
      hidden.remove(player.getUniqueId());
      // Forget the old client on every viewer before its UUID can reconnect.
      hidden.values().forEach(targets -> targets.remove(player.getUniqueId()));
      refresh();
   }

   public void shutdown() {
      active.clear();
      refresh();
   }

   public void refresh() {
      for (Player viewer : new ArrayList<>(players.get())) {
         dispatch.accept(viewer, () -> reconcile(viewer));
      }
   }

   private void reconcile(Player viewer) {
      if (!viewer.isOnline()) return;
      UUID viewerId = viewer.getUniqueId();
      Set<UUID> requests = hidden.computeIfAbsent(viewerId, ignored -> ConcurrentHashMap.newKeySet());
      for (Player target : new ArrayList<>(players.get())) {
         UUID targetId = target.getUniqueId();
         if (viewerId.equals(targetId) || !target.isOnline()) continue;
         // A private-by-default player must not receive an explicit show request on close.
         // Bukkit reverses hide/show semantics for entities with this flag.
         if (!target.isVisibleByDefault()) {
            requests.remove(targetId);
            continue;
         }
         MenuPrivacySettings viewerSettings = active.get(viewerId);
         MenuPrivacySettings targetSettings = active.get(targetId);
         boolean hide = (viewerSettings != null && viewerSettings.hideOtherPlayers())
            || (targetSettings != null && targetSettings.hidePlayerForOtherPlayer());
         if (hide && (!requests.contains(targetId) || viewer.canSee(target))) {
            viewer.hidePlayer(plugin, target);
            requests.add(targetId);
         } else if (!hide && requests.contains(targetId)) {
            viewer.showPlayer(plugin, target);
            requests.remove(targetId);
         }
      }
   }
}
