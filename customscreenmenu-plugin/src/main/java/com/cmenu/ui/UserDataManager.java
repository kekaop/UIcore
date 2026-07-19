package com.cmenu.ui;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mindrot.jbcrypt.BCrypt;

public class UserDataManager {
   private final JavaPlugin plugin;
   private final DatabaseManager databaseManager;
   private final Map<UUID, Integer> loginAttempts = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lockedPlayers = new ConcurrentHashMap<>();
   private static final long DEFAULT_LOCKOUT_DURATION = 300000L;
   private static final int DEFAULT_MAX_LOGIN_ATTEMPTS = 5;
   private static final int BCRYPT_ROUNDS = 12;

   public UserDataManager(JavaPlugin plugin, DatabaseManager databaseManager) {
      this.plugin = plugin;
      this.databaseManager = databaseManager;
   }

   public boolean canRegister(Player player) {
      String playerIp = this.getPlayerIp(player);
      boolean ipBindingEnabled = CursorMenuPlugin.plugin.isIpBindingEnabled();
      boolean usernameExists = this.databaseManager.isUsernameExists(player.getName());
      if (ipBindingEnabled && this.databaseManager.isIpAlreadyRegistered(playerIp) && !usernameExists) {
         DatabaseManager.UserInfo existingUser = this.databaseManager.getUserInfoByIp(playerIp);
         if (existingUser != null && !existingUser.getUsername().equalsIgnoreCase(player.getName())) {
            return false;
         }
      }

      return true;
   }

   public UserDataManager.RegistrationResult registerUser(Player player, String password) {
      String playerIp = this.getPlayerIp(player);
      if (CursorMenuPlugin.plugin.isIpBlacklistEnabled() && CursorMenuPlugin.plugin.isIpInBlacklist(playerIp)) {
         this.plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试注册，但IP在黑名单中");
         return new UserDataManager.RegistrationResult(false, "ip_blacklisted");
      } else if (CursorMenuPlugin.plugin.isIpWhitelistEnabled() && !CursorMenuPlugin.plugin.isIpInWhitelist(playerIp)) {
         this.plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试注册，但IP不在白名单中");
         return new UserDataManager.RegistrationResult(false, "ip_not_in_whitelist");
      } else if (password != null && !password.isEmpty()) {
         if (password.length() < 6 || password.length() > 12) {
            return new UserDataManager.RegistrationResult(false, "invalid_password_length");
         } else if (!this.isValidPassword(password)) {
            return new UserDataManager.RegistrationResult(false, "weak_password");
         } else {
            boolean ipBindingEnabled = CursorMenuPlugin.plugin.isIpBindingEnabled();
            if (this.databaseManager.isUsernameExists(player.getName())) {
               return new UserDataManager.RegistrationResult(false, "user_exists");
            } else {
               if (ipBindingEnabled && this.databaseManager.isIpAlreadyRegistered(playerIp)) {
                  DatabaseManager.UserInfo existingUser = this.databaseManager.getUserInfoByIp(playerIp);
                  if (existingUser != null && !existingUser.getUsername().equalsIgnoreCase(player.getName())) {
                     return new UserDataManager.RegistrationResult(false, "ip_already_registered");
                  }
               }

               String hashedPassword = this.hashPassword(password);
               if (this.databaseManager.registerUser(player.getName(), hashedPassword, playerIp)) {
                  this.plugin.getLogger().info("玩家 " + player.getName() + " 已注册，IP: " + playerIp);
                  return new UserDataManager.RegistrationResult(true, "success");
               } else {
                  return new UserDataManager.RegistrationResult(false, "failed");
               }
            }
         }
      } else {
         return new UserDataManager.RegistrationResult(false, "empty_password");
      }
   }

