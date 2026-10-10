package org.orvencraft.uicore.session;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

/** Identifies the expected teleport without consuming unrelated player events. */
public final class MenuTeleportTracker {
   private record Pending(UUID token, Location destination) {}
   private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

   public UUID begin(UUID player, Location destination) {
      UUID token = UUID.randomUUID();
      pending.put(player, new Pending(token, destination.clone()));
      return token;
   }

   public boolean consume(UUID player, Location destination) {
      Pending expected = pending.get(player);
      if (expected == null || destination == null) return false;
      Location target = expected.destination();
      if (!Objects.equals(target.getWorld(), destination.getWorld())
         || Math.abs(target.getX() - destination.getX()) > 1.0E-6
         || Math.abs(target.getY() - destination.getY()) > 1.0E-6
         || Math.abs(target.getZ() - destination.getZ()) > 1.0E-6) return false;
      return pending.remove(player, expected);
   }

   public void complete(UUID player, UUID token) {
      pending.computeIfPresent(player, (id, expected) -> expected.token().equals(token) ? null : expected);
   }
}
