package com.cmenu.ui;

import com.cmenu.ui.npc.NPCMirrorHook;
import com.cmenu.ui.section.Section;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Commands implements CommandExecutor, TabExecutor {
   private static final Map<UUID, Map<String, String>> userInputData = new ConcurrentHashMap<>();

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      switch (args.length) {
         case 0:
            sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] 未知参数"));
            return true;
         case 1:
            String var19 = args[0].toLowerCase();
            switch (var19) {
               case "run":
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.input_menu_hint", "&c[CursorMenu] 输入对应的菜单选项"));
                  return true;
               case "stop":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.stop")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     CursorMenuPlugin.plugin.stopCursor((Player)sender, true);
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "reload":
                  if (sender.hasPermission("cursormenu.reload")) {
                     CursorMenuPlugin.plugin.reloadPluginConfig();
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.reload", "&a[CursorMenu] 插件重载..."));
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                  }

                  return true;
               case "itemsstop":
                  if (sender instanceof Player) {
                     if (sender.hasPermission("cursormenu.itemsstop")) {
                        CursorMenuPlugin.itemDisplayManager.hideItem((Player)sender);
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.item_disabled", "&a[CursorMenu] 已关闭物品显示"));
                     } else {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     }
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "register_confirm":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.register_confirm")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player playerxxx = (Player)sender;
                     UUID playerIdxxx = playerxxx.getUniqueId();
                     Map<String, String> userDataxxx = CursorMenuPlugin.plugin.getPlayerInputData(playerxxx);
                     if (userDataxxx.isEmpty() || !userDataxxx.containsKey("password") || !userDataxxx.containsKey("confirm_password")) {
                        Map<String, String> oldUserData = userInputData.get(playerIdxxx);
                        if (oldUserData != null) {
                           userDataxxx = oldUserData;
                        }
                     }

                     if (userDataxxx.isEmpty() || !userDataxxx.containsKey("password") || !userDataxxx.containsKey("confirm_password")) {
                        this.sendActionBarMessage(playerxxx, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                        return true;
                     }

                     String usernamexx = playerxxx.getName();
                     String passwordxxx = userDataxxx.get("password");
                     String confirmPasswordx = userDataxxx.get("confirm_password");
                     if (passwordxxx == null || passwordxxx.isEmpty()) {
                        this.sendActionBarMessage(playerxxx, CursorMenuPlugin.plugin.getLangMessage("register.empty_password", "&c[CursorMenu] 密码不能为空"));
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                        return true;
                     }

                     if (confirmPasswordx == null || confirmPasswordx.isEmpty()) {
                        this.sendActionBarMessage(
                           playerxxx, CursorMenuPlugin.plugin.getLangMessage("register.empty_confirm_password", "&c[CursorMenu] 确认密码不能为空")
                        );
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                        return true;
                     }

                     if (CursorMenuPlugin.plugin.getUserDataManager().isUserRegistered(usernamexx)) {
                        this.sendActionBarMessage(playerxxx, CursorMenuPlugin.plugin.getLangMessage("register.user_exists", "&c[CursorMenu] 该账户已注册"));
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login");
                        return true;
                     }

                     if (!passwordxxx.equals(confirmPasswordx)) {
                        this.sendActionBarMessage(playerxxx, CursorMenuPlugin.plugin.getLangMessage("register.password_mismatch", "&c[CursorMenu] 两次输入的密码不一致"));
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                        return true;
                     }

                     if (passwordxxx.length() < 6 || passwordxxx.length() > 12) {
                        this.sendActionBarMessage(
                           playerxxx, CursorMenuPlugin.plugin.getLangMessage("register.invalid_password_length", "&c[CursorMenu] 密码长度必须在6-12位之间")
                        );
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                        return true;
                     }

                     if (isWeakPassword(passwordxxx)) {
                        this.sendActionBarMessage(
                           playerxxx, CursorMenuPlugin.plugin.getLangMessage("register.weak_password", "&c[CursorMenu] 密码过于简单，请使用更复杂的密码")
                        );
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                        return true;
                     }

                     UserDataManager.RegistrationResult result = CursorMenuPlugin.plugin.getUserDataManager().registerUser(playerxxx, passwordxxx);
                     if (result.isSuccess()) {
                        this.sendActionBarMessage(
                           playerxxx,
                           CursorMenuPlugin.plugin
                              .getLangMessage("register.success", "&a[CursorMenu] 用户 %player_name% 注册成功!")
                              .replace("%player_name%", playerxxx.getName())
                        );
                        CursorMenuPlugin.plugin.setPlayerLoggedIn(playerxxx, true);
                        userInputData.remove(playerIdxxx);
                        CursorMenuPlugin.plugin.setPlayerInputData(playerxxx, "password", null);
                        CursorMenuPlugin.plugin.setPlayerInputData(playerxxx, "confirm_password", null);
                     } else {
                        String errorMessage = this.getRegistrationErrorMessage(result.getMessageKey());
                        this.sendActionBarMessage(playerxxx, errorMessage);
                        CursorMenuPlugin.plugin.setupCursor(playerxxx, "login_menu");
                     }
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "close":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.close")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player player = (Player)sender;
                     userInputData.remove(player.getUniqueId());
                     CursorMenuPlugin.plugin.setPlayerInputData(player, "password", null);
                     CursorMenuPlugin.plugin.setPlayerInputData(player, "confirm_password", null);
                     CursorMenuPlugin.plugin.stopCursor(player, true);
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "check_registered":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.check_registered")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player player = (Player)sender;
                     String username = player.getName();
                     if (CursorMenuPlugin.plugin.getUserDataManager().isUserRegistered(username)) {
                        this.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("register.user_exists", "&c[CursorMenu] 该账户已注册"));
                        CursorMenuPlugin.plugin.setupCursor(player, "login");
                        return true;
                     }
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "check_and_register":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.check_and_register")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player playerxx = (Player)sender;
                     UUID playerIdxx = playerxx.getUniqueId();
                     Map<String, String> userDataxx = CursorMenuPlugin.plugin.getPlayerInputData(playerxx);
                     if (userDataxx.isEmpty() || !userDataxx.containsKey("password") || !userDataxx.containsKey("confirm_password")) {
                        Map<String, String> oldUserData = userInputData.get(playerIdxx);
                        if (oldUserData != null) {
                           userDataxx = oldUserData;
                        }
                     }

                     if (userDataxx.isEmpty() || !userDataxx.containsKey("password") || !userDataxx.containsKey("confirm_password")) {
                        this.sendActionBarMessage(playerxx, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                        return true;
                     }

                     String passwordxx = userDataxx.get("password");
                     String confirmPassword = userDataxx.get("confirm_password");
                     if (passwordxx == null || confirmPassword == null) {
                        this.sendActionBarMessage(playerxx, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                        return true;
                     }

                     if (!passwordxx.equals(confirmPassword)) {
                        this.sendActionBarMessage(playerxx, CursorMenuPlugin.plugin.getLangMessage("register.password_mismatch", "&c[CursorMenu] 两次输入的密码不一致"));
                        return true;
                     }

                     if (passwordxx.length() < 6 || passwordxx.length() > 12) {
                        this.sendActionBarMessage(
                           playerxx, CursorMenuPlugin.plugin.getLangMessage("register.invalid_password_length", "&c[CursorMenu] 密码长度必须在6-12位之间")
                        );
                        return true;
                     }

                     if (isWeakPassword(passwordxx)) {
                        this.sendActionBarMessage(playerxx, CursorMenuPlugin.plugin.getLangMessage("register.weak_password", "&c[CursorMenu] 密码过于简单，请使用更复杂的密码"));
                        return true;
                     }

                     Bukkit.dispatchCommand(playerxx, "cursormenu register_confirm");
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "check_password_before_login":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.check_password_before_login")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player playerx = (Player)sender;
                     UUID playerIdx = playerx.getUniqueId();
                     Map<String, String> userDatax = CursorMenuPlugin.plugin.getPlayerInputData(playerx);
                     if (userDatax.isEmpty() || !userDatax.containsKey("password")) {
                        Map<String, String> oldUserData = userInputData.get(playerIdx);
                        if (oldUserData != null) {
                           userDatax = oldUserData;
                        }
                     }

                     if (userDatax.isEmpty() || !userDatax.containsKey("password")) {
                        this.sendActionBarMessage(playerx, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                        return true;
                     }

                     String usernamex = playerx.getName();
                     String passwordx = userDatax.get("password");
                     if (passwordx == null || passwordx.isEmpty()) {
                        this.sendActionBarMessage(playerx, CursorMenuPlugin.plugin.getLangMessage("login.empty_password", "&c[CursorMenu] 密码不能为空"));
                        return true;
                     }

                     UserDataManager.AuthenticationResult authResult = CursorMenuPlugin.plugin.getUserDataManager().authenticateUser(playerx, passwordx);
                     if (!authResult.isSuccess()) {
                        String errorMessage = this.getLoginErrorMessage(authResult.getMessageKey());
                        this.sendActionBarMessage(playerx, errorMessage);
                        return true;
                     }

                     this.sendActionBarMessage(playerx, CursorMenuPlugin.plugin.getLangMessage("login.success", "&a[CursorMenu] 登录成功!"));
                     CursorMenuPlugin.plugin.setPlayerLoggedIn(playerx, true);
                     userInputData.remove(playerIdx);
                     CursorMenuPlugin.plugin.setPlayerInputData(playerx, "password", null);
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "login":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.login")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player player = (Player)sender;
                     UUID playerId = player.getUniqueId();
                     Map<String, String> userData = CursorMenuPlugin.plugin.getPlayerInputData(player);
                     if (userData.isEmpty() || !userData.containsKey("password")) {
                        Map<String, String> oldUserData = userInputData.get(playerId);
                        if (oldUserData != null) {
                           userData = oldUserData;
                        }
                     }

                     if (userData.isEmpty() || !userData.containsKey("password")) {
                        this.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                        return true;
                     }

                     String username = player.getName();
                     String password = userData.get("password");
                     if (password == null || password.isEmpty()) {
                        this.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("login.empty_password", "&c[CursorMenu] 密码不能为空"));
                        return true;
                     }

                     UserDataManager.AuthenticationResult authResult = CursorMenuPlugin.plugin.getUserDataManager().authenticateUser(player, password);
                     if (!authResult.isSuccess()) {
                        String errorMessage = this.getLoginErrorMessage(authResult.getMessageKey());
                        this.sendActionBarMessage(player, errorMessage);
                        return true;
                     }

                     this.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("login.success", "&a[CursorMenu] 登录成功!"));
                     CursorMenuPlugin.plugin.setPlayerLoggedIn(player, true);
                     userInputData.remove(playerId);
                     CursorMenuPlugin.plugin.setPlayerInputData(player, "password", null);
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "toggle_password_visibility":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.toggle_password_visibility")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player player = (Player)sender;
                     CursorMenuPlugin.plugin.togglePasswordVisibility(player);
                     String currentMenu = CursorMenuPlugin.plugin.getCurrentPlayerMenu(player);
                     if (currentMenu != null && !currentMenu.isEmpty()) {
                        CursorMenuPlugin.plugin.setupCursor(player, currentMenu);
                     }

                     boolean isVisible = CursorMenuPlugin.plugin.getPasswordVisibility(player);
                     String languageKey = isVisible ? "password.visibility_shown" : "password.visibility_hidden";
                     String defaultMessage = isVisible ? "&a[CursorMenu] 密码已显示" : "&a[CursorMenu] 密码已隐藏";
                     String message = CursorMenuPlugin.plugin.getLangMessage(languageKey, defaultMessage);
                     this.sendActionBarMessage(player, message);
                     player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                     return true;
                  }

                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  return true;
               case "set_input_field":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.set_input_field")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player player = (Player)sender;
                     if (args.length > 1) {
                        String field = args[1];
                        CursorMenuPlugin.plugin.setCurrentPlayerInputField(player, field);
                        this.sendActionBarMessage(
                           player, CursorMenuPlugin.plugin.getLangMessage("input.field_set", "&a[CursorMenu] 当前输入字段已设置为: %field%").replace("%field%", field)
                        );
                     } else {
                        this.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                     }

                     return true;
                  }

                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  return true;
               case "npc":
                  this.handleNPCCommand(sender, args);
                  return true;
               default:
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] 未知参数"));
                  return true;
            }
         case 2:
            String var18 = args[0].toLowerCase();
            switch (var18) {
               case "run":
                  if (sender.hasPermission("cursormenu.start")) {
                     Section section = CursorMenuPlugin.sectionManager.get(args[1]);
                     if (section == null) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] 无该菜单选项"));
                        return true;
                     }

                     if (!section.permission.isEmpty() && !sender.hasPermission(section.permission) && !sender.isOp()) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission_menu", "&c[CursorMenu] 你没有权限打开该菜单"));
                        return true;
                     }

                     if (CursorMenuPlugin.sectionManager.has(args[1])) {
                        if (sender instanceof Player) {
                           CursorMenuPlugin.plugin.setupCursor((Player)sender, args[1]);
                        } else {
                           sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                        }
                     } else {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] 无该菜单选项"));
                     }

                     return true;
                  }

                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                  return true;
               case "items":
                  if (sender instanceof Player) {
                     if (sender.hasPermission("cursormenu.items")) {
                        String id = args[1];
                        if (!CursorMenuPlugin.itemDisplayManager.showItem((Player)sender, id)) {
                           sender.sendMessage(
                              CursorMenuPlugin.plugin.getLangMessage("command.item_not_found", "&c[CursorMenu] 物品ID不存在: %item_id%").replace("%item_id%", id)
                           );
                        }
                     } else {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     }
                  } else {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  }

                  return true;
               case "set_input_field":
                  if (sender instanceof Player) {
                     if (!sender.hasPermission("cursormenu.set_input_field")) {
                        sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                        return true;
                     }

                     Player player = (Player)sender;
                     String field = args[1];
                     CursorMenuPlugin.plugin.setCurrentPlayerInputField(player, field);
                     this.sendActionBarMessage(
                        player, CursorMenuPlugin.plugin.getLangMessage("input.field_set", "&a[CursorMenu] 当前输入字段已设置为: %field%").replace("%field%", field)
                     );
                     return true;
                  }

                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] 仅玩家可用"));
                  return true;
               case "deleteuser":
                  if (!sender.hasPermission("cursormenu.deleteuser")) {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return true;
                  } else {
                     String targetPlayerName = args[1];
                     if (!CursorMenuPlugin.plugin.getDatabaseManager().isUsernameExists(targetPlayerName)) {
                        sender.sendMessage(
                           CursorMenuPlugin.plugin
                              .getLangMessage("player.not_registered", "&c[CursorMenu] 玩家 %player% 不存在于注册列表中")
                              .replace("%player%", targetPlayerName)
                        );
                        return true;
                     }

                     boolean success = CursorMenuPlugin.plugin.getDatabaseManager().deleteUser(targetPlayerName);
                     if (success) {
                        sender.sendMessage(
                           CursorMenuPlugin.plugin
                              .getLangMessage("player.delete_success", "&a[CursorMenu] 玩家 %player% 的注册信息已成功删除，该玩家现在可以重新注册")
                              .replace("%player%", targetPlayerName)
                        );
                        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                           CursorMenuPlugin.plugin.setPlayerLoggedIn(targetPlayer, false);
                           targetPlayer.sendMessage(CursorMenuPlugin.plugin.getLangMessage("player.delete_notify", "&e[CursorMenu] 您的注册信息已被管理员删除，请重新注册"));
                           CursorMenuPlugin.plugin.setupCursor(targetPlayer, "login_menu");
                        }
                     } else {
                        sender.sendMessage(
                           CursorMenuPlugin.plugin
                              .getLangMessage("player.delete_failed", "&c[CursorMenu] 删除玩家 %player% 的注册信息失败")
                              .replace("%player%", targetPlayerName)
                        );
                     }

                     return true;
                  }
               default:
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] 未知参数"));
                  return true;
            }
         case 3:
            if (args[0].equalsIgnoreCase("input") && sender instanceof Player) {
               if (!sender.hasPermission("cursormenu.input")) {
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                  return true;
               } else {
                  Player player = (Player)sender;
                  String field = args[1];
                  String value = args[2];
                  UUID playerId = player.getUniqueId();
                  userInputData.computeIfAbsent(playerId, k -> new HashMap<>());
                  Map<String, String> userData = userInputData.get(playerId);
                  switch (field) {
                     case "username":
                        userData.put("username", value);
                        this.sendActionBarMessage(
                           player,
                           CursorMenuPlugin.plugin
                              .getLangMessage("input.data_saved", "&a[CursorMenu] %field% 已设置为: %value%")
                              .replace("%field%", "用户名")
                              .replace("%value%", value)
                        );
                        CursorMenuPlugin.plugin.setupCursor(player, CursorMenuPlugin.plugin.getCurrentPlayerMenu(player));
                        break;
                     case "password":
                        userData.put("password", value);
                        this.sendActionBarMessage(
                           player,
                           CursorMenuPlugin.plugin
                              .getLangMessage("input.data_saved", "&a[CursorMenu] %field% 已设置为: %value%")
                              .replace("%field%", "密码")
                              .replace("%value%", "*****")
                        );
                        CursorMenuPlugin.plugin.setupCursor(player, CursorMenuPlugin.plugin.getCurrentPlayerMenu(player));
                        break;
                     case "confirm_password":
                        userData.put("confirm_password", value);
                        this.sendActionBarMessage(
                           player,
                           CursorMenuPlugin.plugin
                              .getLangMessage("input.data_saved", "&a[CursorMenu] %field% 已设置为: %value%")
                              .replace("%field%", "确认密码")
                              .replace("%value%", "*****")
                        );
                        CursorMenuPlugin.plugin.setupCursor(player, CursorMenuPlugin.plugin.getCurrentPlayerMenu(player));
                        break;
                     default:
                        this.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                        return true;
                  }

                  CursorMenuPlugin.plugin.setPlayerInputData(player, field, value);
                  return true;
               }
            } else if (args[0].equalsIgnoreCase("resetpassword")) {
               if (!sender.hasPermission("cursormenu.resetpassword")) {
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                  return true;
               } else {
                  String targetPlayerName = args[1];
                  String newPassword = args[2];
                  Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
                  if (targetPlayer == null) {
                     sender.sendMessage(
                        CursorMenuPlugin.plugin.getLangMessage("player.not_found", "&c[CursorMenu] 玩家 %player% 不存在或不在线").replace("%player%", targetPlayerName)
                     );
                     return true;
                  } else {
                     if (newPassword.length() >= 6 && newPassword.length() <= 12) {
                        if (isWeakPassword(newPassword)) {
                           sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("register.weak_password", "&c[CursorMenu] 密码过于简单，请使用更复杂的密码"));
                           return true;
                        }

                        String hashedPassword = CursorMenuPlugin.plugin.getUserDataManager().hashPasswordForTest(newPassword);
                        boolean success = CursorMenuPlugin.plugin.getDatabaseManager().updateUserPassword(targetPlayerName, hashedPassword);
                        if (success) {
                           sender.sendMessage(
                              CursorMenuPlugin.plugin
                                 .getLangMessage("player.reset_password_success", "&a[CursorMenu] 玩家 %player% 的密码已成功重置")
                                 .replace("%player%", targetPlayerName)
                           );
                           if (targetPlayer.isOnline()) {
                              CursorMenuPlugin.plugin.setPlayerLoggedIn(targetPlayer, false);
                              targetPlayer.sendMessage(
                                 CursorMenuPlugin.plugin.getLangMessage("player.reset_password_notify", "&e[CursorMenu] 您的密码已被管理员重置，请重新登录")
                              );
                              CursorMenuPlugin.plugin.setupCursor(targetPlayer, "login");
                           }
                        } else {
                           sender.sendMessage(
                              CursorMenuPlugin.plugin
                                 .getLangMessage("player.reset_password_failed", "&c[CursorMenu] 重置玩家 %player% 的密码失败")
                                 .replace("%player%", targetPlayerName)
                           );
                        }

                        return true;
                     }

                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("register.invalid_password_length", "&c[CursorMenu] 密码长度必须在6-12位之间"));
                     return true;
                  }
               }
            } else if (args[0].equalsIgnoreCase("run")) {
               Section section = CursorMenuPlugin.sectionManager.get(args[1]);
               if (section == null) {
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] 无该菜单选项"));
                  return true;
               } else if (!section.permission.isEmpty() && !sender.hasPermission(section.permission) && !sender.isOp()) {
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission_menu", "&c[CursorMenu] 你没有权限打开该菜单"));
                  return true;
               } else if (!sender.hasPermission("cursormenu.start")) {
                  sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                  return true;
               } else {
                  String menuKey = args[1];
                  if (!CursorMenuPlugin.sectionManager.has(menuKey)) {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] 无该菜单选项"));
                     return true;
                  } else {
                     Player target = Bukkit.getPlayer(args[2]);
                     if (target != null && target.isOnline()) {
                        CursorMenuPlugin.plugin.setupCursor(target, menuKey);
                        sender.sendMessage(
                           CursorMenuPlugin.plugin
                              .getLangMessage("command.menu_opened", "&a[CursorMenu] 已为玩家 %player% 打开菜单 %menu%")
                              .replace("%player%", target.getName())
                              .replace("%menu%", menuKey)
                        );
                        return true;
                     }

                     sender.sendMessage(
                        CursorMenuPlugin.plugin.getLangMessage("player.not_found", "&c[CursorMenu] 玩家 %player% 不存在或不在线").replace("%player%", args[2])
                     );
                     return true;
                  }
               }
            } else {
               if (args[0].equalsIgnoreCase("text") && args[1].equalsIgnoreCase("open")) {
                  if (!sender.hasPermission("cursormenu.text")) {
                     sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return true;
                  }

                  String textId = args[2];
                  Player target = sender instanceof Player ? (Player)sender : null;
                  if (args.length == 4) {
                     target = Bukkit.getPlayer(args[3]);
                  }

                  if (target != null && target.isOnline()) {
                     boolean success = CursorMenuPlugin.textDisplayManager.showTextDisplayById(target, textId);
                     if (!success) {
                        sender.sendMessage(
                           CursorMenuPlugin.plugin
                              .getLangMessage("command.text_display_not_found", "&c[CursorMenu] 文字显示ID不存在: %text_id%")
                              .replace("%text_id%", textId)
                        );
                        return true;
                     }

                     sender.sendMessage(
                        CursorMenuPlugin.plugin
                           .getLangMessage("command.text_display_opened", "&a[CursorMenu] 已为玩家 %player% 打开文字显示: %text_id%")
                           .replace("%player%", target.getName())
                           .replace("%text_id%", textId)
                     );
                     return true;
                  }

                  sender.sendMessage(
                     CursorMenuPlugin.plugin
                        .getLangMessage("player.not_found", "&c[CursorMenu] 玩家 %player% 不存在或不在线")
                        .replace("%player%", args.length == 4 ? args[3] : "sender")
                  );
                  return true;
               }

               sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] 未知参数"));
               return true;
            }
         default:
            sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] 未知参数"));
            return true;
      }
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
      List<String> list = new ArrayList<>();
      switch (args.length) {
         case 1:
            if (commandSender.hasPermission("cursormenu.reload")) {
               list.add("reload");
            }

            if (commandSender.hasPermission("cursormenu.start")) {
               list.add("run");
            }

            if (commandSender.hasPermission("cursormenu.stop")) {
               list.add("stop");
            }

            if (commandSender.hasPermission("cursormenu.items")) {
               list.add("items");
            }

            if (commandSender.hasPermission("cursormenu.itemsstop")) {
               list.add("itemsstop");
            }

            if (commandSender.hasPermission("cursormenu.text")) {
               list.add("text");
            }

            if (commandSender.hasPermission("cursormenu.resetpassword")) {
               list.add("resetpassword");
            }

            list.add("input");
            list.add("toggle_password_visibility");
            list.add("register_confirm");
            list.add("check_and_register");
            list.add("check_registered");
            list.add("login");
            list.add("close");
            list.add("npc");
            return (List<String>)StringUtil.copyPartialMatches(args[0].toLowerCase(), list, new ArrayList());
         case 2:
            String var6 = args[0].toLowerCase();
            switch (var6) {
               case "run":
                  if (commandSender.hasPermission("cursormenu.start")) {
                     list.addAll(CursorMenuPlugin.sectionManager.keySet());
                     return (List<String>)StringUtil.copyPartialMatches(args[1].toLowerCase(), list, new ArrayList());
                  }
               case "items":
                  if (commandSender.hasPermission("cursormenu.items")) {
                     list.addAll(CursorMenuPlugin.itemDisplayManager.getAllItemIds());
                     return (List<String>)StringUtil.copyPartialMatches(args[1], list, new ArrayList());
                  }
               case "input":
                  list.add("username");
                  list.add("password");
                  list.add("confirm_password");
                  return (List<String>)StringUtil.copyPartialMatches(args[1], list, new ArrayList());
               case "resetpassword":
                  if (commandSender.hasPermission("cursormenu.resetpassword")) {
                     for (Player player : Bukkit.getOnlinePlayers()) {
                        list.add(player.getName());
                     }

                     return (List<String>)StringUtil.copyPartialMatches(args[1], list, new ArrayList());
                  }
               case "text":
                  if (commandSender.hasPermission("cursormenu.text")) {
                     list.add("open");
                  }

                  return (List<String>)StringUtil.copyPartialMatches(args[1], list, new ArrayList());
               case "npc":
                  list.add("toggle");
                  list.add("enable");
                  list.add("disable");
                  list.add("status");
                  list.add("reload");
                  list.add("rotate");
                  list.add("help");
                  return (List<String>)StringUtil.copyPartialMatches(args[1], list, new ArrayList());
               default:
                  return Collections.emptyList();
            }
         case 3:
            if (args[0].equalsIgnoreCase("resetpassword") && commandSender.hasPermission("cursormenu.resetpassword")) {
               return Collections.emptyList();
            } else {
               if (args[0].equalsIgnoreCase("run")) {
                  return null;
               }

               return Collections.emptyList();
            }
         default:
            return Collections.emptyList();
      }
   }

   public static Map<UUID, Map<String, String>> getUserInputData() {
      return userInputData;
   }

   private static boolean isWeakPassword(String password) {
      String[] weakPasswords = new String[]{"123456", "password", "123456789", "12345678", "12345", "1234567", "1234567890", "qwerty", "abc123", "111111"};

      for (String weak : weakPasswords) {
         if (password.equalsIgnoreCase(weak)) {
            return true;
         }
      }

      return password.matches("\\d+") || password.matches("[a-zA-Z]+") ? true : isSequential(password);
   }

   private static boolean isSequential(String str) {
      boolean isAscending = true;
      boolean isDescending = true;

      for (int i = 1; i < str.length(); i++) {
         if (str.charAt(i) != str.charAt(i - 1) + 1) {
            isAscending = false;
         }

         if (str.charAt(i) != str.charAt(i - 1) - 1) {
            isDescending = false;
         }
      }

      return isAscending || isDescending;
   }

   private void sendActionBarMessage(Player player, String message) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
   }

   private String getRegistrationErrorMessage(String messageKey) {
      if (messageKey == null) {
         return CursorMenuPlugin.plugin.getLangMessage("register.error", "&c[CursorMenu] 注册失败，请稍后重试");
      } else {
         switch (messageKey) {
            case "ip_blacklisted":
               return CursorMenuPlugin.plugin.getLangMessage("register.error.ip_blacklisted", "&c[CursorMenu] 您的IP地址已被禁止注册");
            case "ip_not_in_whitelist":
               return CursorMenuPlugin.plugin.getLangMessage("register.error.ip_not_in_whitelist", "&c[CursorMenu] 您的IP地址不在允许注册的列表中");
            default:
               return CursorMenuPlugin.plugin.getLangMessage("register.error", "&c[CursorMenu] 注册失败，请稍后重试");
         }
      }
   }

   private String getLoginErrorMessage(String messageKey) {
      if (messageKey == null) {
         return CursorMenuPlugin.plugin.getLangMessage("login.error", "&c[CursorMenu] 登录失败，请稍后重试");
      } else {
         switch (messageKey) {
            case "ip_blacklisted":
               return CursorMenuPlugin.plugin.getLangMessage("login.error.ip_blacklisted", "&c[CursorMenu] 您的IP地址已被禁止登录");
            case "ip_not_in_whitelist":
               return CursorMenuPlugin.plugin.getLangMessage("login.error.ip_not_in_whitelist", "&c[CursorMenu] 您的IP地址不在允许登录的列表中");
            case "same_ip_login_not_allowed":
               return CursorMenuPlugin.plugin.getLangMessage("login.same_ip_not_allowed", "&c[CursorMenu] 当前配置不允许使用该IP登录");
            case "locked_out":
               return CursorMenuPlugin.plugin.getLangMessage("login.error.locked_out", "&c[CursorMenu] 账户已被锁定，请稍后重试");
            case "invalid_password":
               return CursorMenuPlugin.plugin.getLangMessage("login.error.invalid_password", "&c[CursorMenu] 密码错误");
            case "user_not_found":
               return CursorMenuPlugin.plugin.getLangMessage("login.error.user_not_found", "&c[CursorMenu] 用户不存在");
            default:
               return CursorMenuPlugin.plugin.getLangMessage("login.error", "&c[CursorMenu] 登录失败，请稍后重试");
         }
      }
   }

   private void handleNPCCommand(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.player_only", "&c[CursorMenu] NPC命令仅玩家可用"));
      } else {
         NPCMirrorHook npcHook = NPCMirrorHook.getInstance(CursorMenuPlugin.plugin);
         if (npcHook != null && npcHook.isEnabled()) {
            String subCommand = args.length > 1 ? args[1].toLowerCase() : "help";
            switch (subCommand) {
               case "toggle":
                  if (!player.hasPermission("cursormenu.npc.toggle")) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return;
                  }

                  boolean newState = npcHook.toggleNPCForPlayer(player);
                  player.sendMessage(
                     newState
                        ? CursorMenuPlugin.plugin.getLangMessage("npc.toggle_on", "&a[CursorMenu] NPC镜像创建已启用")
                        : CursorMenuPlugin.plugin.getLangMessage("npc.toggle_off", "&c[CursorMenu] NPC镜像创建已禁用")
                  );
                  break;
               case "enable":
                  if (!player.hasPermission("cursormenu.npc.toggle")) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return;
                  }

                  npcHook.setNPCEnabledForPlayer(player, true);
                  player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("npc.toggle_on", "&a[CursorMenu] NPC镜像创建已启用"));
                  break;
               case "disable":
                  if (!player.hasPermission("cursormenu.npc.toggle")) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return;
                  }

                  npcHook.setNPCEnabledForPlayer(player, false);
                  player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("npc.toggle_off", "&c[CursorMenu] NPC镜像创建已禁用"));
                  break;
               case "status":
                  if (!player.hasPermission("cursormenu.npc.status")) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return;
                  }

                  player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("npc.status_enabled", "&a[CursorMenu] NPC创建状态: 已启用"));
                  break;
               case "reload":
                  if (!player.hasPermission("cursormenu.npc.reload")) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return;
                  }

                  npcHook.reload();
                  player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("npc.reloaded", "&a[CursorMenu] NPC配置已重载"));
                  break;
               case "rotate":
                  if (!player.hasPermission("cursormenu.npc.rotate")) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] 你没有权限使用该命令"));
                     return;
                  }

                  if (args.length < 3) {
                     player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("npc.rotate_usage", "&c[CursorMenu] 用法: /cursormenu npc rotate <角度>"));
                     return;
                  }

                  try {
                     float angle = Float.parseFloat(args[2]);
                     npcHook.rotateNPC(player, angle);
                     player.sendMessage(
                        CursorMenuPlugin.plugin.getLangMessage("npc.rotated", "&a[CursorMenu] NPC已旋转 %angle% 度").replace("%angle%", String.valueOf(angle))
                     );
                  } catch (NumberFormatException var10) {
                     player.sendMessage(
                        CursorMenuPlugin.plugin.getLangMessage("npc.invalid_angle", "&c[CursorMenu] 无效的角度值: %angle%").replace("%angle%", args[2])
                     );
                  }
                  break;
               case "help":
               default:
                  player.sendMessage(ChatColor.YELLOW + "===== NPC镜像系统帮助 =====");
                  player.sendMessage(ChatColor.GRAY + "/cursormenu npc toggle " + ChatColor.WHITE + "- 切换NPC创建状态");
                  player.sendMessage(ChatColor.GRAY + "/cursormenu npc enable " + ChatColor.WHITE + "- 启用NPC创建");
                  player.sendMessage(ChatColor.GRAY + "/cursormenu npc disable " + ChatColor.WHITE + "- 禁用NPC创建");
                  player.sendMessage(ChatColor.GRAY + "/cursormenu npc status " + ChatColor.WHITE + "- 查看NPC状态");
                  player.sendMessage(ChatColor.GRAY + "/cursormenu npc reload " + ChatColor.WHITE + "- 重载NPC配置");
                  player.sendMessage(ChatColor.GRAY + "/cursormenu npc rotate <角度> " + ChatColor.WHITE + "- 旋转NPC");
            }
         } else {
            player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("npc.system_disabled", "&e[CursorMenu] NPC镜像系统未启用（需要安装FancyNpcs插件）"));
         }
      }
   }
}
