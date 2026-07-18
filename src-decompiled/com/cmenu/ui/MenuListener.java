package com.cmenu.ui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class MenuListener implements Listener {
   private final CursorMenuPlugin plugin;

   public MenuListener(CursorMenuPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.plugin.stopCursor(event.getPlayer(), true);
   }

   @EventHandler
   public void onVehicleLeave(VehicleExitEvent event) {
      if (event.getExited() instanceof Player) {
         Player player = (Player)event.getExited();
         if (CursorMenuPlugin.playerSit.containsKey(player)) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onCommandCancel(PlayerCommandPreprocessEvent event) {
      Player player = event.getPlayer();
      if (CursorMenuPlugin.playerSit.containsKey(player)) {
         String message = event.getMessage().toLowerCase().trim();
         String command = message.startsWith("/") ? message.substring(1) : message;
         if (!command.startsWith("cmenu") && !command.startsWith("cursormenu")) {
            for (String allowed : CursorMenuPlugin.allowedCommands) {
               if (command.startsWith(allowed)) {
                  return;
               }
            }

            String prefix = this.plugin.getConfig().getString("messages.prefix", "&c[CursorMenu] ");
            String blocked = this.plugin.getConfig().getString("messages.command_blocked", "&7该命令在菜单模式下不可用。");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + blocked));
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      String currentMenu = this.plugin.getCurrentPlayerMenu(player);
      if (currentMenu != null && (currentMenu.equals("登录菜单") || currentMenu.equals("注册菜单"))) {
         event.setCancelled(true);
         String inputField = this.plugin.getCurrentPlayerInputField(player);
         if (inputField != null && !inputField.isEmpty()) {
            String inputValue = event.getMessage();
            this.plugin.setPlayerInputData(player, inputField, inputValue);
            player.sendMessage(ChatColor.GREEN + "[CursorMenu] " + inputField + " 已设置为: " + inputValue);
            CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> this.plugin.setupCursor(player, currentMenu), null);
         }
      }
   }

   @EventHandler
   public void onAdminJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (player.isOp()) {
         player.sendMessage(
            ChatColor.translateAlternateColorCodes(
               '&',
               "&a[CustomScreenMenu] &fThis version is open source and does not receive author support and assistance\n&f插件版本：&e"
                  + this.plugin.getDescription().getVersion()
                  + "\n&f作者：&e野比大雄\nQQ:3357153117\n&fDiscord: https://discord.gg/YpZNACup"
            )
         );
      }
   }
}
