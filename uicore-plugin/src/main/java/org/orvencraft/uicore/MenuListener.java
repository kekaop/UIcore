package org.orvencraft.uicore;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
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
         if (!matchesCommandLabel(command, "uicore") && !matchesCommandLabel(command, "ui") && !matchesCommandLabel(command, "uicore:uicore") && !matchesCommandLabel(command, "uicore:ui")) {
            for (String allowed : UIcorePlugin.allowedCommands) {
               if (matchesCommandLabel(command, allowed)) {
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

   private static boolean matchesCommandLabel(String command, String allowed) {
      if (command == null || allowed == null) {
         return false;
      }
      String normalized = command.trim();
      String allowedLabel = allowed.trim();
      if (normalized.isEmpty() || allowedLabel.isEmpty()) {
         return false;
      }
      int commandEnd = normalized.indexOf(' ');
      int allowedEnd = allowedLabel.indexOf(' ');
      String commandLabel = commandEnd < 0 ? normalized : normalized.substring(0, commandEnd);
      String expectedLabel = allowedEnd < 0 ? allowedLabel : allowedLabel.substring(0, allowedEnd);
      return commandLabel.equalsIgnoreCase(expectedLabel)
         && (allowedEnd < 0 || normalized.equalsIgnoreCase(allowedLabel) || normalized.regionMatches(true, 0, allowedLabel + " ", 0, allowedLabel.length() + 1));
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      String inputField = this.plugin.getCurrentPlayerInputField(player);
      if (inputField == null || inputField.isBlank()) {
         return;
      }
      long sessionGeneration = this.plugin.getMenuSessionGeneration(player);
      String inputValue = event.getMessage();
      event.setCancelled(true);
      UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
         if (!this.plugin.isMenuSessionCurrent(player, sessionGeneration) || !inputField.equals(this.plugin.getCurrentPlayerInputField(player))) {
            return;
         }
         if (this.plugin.submitMenuInput(player, inputField, inputValue)) {
            this.plugin.setCurrentPlayerInputField(player, "");
            this.plugin.refreshCurrentMenu(player);
            this.plugin.setCurrentPlayerInputField(player, "");
            String shownValue = this.plugin.isPasswordInputField(inputField) ? "*****" : inputValue;
            player.sendMessage(ChatColor.GREEN + "[UIcore] " + inputField + " saved: " + shownValue);
         }
      }, null);
   }

}
