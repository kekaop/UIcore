package com.cmenu.ui.wasd;

import java.nio.charset.StandardCharsets;
import java.util.List;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class WASDSession {
   private final JavaPlugin plugin;
   private final Player player;
   private boolean enabled;
   private TextDisplay[] textDisplays;
   private double[] originalScales;
   private List<Double> configScales;

   public WASDSession(JavaPlugin plugin, Player player, boolean enabled) {
      this.plugin = plugin;
      this.player = player;
      this.enabled = enabled;
   }

   public void setTextDisplays(TextDisplay[] displays, List<Double> scales) {
      this.textDisplays = displays;
      this.configScales = scales;
      this.originalScales = new double[displays.length];

      for (int i = 0; i < displays.length; i++) {
         this.originalScales[i] = scales.get(i);
      }
   }

   public void onSelectionChanged(Player player, int oldIndex, int newIndex) {
      if (this.textDisplays != null) {
         if (oldIndex >= 0 && oldIndex < this.textDisplays.length && this.textDisplays[oldIndex] != null) {
            double oldScale = this.configScales != null && oldIndex < this.configScales.size() ? this.configScales.get(oldIndex) : 1.0;
            this.setDisplayScale(this.textDisplays[oldIndex], oldScale);
         }

         if (newIndex >= 0 && newIndex < this.textDisplays.length && this.textDisplays[newIndex] != null) {
            double newScale = this.configScales != null && newIndex < this.configScales.size() ? this.configScales.get(newIndex) + 0.5 : 1.5;
            this.setDisplayScale(this.textDisplays[newIndex], newScale);
         }

         this.playSelectionSound(player);
      }
   }

   private void setDisplayScale(TextDisplay display, double scale) {
      if (display != null && display.isValid()) {
         try {
            display.setTransformation(
               new Transformation(new Vector3f(-1.0F, 0.0F, 0.0F), new AxisAngle4f(), new Vector3f((float)scale, (float)scale, (float)scale), new AxisAngle4f())
            );
         } catch (Exception var5) {
         }
      }
   }

   private void playSelectionSound(Player player) {
      WASDConfig config = WASDNavigationManager.getInstance().getConfig();
      if (config.isSoundEnabled()) {
         try {
            player.playSound(player.getLocation(), config.getSelectionSound(), config.getSoundVolume(), config.getSoundPitch());
         } catch (Exception var4) {
         }
      }
   }

   public void onExecuteCommand(Player player, String command, int index) {
      if (command != null && !command.isEmpty()) {
         WASDConfig config = WASDNavigationManager.getInstance().getConfig();
         if (config.isDebugMode()) {
            this.plugin.getLogger().info("[WASDNavigation] 玩家 " + player.getName() + " 执行命令: " + command);
         }

         if (command.toLowerCase().startsWith("[console]")) {
            String cmd = command.replaceAll("\\[console\\]", "").trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
         } else if (command.toLowerCase().startsWith("[op]")) {
            String cmd = command.replaceAll("\\[op\\]", "").trim();
            if (player.isOp()) {
               player.performCommand(cmd);
            } else {
               try {
                  player.setOp(true);
                  player.performCommand(cmd);
               } finally {
                  player.setOp(false);
               }
            }
         } else if (command.toLowerCase().startsWith("[player]")) {
            String cmd = command.replaceAll("\\[player\\]", "").trim();
            player.performCommand(cmd);
         } else if (command.toLowerCase().startsWith("[server]")) {
            String server = command.replaceAll("\\[server\\]", "").trim();
            player.sendPluginMessage(this.plugin, "BungeeCord", ("Connect " + server).getBytes(StandardCharsets.UTF_8));
         } else if (command.toLowerCase().startsWith("[link]")) {
            String link = command.replaceAll("\\[link\\]", "").trim();
            player.spigot().sendMessage(new TextComponent(ChatColor.GREEN + "点击打开链接: " + link));
         } else {
            player.performCommand(command);
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public Player getPlayer() {
      return this.player;
   }
}
