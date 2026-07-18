package com.cmenu.ui;

import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.util.TypewriterEffect;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class TextDisplayManager {
   private final JavaPlugin plugin;
   private final File configFile;
   private FileConfiguration config;
   private final Map<UUID, List<TextDisplayManager.ActiveTextDisplay>> activeDisplays = new ConcurrentHashMap<>();
   private final Map<UUID, List<Integer>> activeTasks = new ConcurrentHashMap<>();
   private final TypewriterEffect typewriterEffect;

   public TextDisplayManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.configFile = new File(plugin.getDataFolder(), "text.yml");
      this.typewriterEffect = new TypewriterEffect(CursorMenuPlugin.plugin);
      this.loadConfig();
   }

   public TypewriterEffect getTypewriterEffect() {
      return this.typewriterEffect;
   }

   public void loadConfig() {
      if (!this.configFile.exists()) {
         this.plugin.saveResource("text.yml", false);
      }

      this.config = YamlConfiguration.loadConfiguration(this.configFile);
   }

   public void reloadConfig() {
      this.activeDisplays.keySet().forEach(this::clearPlayerDisplays);
      this.activeDisplays.clear();
      this.activeTasks.clear();
      this.typewriterEffect.cleanupAll();
      this.loadConfig();
   }

   public void showTextDisplays(Player player, String menuKey) {
      if (menuKey != null) {
         if (!this.activeDisplays.containsKey(player.getUniqueId())) {
            this.clearPlayerDisplays(player.getUniqueId());
            if (this.config.contains("text-displays")) {
               ConfigurationSection displays = this.config.getConfigurationSection("text-displays");

               for (String key : displays.getKeys(false)) {
                  String path = "text-displays." + key;
                  List<String> menus = this.config.getStringList(path + ".menus");
                  if (menus.contains(menuKey)) {
                     String content = this.config.getString(path + ".content", "");
                     List<String> contents = this.config.isList(path + ".content")
                        ? this.config.getStringList(path + ".content")
                        : Collections.singletonList(content);
                     double scale = this.config.getDouble(path + ".scale", 1.0);
                     double x = this.config.getDouble(path + ".position.x", 0.0);
                     double y = this.config.getDouble(path + ".position.y", 0.0);
                     double z = this.config.getDouble(path + ".position.z", 0.0);
                     double tiltX = this.config.getDouble(path + ".tilt.x", 0.0);
                     double tiltY = this.config.getDouble(path + ".tilt.y", 0.0);
                     double tiltZ = this.config.getDouble(path + ".tilt.z", 0.0);
                     String animType = this.config.getString(path + ".animation.type", "none");
                     boolean animEnabled = this.config.getBoolean(path + ".animation.enabled", !"none".equalsIgnoreCase(animType));
                     double animSpeed = this.config.getDouble(path + ".animation.speed", 1.0);
                     double animAmplitude = this.config.getDouble(path + ".animation.amplitude", 0.25);
                     double animPhaseOffset = this.config.getDouble(path + ".animation.phase-offset", 0.0);
                     double spacing = this.config.getDouble(path + ".animation.spacing", 0.0);
                     String alignment = this.config.getString(path + ".alignment", "center");
                     int duration = this.config.getInt(path + ".duration", 30) * 20;
                     boolean typewriterEnabled = this.config.getBoolean(path + ".typewriter.enabled", false);
                     int typewriterSpeed = Math.max(1, this.config.getInt(path + ".typewriter.speed", 2));
                     int typewriterStartDelay = Math.max(0, this.config.getInt(path + ".typewriter.start-delay", 0));
                     String typewriterCursorChar = this.config.getString(path + ".typewriter.cursor-char", "|");
                     this.createTextDisplays(
                        player,
                        contents,
                        scale,
                        x,
                        y,
                        z,
                        tiltX,
                        tiltY,
                        tiltZ,
                        animEnabled,
                        animType,
                        animSpeed,
                        animAmplitude,
                        animPhaseOffset,
                        spacing,
                        alignment,
                        duration,
                        typewriterEnabled,
                        typewriterSpeed,
                        typewriterStartDelay,
                        typewriterCursorChar
                     );
                  }
               }
            }
         }
      }
   }

   private void createTextDisplays(
      Player player,
      List<String> contents,
      double scale,
      double x,
      double y,
      double z,
      double tiltX,
      double tiltY,
      double tiltZ,
      boolean animEnabled,
      String animType,
      double animSpeed,
      double animAmplitude,
      double animPhaseOffset,
      double spacing,
      String alignment,
      int duration,
      boolean typewriterEnabled,
      int typewriterSpeed,
      int typewriterStartDelay,
      String typewriterCursorChar
   ) {
      List<TextDisplayManager.ActiveTextDisplay> displays = new CopyOnWriteArrayList<>();
      List<Integer> tasks = new CopyOnWriteArrayList<>();
      World world = player.getWorld();
      Location baseLoc = player.getLocation();
      double yOffset = 0.0;
      if ("center".equals(alignment)) {
         yOffset = -(contents.size() - 1) * spacing / 2.0;
      } else if ("right".equals(alignment)) {
         yOffset = -(contents.size() - 1) * spacing;
      }

      for (int i = 0; i < contents.size(); i++) {
         String rawLine = contents.get(i);
         String line = ColorParser.toLegacyString(CursorMenuPlugin.parsePlaceholders(player, rawLine));
         Location loc = baseLoc.clone().add(new Vector(x, y + yOffset + (contents.size() - 1 - i) * spacing, z));
         TextDisplay display = (TextDisplay)world.spawn(loc, TextDisplay.class);
         if (typewriterEnabled) {
            display.setText(typewriterCursorChar);
            this.typewriterEffect.applyEffect(display, line, typewriterSpeed, typewriterStartDelay, typewriterCursorChar);
         } else {
            display.setText(ColorParser.toLegacyString(rawLine));
         }

         display.setCustomName("text-display-" + i);
         display.setCustomNameVisible(false);
         display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
         display.setDefaultBackground(false);
         display.setShadowed(true);
         display.setBillboard(Billboard.CENTER);
         display.setVisibleByDefault(false);
         player.showEntity(CursorMenuPlugin.plugin, display);
         display.setInterpolationDuration(2);
         Transformation oldTrans = display.getTransformation();
         Transformation newTrans = new Transformation(
            oldTrans.getTranslation(), oldTrans.getLeftRotation(), new Vector3f((float)scale, (float)scale, (float)scale), oldTrans.getRightRotation()
         );
         display.setTransformation(newTrans);
         displays.add(new TextDisplayManager.ActiveTextDisplay(display, loc.clone(), i));
         this.applyAnimation(display, animEnabled, animType, animSpeed, animAmplitude, animPhaseOffset, i, tasks);
      }

      this.activeDisplays.put(player.getUniqueId(), displays);
      this.activeTasks.put(player.getUniqueId(), tasks);
      BukkitTask durationTask = Bukkit.getScheduler().runTaskLater(CursorMenuPlugin.plugin, () -> this.clearPlayerDisplays(player.getUniqueId()), duration);
      tasks.add(durationTask.getTaskId());
   }

   public void createTextDisplayWithLayout(Player player, String text, Location loc, float scale, MenuLayout layout) {
      if (player != null && loc != null && layout != null) {
         String parsedText = ColorParser.toLegacyString(CursorMenuPlugin.parsePlaceholders(player, text));
         World world = loc.getWorld();
         if (world != null) {
            TextDisplay display = (TextDisplay)world.spawn(loc, TextDisplay.class);
            if (layout.isTypewriterEnabled()) {
               display.setText(layout.getTypewriterCursorChar());
               this.typewriterEffect
                  .applyEffect(display, parsedText, layout.getTypewriterSpeed(), layout.getTypewriterStartDelay(), layout.getTypewriterCursorChar());
            } else {
               display.setText(ColorParser.toLegacyString(text));
            }

            display.setCustomName(layout.key);
            display.setCustomNameVisible(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setDefaultBackground(false);
            display.setShadowed(true);
            display.setBillboard(Billboard.CENTER);
            display.setVisibleByDefault(false);
            player.showEntity(CursorMenuPlugin.plugin, display);
            display.setInterpolationDuration(2);
            Transformation oldTrans = display.getTransformation();
            Transformation newTrans = new Transformation(
               oldTrans.getTranslation(),
               oldTrans.getLeftRotation()
                  .rotationYXZ((float)Math.toRadians(layout.tiltX), (float)Math.toRadians(layout.tiltY), (float)Math.toRadians(layout.tiltZ)),
               new Vector3f((float)layout.getTextSize(), (float)layout.getTextSize(), (float)layout.getTextSize()),
               oldTrans.getRightRotation()
            );
            display.setTransformation(newTrans);
         }
      }
   }

   public void completeTypewriterEffects(UUID playerId) {
      List<TextDisplayManager.ActiveTextDisplay> displays = this.activeDisplays.get(playerId);
      if (displays != null) {
         displays.forEach(ad -> {
            if (ad.display.isValid() && this.typewriterEffect.isRunning(ad.display)) {
               this.typewriterEffect.completeEffect(ad.display);
            }
         });
      }
   }

   public void cancelTypewriterEffects(UUID playerId) {
      List<TextDisplayManager.ActiveTextDisplay> displays = this.activeDisplays.get(playerId);
      if (displays != null) {
         displays.forEach(ad -> {
            if (ad.display.isValid()) {
               this.typewriterEffect.cancelEffect(ad.display);
            }
         });
      }
   }

   private void applyAnimation(
      final TextDisplay display,
      boolean enabled,
      String animType,
      final double speed,
      double amplitude,
      final double phaseOffset,
      final int index,
      List<Integer> tasks
   ) {
      if (enabled && animType != null && !"none".equalsIgnoreCase(animType)) {
         long period = 1L;
         double safeSpeed = Math.max(0.01, speed);
         final double safeAmplitude = Math.max(0.0, amplitude);
         final double step = safeSpeed * 0.05;
         String var20 = animType.toLowerCase();
         switch (var20) {
            case "rotate": {
               BukkitTask task = (new BukkitRunnable() {
                     float angle = 0.0F;
                     long tick = 0L;

                     public void run() {
                        if (!display.isValid()) {
                           this.cancel();
                        } else {
                           this.angle = (float)(this.angle + step);
                           Transformation oldT = display.getTransformation();
                           Transformation newT = new Transformation(
                              oldT.getTranslation(), oldT.getLeftRotation().rotationYXZ(0.0F, this.angle, 0.0F), oldT.getScale(), oldT.getRightRotation()
                           );
                           display.setTransformation(newT);
                        }
                     }
                  })
                  .runTaskTimer(CursorMenuPlugin.plugin, 0L, 1L);
               tasks.add(task.getTaskId());
               break;
            }
            case "float-y":
            case "up-down": {
               BukkitTask task = (new BukkitRunnable() {
                     final Vector3f baseTranslation = new Vector3f(display.getTransformation().getTranslation());
                     final double phase = phaseOffset * index;
                     long tick = 0L;

                     public void run() {
                        if (!display.isValid()) {
                           this.cancel();
                        } else {
                           this.tick++;
                           float offset = (float)(Math.sin(this.tick * step + this.phase) * safeAmplitude);
                           Transformation oldT = display.getTransformation();
                           Transformation newT = new Transformation(
                              new Vector3f(this.baseTranslation.x, this.baseTranslation.y + offset, this.baseTranslation.z),
                              oldT.getLeftRotation(),
                              oldT.getScale(),
                              oldT.getRightRotation()
                           );
                           display.setTransformation(newT);
                        }
                     }
                  })
                  .runTaskTimer(CursorMenuPlugin.plugin, 0L, 1L);
               tasks.add(task.getTaskId());
               break;
            }
            case "__old-float-y":
            case "__old-up-down": {
               BukkitTask task = (new BukkitRunnable() {
                     final Location start = display.getLocation().clone();
                     final double total = 2.0;
                     final long totalTicks = (long)(60.0 / speed);
                     long tick = 0L;

                     public void run() {
                        if (!display.isValid()) {
                           this.cancel();
                        } else {
                           long tick = 1L;
                           float progress = Math.min((float)tick / (float)this.totalTicks, 1.0F);
                           double y = this.start.getY() + 2.0 * progress;
                           Transformation oldT = display.getTransformation();
                           Transformation newT = new Transformation(
                              new Vector3f(
                                 (float)(this.start.getX() - display.getLocation().getX()),
                                 (float)(y - display.getLocation().getY()),
                                 (float)(this.start.getZ() - display.getLocation().getZ())
                              ),
                              oldT.getLeftRotation(),
                              oldT.getScale(),
                              oldT.getRightRotation()
                           );
                           display.setTransformation(newT);
                           if (tick >= this.totalTicks) {
                              this.cancel();
                           }
                        }
                     }
                  })
                  .runTaskTimer(CursorMenuPlugin.plugin, 0L, 1L);
               break;
            }
            case "float-x":
            case "left-right": {
               BukkitTask task = (new BukkitRunnable() {
                     final Vector3f baseTranslation = new Vector3f(display.getTransformation().getTranslation());
                     final double phase = phaseOffset * index;
                     long tick = 0L;

                     public void run() {
                        if (!display.isValid()) {
                           this.cancel();
                        } else {
                           this.tick++;
                           float offset = (float)(Math.sin(this.tick * step + this.phase) * safeAmplitude);
                           Transformation oldT = display.getTransformation();
                           Transformation newT = new Transformation(
                              new Vector3f(this.baseTranslation.x + offset, this.baseTranslation.y, this.baseTranslation.z),
                              oldT.getLeftRotation(),
                              oldT.getScale(),
                              oldT.getRightRotation()
                           );
                           display.setTransformation(newT);
                        }
                     }
                  })
                  .runTaskTimer(CursorMenuPlugin.plugin, 0L, 1L);
               tasks.add(task.getTaskId());
               break;
            }
            case "__old-left-right":
               (new BukkitRunnable() {
                     final Location start = display.getLocation().clone();
                     final double total = 2.0;
                     final long totalTicks = (long)(60.0 / speed);
                     long tick = 0L;

                     public void run() {
                        if (!display.isValid()) {
                           this.cancel();
                        } else {
                           this.tick++;
                           float progress = Math.min((float)this.tick / (float)this.totalTicks, 1.0F);
                           double x = this.start.getX() + 2.0 * Math.sin(progress * 2.0F * Math.PI);
                           Transformation oldT = display.getTransformation();
                           Transformation newT = new Transformation(
                              new Vector3f((float)(x - display.getLocation().getX()), 0.0F, 0.0F),
                              oldT.getLeftRotation(),
                              oldT.getScale(),
                              oldT.getRightRotation()
                           );
                           display.setTransformation(newT);
                        }
                     }
                  })
                  .runTaskTimer(CursorMenuPlugin.plugin, 0L, 1L);
         }
      }
   }

   public void clearPlayerDisplays(UUID playerId) {
      List<Integer> tasks = this.activeTasks.remove(playerId);
      if (tasks != null) {
         tasks.forEach(Bukkit.getScheduler()::cancelTask);
      }

      List<TextDisplayManager.ActiveTextDisplay> displays = this.activeDisplays.remove(playerId);
      if (displays != null) {
         displays.forEach(display -> {
            if (display.display.isValid()) {
               this.typewriterEffect.cancelEffect(display.display);
               display.display.remove();
            }
         });
      }
   }

   public void cleanup() {
      this.activeDisplays.keySet().forEach(this::clearPlayerDisplays);
      this.typewriterEffect.cleanupAll();
   }

   public boolean showTextDisplayById(Player player, String textId) {
      if (!this.config.contains("text-displays." + textId)) {
         return false;
      } else {
         ConfigurationSection section = this.config.getConfigurationSection("text-displays." + textId);
         List<String> contents = section.isList("content") ? section.getStringList("content") : Collections.singletonList(section.getString("content", ""));
         double scale = section.getDouble("scale", 1.0);
         double x = section.getDouble("position.x", 0.0);
         double y = section.getDouble("position.y", 0.0);
         double z = section.getDouble("position.z", 0.0);
         double tiltX = section.getDouble("tilt.x", 0.0);
         double tiltY = section.getDouble("tilt.y", 0.0);
         double tiltZ = section.getDouble("tilt.z", 0.0);
         String animType = section.getString("animation.type", "none");
         boolean animEnabled = section.getBoolean("animation.enabled", !"none".equalsIgnoreCase(animType));
         double animSpeed = section.getDouble("animation.speed", 1.0);
         double animAmplitude = section.getDouble("animation.amplitude", 0.25);
         double animPhaseOffset = section.getDouble("animation.phase-offset", 0.0);
         double spacing = section.getDouble("animation.spacing", 0.5);
         String alignment = section.getString("alignment", "center");
         int duration = section.getInt("duration", 30) * 20;
         boolean typewriterEnabled = section.getBoolean("typewriter.enabled", false);
         int typewriterSpeed = Math.max(1, section.getInt("typewriter.speed", 2));
         int typewriterStartDelay = Math.max(0, section.getInt("typewriter.start-delay", 0));
         String typewriterCursorChar = section.getString("typewriter.cursor-char", "|");
         this.clearPlayerDisplays(player.getUniqueId());
         this.createTextDisplays(
            player,
            contents,
            scale,
            x,
            y,
            z,
            tiltX,
            tiltY,
            tiltZ,
            animEnabled,
            animType,
            animSpeed,
            animAmplitude,
            animPhaseOffset,
            spacing,
            alignment,
            duration,
            typewriterEnabled,
            typewriterSpeed,
            typewriterStartDelay,
            typewriterCursorChar
         );
         return true;
      }
   }

   public List<String> getAllTextIds() {
      return this.config.contains("text-displays") ? new ArrayList<>(this.config.getConfigurationSection("text-displays").getKeys(false)) : new ArrayList<>();
   }

   private static class ActiveTextDisplay {
      final TextDisplay display;
      final Location originalLocation;
      final int index;

      ActiveTextDisplay(TextDisplay display, Location location, int index) {
         this.display = display;
         this.originalLocation = location;
         this.index = index;
      }
   }
}