   public UserDataManager.AuthenticationResult authenticateUser(Player player, String password) {
      UUID playerId = player.getUniqueId();
      String playerIp = this.getPlayerIp(player);
      if (CursorMenuPlugin.plugin.isIpBlacklistEnabled() && CursorMenuPlugin.plugin.isIpInBlacklist(playerIp)) {
         this.plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试登录，但IP在黑名单中");
         return new UserDataManager.AuthenticationResult(false, "ip_blacklisted");
      } else {
         boolean hasRealOpPermission = player.isOp() && (player.hasPermission("bukkit.command.op") || player.hasPermission("*"));
         if (CursorMenuPlugin.plugin.getAdminIpValidator() != null
            && hasRealOpPermission
            && !CursorMenuPlugin.plugin.getAdminIpValidator().canAdminLogin(player)) {
            this.plugin.getLogger().warning("OP玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试登录，但IP不在管理员白名单中");
            return new UserDataManager.AuthenticationResult(false, "admin_ip_not_allowed");
         } else if (CursorMenuPlugin.plugin.isIpWhitelistEnabled() && !CursorMenuPlugin.plugin.isIpInWhitelist(playerIp)) {
            this.plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试登录，但IP不在白名单中");
            return new UserDataManager.AuthenticationResult(false, "ip_not_in_whitelist");
         } else if (this.isLockedOut(playerId)) {
            long remainingTime = (this.lockedPlayers.get(playerId) - System.currentTimeMillis()) / 1000L;
            return new UserDataManager.AuthenticationResult(false, "locked_out", remainingTime);
         } else if (password != null && !password.isEmpty()) {
            if (!this.databaseManager.isUsernameExists(player.getName())) {
               return new UserDataManager.AuthenticationResult(false, "user_not_found");
            } else {
               String storedHash = this.databaseManager.getStoredPasswordHash(player.getName());
               if (storedHash == null) {
                  return new UserDataManager.AuthenticationResult(false, "user_not_found");
               } else {
                  boolean authenticated = this.verifyPassword(password, storedHash);
                  if (authenticated) {
                     DatabaseManager.UserInfo userInfo = this.databaseManager.getUserInfo(player.getName());
                     if (userInfo == null || !CursorMenuPlugin.plugin.isIpBindingEnabled()) {
                        this.databaseManager.updateLoginInfo(player.getName(), playerIp, true);
                        this.resetLoginAttempts(playerId);
                        return new UserDataManager.AuthenticationResult(true, "success");
                     } else if (userInfo.isSameIp(playerIp)) {
                        if (!CursorMenuPlugin.plugin.isAllowSameIpLogin()) {
                           this.databaseManager.updateLoginInfo(player.getName(), playerIp, false);
                           this.incrementLoginAttempts(playerId);
                           return new UserDataManager.AuthenticationResult(false, "same_ip_login_not_allowed");
                        } else {
                           this.databaseManager.updateLoginInfo(player.getName(), playerIp, true);
                           this.resetLoginAttempts(playerId);
                           return new UserDataManager.AuthenticationResult(true, "success");
                        }
                     } else if (CursorMenuPlugin.plugin.isIpBindingStrict()) {
                        this.databaseManager.updateLoginInfo(player.getName(), playerIp, false);
                        this.incrementLoginAttempts(playerId);
                        String lastIp = userInfo.getLastLoginIp() != null ? userInfo.getLastLoginIp() : userInfo.getRegisterIp();
                        return new UserDataManager.AuthenticationResult(false, "ip_mismatch", lastIp);
                     } else {
                        this.databaseManager.updateLoginInfo(player.getName(), playerIp, true);
                        this.resetLoginAttempts(playerId);
                        player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("system.ip_warning", "&e[警告] 您的IP与注册时不同！"));
                        return new UserDataManager.AuthenticationResult(true, "success_with_ip_warning");
                     }
                  } else {
                     this.databaseManager.updateLoginInfo(player.getName(), playerIp, false);
                     this.incrementLoginAttempts(playerId);
                     return new UserDataManager.AuthenticationResult(false, "wrong_password");
                  }
               }
            }
         } else {
            this.incrementLoginAttempts(playerId);
            return new UserDataManager.AuthenticationResult(false, "empty_password");
         }
      }
   }

   private void incrementLoginAttempts(UUID playerId) {
      int attempts = this.loginAttempts.getOrDefault(playerId, 0) + 1;
      this.loginAttempts.put(playerId, attempts);
      int maxAttempts = this.getMaxLoginAttempts();
      long lockoutDuration = this.getLockoutDurationMillis();
      if (attempts >= maxAttempts) {
         this.lockedPlayers.put(playerId, System.currentTimeMillis() + lockoutDuration);
         this.plugin.getLogger().warning("玩家 " + playerId + " 因登录尝试次数过多被锁定 " + lockoutDuration / 1000L + " 秒");
      }

      if (this.databaseManager != null) {
         this.databaseManager.updateLoginAttempts(Bukkit.getPlayer(playerId) != null ? Bukkit.getPlayer(playerId).getName() : playerId.toString(), attempts);
      }
   }

   private void resetLoginAttempts(UUID playerId) {
      this.loginAttempts.remove(playerId);
      this.lockedPlayers.remove(playerId);
      if (this.databaseManager != null) {
         String playerName = Bukkit.getPlayer(playerId) != null ? Bukkit.getPlayer(playerId).getName() : playerId.toString();
         this.databaseManager.updateLoginAttempts(playerName, 0);
      }
   }

   private boolean isLockedOut(UUID playerId) {
      Long lockoutEnd = this.lockedPlayers.get(playerId);
      if (lockoutEnd == null) {
         return false;
      } else if (System.currentTimeMillis() > lockoutEnd) {
         this.lockedPlayers.remove(playerId);
         return false;
      } else {
         return true;
      }
   }

   public int getRemainingAttempts(UUID playerId) {
      int attempts = this.loginAttempts.getOrDefault(playerId, 0);
      return Math.max(0, this.getMaxLoginAttempts() - attempts);
   }

   private int getMaxLoginAttempts() {
      return CursorMenuPlugin.plugin == null ? 5 : Math.max(1, CursorMenuPlugin.plugin.getMaxLoginAttempts());
   }

   private long getLockoutDurationMillis() {
      return CursorMenuPlugin.plugin == null ? 300000L : Math.max(1L, (long)CursorMenuPlugin.plugin.getLockoutDuration()) * 60000L;
   }

   public boolean isUserRegistered(String username) {
      return this.databaseManager.isUserRegistered(username);
   }

   public String hashPasswordForTest(String password) {
      return this.hashPassword(password);
   }

   private String hashPassword(String password) {
      if (password == null) {
         this.plugin.getLogger().warning("尝试哈希空密码");
         return "";
      } else {
         try {
            return BCrypt.hashpw(password, BCrypt.gensalt(12));
         } catch (Exception var3) {
            this.plugin.getLogger().severe("无法创建密码哈希: " + var3.getMessage());
            return "";
         }
      }
   }

   private boolean verifyPassword(String password, String hash) {
      if (password == null || hash == null || hash.isEmpty()) {
         return false;
      } else if (!hash.startsWith("$2a$") && !hash.startsWith("$2b$") && !hash.startsWith("$2y$")) {
         return this.verifyLegacyPassword(password, hash);
      } else {
         try {
            return BCrypt.checkpw(password, hash);
         } catch (Exception var4) {
            this.plugin.getLogger().warning("密码验证失败: " + var4.getMessage());
            return false;
         }
      }
   }

   private boolean verifyLegacyPassword(String password, String hash) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         byte[] hashedBytes = md.digest(password.getBytes());
         StringBuilder sb = new StringBuilder();

         for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
         }

         return sb.toString().equals(hash);
      } catch (NoSuchAlgorithmException var10) {
         this.plugin.getLogger().severe("无法验证旧密码格式: " + var10.getMessage());
         return false;
      }
   }

   private boolean isValidPassword(String password) {
      return password != null && password.length() >= 6 ? password.matches("^[a-zA-Z0-9]+$") : false;
   }

   private String getPlayerIp(Player player) {
      return player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
   }

   public void cleanupPlayer(UUID playerId) {
      this.loginAttempts.remove(playerId);
      this.lockedPlayers.remove(playerId);
   }

   public static class AuthenticationResult {
      private final boolean success;
      private final String messageKey;
      private final Object extraData;

      public AuthenticationResult(boolean success, String messageKey, Object extraData) {
         this.success = success;
         this.messageKey = messageKey;
         this.extraData = extraData;
      }

      public AuthenticationResult(boolean success, String messageKey) {
         this(success, messageKey, null);
      }

      public boolean isSuccess() {
         return this.success;
      }

      public String getMessageKey() {
         return this.messageKey;
      }

      public Object getExtraData() {
         return this.extraData;
      }
   }

   public static class RegistrationResult {
      private final boolean success;
      private final String messageKey;
      private final Object[] args;

      public RegistrationResult(boolean success, String messageKey, Object... args) {
         this.success = success;
         this.messageKey = messageKey;
         this.args = args;
      }

      public boolean isSuccess() {
         return this.success;
      }

      public String getMessageKey() {
         return this.messageKey;
      }

      public Object[] getArgs() {
         return this.args;
      }
   }
}
