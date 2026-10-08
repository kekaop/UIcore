package org.orvencraft.uicore;

import org.orvencraft.uicore.api.UIcoreApi;
import org.orvencraft.uicore.api.MenuMutationResult;
import org.orvencraft.uicore.npc.NPCMirrorHook;
import org.orvencraft.uicore.section.Section;
import org.orvencraft.uicore.util.CommandUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Commands implements TabExecutor {
   private static final Map<UUID, Map<String, String>> USER_INPUT_DATA = new ConcurrentHashMap<>();

   @Override
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player player && !Bukkit.isOwnedByCurrentRegion(player)) {
         player.getScheduler().execute(UIcorePlugin.plugin, () -> this.onCommand(sender, command, label, args), null, 0L);
         return true;
      }
      if (args.length == 0) {
         sendUsage(sender);
         return true;
      }

      return switch (args[0].toLowerCase()) {
         case "run" -> runMenu(sender, args);
         case "stop", "close" -> closeMenu(sender);
         case "reload" -> reload(sender);
         case "items" -> showItem(sender, args);
         case "itemsstop" -> hideItem(sender);
         case "text" -> showText(sender, args);
         case "set_input_field" -> selectInputField(sender, args);
         case "input" -> setInputValue(sender, args);
         case "npc" -> handleNpcCommand(sender, args);
         case "slot" -> handleSlotCommand(sender, args);
         default -> {
            sendUsage(sender);
            yield true;
         }
      };
   }

   private boolean runMenu(CommandSender sender, String[] args) {
      if (!sender.hasPermission("uicore.start")) {
         noPermission(sender);
         return true;
      }
      if (args.length < 2) {
         sender.sendMessage("Usage: /uicore run <menu> [player]");
         return true;
      }

      Section section = UIcorePlugin.sectionManager.get(args[1]);
      if (section == null) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("command.menu_option_not_found", "&c[UIcore] Menu not found."));
         return true;
      }
      if (!section.permission.isEmpty() && !sender.hasPermission(section.permission) && !sender.isOp()) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("permission.no_permission_menu", "&c[UIcore] You cannot open this menu."));
         return true;
      }

      Player target;
      if (args.length >= 3) {
         target = Bukkit.getPlayerExact(args[2]);
      } else if (sender instanceof Player player) {
         target = player;
      } else {
         sender.sendMessage("Usage from console: /uicore run <menu> <player>");
         return true;
      }

      if (target == null || !target.isOnline()) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("player.not_found", "&c[UIcore] Player not found."));
         return true;
      }

      UIcorePlugin.plugin.setupCursor(target, args[1]);
      return true;
   }

   private boolean closeMenu(CommandSender sender) {
      if (!(sender instanceof Player player)) {
         playerOnly(sender);
         return true;
      }
      if (!sender.hasPermission("uicore.stop") && !sender.hasPermission("uicore.close")) {
         noPermission(sender);
         return true;
      }

      USER_INPUT_DATA.remove(player.getUniqueId());
      UIcorePlugin.plugin.stopCursor(player, true);
      return true;
   }

   private boolean reload(CommandSender sender) {
      if (!sender.hasPermission("uicore.reload")) {
         noPermission(sender);
         return true;
      }
      UIcorePlugin.plugin.reloadPluginConfig();
      sender.sendMessage(UIcorePlugin.plugin.getLangMessage("command.reload", "&a[UIcore] Configuration reloaded."));
      return true;
   }

   private boolean showItem(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         playerOnly(sender);
         return true;
      }
      if (!sender.hasPermission("uicore.items")) {
         noPermission(sender);
         return true;
      }
      if (args.length < 2 || !UIcorePlugin.itemDisplayManager.showItem(player, args[1])) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("command.item_not_found", "&c[UIcore] Item display not found."));
      }
      return true;
   }

   private boolean hideItem(CommandSender sender) {
      if (!(sender instanceof Player player)) {
         playerOnly(sender);
         return true;
      }
      if (!sender.hasPermission("uicore.itemsstop")) {
         noPermission(sender);
         return true;
      }
      UIcorePlugin.itemDisplayManager.hideItem(player);
      return true;
   }

   private boolean showText(CommandSender sender, String[] args) {
      if (!sender.hasPermission("uicore.text")) {
         noPermission(sender);
         return true;
      }
      if (args.length < 3 || !"open".equalsIgnoreCase(args[1])) {
         sender.sendMessage("Usage: /uicore text open <id> [player]");
         return true;
      }

      Player target = args.length >= 4 ? Bukkit.getPlayerExact(args[3]) : sender instanceof Player player ? player : null;
      if (target == null || !target.isOnline()) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("player.not_found", "&c[UIcore] Player not found."));
         return true;
      }
      if (!Bukkit.isOwnedByCurrentRegion(target)) {
         target.getScheduler().execute(
            UIcorePlugin.plugin,
            () -> UIcorePlugin.textDisplayManager.showTextDisplayById(target, args[2]),
            null,
            0L
         );
         return true;
      }
      if (!UIcorePlugin.textDisplayManager.showTextDisplayById(target, args[2])) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("command.text_display_not_found", "&c[UIcore] Text display not found."));
      }
      return true;
   }

   private boolean selectInputField(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         playerOnly(sender);
         return true;
      }
      if (!sender.hasPermission("uicore.set_input_field")) {
         noPermission(sender);
         return true;
      }
      if (args.length < 2) {
         sender.sendMessage("Usage: /uicore set_input_field <field>");
         return true;
      }

      if (!UIcorePlugin.plugin.isMenuInputField(player, args[1])) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("input.invalid", "&c[UIcore] This input field is not available in the active menu."));
         return true;
      }

      UIcorePlugin.plugin.setCurrentPlayerInputField(player, args[1]);
      CommandUtils.sendActionBar(player, UIcorePlugin.plugin.getLangMessage("input.field_set", "&a[UIcore] Input field: %field%").replace("%field%", args[1]));
      return true;
   }

   private boolean setInputValue(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         playerOnly(sender);
         return true;
      }
      if (!sender.hasPermission("uicore.input")) {
         noPermission(sender);
         return true;
      }
      if (args.length < 3) {
         sender.sendMessage("Usage: /uicore input <field> <value>");
         return true;
      }

      String field = args[1];
      String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
      if (!UIcorePlugin.plugin.submitMenuInput(player, field, value)) {
         sender.sendMessage(UIcorePlugin.plugin.getLangMessage("input.invalid", "&c[UIcore] This input field is not available in the active menu."));
         return true;
      }
      String displayed = UIcorePlugin.plugin.isPasswordInputField(field) ? "*".repeat(value.length()) : value;
      CommandUtils.sendActionBar(
         player,
         UIcorePlugin.plugin.getLangMessage("input.data_saved", "&a[UIcore] %field% set to: %value%")
            .replace("%field%", field)
            .replace("%value%", displayed)
      );
      return true;
   }

   private boolean handleNpcCommand(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         playerOnly(sender);
         return true;
      }

      NPCMirrorHook npcHook = NPCMirrorHook.getInstance(UIcorePlugin.plugin);
      if (npcHook == null || !npcHook.isEnabled()) {
         player.sendMessage("§e[UIcore] NPC mirror is unavailable. FancyNpcs is required.");
         return true;
      }

      String subcommand = args.length > 1 ? args[1].toLowerCase() : "help";
      switch (subcommand) {
         case "toggle" -> {
            if (!checkPermission(player, "uicore.npc.toggle")) return true;
            player.sendMessage(npcHook.toggleNPCForPlayer(player) ? "§aNPC mirror enabled." : "§cNPC mirror disabled.");
         }
         case "enable" -> {
            if (!checkPermission(player, "uicore.npc.toggle")) return true;
            npcHook.setNPCEnabledForPlayer(player, true);
         }
         case "disable" -> {
            if (!checkPermission(player, "uicore.npc.toggle")) return true;
            npcHook.setNPCEnabledForPlayer(player, false);
         }
         case "status" -> {
            if (!checkPermission(player, "uicore.npc.status")) return true;
            player.sendMessage(npcHook.isNPCEnabledForPlayer(player) ? "§aNPC mirror enabled." : "§cNPC mirror disabled.");
         }
         case "reload" -> {
            if (!checkPermission(player, "uicore.npc.reload")) return true;
            npcHook.reload();
            player.sendMessage("§aNPC configuration reloaded.");
         }
         case "rotate" -> {
            if (!checkPermission(player, "uicore.npc.rotate")) return true;
            if (args.length < 3) {
               player.sendMessage("Usage: /uicore npc rotate <degrees>");
               return true;
            }
            try {
               npcHook.rotateNPC(player, Float.parseFloat(args[2]));
            } catch (NumberFormatException ignored) {
               player.sendMessage("§cInvalid angle: " + args[2]);
            }
         }
         default -> {
            player.sendMessage(ChatColor.YELLOW + "NPC mirror commands:");
            player.sendMessage("/uicore npc toggle|enable|disable|status|reload|rotate <degrees>");
         }
      }
      return true;
   }

   private boolean handleSlotCommand(CommandSender sender, String[] args) {
      if (!sender.hasPermission("uicore.slot")) {
         noPermission(sender);
         return true;
      }

      if (args.length < 2) {
         sendSlotUsage(sender);
         return true;
      }

      UIcoreApi api = UIcorePlugin.plugin.getMenuApi();
      String operation = args[1].toLowerCase();
      MenuMutationResult result;
      switch (operation) {
         case "set" -> {
            if (args.length < 6) {
               sendSlotUsage(sender);
               return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
               sender.sendMessage("§cPlayer not found: " + args[2]);
               return true;
            }
            result = api.setSlot(target, args[3], args[4], args[5]);
         }
         case "clear" -> {
            if (args.length < 5) {
               sendSlotUsage(sender);
               return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
               sender.sendMessage("§cPlayer not found: " + args[2]);
               return true;
            }
            result = api.clearSlot(target, args[3], args[4]);
         }
         case "reset" -> {
            if (args.length < 5) {
               sendSlotUsage(sender);
               return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
               sender.sendMessage("§cPlayer not found: " + args[2]);
               return true;
            }
            result = api.resetSlot(target, args[3], args[4]);
         }
         case "transition" -> {
            if (args.length < 8) {
               sendSlotUsage(sender);
               return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
               sender.sendMessage("§cPlayer not found: " + args[2]);
               return true;
            }
            int duration;
            double distance;
            try {
               duration = Integer.parseInt(args[7]);
               distance = args.length >= 9 ? Double.parseDouble(args[8]) : 2.4;
            } catch (NumberFormatException exception) {
               sender.sendMessage("§cTransition duration and distance must be numbers.");
               return true;
            }
            String easing = args.length >= 10 ? args[9] : "ease-in-cubic";
            if (!Bukkit.isOwnedByCurrentRegion(target)) {
               target.getScheduler().execute(
                  UIcorePlugin.plugin,
                  () -> UIcorePlugin.plugin.transitionPlayerMenuSlot(
                     target, args[3], args[4], args[5], args[6], duration, distance, easing
                  ),
                  null,
                  0L
               );
               result = MenuMutationResult.success("Player slot transition scheduled", 0);
            } else {
               result = UIcorePlugin.plugin.transitionPlayerMenuSlot(
                  target,
                  args[3],
                  args[4],
                  args[5],
                  args[6],
                  duration,
                  distance,
                  easing
               );
            }
         }
         case "refresh" -> {
            if (args.length < 4) {
               sendSlotUsage(sender);
               return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
               sender.sendMessage("§cPlayer not found: " + args[2]);
               return true;
            }
            if (!args[3].equals(api.getOpenMenu(target).orElse(null))) {
               sender.sendMessage("§ePlayer does not currently have menu '" + args[3] + "' open.");
               return true;
            }
            api.refresh(target);
            sender.sendMessage("§aSlot content refreshed for " + target.getName() + ".");
            return true;
         }
         case "set-global" -> {
            if (args.length < 5) {
               sendSlotUsage(sender);
               return true;
            }
            result = api.setSlot(args[2], args[3], args[4]);
         }
         case "clear-global" -> {
            if (args.length < 4) {
               sendSlotUsage(sender);
               return true;
            }
            result = api.clearSlot(args[2], args[3]);
         }
         case "reset-global" -> {
            if (args.length < 4) {
               sendSlotUsage(sender);
               return true;
            }
            result = api.resetSlot(args[2], args[3]);
         }
         default -> {
            sendSlotUsage(sender);
            return true;
         }
      }

      sender.sendMessage((result.success() ? "§a" : "§c") + result.message() + (result.refreshedViewers() > 0 ? " (refreshed: " + result.refreshedViewers() + ")" : ""));
      return true;
   }

   private void sendSlotUsage(CommandSender sender) {
      sender.sendMessage("§e/uicore slot set <player> <menu> <slot> <template>");
      sender.sendMessage("§e/uicore slot transition <player> <menu> <slot> <template> <direction> <ticks> [distance] [easing]");
      sender.sendMessage("§e/uicore slot clear|reset <player> <menu> <slot>");
      sender.sendMessage("§e/uicore slot refresh <player> <menu>");
      sender.sendMessage("§e/uicore slot set-global <menu> <slot> <template>");
      sender.sendMessage("§e/uicore slot clear-global|reset-global <menu> <slot>");
   }

   private boolean checkPermission(Player player, String permission) {
      if (player.hasPermission(permission)) return true;
      noPermission(player);
      return false;
   }

   private void sendUsage(CommandSender sender) {
      sender.sendMessage("§e/uicore run <menu> [player], stop, reload, items, itemsstop, text, input, npc, slot");
   }

   private void noPermission(CommandSender sender) {
      sender.sendMessage(UIcorePlugin.plugin.getLangMessage("permission.no_permission", "&c[UIcore] No permission."));
   }

   private void playerOnly(CommandSender sender) {
      sender.sendMessage(UIcorePlugin.plugin.getLangMessage("permission.player_only", "&c[UIcore] Player only."));
   }

   @Override
   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      List<String> choices = new ArrayList<>();
      if (args.length == 1) {
         Collections.addAll(choices, "run", "stop", "close", "reload", "items", "itemsstop", "text", "set_input_field", "input", "npc", "slot");
         return StringUtil.copyPartialMatches(args[0], choices, new ArrayList<>());
      }
      if (args.length == 2) {
         if ("run".equalsIgnoreCase(args[0])) choices.addAll(UIcorePlugin.sectionManager.keySet());
         if ("items".equalsIgnoreCase(args[0])) choices.addAll(UIcorePlugin.itemDisplayManager.getAllItemIds());
         if ("text".equalsIgnoreCase(args[0])) choices.add("open");
         if ("npc".equalsIgnoreCase(args[0])) Collections.addAll(choices, "toggle", "enable", "disable", "status", "reload", "rotate", "help");
         if ("slot".equalsIgnoreCase(args[0])) Collections.addAll(choices, "set", "clear", "reset", "refresh", "set-global", "clear-global", "reset-global");
         return StringUtil.copyPartialMatches(args[1], choices, new ArrayList<>());
      }
      if ("slot".equalsIgnoreCase(args[0])) {
         String operation = args[1].toLowerCase();
         boolean global = operation.endsWith("-global");
         if (args.length == 3) {
            if (global) {
               choices.addAll(UIcorePlugin.sectionManager.keySet());
            } else {
               for (Player player : Bukkit.getOnlinePlayers()) choices.add(player.getName());
            }
         } else {
            int menuIndex = global ? 2 : 3;
            int slotIndex = global ? 3 : 4;
            int templateIndex = global ? 4 : 5;
            if (args.length - 1 == menuIndex) {
               choices.addAll(UIcorePlugin.sectionManager.keySet());
            } else if (args.length - 1 == slotIndex) {
               Section section = UIcorePlugin.sectionManager.get(args[menuIndex]);
               if (section != null) choices.addAll(section.slots.keySet());
            } else if (("set".equals(operation) || "set-global".equals(operation)) && args.length - 1 == templateIndex) {
               Section section = UIcorePlugin.sectionManager.get(args[menuIndex]);
               if (section != null) choices.addAll(section.templates.keySet());
            }
         }
         return StringUtil.copyPartialMatches(args[args.length - 1], choices, new ArrayList<>());
      }
      if (args.length == 3 && "run".equalsIgnoreCase(args[0])) {
         for (Player player : Bukkit.getOnlinePlayers()) choices.add(player.getName());
         return StringUtil.copyPartialMatches(args[2], choices, new ArrayList<>());
      }
      return Collections.emptyList();
   }

   public static Map<UUID, Map<String, String>> getUserInputData() {
      return USER_INPUT_DATA;
   }
}
