package com.cmenu.ui.util;

import com.cmenu.ui.ColorParser;
import com.cmenu.ui.CursorMenuPlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class CommandUtils {
   private CommandUtils() {
   }

   public static void executeCommand(Player player, String cmd) {
      if (cmd == null || cmd.trim().isEmpty()) {
         return;
      }

      String processedCmd = cmd;
      if (CursorMenuPlugin.plugin != null) {
         processedCmd = CursorMenuPlugin.plugin.applyInputPlaceholders(player, processedCmd, false);
      }

      if (CursorMenuPlugin.hasPAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         processedCmd = PlaceholderAPI.setPlaceholders(player, processedCmd);
      }

      processedCmd = replaceBuiltinPlaceholders(player, processedCmd);
      String lowerCmd = processedCmd.toLowerCase();
      if (lowerCmd.startsWith("[console]")) {
         String finalCmd = processedCmd.replaceAll("\\[console\\]", "").trim();
         if (!finalCmd.isEmpty() && isConsoleCommandAllowed(player, finalCmd)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
         }
      } else if (lowerCmd.startsWith("[op]")) {
         String finalCmd = processedCmd.replaceAll("\\[op\\]", "").trim();
         if (!finalCmd.isEmpty() && isOpCommandAllowed(player, finalCmd)) {
            player.performCommand(finalCmd);
         }
      } else if (lowerCmd.startsWith("[player]")) {
         String finalCmd = processedCmd.replaceAll("\\[player\\]", "").trim();
         if (!finalCmd.isEmpty()) {
            player.performCommand(finalCmd);
         }
      } else if (lowerCmd.startsWith("[server]")) {
         String serverName = processedCmd.replaceAll("\\[server\\]", "").trim();
         if (!serverName.isEmpty()) {
            connectToServer(player, serverName);
         }
      } else if (lowerCmd.startsWith("[message]")) {
         String message = processedCmd.replaceAll("\\[message\\]", "").trim();
         if (!message.isEmpty()) {
            sendChatMessage(player, message);
         }
      } else if (lowerCmd.startsWith("[actionbar]")) {
         String message = processedCmd.replaceAll("\\[actionbar\\]", "").trim();
         if (!message.isEmpty()) {
            sendActionBar(player, message);
         }
      } else {
         String finalCmd = processedCmd.trim();
         if (!finalCmd.isEmpty()) {
            player.performCommand(finalCmd);
         }
      }
   }

   private static boolean isConsoleCommandAllowed(Player player, String command) {
      if (CursorMenuPlugin.plugin == null || !CursorMenuPlugin.plugin.getConfig().getBoolean("security.allow-console-commands", false)) {
         denyCommand(player, "console", command);
         return false;
      }

      List<String> allowlist = CursorMenuPlugin.plugin.getConfig().getStringList("security.console-command-allowlist");
      if (allowlist.isEmpty()) {
         return true;
      }

      String normalized = command.toLowerCase().trim();
      for (String allowed : allowlist) {
         if (allowed != null && !allowed.isBlank() && normalized.startsWith(allowed.toLowerCase().trim())) {
            return true;
         }
      }

      denyCommand(player, "console-not-allowlisted", command);
      return false;
   }

   private static boolean isOpCommandAllowed(Player player, String command) {
      boolean allowForExistingOps = CursorMenuPlugin.plugin != null
         && CursorMenuPlugin.plugin.getConfig().getBoolean("security.allow-op-commands-for-existing-ops", false);
      if (allowForExistingOps && player != null && player.isOp()) {
         return true;
      }

      denyCommand(player, "op", command);
      return false;
   }

   private static void denyCommand(Player player, String reason, String command) {
      if (CursorMenuPlugin.plugin != null && CursorMenuPlugin.plugin.getConfig().getBoolean("security.debug-denied-commands", false)) {
         String playerName = player == null ? "<no-player>" : player.getName();
         CursorMenuPlugin.plugin
            .getLogger()
            .warning("[CommandPolicy] Denied " + reason + " command for " + playerName + ": " + sanitizeForLog(command));
      }
   }

   private static String sanitizeForLog(String command) {
      if (command == null) {
         return "";
      }
      return command.replaceAll("(?i)(password|pass|pwd)\\s+\\S+", "$1 <redacted>");
   }

   private static String replaceBuiltinPlaceholders(Player player, String text) {
      if (text == null || player == null) {
         return text;
      }

      text = text.replace("%player%", player.getName());
      if (CursorMenuPlugin.plugin != null) {
         text = CursorMenuPlugin.plugin.applyInputPlaceholders(player, text, false);
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
            player.sendPluginMessage(CursorMenuPlugin.plugin, "BungeeCord", b.toByteArray());
         } catch (Exception e) {
            player.sendMessage(
               CursorMenuPlugin.plugin.getLangMessage("system.server_transfer_failed", "&c[CursorMenu] Server transfer failed: %error%")
                  .replace("%error%", e.getMessage())
            );
            CursorMenuPlugin.plugin.getLogger().warning("Server transfer failed: " + e.getMessage());
         }
      }
   }

   public static void sendActionBar(Player player, String message) {
      if (player != null && message != null) {
         try {
            player.sendActionBar(ColorParser.parse(message));
         } catch (Exception e) {
            CursorMenuPlugin.plugin.getLogger().warning("Failed to send actionbar message: " + e.getMessage());
         }
      }
   }

   public static void sendChatMessage(Player player, String message) {
      if (player != null && message != null) {
         try {
            player.sendMessage(ColorParser.parse(message));
         } catch (RuntimeException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
         }
      }
   }
}
