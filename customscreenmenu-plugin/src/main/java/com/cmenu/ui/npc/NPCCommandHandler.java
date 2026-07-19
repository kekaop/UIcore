package com.cmenu.ui.npc;

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
         sender.sendMessage(ChatColor.RED + "[NPCMirror] 此命令只能由玩家执行");
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
               player.sendMessage(ChatColor.RED + "[NPCMirror] 未知命令: " + subCommand);
               this.sendHelp(player);
               return true;
         }
      }
   }

   private boolean handleToggle(Player player) {
      if (!player.hasPermission("cursormenu.npc.toggle")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] 你没有权限执行此命令");
         return true;
      } else {
         boolean newState = this.npcHook.toggleNPCForPlayer(player);
         player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC镜像创建已" + (newState ? "启用" : "禁用"));
         return true;
      }
   }

   private boolean handleEnable(Player player, boolean enable) {
      if (!player.hasPermission("cursormenu.npc.toggle")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] 你没有权限执行此命令");
         return true;
      } else {
         this.npcHook.setNPCEnabledForPlayer(player, enable);
         player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC镜像创建已" + (enable ? "启用" : "禁用"));
         return true;
      }
   }

   private boolean handleStatus(Player player) {
      if (!player.hasPermission("cursormenu.npc.status")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] 你没有权限执行此命令");
         return true;
      } else {
         player.sendMessage(ChatColor.YELLOW + "===== NPC镜像系统状态 =====");
         player.sendMessage(ChatColor.GRAY + "系统启用: " + ChatColor.WHITE + this.npcHook.isEnabled());
         player.sendMessage(ChatColor.GRAY + "你的NPC创建: " + ChatColor.WHITE + (this.npcHook.isNPCEnabledForPlayer(player) ? "启用" : "禁用"));
         player.sendMessage(ChatColor.GRAY + "当前有NPC: " + ChatColor.WHITE + (this.npcHook.getNPCManager().hasNPC(player) ? "是" : "否"));
         return true;
      }
   }

   private boolean handleReload(Player player) {
      if (!player.hasPermission("cursormenu.npc.reload")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] 你没有权限执行此命令");
         return true;
      } else {
         this.npcHook.reload();
         player.sendMessage(ChatColor.GREEN + "[NPCMirror] 配置已重载");
         return true;
      }
   }

   private boolean handleRotate(Player player, String[] args) {
      if (!player.hasPermission("cursormenu.npc.rotate")) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] 你没有权限执行此命令");
         return true;
      } else if (args.length < 2) {
         player.sendMessage(ChatColor.RED + "[NPCMirror] 用法: /cursormenu npc rotate <角度>");
         return true;
      } else {
         try {
            float angle = Float.parseFloat(args[1]);
            this.npcHook.rotateNPC(player, angle);
            player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC已旋转 " + angle + " 度");
         } catch (NumberFormatException var4) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] 无效的角度值: " + args[1]);
         }

         return true;
      }
   }

   private void sendHelp(Player player) {
      player.sendMessage(ChatColor.YELLOW + "===== NPC镜像系统帮助 =====");
      player.sendMessage(ChatColor.GRAY + "/cursormenu npc toggle " + ChatColor.WHITE + "- 切换NPC创建状态");
      player.sendMessage(ChatColor.GRAY + "/cursormenu npc enable " + ChatColor.WHITE + "- 启用NPC创建");
      player.sendMessage(ChatColor.GRAY + "/cursormenu npc disable " + ChatColor.WHITE + "- 禁用NPC创建");
      player.sendMessage(ChatColor.GRAY + "/cursormenu npc status " + ChatColor.WHITE + "- 查看NPC状态");
      player.sendMessage(ChatColor.GRAY + "/cursormenu npc reload " + ChatColor.WHITE + "- 重载NPC配置");
      player.sendMessage(ChatColor.GRAY + "/cursormenu npc rotate <角度> " + ChatColor.WHITE + "- 旋转NPC");
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
