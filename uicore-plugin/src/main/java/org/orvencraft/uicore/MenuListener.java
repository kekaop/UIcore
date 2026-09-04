package org.orvencraft.uicore;

import org.orvencraft.uicore.api.event.MenuInputEvent;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class MenuListener implements Listener {
   private final UIcorePlugin plugin;

   public MenuListener(UIcorePlugin plugin) {
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
         if (UIcorePlugin.playerSit.containsKey(player)) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onCommandCancel(PlayerCommandPreprocessEvent event) {
      Player player = event.getPlayer();
      if (UIcorePlugin.playerSit.containsKey(player)) {
         String message = event.getMessage().toLowerCase().trim();
         String command = message.startsWith("/") ? message.substring(1) : message;
         if (!command.startsWith("uicore")) {
            for (String allowed : UIcorePlugin.allowedCommands) {
               if (command.startsWith(allowed)) {
                  return;
               }
            }

            String prefix = this.plugin.getConfig().getString("messages.prefix", "&c[UIcore] ");
            String blocked = this.plugin.getConfig().getString("messages.command_blocked", "&7This command is unavailable while a menu is active.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + blocked));
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      String currentMenu = this.plugin.getCurrentPlayerMenu(player);
      String inputField = this.plugin.getCurrentPlayerInputField(player);
      if (currentMenu != null && inputField != null && !inputField.isEmpty()) {
         event.setCancelled(true);
         String inputValue = event.getMessage();
         MenuInputEvent inputEvent = new MenuInputEvent(player, currentMenu, inputField, inputValue);
         Bukkit.getPluginManager().callEvent(inputEvent);
         if (inputEvent.isCancelled()) {
            return;
         }

         inputValue = inputEvent.getValue();
         this.plugin.setPlayerInputData(player, inputField, inputValue);
         Commands.getUserInputData().computeIfAbsent(player.getUniqueId(), var0 -> new java.util.concurrent.ConcurrentHashMap<>()).put(inputField, inputValue);
         this.plugin.setCurrentPlayerInputField(player, "");
         String shownValue = this.plugin.isPasswordInputField(inputField) ? "*****" : inputValue;
         player.sendMessage(ChatColor.GREEN + "[UIcore] " + inputField + " saved: " + shownValue);
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> this.plugin.setupCursor(player, currentMenu), null);
      }
   }

   @EventHandler
   public void onAdminJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (player.isOp()) {
         player.sendMessage(
            ChatColor.translateAlternateColorCodes(
               '&',
               "&a[UIcore] &fThis is an independently maintained custom build.\n&fPlugin version: &e"
                  + this.plugin.getDescription().getVersion()
                  + "\n&fAuthors: &e"
                  + this.plugin.getDescription().getAuthors()
            )
         );
      }
   }
}
