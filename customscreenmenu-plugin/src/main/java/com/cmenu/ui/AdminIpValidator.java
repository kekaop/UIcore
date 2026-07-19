package com.cmenu.ui;

import java.net.InetAddress;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AdminIpValidator {
   private final JavaPlugin plugin;
   private static boolean adminIpCheckEnabled = false;
   private static List<String> adminIpWhitelist = null;
   private static final ThreadLocal<Boolean> isTemporaryOpOperation = ThreadLocal.withInitial(() -> false);

   public AdminIpValidator(JavaPlugin plugin) {
      this.plugin = plugin;
      this.loadConfig();
   }

   public void loadConfig() {
      adminIpCheckEnabled = this.plugin.getConfig().getBoolean("ip-binding.admin-ip-whitelist.enabled", false);
      adminIpWhitelist = this.plugin.getConfig().getStringList("ip-binding.admin-ip-whitelist.ips");
      this.plugin.getLogger().info("管理员IP白名单配置已加载 - 启用: " + adminIpCheckEnabled + ", IP数: " + adminIpWhitelist.size());
   }

   public boolean canAdminLogin(Player player) {
      if (!adminIpCheckEnabled) {
         return true;
      } else if (isInTemporaryOpOperation()) {
         return true;
      } else {
         boolean isRealOp = player.isOp() && (player.hasPermission("bukkit.command.op") || player.hasPermission("*"));
         if (!isRealOp) {
            return true;
         } else {
            String playerIp = this.getPlayerIp(player);
            this.plugin.getLogger().info("正在检查OP玩家 " + player.getName() + " 的登录IP: " + playerIp);
            if (adminIpWhitelist != null && !adminIpWhitelist.isEmpty()) {
               for (String allowedIp : adminIpWhitelist) {
                  allowedIp = allowedIp.trim();
                  if (!allowedIp.isEmpty()) {
                     if (allowedIp.equals(playerIp)) {
                        this.plugin.getLogger().info("OP玩家 " + player.getName() + " 的IP " + playerIp + " 在白名单中，允许登录");
                        return true;
                     }

                     if (allowedIp.contains("/") && this.matchesCidr(playerIp, allowedIp)) {
                        this.plugin.getLogger().info("OP玩家 " + player.getName() + " 的IP " + playerIp + " 匹配CIDR " + allowedIp + "，允许登录");
                        return true;
                     }
                  }
               }

               this.plugin.getLogger().warning("OP玩家 " + player.getName() + " 尝试使用IP " + playerIp + " 登录，但该IP不在管理员IP白名单中");
               return false;
            } else {
               this.plugin.getLogger().warning("管理员IP白名单为空，允许OP玩家 " + player.getName() + " 登录");
               return true;
            }
         }
      }
   }

   private String getPlayerIp(Player player) {
      return player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
   }

   private boolean matchesCidr(String ip, String cidr) {
      try {
         String[] parts = cidr.split("/");
         if (parts.length != 2) {
            return false;
         } else {
            String networkAddress = parts[0].trim();
            int prefixLength = Integer.parseInt(parts[1].trim());
            byte[] ipBytes = this.ipToBytes(ip);
            byte[] networkBytes = this.ipToBytes(networkAddress);
            if (ipBytes != null && networkBytes != null && ipBytes.length == networkBytes.length) {
               int totalBits = ipBytes.length * 8;
               if (prefixLength >= 0 && prefixLength <= totalBits) {
                  int remainingBits = prefixLength;

                  for (int i = 0; i < ipBytes.length; i++) {
                     int ipBit = ipBytes[i] < 0 ? ipBytes[i] + 256 : ipBytes[i];
                     int networkBit = networkBytes[i] < 0 ? networkBytes[i] + 256 : networkBytes[i];
                     if (remainingBits <= 0) {
                        break;
                     }

                     int bitsInByte = Math.min(8, remainingBits);
                     int mask = 255 << 8 - bitsInByte & 0xFF;
                     if ((ipBit & mask) != (networkBit & mask)) {
                        return false;
                     }

                     remainingBits -= bitsInByte;
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } catch (Exception var15) {
         this.plugin.getLogger().warning("CIDR格式解析错误: " + cidr + ", 错误信息: " + var15.getMessage());
         return false;
      }
   }

   private byte[] ipToBytes(String ip) {
      if (ip != null && !ip.isEmpty()) {
         try {
            return InetAddress.getByName(ip.trim()).getAddress();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean isAdminIpCheckEnabled() {
      return adminIpCheckEnabled;
   }

   public List<String> getAdminIpWhitelist() {
      return adminIpWhitelist;
   }

   public static void beginTemporaryOpOperation() {
      isTemporaryOpOperation.set(true);
   }

   public static void endTemporaryOpOperation() {
      isTemporaryOpOperation.remove();
   }

   private static boolean isInTemporaryOpOperation() {
      return Boolean.TRUE.equals(isTemporaryOpOperation.get());
   }
}
