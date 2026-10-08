package org.orvencraft.uicore.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.junit.jupiter.api.Test;

class CleanupTasksTest {
   @Test void disabledPluginCleansOwnedResourcesWithoutScheduling() {
      AtomicInteger removed = new AtomicInteger();
      assertTrue(CleanupTasks.dispatch(() -> false, () -> true, removed::incrementAndGet,
         () -> { throw new IllegalPluginAccessException("Plugin attempted to register task while disabled"); }));
      assertEquals(1, removed.get());
   }

   @Test void disabledPluginNeverAccessesAnotherRegionOrSchedulesWork() {
      assertFalse(CleanupTasks.dispatch(() -> false, () -> false,
         () -> fail("Wrong region"), () -> fail("Disabled scheduler")));
   }

   @Test void enabledPluginDispatchesCleanupToItsOwner() {
      AtomicBoolean scheduled = new AtomicBoolean();
      assertTrue(CleanupTasks.dispatch(() -> true, () -> false,
         () -> fail("Must wait for owner"), () -> { scheduled.set(true); return true; }));
      assertTrue(scheduled.get());
   }

   @Test void concurrentDisableDoesNotAbortRemainingCleanup() {
      AtomicBoolean enabled = new AtomicBoolean(true);
      assertFalse(CleanupTasks.dispatch(enabled::get, () -> false, () -> fail("Wrong region"), () -> {
         enabled.set(false);
         throw new IllegalPluginAccessException("Disabled during dispatch");
      }));
   }

   @Test void retiredEntityReportsThatCleanupWasNotQueued() {
      assertFalse(CleanupTasks.dispatch(() -> true, () -> false,
         () -> fail("Entity already retired"), () -> false));
   }

   @Test void unexpectedSchedulerAndCleanupErrorsRemainVisible() {
      assertThrows(IllegalPluginAccessException.class, () -> CleanupTasks.dispatch(() -> true, () -> false,
         () -> {}, () -> { throw new IllegalPluginAccessException("Unexpected scheduler failure"); }));
      assertThrows(IllegalStateException.class, () -> CleanupTasks.dispatch(() -> false, () -> true,
         () -> { throw new IllegalStateException("Removal failed"); }, () -> fail("Disabled scheduler")));
   }
}
