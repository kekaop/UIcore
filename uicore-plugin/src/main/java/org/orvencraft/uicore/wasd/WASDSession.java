package org.orvencraft.uicore.wasd;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.orvencraft.uicore.UIcorePlugin;
import org.joml.Vector3f;

public class WASDSession {
   private final JavaPlugin plugin;
   private final Player player;
   private boolean enabled;
   private TextDisplay[] textDisplays;
   private Transformation[] originalTransformations;

   public WASDSession(JavaPlugin plugin, Player player, boolean enabled) {
      this.plugin = plugin;
      this.player = player;
      this.enabled = enabled;
   }

   public void setTextDisplays(TextDisplay[] displays, List<Double> scales) {
      this.textDisplays = displays;
      this.originalTransformations = new Transformation[displays == null ? 0 : displays.length];
      for (int i = 0; i < this.originalTransformations.length; i++) {
         TextDisplay display = displays[i];
         if (display == null) continue;
         final int index = i;
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).run(() -> {
            if (display.isValid()) this.originalTransformations[index] = display.getTransformation();
         }, null);
      }
   }

   public void onSelectionChanged(Player player, int oldIndex, int newIndex) {
      if (this.textDisplays == null) {
         return;
      }

      if (oldIndex >= 0 && oldIndex < this.textDisplays.length && this.textDisplays[oldIndex] != null) {
         this.restoreDisplay(this.textDisplays[oldIndex], oldIndex);
      }

      if (newIndex >= 0 && newIndex < this.textDisplays.length && this.textDisplays[newIndex] != null) {
         this.setDisplayScale(this.textDisplays[newIndex], newIndex, 1.5F);
      }

      this.playSelectionSound(player);
   }

   public void restoreDisplays() {
      if (this.textDisplays == null) {
         return;
      }
      for (int index = 0; index < this.textDisplays.length; index++) {
         this.restoreDisplay(this.textDisplays[index], index);
      }
   }

   private void restoreDisplay(TextDisplay display, int index) {
      if (display == null || this.originalTransformations == null || index < 0 || index >= this.originalTransformations.length) return;
      org.orvencraft.uicore.scheduler.CleanupTasks.entity(this.plugin, display, () -> {
         if (!display.isValid()) return;
         Transformation original = this.originalTransformations[index];
         if (original != null) display.setTransformation(original);
      });
   }

   private void setDisplayScale(TextDisplay display, int index, float multiplier) {
      if (display == null || this.originalTransformations == null || index < 0 || index >= this.originalTransformations.length) return;
      UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).run(() -> {
         if (!display.isValid()) return;
         Transformation original = this.originalTransformations[index];
         if (original == null) return;
         Vector3f scale = new Vector3f(original.getScale()).mul(multiplier);
         display.setTransformation(new Transformation(original.getTranslation(), original.getLeftRotation(), scale, original.getRightRotation()));
      }, null);
   }

   private void playSelectionSound(Player player) {
      if (player == null) {
         return;
      }
      if (!Bukkit.isOwnedByCurrentRegion(player)) {
         player.getScheduler().execute(this.plugin, () -> this.playSelectionSound(player), null, 0L);
         return;
      }
      WASDConfig config = WASDNavigationManager.getInstance().getConfig();
      if (config.isSoundEnabled()) {
         try {
            player.playSound(player.getLocation(), config.getSelectionSound(), config.getSoundVolume(), config.getSoundPitch());
         } catch (Exception ignored) {
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
