package org.orvencraft.uicore.extension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class ExtensionManagerTest {
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
