package org.orvencraft.uicore.scheduler;

import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.orvencraft.uicore.UIcorePlugin;

/** Cleanup may run inline on its owner, but must never schedule work for a disabled plugin. */
public final class CleanupTasks {
   private CleanupTasks() { }

   public static boolean entity(Plugin plugin, Entity entity, Runnable cleanup) {
      return entity(plugin, entity, cleanup, null);
   }

   public static boolean entity(Plugin plugin, Entity entity, Runnable cleanup, Runnable retired) {
      return dispatch(plugin::isEnabled, () -> Bukkit.isOwnedByCurrentRegion(entity), cleanup,
         () -> UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(entity).run(cleanup, retired) != null);
   }

   public static boolean region(Plugin plugin, Location location, Runnable cleanup) {
      return dispatch(plugin::isEnabled, () -> Bukkit.isOwnedByCurrentRegion(location), cleanup,
         () -> UIcorePlugin.foliaLib.scheduling().regionSpecificScheduler(location).run(cleanup) != null);
   }

   static boolean dispatch(BooleanSupplier enabled, BooleanSupplier owned, Runnable cleanup, BooleanSupplier schedule) {
      if (owned.getAsBoolean()) {
         cleanup.run();
         return true;
      }
      if (!enabled.getAsBoolean()) return false;
      try {
         return schedule.getAsBoolean();
      } catch (IllegalPluginAccessException exception) {
         // Disable can race the ownership check on a different Folia region.
         if (enabled.getAsBoolean()) throw exception;
         return false;
      }
   }
}
