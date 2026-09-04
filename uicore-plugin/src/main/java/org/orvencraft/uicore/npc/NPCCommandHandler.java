package org.orvencraft.uicore.npc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NPCCommandHandler implements CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private final NPCMirrorHook npcHook;

   public NPCCommandHandler(JavaPlugin plugin) {
      this.plugin = plugin;
      this.npcHook = NPCMirrorHook.getInstance(plugin);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatColor.RED + "[NPCMirror] This command can only be used by a player.");
         return true;
      } else if (args.length == 0) {
         this.sendHelp(player);
         return true;
      } else {
         String subCommand = args[0].toLowerCase();
         switch (subCommand) {
            case "toggle":
               return this.handleToggle(player);
            case "enable":
               return this.handleEnable(player, true);
            case "disable":
               return this.handleEnable(player, false);
            case "status":
               return this.handleStatus(player);
            case "reload":
               return this.handleReload(player);
            case "rotate":
               return this.handleRotate(player, args);
            case "help":
               this.sendHelp(player);
               return true;
            default:
               player.sendMessage(ChatColor.RED + "[NPCMirror] Unknown command: " + subCommand);
               this.sendHelp(player);
               return true;
         }
      }
   }

   private boolean handleToggle(Player player) {
      if (!player.hasPermission("uicore.npc.toggle")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to use this command.");
         return true;
      } else {
         boolean newState = this.npcHook.toggleNPCForPlayer(player);
         player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC mirror creation " + (newState ? "enabled" : "disabled") + ".");
         return true;
      }
   }

   private boolean handleEnable(Player player, boolean enable) {
      if (!player.hasPermission("uicore.npc.toggle")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to use this command.");
         return true;
      } else {
         this.npcHook.setNPCEnabledForPlayer(player, enable);
         player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC mirror creation " + (enable ? "enabled" : "disabled") + ".");
         return true;
      }
   }

   private boolean handleStatus(Player player) {
      if (!player.hasPermission("uicore.npc.status")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to use this command.");
         return true;
      } else {
         player.sendMessage(ChatColor.YELLOW + "===== NPC Mirror Status =====");
         player.sendMessage(ChatColor.GRAY + "System enabled: " + ChatColor.WHITE + this.npcHook.isEnabled());
         player.sendMessage(ChatColor.GRAY + "Your NPC mirror: " + ChatColor.WHITE + (this.npcHook.isNPCEnabledForPlayer(player) ? "enabled" : "disabled"));
         player.sendMessage(ChatColor.GRAY + "NPC currently present: " + ChatColor.WHITE + (this.npcHook.getNPCManager().hasNPC(player) ? "yes" : "no"));
         return true;
      }
   }

   private boolean handleReload(Player player) {
      if (!player.hasPermission("uicore.npc.reload")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to use this command.");
         return true;
      } else {
         this.npcHook.reload();
         player.sendMessage(ChatColor.GREEN + "[NPCMirror] Configuration reloaded.");
         return true;
      }
   }

   private boolean handleRotate(Player player, String[] args) {
      if (!player.hasPermission("uicore.npc.rotate")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to use this command.");
         return true;
      } else if (args.length < 2) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] Usage: /uicore npc rotate <degrees>");
         return true;
      } else {
         try {
            float angle = Float.parseFloat(args[1]);
            this.npcHook.rotateNPC(player, angle);
            player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC rotated by " + angle + " degrees.");
         } catch (NumberFormatException var4) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] Invalid angle: " + args[1]);
         }

         return true;
      }
   }

   private void sendHelp(Player player) {
      player.sendMessage(ChatColor.YELLOW + "===== NPC Mirror Help =====");
      player.sendMessage(ChatColor.GRAY + "/uicore npc toggle " + ChatColor.WHITE + "- Toggle NPC mirror creation");
      player.sendMessage(ChatColor.GRAY + "/uicore npc enable " + ChatColor.WHITE + "- Enable NPC mirror creation");
      player.sendMessage(ChatColor.GRAY + "/uicore npc disable " + ChatColor.WHITE + "- Disable NPC mirror creation");
      player.sendMessage(ChatColor.GRAY + "/uicore npc status " + ChatColor.WHITE + "- Show NPC mirror status");
      player.sendMessage(ChatColor.GRAY + "/uicore npc reload " + ChatColor.WHITE + "- Reload NPC configuration");
      player.sendMessage(ChatColor.GRAY + "/uicore npc rotate <degrees> " + ChatColor.WHITE + "- Rotate the NPC");
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         completions.addAll(Arrays.asList("toggle", "enable", "disable", "status", "reload", "rotate", "help"));
      } else if (args.length == 2 && args[0].equalsIgnoreCase("rotate")) {
         completions.addAll(Arrays.asList("45", "90", "180", "-45", "-90", "-180"));
      }

      String lastArg = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
      return completions;
   }
}
