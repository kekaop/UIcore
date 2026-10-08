package org.orvencraft.uicore.session;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class MenuSessionTest {
   @org.junit.jupiter.api.io.TempDir java.nio.file.Path temp;

   @Test void journalRetainsMissingWorldAndRejectsInvalidAngles() throws Exception {
      java.nio.file.Path file = temp.resolve("session.yml");
      String yaml = "gamemode: CREATIVE\ninvisible: true\ncollidable: false\nworld: missing\nx: 1\ny: 2\nz: 3\nyaw: 40\npitch: 10\n";
      java.nio.file.Files.writeString(file, yaml);
      MenuSession saved = MenuSession.load(file, name -> null);
      assertNull(saved.originalLocation().getWorld());
      assertEquals(1, saved.originalLocation().getX());
      assertTrue(java.nio.file.Files.exists(file));
      java.nio.file.Files.writeString(file, yaml.replace("yaw: 40", "yaw: 1.0e300"));
      assertThrows(java.io.IOException.class, () -> MenuSession.load(file, name -> null));
      java.nio.file.Files.writeString(file, yaml.replace("pitch: 10", "pitch: 91"));
      assertThrows(java.io.IOException.class, () -> MenuSession.load(file, name -> null));
      java.nio.file.Files.writeString(file, yaml.replace("invisible: true", "invisible: yes-please"));
      assertThrows(java.io.IOException.class, () -> MenuSession.load(file, name -> null));
   }

   @Test void originJournalRoundTripsAndLeavesNoTemporaryFiles() throws Exception {
      org.bukkit.World world = (org.bukkit.World)Proxy.newProxyInstance(org.bukkit.World.class.getClassLoader(), new Class<?>[]{org.bukkit.World.class},
         (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "origin";
            case "toString" -> "origin";
            case "equals" -> proxy == args[0];
            case "hashCode" -> 1;
            default -> throw new UnsupportedOperationException(method.getName());
         });
      Player player = (Player)Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
         (proxy, method, args) -> switch (method.getName()) {
            case "getGameMode" -> GameMode.SPECTATOR;
            case "isInvisible" -> true;
            case "isCollidable" -> false;
            case "getLocation" -> new Location(world, -18.25, 70, 32.75, 40, 10);
            default -> throw new UnsupportedOperationException(method.getName());
         });
      java.nio.file.Path journal = temp.resolve("sessions/player.yml");
      new MenuSession(player).save(journal);
      MenuSession loaded = MenuSession.load(journal, name -> name.equals("origin") ? world : null);
      assertEquals(-18.25, loaded.originalLocation().getX());
      assertEquals(40, loaded.originalLocation().getYaw());
      assertSame(world, loaded.originalLocation().getWorld());
      try (var files = java.nio.file.Files.list(journal.getParent())) { assertEquals(1, files.count()); }
   }

   @Test void restoresExactBorrowedStateForEveryMode() {
      for (GameMode mode : GameMode.values()) {
         AtomicReference<GameMode> current = new AtomicReference<>(mode);
         boolean[] properties = {true, false};
         Location origin = new Location(null, 1, 2, 3, 40, 10);
         Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
               case "getGameMode" -> current.get();
               case "setGameMode" -> { current.set((GameMode) args[0]); yield null; }
               case "isInvisible" -> properties[0];
               case "isCollidable" -> properties[1];
               case "setInvisible" -> { properties[0] = (boolean) args[0]; yield null; }
               case "setCollidable" -> { properties[1] = (boolean) args[0]; yield null; }
               case "getLocation" -> origin;
               default -> throw new UnsupportedOperationException(method.getName());
            };
         });
         MenuSession session = new MenuSession(player);
         origin.setX(99);
         assertEquals(1, session.originalLocation().getX());
         session.originalLocation().setY(99);
         assertEquals(2, session.originalLocation().getY());
         session.activate();
         session.prepare();
         assertEquals(MenuSession.Stage.PREPARING, session.stage());
         session.activate();
         current.set(GameMode.ADVENTURE); properties[0] = false; properties[1] = true;
         assertTrue(session.beginClose());
         assertFalse(session.beginClose());
         session.restore(player);
         assertEquals(mode, current.get());
         assertTrue(properties[0]); assertFalse(properties[1]);
         assertEquals(MenuSession.Stage.CLOSED, session.stage());
         assertFalse(session.beginClose());
      }
   }
}
