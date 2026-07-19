package com.cmenu.ui;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
   private final JavaPlugin plugin;
   private Connection connection;

   public DatabaseManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.initializeDatabase();
   }

   private synchronized void initializeDatabase() {
      try {
         File dataFolder = this.plugin.getDataFolder();
         if (!dataFolder.exists()) {
            dataFolder.mkdirs();
         }

         String url = "jdbc:sqlite:" + new File(this.plugin.getDataFolder(), "users.db").getAbsolutePath();
         this.connection = DriverManager.getConnection(url);

         try (Statement stmt = this.connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT NOT NULL UNIQUE,password_hash TEXT NOT NULL,registered BOOLEAN NOT NULL DEFAULT 0,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,register_ip TEXT NOT NULL DEFAULT '',last_login_ip TEXT,login_attempts INTEGER DEFAULT 0,last_login_time TIMESTAMP)";
            stmt.execute(sql);
         }

         try (Statement stmt = this.connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)");
            boolean hasRegisterIp = false;
            boolean hasLastLoginIp = false;

            while (rs.next()) {
               String columnName = rs.getString("name");
               if ("register_ip".equals(columnName)) {
                  hasRegisterIp = true;
               }

               if ("last_login_ip".equals(columnName)) {
                  hasLastLoginIp = true;
               }
            }

            if (!hasRegisterIp) {
               stmt.execute("ALTER TABLE users ADD COLUMN register_ip TEXT NOT NULL DEFAULT ''");
               this.plugin.getLogger().info("已添加 register_ip 列到用户表");
            }

            if (!hasLastLoginIp) {
               stmt.execute("ALTER TABLE users ADD COLUMN last_login_ip TEXT");
               this.plugin.getLogger().info("已添加 last_login_ip 列到用户表");
            }

            try {
               stmt.execute("ALTER TABLE users ADD COLUMN login_attempts INTEGER DEFAULT 0");
            } catch (SQLException var11) {
            }

            try {
               stmt.execute("ALTER TABLE users ADD COLUMN last_login_time TIMESTAMP");
            } catch (SQLException var10) {
            }
         }

         this.plugin.getLogger().info("用户数据库初始化成功（增强版 - IP绑定）");
      } catch (SQLException var14) {
         this.plugin.getLogger().severe("初始化数据库时出错: " + var14.getMessage());
      }
   }

   private synchronized Connection getConnection() throws SQLException {
      if (this.connection == null || this.connection.isClosed()) {
         String url = "jdbc:sqlite:" + new File(this.plugin.getDataFolder(), "users.db").getAbsolutePath();
         this.connection = DriverManager.getConnection(url);
         this.plugin.getLogger().info("重新建立数据库连接成功");
      }

      return this.connection;
   }

   public synchronized boolean isUserRegistered(String username) {
      String sql = "SELECT registered FROM users WHERE username = ?";

      try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
         pstmt.setString(1, username);
         ResultSet rs = pstmt.executeQuery();
         return rs.next() ? rs.getBoolean("registered") : false;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("查询用户注册状态时出错: " + var8.getMessage());
         return false;
      }
   }

   public synchronized boolean isUsernameExists(String username) {
      String sql = "SELECT id FROM users WHERE username = ?";

      try {
         boolean var5;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            var5 = rs.next();
         }

         return var5;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("检查用户名是否存在时出错: " + var8.getMessage());
         return false;
      }
   }

   public synchronized boolean isIpAlreadyRegistered(String registerIp) {
      String sql = "SELECT id FROM users WHERE register_ip = ? AND registered = 1";

      try {
         boolean var5;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, registerIp);
            ResultSet rs = pstmt.executeQuery();
            var5 = rs.next();
         }

         return var5;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("检查IP注册状态时出错: " + var8.getMessage());
         return false;
      }
   }

   public synchronized boolean registerUser(String username, String passwordHash, String registerIp) {
      String sql = "INSERT INTO users(username, password_hash, registered, register_ip) VALUES(?, ?, ?, ?)";

      try {
         boolean var7;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setBoolean(3, true);
            pstmt.setString(4, registerIp);
            int affectedRows = pstmt.executeUpdate();
            var7 = affectedRows > 0;
         }

         return var7;
      } catch (SQLException var10) {
         this.plugin.getLogger().severe("注册用户时出错: " + var10.getMessage());
         return false;
      }
   }

   public synchronized boolean authenticateUser(String username, String passwordHash) {
      String sql = "SELECT password_hash FROM users WHERE username = ? AND registered = ?";

      try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
         pstmt.setString(1, username);
         pstmt.setBoolean(2, true);
         ResultSet rs = pstmt.executeQuery();
         if (rs.next()) {
            String storedHash = rs.getString("password_hash");
            return storedHash.equals(passwordHash);
         } else {
            return false;
         }
      } catch (SQLException var10) {
         this.plugin.getLogger().severe("验证用户时出错: " + var10.getMessage());
         return false;
      }
   }

   public synchronized String getStoredPasswordHash(String username) {
      String sql = "SELECT password_hash FROM users WHERE username = ? AND registered = ?";

      try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
         pstmt.setString(1, username);
         pstmt.setBoolean(2, true);
         ResultSet rs = pstmt.executeQuery();
         return rs.next() ? rs.getString("password_hash") : null;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("获取用户密码哈希时出错: " + var8.getMessage());
         return null;
      }
   }

   public synchronized DatabaseManager.UserInfo getUserInfo(String username) {
      String sql = "SELECT username, register_ip, last_login_ip, login_attempts, last_login_time FROM users WHERE username = ? AND registered = 1";

      try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
         pstmt.setString(1, username);
         ResultSet rs = pstmt.executeQuery();
         return rs.next()
            ? new DatabaseManager.UserInfo(
               rs.getString("username"),
               rs.getString("register_ip"),
               rs.getString("last_login_ip"),
               rs.getInt("login_attempts"),
               rs.getTimestamp("last_login_time")
            )
            : null;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("获取用户信息时出错: " + var8.getMessage());
         return null;
      }
   }

   public synchronized DatabaseManager.UserInfo getUserInfoByIp(String ip) {
      String sql = "SELECT username, register_ip, last_login_ip, login_attempts, last_login_time FROM users WHERE register_ip = ? AND registered = 1 LIMIT 1";

      try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
         pstmt.setString(1, ip);
         ResultSet rs = pstmt.executeQuery();
         return rs.next()
            ? new DatabaseManager.UserInfo(
               rs.getString("username"),
               rs.getString("register_ip"),
               rs.getString("last_login_ip"),
               rs.getInt("login_attempts"),
               rs.getTimestamp("last_login_time")
            )
            : null;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("根据IP获取用户信息时出错: " + var8.getMessage());
         return null;
      }
   }

   public synchronized boolean updateLoginInfo(String username, String currentIp, boolean success) {
      String sql = "UPDATE users SET last_login_ip = ?, login_attempts = ?, last_login_time = CURRENT_TIMESTAMP WHERE username = ?";

      try {
         boolean var6;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, currentIp);
            pstmt.setInt(2, success ? 0 : -1);
            pstmt.setString(3, username);
            var6 = pstmt.executeUpdate() > 0;
         }

         return var6;
      } catch (SQLException var10) {
         this.plugin.getLogger().severe("更新登录信息时出错: " + var10.getMessage());
         return false;
      }
   }

   public synchronized boolean updateLoginAttempts(String username, int attempts) {
      String sql = "UPDATE users SET login_attempts = ? WHERE username = ?";

      try {
         boolean var5;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, attempts);
            pstmt.setString(2, username);
            var5 = pstmt.executeUpdate() > 0;
         }

         return var5;
      } catch (SQLException var9) {
         this.plugin.getLogger().severe("更新登录尝试次数时出错: " + var9.getMessage());
         return false;
      }
   }

   public synchronized int getLoginAttempts(String username) {
      String sql = "SELECT login_attempts FROM users WHERE username = ?";

      try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
         pstmt.setString(1, username);
         ResultSet rs = pstmt.executeQuery();
         return rs.next() ? rs.getInt("login_attempts") : 0;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("获取登录尝试次数时出错: " + var8.getMessage());
         return 0;
      }
   }

   public synchronized boolean updateUserPassword(String username, String newPasswordHash) {
      String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

      try {
         boolean var6;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, username);
            int affectedRows = pstmt.executeUpdate();
            var6 = affectedRows > 0;
         }

         return var6;
      } catch (SQLException var9) {
         this.plugin.getLogger().severe("更新用户密码时出错: " + var9.getMessage());
         return false;
      }
   }

   public synchronized boolean deleteUser(String username) {
      String sql = "DELETE FROM users WHERE username = ?";

      try {
         boolean var5;
         try (PreparedStatement pstmt = this.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            var5 = affectedRows > 0;
         }

         return var5;
      } catch (SQLException var8) {
         this.plugin.getLogger().severe("删除用户时出错: " + var8.getMessage());
         return false;
      }
   }

   public synchronized void closeConnection() {
      try {
         if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
         }

         this.connection = null;
      } catch (SQLException var2) {
         this.plugin.getLogger().severe("关闭数据库连接时出错: " + var2.getMessage());
      }
   }

   public static class UserInfo {
      private final String username;
      private final String registerIp;
      private final String lastLoginIp;
      private final int loginAttempts;
      private final Timestamp lastLoginTime;

      public UserInfo(String username, String registerIp, String lastLoginIp, int loginAttempts, Timestamp lastLoginTime) {
         this.username = username;
         this.registerIp = registerIp;
         this.lastLoginIp = lastLoginIp;
         this.loginAttempts = loginAttempts;
         this.lastLoginTime = lastLoginTime;
      }

      public String getUsername() {
         return this.username;
      }

      public String getRegisterIp() {
         return this.registerIp;
      }

      public String getLastLoginIp() {
         return this.lastLoginIp;
      }

      public int getLoginAttempts() {
         return this.loginAttempts;
      }

      public Timestamp getLastLoginTime() {
         return this.lastLoginTime;
      }

      public boolean isSameIp(String currentIp) {
         return this.registerIp != null && !this.registerIp.isEmpty() && this.registerIp.equals(currentIp);
      }
   }
}
