package org.orvencraft.uicore.cutscene;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.cutscene.CutsceneStartResult;
import org.orvencraft.uicore.cutscene.config.CutsceneConfigLoader;

/** Administration only: file operations never run inside the cutscene update loop. */
public final class CutsceneCommands {
   private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,80}");
   private static final Object POINT_WRITE_LOCK = new Object();
   private final UIcorePlugin plugin;

   public CutsceneCommands(UIcorePlugin plugin) {
      this.plugin = plugin;
   }

   /** Receives the complete command arguments, including {@code cutscene} at index zero. */
   public boolean execute(CommandSender sender, String[] args) {
      String operation = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "help";
      boolean editing = operation.equals("point") || operation.equals("validate") || operation.equals("reload");
      if (!sender.hasPermission(editing ? "uicore.cutscene.edit" : "uicore.cutscene.preview")) {
         sender.sendMessage("§c[UIcore] Missing permission: " + (editing ? "uicore.cutscene.edit" : "uicore.cutscene.preview"));
         return true;
      }
      switch (operation) {
         case "point" -> point(sender, args);
         case "validate" -> validate(sender);
         case "reload" -> sender.sendMessage(plugin.reloadCutscenes()
            ? "§a[UIcore] Cutscenes reloaded. Existing playback keeps its configuration snapshot."
            : "§c[UIcore] Cutscenes were not reloaded. Check the server log for the configuration error.");
         case "preview", "start" -> {
            if (args.length < 3) { usage(sender); break; }
            withPlayer(sender, args, 3, player -> {
               CutsceneStartResult result = plugin.getCutsceneManager().start(player, args[2]);
               sender.sendMessage((result == CutsceneStartResult.ACCEPTED ? "§a" : "§c")
                  + "[UIcore] Cutscene start for " + player.getName() + ": " + result
                  + (result == CutsceneStartResult.ACCEPTED ? " (preparing; playback has not started yet)" : ""));
            });
         }
         case "stop", "pause", "resume", "skip" -> withPlayer(sender, args, 2, player -> {
            boolean changed = switch (operation) {
               case "pause" -> plugin.getCutsceneManager().pause(player);
               case "resume" -> plugin.getCutsceneManager().resume(player);
               case "skip" -> plugin.getCutsceneManager().skip(player);
               default -> plugin.getCutsceneManager().stop(player);
            };
            sender.sendMessage((changed ? "§a" : "§e") + "[UIcore] " + operation + " for " + player.getName()
               + (changed ? " accepted." : " unavailable in the current state."));
         });
         case "status" -> withPlayer(sender, args, 2, player -> {
            var snapshot = plugin.getCutsceneManager().state(player);
            if (snapshot.isEmpty()) sender.sendMessage("§e[UIcore] No active cutscene for " + player.getName() + ".");
            else {
               var state = snapshot.get();
               sender.sendMessage("§e[UIcore] " + player.getName() + ": " + state.id() + ", " + state.state()
                  + ", step=" + state.stepId() + ", ticks=" + state.elapsedTicks()
                  + ", progress=" + String.format(Locale.ROOT, "%.3f", state.stepProgress()));
            }
         });
         default -> usage(sender);
      }
      return true;
   }

   private void validate(CommandSender sender) {
      try {
         var definitions = plugin.validateCutscenes();
         sender.sendMessage("§a[UIcore] Valid cutscene configuration: " + definitions.size() + " scenario(s). Nothing reloaded.");
      } catch (RuntimeException exception) {
         sender.sendMessage("§c[UIcore] " + exception.getMessage());
      }
   }

   private void point(CommandSender sender, String[] args) {
      if (args.length < 4) { usage(sender); return; }
      String operation = args[2].toLowerCase(Locale.ROOT);
      String sceneId = args[3];
      try {
         Path directory = plugin.getDataFolder().toPath().resolve("cutscenes");
         if (operation.equals("list")) {
            Path file = scenarioFile(directory, sceneId);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
               sender.sendMessage("§e[UIcore] Scenario file does not exist: " + sceneId + ".yml");
               return;
            }
            YamlConfiguration config = loadFile(file);
            var points = config.getConfigurationSection("points");
            if (points == null || points.getKeys(false).isEmpty()) {
               sender.sendMessage("§e[UIcore] No points in " + sceneId + ".");
               return;
            }
            for (String id : points.getKeys(false)) {
               sender.sendMessage("§e[UIcore] " + id + ": " + points.getString(id + ".world", "?")
                  + " " + points.getDouble(id + ".x") + ", " + points.getDouble(id + ".y") + ", " + points.getDouble(id + ".z")
                  + " yaw=" + points.getDouble(id + ".yaw") + " pitch=" + points.getDouble(id + ".pitch"));
            }
         } else if (operation.equals("save") && args.length >= 5) {
            if (!(sender instanceof Player player)) {
               sender.sendMessage("§c[UIcore] Saving a camera point requires a player.");
               return;
            }
            if (!Bukkit.isOwnedByCurrentRegion(player)) {
               player.getScheduler().execute(plugin, () -> point(sender, args), null, 0L);
               return;
            }
            Location eye = player.getEyeLocation();
            savePointFile(directory, sceneId, args[4], eye.getWorld().getName(),
               eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch());
            sender.sendMessage("§a[UIcore] Saved camera eye point " + sceneId + "." + args[4]
               + ". Use /uicore cutscene validate, then /uicore cutscene reload when the scenario is ready.");
         } else usage(sender);
      } catch (IOException | InvalidConfigurationException | IllegalArgumentException exception) {
         sender.sendMessage("§c[UIcore] Could not access camera points: " + exception.getMessage());
      }
   }

   private void withPlayer(CommandSender sender, String[] args, int playerArgument, Consumer<Player> action) {
      Player player = args.length > playerArgument ? Bukkit.getPlayerExact(args[playerArgument])
         : sender instanceof Player self ? self : null;
      if (player == null || !player.isOnline()) {
         sender.sendMessage("§c[UIcore] Specify an online player when using the console.");
         return;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         player.getScheduler().execute(plugin, () -> { if (player.isOnline()) action.accept(player); }, null, 0L);
      } else action.accept(player);
   }

   private static void usage(CommandSender sender) {
      sender.sendMessage("§e/uicore cutscene point save <scene> <point> | point list <scene>");
      sender.sendMessage("§e/uicore cutscene preview|start <scene> [player]");
      sender.sendMessage("§e/uicore cutscene stop|pause|resume|skip|status [player]");
      sender.sendMessage("§e/uicore cutscene validate | reload");
   }

   public List<String> tabComplete(CommandSender sender, String[] args) {
      List<String> choices = new ArrayList<>();
      boolean edit = sender.hasPermission("uicore.cutscene.edit");
      boolean preview = sender.hasPermission("uicore.cutscene.preview");
      if (args.length == 2) {
         if (edit) choices.addAll(List.of("point", "validate", "reload"));
         if (preview) choices.addAll(List.of("preview", "start", "stop", "pause", "resume", "skip", "status"));
      } else if (args.length == 3) {
         if (edit && args[1].equalsIgnoreCase("point")) choices.addAll(List.of("save", "list"));
         if (preview && List.of("preview", "start").contains(args[1].toLowerCase(Locale.ROOT)))
            choices.addAll(plugin.getCutsceneManager().ids());
         if (preview && List.of("stop", "pause", "resume", "skip", "status").contains(args[1].toLowerCase(Locale.ROOT)))
            Bukkit.getOnlinePlayers().forEach(player -> choices.add(player.getName()));
      } else if (args.length == 4) {
         if (edit && args[1].equalsIgnoreCase("point")) choices.addAll(plugin.getCutsceneManager().ids());
         if (preview && List.of("preview", "start").contains(args[1].toLowerCase(Locale.ROOT)))
            Bukkit.getOnlinePlayers().forEach(player -> choices.add(player.getName()));
      }
      return args.length == 0 ? List.of() : StringUtil.copyPartialMatches(args[args.length - 1], choices, new ArrayList<>());
   }

   static Path scenarioFile(Path directory, String sceneId) {
      requireId(sceneId);
      Path root = directory.toAbsolutePath().normalize();
      Path target = root.resolve(sceneId + ".yml").normalize();
      if (!target.getParent().equals(root)) throw new IllegalArgumentException("Scenario path must stay in cutscenes/");
      if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Scenario file must not be a symbolic link");
      return target;
   }

   static void savePointFile(Path directory, String sceneId, String pointId, String world,
      double x, double y, double z, float yaw, float pitch) throws IOException, InvalidConfigurationException {
      requireId(pointId);
      if (world == null || world.isBlank()) throw new IllegalArgumentException("Camera point world is required");
      if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
         || !Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90 || pitch > 90)
         throw new IllegalArgumentException("Camera point coordinates and angles must be finite; pitch must be within -90..90");
      synchronized (POINT_WRITE_LOCK) {
         Path file = scenarioFile(directory, sceneId);
         Files.createDirectories(file.getParent());
         YamlConfiguration config = Files.exists(file) ? loadFile(file) : new YamlConfiguration();
         String declaredId = config.getString("id", sceneId);
         if (!sceneId.equals(declaredId)) throw new IllegalArgumentException("File declares another scenario ID: " + declaredId);
         config.set("id", sceneId);
         String base = "points." + pointId + ".";
         config.set(base + "world", world);
         config.set(base + "x", x);
         config.set(base + "y", y);
         config.set(base + "z", z);
         config.set(base + "yaw", yaw);
         config.set(base + "pitch", pitch);
         Path temporary = Files.createTempFile(file.getParent(), "." + sceneId + "-", ".tmp");
         try {
            Files.writeString(temporary, config.saveToString(), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } finally { Files.deleteIfExists(temporary); }
      }
   }

   private static YamlConfiguration loadFile(Path file) throws IOException, InvalidConfigurationException {
      return CutsceneConfigLoader.loadFile(file.toFile());
   }

   private static void requireId(String id) {
      if (id == null || !SAFE_ID.matcher(id).matches())
         throw new IllegalArgumentException("IDs must contain 1-80 letters, numbers, underscores, or hyphens");
   }
}
