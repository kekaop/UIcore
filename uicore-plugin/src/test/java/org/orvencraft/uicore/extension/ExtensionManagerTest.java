package org.orvencraft.uicore.extension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class ExtensionManagerTest {
   @Test
   void absentAndFailedDataHonorTheRefreshIntervalAndCanBeInvalidated() {
      var tick = new java.util.concurrent.atomic.AtomicLong(10);
      var calls = new java.util.concurrent.atomic.AtomicInteger();
      Plugin owner = (Plugin)Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
         (proxy, method, args) -> method.getName().equals("isEnabled") ? true : "ExampleExtension");
      org.bukkit.entity.Player player = (org.bukkit.entity.Player)Proxy.newProxyInstance(
         org.bukkit.entity.Player.class.getClassLoader(), new Class<?>[]{org.bukkit.entity.Player.class},
         (proxy, method, args) -> new java.util.UUID(0, 1));
      ExtensionManager manager = new ExtensionManager(null, p -> true, tick::get);
      manager.data().register(owner, "example:absent", 20, context -> { calls.incrementAndGet(); return null; });
      manager.data().register(owner, "example:broken", 20, context -> { calls.incrementAndGet(); throw new IllegalStateException(); });
      for (int i = 0; i < 100; i++) {
         assertTrue(manager.resolveData("example:absent", player, "menu", "label").isEmpty());
         assertTrue(manager.resolveData("example:broken", player, "menu", "label").isEmpty());
      }
      assertEquals(2, calls.get(), "Empty/error results must not be invoked on every render");
      tick.set(30);
      manager.resolveData("example:absent", player, "menu", "label");
      manager.resolveData("example:broken", player, "menu", "label");
      assertEquals(4, calls.get());
      manager.invalidateData(player);
      manager.resolveData("example:absent", player, "menu", "label");
      assertEquals(5, calls.get(), "Explicit refresh retries immediately");
   }
   @Test
   void registriesRejectConflictsAndReleaseDisabledOwners() {
      AtomicBoolean enabled = new AtomicBoolean(true);
      Plugin owner = (Plugin)Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
         (proxy, method, args) -> switch (method.getName()) {
            case "isEnabled" -> enabled.get();
            case "getName" -> "ExampleExtension";
            default -> null;
         });
      ExtensionManager manager = new ExtensionManager(null);
      var registration = manager.actions().register(owner, "example:hello", context -> {});
      assertTrue(manager.actions().isRegistered("example:hello"));
      assertThrows(IllegalArgumentException.class, () -> manager.actions().register(owner, "example:hello", context -> {}));
      assertThrows(IllegalArgumentException.class, () -> manager.data().register(owner, "hello", context -> "hello"));
      assertTrue(manager.data().register(owner, "example:balance", context -> 10) != null);
      assertTrue(manager.conditions().register(owner, "example:allowed", context -> true) != null);
      assertTrue(manager.components().register(owner, "example:card", context -> java.util.List.of()) != null);
      manager.removeOwner(owner);
      assertFalse(manager.actions().isRegistered("example:hello"));
      assertFalse(manager.data().isRegistered("example:balance"));
      assertFalse(manager.conditions().isRegistered("example:allowed"));
      assertFalse(manager.components().isRegistered("example:card"));
      manager.actions().register(owner, "example:hello", context -> {});
      enabled.set(false);
      assertFalse(manager.actions().isRegistered("example:hello"));
      assertFalse(manager.data().isRegistered("example:balance"));
      registration.close();
      assertFalse(manager.actions().isRegistered("example:hello"));
   }
}
