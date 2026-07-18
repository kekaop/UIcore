package com.cmenu.ui.util;

import com.cmenu.ui.CursorMenuPlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class CommandUtils {
   private CommandUtils() {
   }

   public static void executeCommand(Player player, String cmd) {
      if (cmd != null && !cmd.trim().isEmpty()) {
         String processedCmd = cmd;
         if (CursorMenuPlugin.hasPAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            processedCmd = PlaceholderAPI.setPlaceholders(player, cmd);
         }

         processedCmd = replaceBuiltinPlaceholders(player, processedCmd);
         String lowerCmd = processedCmd.toLowerCase();
         if (lowerCmd.startsWith("[console]")) {
            String finalCmd = processedCmd.replaceAll("\\[console\\]", "").trim();
            if (!finalCmd.isEmpty()) {
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
         } else if (lowerCmd.startsWith("[op]")) {
            String finalCmd = processedCmd.replaceAll("\\[op\\]", "").trim();
            if (!finalCmd.isEmpty()) {
               executeWithTempOp(player, finalCmd);
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
               player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
         } else if (lowerCmd.startsWith("[actionbar]")) {
            String message = processedCmd.replaceAll("\\[actionbar\\]", "").trim();
            if (!message.isEmpty()) {
               sendActionBar(player, ChatColor.translateAlternateColorCodes('&', message));
            }
         } else {
            String finalCmd = processedCmd.trim();
            if (!finalCmd.isEmpty()) {
               player.performCommand(finalCmd);
            }
         }
      }
   }

   private static String replaceBuiltinPlaceholders(Player player, String text) {
      if (text != null && player != null) {
         text = text.replace("%player%", player.getName());
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
      } else {
         return text;
      }
   }

   public static void executeWithTempOp(Player player, String command) {
      if (player != null && command != null && !command.trim().isEmpty()) {
         boolean wasOp = player.isOp();

         try {
            if (!wasOp) {
               player.setOp(true);
            }

            player.performCommand(command.trim());
         } catch (Exception var7) {
            CursorMenuPlugin.plugin.getLogger().warning("执行临时OP命令时出错: " + var7.getMessage());
         } finally {
            if (!wasOp) {
               player.setOp(false);
            }
         }
      }
   }

   public static void connectToServer(Player player, String serverName) {
      if (player != null && serverName != null && !serverName.trim().isEmpty()) {
         try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName.trim());
            player.sendPluginMessage(CursorMenuPlugin.plugin, "BungeeCord", b.toByteArray());
         } catch (Exception var4) {
            player.sendMessage(
               CursorMenuPlugin.plugin.getLangMessage("system.server_transfer_failed", "&c[CursorMenu] 跨服传送失败: %error%").replace("%error%", var4.getMessage())
            );
            CursorMenuPlugin.plugin.getLogger().warning("跨服传送失败: " + var4.getMessage());
         }
      }
   }

   public static void sendActionBar(Player player, String message) {
      if (player != null && message != null) {
         try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
         } catch (Exception var3) {
            CursorMenuPlugin.plugin.getLogger().warning("发送ActionBar消息失败: " + var3.getMessage());
         }
      }
   }
}
