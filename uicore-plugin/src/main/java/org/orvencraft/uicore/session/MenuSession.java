package org.orvencraft.uicore.session;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

/** Owns the state borrowed from a player for the entire menu visit, including switches. */
public final class MenuSession {
   public enum Stage { PREPARING, ACTIVE, CLOSING, CLOSED }
   private final GameMode gameMode;
   private final boolean invisible;
   private final boolean collidable;
   private final Location location;
   private Stage stage = Stage.PREPARING;

   public MenuSession(Player player) {
      gameMode = player.getGameMode();
      invisible = player.isInvisible();
      collidable = player.isCollidable();
      location = player.getLocation().clone();
   }

   private MenuSession(GameMode mode, boolean invisible, boolean collidable, Location location) {
      this.gameMode = mode;
      this.invisible = invisible;
      this.collidable = collidable;
      this.location = location;
   }

   /** Must complete before applying any menu side effects. */
   public void save(Path destination) throws IOException {
      if (location.getWorld() == null) throw new IOException("Session origin world is unavailable");
      YamlConfiguration data = new YamlConfiguration();
      data.set("gamemode", gameMode.name());
      data.set("invisible", invisible);
      data.set("collidable", collidable);
      data.set("world", location.getWorld().getName());
      data.set("x", location.getX()); data.set("y", location.getY()); data.set("z", location.getZ());
      data.set("yaw", location.getYaw()); data.set("pitch", location.getPitch());
      Files.createDirectories(destination.getParent());
      Path temporary = Files.createTempFile(destination.getParent(), "session-", ".tmp");
      try {
         data.save(temporary.toFile());
         try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
         catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally { Files.deleteIfExists(temporary); }
   }

   public static MenuSession load(Path source, Function<String, World> worlds) throws IOException {
      YamlConfiguration data = new YamlConfiguration();
      try { data.load(source.toFile()); }
      catch (org.bukkit.configuration.InvalidConfigurationException exception) { throw new IOException("Invalid session journal", exception); }
      try {
         if (!data.isString("world") || !data.isBoolean("invisible") || !data.isBoolean("collidable")) throw new IllegalArgumentException("Missing session fields");
         for (String coordinate : new String[]{"x", "y", "z", "yaw", "pitch"})
            if (!(data.get(coordinate) instanceof Number) || !Double.isFinite(data.getDouble(coordinate))) throw new IllegalArgumentException("Invalid " + coordinate);
         if (Math.abs(data.getDouble("yaw")) > Float.MAX_VALUE || Math.abs(data.getDouble("pitch")) > 90D)
            throw new IllegalArgumentException("Invalid camera orientation");
         Location location = new Location(worlds.apply(data.getString("world")), data.getDouble("x"), data.getDouble("y"), data.getDouble("z"),
            (float)data.getDouble("yaw"), (float)data.getDouble("pitch"));
         return new MenuSession(GameMode.valueOf(data.getString("gamemode", "")), data.getBoolean("invisible"), data.getBoolean("collidable"), location);
      } catch (IllegalArgumentException exception) { throw new IOException("Invalid session journal fields", exception); }
   }

   public void prepare() { if (stage == Stage.ACTIVE) stage = Stage.PREPARING; }
   public void activate() { if (stage == Stage.PREPARING) stage = Stage.ACTIVE; }
   public boolean beginClose() {
      if (stage == Stage.CLOSING || stage == Stage.CLOSED) return false;
      stage = Stage.CLOSING;
      return true;
   }
   public void restore(Player player) {
      RuntimeException failure = null;
      try { player.setGameMode(gameMode); } catch (RuntimeException exception) { failure = exception; }
      try { player.setInvisible(invisible); } catch (RuntimeException exception) { if (failure == null) failure = exception; }
      try { player.setCollidable(collidable); } catch (RuntimeException exception) { if (failure == null) failure = exception; }
      if (failure != null) throw failure;
      if (player.getGameMode() != gameMode || player.isInvisible() != invisible || player.isCollidable() != collidable)
         throw new IllegalStateException("Player state restoration rejected");
      stage = Stage.CLOSED;
   }
   public Location originalLocation() { return location.clone(); }
   public Stage stage() { return stage; }
}
