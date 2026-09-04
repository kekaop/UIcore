package org.orvencraft.uicore.wasd;

import org.orvencraft.uicore.util.CommandUtils;
import java.util.List;
import net.md_5.bungee.api.chat.TextComponent;
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
      if (this.textDisplays == null) {
         return;
      }

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

   private void setDisplayScale(TextDisplay display, double scale) {
      if (display != null && display.isValid()) {
         try {
            display.setTransformation(
               new Transformation(new Vector3f(-1.0F, 0.0F, 0.0F), new AxisAngle4f(), new Vector3f((float)scale, (float)scale, (float)scale), new AxisAngle4f())
            );
         } catch (Exception ignored) {
         }
      }
   }

   private void playSelectionSound(Player player) {
      WASDConfig config = WASDNavigationManager.getInstance().getConfig();
      if (config.isSoundEnabled()) {
         try {
            player.playSound(player.getLocation(), config.getSelectionSound(), config.getSoundVolume(), config.getSoundPitch());
         } catch (Exception ignored) {
         }
      }
   }

   public void onExecuteCommand(Player player, String command, int index) {
      if (command == null || command.isEmpty()) {
         return;
      }

      WASDConfig config = WASDNavigationManager.getInstance().getConfig();
      if (config.isDebugMode()) {
         this.plugin.getLogger().info("[WASDNavigation] Player " + player.getName() + " executes command: " + command);
      }

      if (command.toLowerCase().startsWith("[link]")) {
         String link = command.replaceAll("\\[link\\]", "").trim();
         player.spigot().sendMessage(new TextComponent(ChatColor.GREEN + "Click to open link: " + link));
      } else {
         CommandUtils.executeCommand(player, command);
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
