package org.orvencraft.uicore.util;

import org.orvencraft.uicore.ColorParser;
import org.orvencraft.uicore.UIcorePlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class CommandUtils {
   private static final Pattern DELAY_PREFIX = Pattern.compile("^\\s*<delay\\s*=\\s*(\\d+)\\s*>\\s*", Pattern.CASE_INSENSITIVE);
   private static final Pattern ACTION_PREFIX = Pattern.compile("^\\s*\\[(console|op|player|server|message|actionbar)]\\s*", Pattern.CASE_INSENSITIVE);

   private CommandUtils() {
   }

   public static void executeCommand(Player player, String cmd) {
      executeCommand(player, cmd, 0L);
   }

   public static void executeCommand(Player player, String cmd, long fallbackDelayTicks) {
      if (player == null || cmd == null || cmd.trim().isEmpty()) {
         return;
      }

      /*
       * Commands may be requested by integrations from an async or a different
       * region thread. Player commands, placeholder expansion and plugin
       * messages belong to the player's entity scheduler.
       */
      if (UIcorePlugin.plugin != null && !Bukkit.isOwnedByCurrentRegion(player)) {
         boolean fromMenu = UIcorePlugin.plugin.getCurrentPlayerMenu(player) != null;
         long generation = UIcorePlugin.plugin.getMenuSessionGeneration(player);
         player.getScheduler().execute(UIcorePlugin.plugin, guardSession(
            () -> !fromMenu || UIcorePlugin.plugin.isMenuSessionCurrent(player, generation),
            () -> executeCommand(player, cmd, fallbackDelayTicks)), null, 0L);
         return;
      }

      DelayedCommand delayed = parseDelayedCommand(cmd, fallbackDelayTicks);
      if (delayed.delayTicks() > 0L && UIcorePlugin.plugin != null && UIcorePlugin.foliaLib != null) {
         boolean requireCurrentSession = UIcorePlugin.plugin.getCurrentPlayerMenu(player) != null;
         long sessionGeneration = requireCurrentSession ? UIcorePlugin.plugin.getMenuSessionGeneration(player) : 0L;
         UIcorePlugin.foliaLib
            .scheduling()
            .entitySpecificScheduler(player)
            .runDelayed(ignored -> {
               if (!requireCurrentSession || UIcorePlugin.plugin.isMenuSessionCurrent(player, sessionGeneration)) {
                  executeCommandNow(player, delayed.command());
               }
            }, null, delayed.delayTicks());
         return;
      }

      executeCommandNow(player, delayed.command());
   }

   static DelayedCommand parseDelayedCommand(String raw, long fallbackDelayTicks) {
      String command = raw == null ? "" : raw;
      Matcher matcher = DELAY_PREFIX.matcher(command);
      if (!matcher.find()) {
         return new DelayedCommand(command, Math.max(0L, fallbackDelayTicks));
      }

      long delay;
      try {
         delay = Long.parseLong(matcher.group(1));
      } catch (NumberFormatException ignored) {
         delay = 0L;
      }
      return new DelayedCommand(command.substring(matcher.end()), Math.max(0L, delay));
   }

   private static void executeCommandNow(Player player, String cmd) {
      if (cmd == null || cmd.trim().isEmpty()) {
         return;
      }

      boolean fromMenu = UIcorePlugin.plugin != null && UIcorePlugin.plugin.getCurrentPlayerMenu(player) != null;
      long generation = fromMenu ? UIcorePlugin.plugin.getMenuSessionGeneration(player) : -1L;
      CommandAction action = parseCommandAction(cmd);
      String processedArgument = action.argument();
      if (UIcorePlugin.plugin != null) {
         processedArgument = UIcorePlugin.plugin.applyInputPlaceholders(player, processedArgument, false);
      }

      if (UIcorePlugin.hasPAPI) {
         processedArgument = PlaceholderAPI.setPlaceholders(player, processedArgument);
      }

      processedArgument = replaceBuiltinPlaceholders(player, processedArgument);
      switch (action.type()) {
         case CONSOLE -> {
            String finalCmd = processedArgument.trim();
            if (!finalCmd.isEmpty() && isConsoleCommandAllowed(player, finalCmd) && UIcorePlugin.plugin != null) {
               // Command-map access is global state and must not run from an entity region.
               Bukkit.getGlobalRegionScheduler().execute(
                  UIcorePlugin.plugin,
                  guardSession(() -> !fromMenu || UIcorePlugin.plugin.isMenuSessionCurrent(player, generation),
                     () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd))
               );
            }
         }
         case OP -> {
            String finalCmd = processedArgument.trim();
         if (!finalCmd.isEmpty() && isOpCommandAllowed(player, finalCmd)) {
            player.performCommand(finalCmd);
         }
         }
         case PLAYER -> {
            String finalCmd = processedArgument.trim();
         if (!finalCmd.isEmpty()) {
            player.performCommand(finalCmd);
         }
         }
         case SERVER -> {
            String serverName = processedArgument.trim();
         if (!serverName.isEmpty()) {
            connectToServer(player, serverName);
         }
         }
         case MESSAGE -> {
            String message = processedArgument.trim();
         if (!message.isEmpty()) {
            sendChatMessage(player, replaceMessageBuiltinPlaceholders(player, action.argument()));
         }
         }
         case ACTIONBAR -> {
            String message = processedArgument.trim();
         if (!message.isEmpty()) {
            sendActionBar(player, replaceMessageBuiltinPlaceholders(player, action.argument()));
         }
         }
      }
   }

   /** Recheck at execution time after crossing an entity/global scheduler boundary. */
   static Runnable guardSession(java.util.function.BooleanSupplier current, Runnable action) {
      return () -> { if (current.getAsBoolean()) action.run(); };
   }

   private static String replaceMessageBuiltinPlaceholders(Player player, String template) {
      return replaceBuiltinPlaceholders(player, template, false);
   }

   static CommandAction parseCommandAction(String raw) {
      String command = raw == null ? "" : raw;
      Matcher matcher = ACTION_PREFIX.matcher(command);
      if (!matcher.find()) {
         return new CommandAction(ActionType.PLAYER, command);
      }
      ActionType type = switch (matcher.group(1).toLowerCase()) {
         case "console" -> ActionType.CONSOLE;
         case "op" -> ActionType.OP;
         case "server" -> ActionType.SERVER;
         case "message" -> ActionType.MESSAGE;
         case "actionbar" -> ActionType.ACTIONBAR;
         default -> ActionType.PLAYER;
      };
      return new CommandAction(type, command.substring(matcher.end()));
   }

   private static boolean isConsoleCommandAllowed(Player player, String command) {
      if (UIcorePlugin.plugin == null || !UIcorePlugin.plugin.getConfig().getBoolean("security.allow-console-commands", false)) {
         denyCommand(player, "console", command);
         return false;
      }

      List<String> allowlist = UIcorePlugin.plugin.getConfig().getStringList("security.console-command-allowlist");
      if (allowlist.isEmpty()) {
         return true;
      }

      for (String allowed : allowlist) {
         if (isAllowedCommand(command, allowed)) {
            return true;
         }
      }

      denyCommand(player, "console-not-allowlisted", command);
      return false;
   }

   static boolean isAllowedCommand(String command, String allowed) {
      if (command == null || allowed == null) {
         return false;
      }
      String normalizedCommand = normalizeCommand(command);
      String normalizedAllowed = normalizeCommand(allowed);
      if (normalizedCommand.isEmpty() || normalizedAllowed.isEmpty()) {
         return false;
      }
      int commandSeparator = normalizedCommand.indexOf(' ');
      int allowedSeparator = normalizedAllowed.indexOf(' ');
      String commandLabel = commandSeparator < 0 ? normalizedCommand : normalizedCommand.substring(0, commandSeparator);
      String allowedLabel = allowedSeparator < 0 ? normalizedAllowed : normalizedAllowed.substring(0, allowedSeparator);
      if (!commandLabel.equals(allowedLabel)) {
         return false;
      }
      return allowedSeparator < 0 || normalizedCommand.equals(normalizedAllowed) || normalizedCommand.startsWith(normalizedAllowed + " ");
   }

   private static String normalizeCommand(String command) {
      String normalized = command.trim().toLowerCase();
      while (normalized.startsWith("/")) {
         normalized = normalized.substring(1).trim();
      }
      return normalized.replaceAll("\\s+", " ");
   }

   private static boolean isOpCommandAllowed(Player player, String command) {
      boolean allowForExistingOps = UIcorePlugin.plugin != null
         && UIcorePlugin.plugin.getConfig().getBoolean("security.allow-op-commands-for-existing-ops", false);
      if (allowForExistingOps && player != null && player.isOp()) {
         return true;
      }

      denyCommand(player, "op", command);
      return false;
   }

   private static void denyCommand(Player player, String reason, String command) {
      if (UIcorePlugin.plugin != null && UIcorePlugin.plugin.getConfig().getBoolean("security.debug-denied-commands", false)) {
         String playerName = player == null ? "<no-player>" : player.getName();
         UIcorePlugin.plugin
            .getLogger()
            .warning("[CommandPolicy] Denied " + reason + " command for " + playerName + ": " + sanitizeForLog(command));
      }
   }

   static String sanitizeForLog(String command) {
      if (command == null) {
         return "";
      }
      return "<command arguments redacted>";
   }

   private static String replaceBuiltinPlaceholders(Player player, String text) {
      return replaceBuiltinPlaceholders(player, text, true);
   }

   static String replaceBuiltinPlaceholders(Player player, String text, boolean expandInput) {
      if (text == null || player == null) {
         return text;
      }

      text = text.replace("%player%", player.getName());
      if (expandInput && UIcorePlugin.plugin != null) {
         text = UIcorePlugin.plugin.applyInputPlaceholders(player, text, false);
      }

      text = text.replace("%player_name%", player.getName());
      text = text.replace("%player_uuid%", player.getUniqueId().toString());
      text = text.replace("%player_displayname%", player.getDisplayName());
      if (player.getLocation() != null) {
         text = text.replace("%player_x%", String.format("%.2f", player.getLocation().getX()));
         text = text.replace("%player_y%", String.format("%.2f", player.getLocation().getY()));
         text = text.replace("%player_z%", String.format("%.2f", player.getLocation().getZ()));
         text = text.replace("%player_world%", player.getWorld().getName());
      }

      text = text.replace("%player_health%", String.format("%.1f", player.getHealth()));
      text = text.replace("%player_max_health%", String.format("%.1f", player.getMaxHealth()));
      text = text.replace("%player_level%", String.valueOf(player.getLevel()));
      text = text.replace("%player_exp%", String.valueOf(player.getTotalExperience()));
      text = text.replace("%player_food%", String.valueOf(player.getFoodLevel()));
      text = text.replace("%player_gamemode%", String.valueOf(player.getGameMode().name()));
      text = text.replace("%player_is_op%", String.valueOf(player.isOp()));
      text = text.replace("%player_is_online%", String.valueOf(player.isOnline()));
      text = text.replace("%player_is_sneaking%", String.valueOf(player.isSneaking()));
      return text.replace("%player_is_flying%", String.valueOf(player.isFlying()));
   }

   public static void connectToServer(Player player, String serverName) {
      if (player != null && serverName != null && !serverName.trim().isEmpty()) {
         try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName.trim());
            player.sendPluginMessage(UIcorePlugin.plugin, "BungeeCord", b.toByteArray());
         } catch (Exception e) {
            player.sendMessage(
               UIcorePlugin.plugin.getLangMessage("system.server_transfer_failed", "&c[UIcore] Server transfer failed: %error%")
                  .replace("%error%", e.getMessage())
            );
            UIcorePlugin.plugin.getLogger().warning("Server transfer failed: " + e.getMessage());
         }
      }
   }

   public static void sendActionBar(Player player, String message) {
      if (player != null && message != null) {
         try {
            player.sendActionBar(UIcorePlugin.renderDisplayComponent(player, TextContentUtils.normalizeLineBreaks(message)));
         } catch (Exception e) {
            UIcorePlugin.plugin.getLogger().warning("Failed to send actionbar message: " + e.getMessage());
         }
      }
   }

   public static void sendChatMessage(Player player, String message) {
      if (player != null && message != null) {
         String normalized = TextContentUtils.normalizeLineBreaks(message);
         try {
            player.sendMessage(UIcorePlugin.renderDisplayComponent(player, normalized));
         } catch (RuntimeException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', normalized));
         }
      }
   }

   record DelayedCommand(String command, long delayTicks) {
   }

   record CommandAction(ActionType type, String argument) {
   }

   enum ActionType {
      CONSOLE,
      OP,
      PLAYER,
      SERVER,
      MESSAGE,
      ACTIONBAR
   }
}
