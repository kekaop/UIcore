package org.orvencraft.uicore.session;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class MenuTeleportTrackerTest {
   @Test void externalTeleportDoesNotConsumeThePendingCameraTeleport() {
      var tracker = new MenuTeleportTracker();
      UUID player = UUID.randomUUID();
      Location camera = new Location(null, 12, 64, -8);
      tracker.begin(player, camera);
      assertFalse(tracker.consume(player, new Location(null, 30, 70, 15)));
      assertTrue(tracker.consume(player, camera));
      assertFalse(tracker.consume(player, camera), "The marker is consumed only once");
   }

   @Test void completionOfOldTeleportCannotClearTheNewRequest() {
      var tracker = new MenuTeleportTracker();
      UUID player = UUID.randomUUID();
      UUID old = tracker.begin(player, new Location(null, 1, 64, 1));
      Location next = new Location(null, 3, 64, 3);
      tracker.begin(player, next);
      tracker.complete(player, old);
      assertTrue(tracker.consume(player, next));
   }

   @Test void failedTeleportClearsItsMarkerAndCopiesTheDestination() {
      var tracker = new MenuTeleportTracker();
      UUID player = UUID.randomUUID();
      Location target = new Location(null, 1, 64, 1);
      UUID token = tracker.begin(player, target);
      target.setX(99);
      assertFalse(tracker.consume(player, target));
      tracker.complete(player, token);
      assertFalse(tracker.consume(player, new Location(null, 1, 64, 1)));
   }
}
