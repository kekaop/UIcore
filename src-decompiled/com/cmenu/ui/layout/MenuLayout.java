package com.cmenu.ui.layout;

import com.cmenu.ui.CursorMenuPlugin;
import com.cmenu.ui.util.CommandUtils;
import java.util.List;
import java.util.Random;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class MenuLayout {
   public final String key;
   public String name;
   private final List<String> command;
   private final boolean stop;
   public final double x;
   public final double y;
   public final double z;
   private boolean isClick = false;
   private final boolean teleportBool;
   private final boolean teleportOriginal;
   private final Location teleportLoc;
   private final boolean stopCommandBool;
   private final List<String> stopCommands;
   public final float tiltX;
   public final float tiltY;
   public final float tiltZ;
   private final String permission;
   private final boolean nextMenuEnabled;
   private final String nextMenuKey;
   private long commandDelay = 20L;
   private boolean useCommandDelay = false;
   private final String linkCommand;
   private String conditionVariable;
   private String conditionOperator;
   private String conditionValue;
   private List<String> randomCommands;
   private List<Integer> randomChances;
   private boolean useRandomCommands;
   private boolean hoverEnlargeEnabled;
   private double hoverEnlargeScale;
   private double textSize = 1.0;
   private boolean typewriterEnabled = false;
   private int typewriterSpeed = 2;
   private int typewriterStartDelay = 0;
   private String typewriterCursorChar = "|";
   private String hoverSound = null;
   private float hoverSoundVolume = 1.0F;
   private float hoverSoundPitch = 1.0F;
   private long hoverSoundCooldown = 20L;
   private List<String> hoverCommands;
   private List<String> leaveCommands;
   private boolean floatingAnimationEnabled = false;
   private String floatingAnimationType = "none";
   private double floatingAnimationSpeed = 1.0;
   private double floatingAnimationAmplitude = 0.25;

   public MenuLayout(
      String var1,
      String var2,
      List<String> var3,
      boolean var4,
      double var5,
      double var7,
      double var9,
      boolean var11,
      boolean var12,
      Location var13,
      boolean var14,
      List<String> var15,
      float var16,
      float var17,
      float var18,
      String var19,
      boolean var20,
      String var21
   ) {
      this(var1, var2, var3, var4, var5, var7, var9, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20, var21, "");
   }

   public MenuLayout(
      String var1,
      String var2,
      List<String> var3,
      boolean var4,
      double var5,
      double var7,
      double var9,
      boolean var11,
      boolean var12,
      Location var13,
      boolean var14,
      List<String> var15,
      float var16,
      float var17,
      float var18,
      String var19,
      boolean var20,
      String var21,
      String var22
   ) {
      this.key = var1;
      this.name = var2;
      this.command = var3;
      this.stop = var4;
      this.x = var5;
      this.y = var7;
      this.z = var9;
      this.isClick = false;
      this.teleportBool = var11;
      this.teleportLoc = var13;
      this.stopCommandBool = var14;
      this.stopCommands = var15;
      this.teleportOriginal = var12;
      this.tiltX = var16;
      this.tiltY = var17;
      this.tiltZ = var18;
      this.permission = var19;
      this.nextMenuEnabled = var20;
      this.nextMenuKey = var21;
      this.commandDelay = 20L;
      this.linkCommand = var22;
   }

   public void setCommandDelay(long var1) {
      this.commandDelay = var1;
      this.useCommandDelay = var1 > 0L;
   }

   public boolean isHoverEnlargeEnabled() {
      return this.hoverEnlargeEnabled;
   }

   public double getHoverEnlargeScale() {
      return this.hoverEnlargeScale;
   }

   public double getTextSize() {
      return this.textSize;
   }

   public boolean isTypewriterEnabled() {
      return this.typewriterEnabled;
   }

   public int getTypewriterSpeed() {
      return this.typewriterSpeed;
   }

   public int getTypewriterStartDelay() {
      return this.typewriterStartDelay;
   }

   public String getTypewriterCursorChar() {
      return this.typewriterCursorChar;
   }

   public String getHoverSound() {
      return this.hoverSound;
   }

   public float getHoverSoundVolume() {
      return this.hoverSoundVolume;
   }

   public float getHoverSoundPitch() {
      return this.hoverSoundPitch;
   }

   public long getHoverSoundCooldown() {
      return this.hoverSoundCooldown;
   }

   public List<String> getHoverCommands() {
      return this.hoverCommands;
   }

   public List<String> getLeaveCommands() {
      return this.leaveCommands;
   }

   public boolean hasHoverCommands() {
      return this.hoverCommands != null && !this.hoverCommands.isEmpty();
   }

   public boolean hasLeaveCommands() {
      return this.leaveCommands != null && !this.leaveCommands.isEmpty();
   }

   public boolean isFloatingAnimationEnabled() {
      return this.floatingAnimationEnabled;
   }

   public String getFloatingAnimationType() {
      return this.floatingAnimationType;
   }

   public double getFloatingAnimationSpeed() {
      return this.floatingAnimationSpeed;
   }

   public double getFloatingAnimationAmplitude() {
      return this.floatingAnimationAmplitude;
   }

   public List<String> getCommands() {
      return this.command;
   }

   public void loadConfig(ConfigurationSection var1) {
      if (var1.contains("command-delay")) {
         this.setCommandDelay(var1.getLong("command-delay", 20L));
      }

      if (var1.contains("condition.variable") && var1.contains("condition.operator") && var1.contains("condition.value")) {
         this.conditionVariable = var1.getString("condition.variable");
         this.conditionOperator = var1.getString("condition.operator");
         this.conditionValue = var1.getString("condition.value");
      }

      if (var1.contains("random-commands") && var1.contains("random-chances")) {
         this.randomCommands = var1.getStringList("random-commands");
         this.randomChances = var1.getIntegerList("random-chances");
         this.useRandomCommands = this.randomCommands.size() == this.randomChances.size() && !this.randomCommands.isEmpty();
      }

      if (var1.contains("hover-enlarge.enabled")) {
         this.hoverEnlargeEnabled = var1.getBoolean("hover-enlarge.enabled");
         this.hoverEnlargeScale = var1.getDouble("hover-enlarge.scale", 1.2);
      }

      if (var1.contains("text-size")) {
         this.textSize = var1.getDouble("text-size", 1.0);
      }

      if (var1.contains("typewriter")) {
         ConfigurationSection var2 = var1.getConfigurationSection("typewriter");
         if (var2 != null) {
            this.typewriterEnabled = var2.getBoolean("enabled", false);
            this.typewriterSpeed = Math.max(1, var2.getInt("speed", 2));
            this.typewriterStartDelay = Math.max(0, var2.getInt("start-delay", 0));
            this.typewriterCursorChar = var2.getString("cursor-char", "|");
         }
      }

      if (var1.contains("hover-sound")) {
         ConfigurationSection var4 = var1.getConfigurationSection("hover-sound");
         if (var4 != null) {
            String var3 = var4.getString("name");
            if (var3 != null && !var3.isEmpty()) {
               this.hoverSound = var3;
               this.hoverSoundVolume = (float)var4.getDouble("volume", 1.0);
               this.hoverSoundPitch = (float)var4.getDouble("pitch", 1.0);
               this.hoverSoundCooldown = Math.max(5L, var4.getLong("cooldown", 20L));
            }
         }
      }

      if (var1.contains("hover-commands")) {
         this.hoverCommands = var1.getStringList("hover-commands");
      }

      if (var1.contains("leave-commands")) {
         this.leaveCommands = var1.getStringList("leave-commands");
      }

      if (var1.contains("floating-animation")) {
         ConfigurationSection var5 = var1.getConfigurationSection("floating-animation");
         if (var5 != null) {
            this.floatingAnimationEnabled = var5.getBoolean("enabled", false);
            this.floatingAnimationType = var5.getString("type", "up-down");
            this.floatingAnimationSpeed = Math.max(0.01, var5.getDouble("speed", 1.0));
            this.floatingAnimationAmplitude = Math.max(0.0, var5.getDouble("amplitude", 0.25));
         }
      }
   }

   public void runCommand(Player var1) {
      if (!this.permission.isEmpty() && !var1.hasPermission(this.permission) && !var1.isOp()) {
         var1.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission_button", "&c[CursorMenu] 你没有权限执行该按钮"));
      } else if (CursorMenuPlugin.plugin.requiresLogin(this.name, this.key) && !CursorMenuPlugin.plugin.isPlayerLoggedIn(var1)) {
         CursorMenuPlugin.plugin.sendActionBarMessage(var1, CursorMenuPlugin.plugin.getLangMessage("menu.require_login", "&c[CursorMenu] 请先登录后再执行此操作"));
      } else if (CursorMenuPlugin.plugin.requiresNotRegistered(this.name, this.key)
         && CursorMenuPlugin.plugin.getUserDataManager().isUserRegistered(var1.getName())) {
         CursorMenuPlugin.plugin.sendActionBarMessage(var1, CursorMenuPlugin.plugin.getLangMessage("menu.already_registered", "&c[CursorMenu] 您已注册，请直接登录"));
         CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var1x -> CursorMenuPlugin.plugin.setupCursor(var1, "登录菜单"), null, 5L);
      } else if (this.conditionVariable != null && this.conditionOperator != null && this.conditionValue != null && !this.checkCondition(var1)) {
         var1.sendMessage(CursorMenuPlugin.plugin.getLangMessage("menu.condition_not_met", "&c[CursorMenu] 条件不满足，无法执行该操作"));
      } else {
         if (this.stop && (!this.nextMenuEnabled || this.nextMenuKey.isEmpty()) && var1.isInsideVehicle()) {
            Entity var2 = var1.getVehicle();
            if (var2 != null) {
               var2.removePassenger(var1);
               var2.remove();
            }
         }

         CursorMenuPlugin.plugin.selectedLayouts.put(var1, this);
         if (!this.isClick) {
            this.isClick = true;
            CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var1x -> this.isClick = false, null, 1L);
            if (CursorMenuPlugin.plugin.getCurrentPlayerMenu(var1) != null
               && (
                  CursorMenuPlugin.plugin.getCurrentPlayerMenu(var1).equals("login_menu")
                     || CursorMenuPlugin.plugin.getCurrentPlayerMenu(var1).equals("register_menu")
                     || CursorMenuPlugin.plugin.getCurrentPlayerMenu(var1).equals("login")
               )
               && (!this.nextMenuEnabled || this.nextMenuKey.isEmpty())) {
               String var6 = this.key;
               byte var3 = -1;
               switch (var6.hashCode()) {
                  case 989304983:
                     if (var6.equals("cancel_button")) {
                        var3 = 0;
                     }
                  default:
                     switch (var3) {
                        case 0:
                           CursorMenuPlugin.plugin.stopCursor(var1, true);
                     }
               }
            }

            if (this.linkCommand != null && !this.linkCommand.isEmpty()) {
               this.sendClickableLink(var1, this.linkCommand);
            } else if (this.useRandomCommands) {
               this.executeRandomCommands(var1);
            } else {
               for (String var8 : this.command) {
                  String var4 = var8;
                  if (CursorMenuPlugin.hasPAPI) {
                     var4 = PlaceholderAPI.setPlaceholders(var1, var8);
                  }

                  String var5 = var4;
                  if (this.useCommandDelay) {
                     CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var3x -> {
                        if (CursorMenuPlugin.playerCursors.containsKey(var1)) {
                           this.dispatchCommand(var1, var5);
                        }
                     }, null, this.commandDelay);
                  } else {
                     this.dispatchCommand(var1, var5);
                  }
               }
            }

            if (this.nextMenuEnabled && !this.nextMenuKey.isEmpty()) {
               CursorMenuPlugin.foliaLib
                  .scheduling()
                  .entitySpecificScheduler(var1)
                  .runDelayed(var2x -> CursorMenuPlugin.plugin.setupCursor(var1, this.nextMenuKey), null, 5L);
            } else {
               if (this.stop) {
                  CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var2x -> {
                     boolean var3x = this.teleportBool && this.teleportOriginal;
                     CursorMenuPlugin.plugin.stopCursor(var1, var3x);
                     this.teleport(var1);
                     this.runStopCommand(var1);
                  }, null, 2L);
               }

               CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var1x -> this.isClick = false, null, 15L);
            }
         }
      }
   }

   private void sendClickableLink(Player var1, String var2) {
      if (var2.startsWith("[link]")) {
         String var3 = var2.substring(6);
         String[] var4 = var3.split("\\|", 2);
         String var5 = var4[0];
         String var6 = var4.length > 1 ? var4[1] : var5;
         TextComponent var7 = new TextComponent(ChatColor.translateAlternateColorCodes('&', var6));
         var7.setClickEvent(new ClickEvent(Action.OPEN_URL, var5));
         var1.spigot().sendMessage(ChatMessageType.CHAT, var7);
      } else {
         String var8 = ChatColor.translateAlternateColorCodes('&', var2);
         var1.spigot().sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(var8));
      }
   }

   private void executeRandomCommands(Player var1) {
      Random var2 = new Random();
      int var3 = 0;

      for (int var5 : this.randomChances) {
         var3 += var5;
      }

      if (var3 > 0) {
         int var10 = var2.nextInt(var3) + 1;
         int var11 = 0;

         for (int var6 = 0; var6 < this.randomCommands.size(); var6++) {
            var11 += this.randomChances.get(var6);
            if (var10 <= var11) {
               String var7 = this.randomCommands.get(var6);
               String var8 = var7;
               if (CursorMenuPlugin.hasPAPI) {
                  var8 = PlaceholderAPI.setPlaceholders(var1, var7);
               }

               String var9 = var8;
               if (this.useCommandDelay) {
                  CursorMenuPlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var3x -> {
                     if (CursorMenuPlugin.playerCursors.containsKey(var1)) {
                        this.dispatchCommand(var1, var9);
                     }
                  }, null, this.commandDelay);
               } else {
                  this.dispatchCommand(var1, var9);
               }
               break;
            }
         }
      }
   }

   private void dispatchCommand(Player var1, String var2) {
      CommandUtils.executeCommand(var1, var2);
   }

   private void connectToServer(Player var1, String var2) {
      CommandUtils.connectToServer(var1, var2);
   }

   private void teleport(Player var1) {
      if (this.teleportBool) {
         Location var2;
         if (this.teleportOriginal) {
            var2 = CursorMenuPlugin.playerLocations.get(var1);
         } else {
            var2 = this.teleportLoc.clone();
         }

         if (var2 != null && var2.getWorld() != null) {
            var2.setYaw(CursorMenuPlugin.exitYaw);
            var2.setPitch(CursorMenuPlugin.exitPitch);
            World var5 = var2.getWorld();
            if (var5 != null) {
               var1.teleport(var2);
            } else {
               Location var4 = var1.getLocation().clone();
               var4.setYaw(CursorMenuPlugin.exitYaw);
               var4.setPitch(CursorMenuPlugin.exitPitch);
               var1.teleport(var4);
            }
         } else {
            Location var3 = var1.getLocation();
            var3.setYaw(CursorMenuPlugin.exitYaw);
            var3.setPitch(CursorMenuPlugin.exitPitch);
            var1.teleport(var3);
         }
      }
   }

   private void runStopCommand(Player var1) {
      if (this.stopCommandBool) {
         for (String var3 : this.stopCommands) {
            CommandUtils.executeCommand(var1, var3);
         }
      }
   }

   private boolean checkCondition(Player var1) {
      if (!CursorMenuPlugin.hasPAPI) {
         return true;
      } else {
         String var2 = PlaceholderAPI.setPlaceholders(var1, this.conditionVariable);

         try {
            double var3 = Double.parseDouble(var2);
            double var5 = Double.parseDouble(this.conditionValue);
            String var7 = this.conditionOperator;
            switch (var7) {
               case ">":
                  return var3 > var5;
               case ">=":
                  return var3 >= var5;
               case "<":
                  return var3 < var5;
               case "<=":
                  return var3 <= var5;
               case "==":
                  return var3 == var5;
               case "!=":
                  return var3 != var5;
               default:
                  return false;
            }
         } catch (NumberFormatException var10) {
            String var4 = this.conditionOperator;
            switch (var4) {
               case "==":
                  return var2.equals(this.conditionValue);
               case "!=":
                  return !var2.equals(this.conditionValue);
               default:
                  return false;
            }
         }
      }
   }

   private boolean requiresLogin() {
      return false;
   }

   private boolean requiresNotRegistered() {
      return false;
   }
}
