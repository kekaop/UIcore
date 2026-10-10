package org.orvencraft.uicore;

import org.orvencraft.uicore.util.TextContentUtils;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.util.TypewriterEffect;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import space.arim.morepaperlib.scheduling.ScheduledTask;

public class TextDisplayManager {
   private final JavaPlugin plugin;
   private final File configFile;
   private FileConfiguration config;
   /** Each configured text entry owns its displays and timeout independently. */
   private final Map<UUID, List<TextDisplayManager.ActiveTextGroup>> activeGroups = new ConcurrentHashMap<>();
   private final TypewriterEffect typewriterEffect;

   public TextDisplayManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.configFile = new File(plugin.getDataFolder(), "text.yml");
      this.typewriterEffect = new TypewriterEffect(UIcorePlugin.plugin);
      this.loadConfig();
   }

   public TypewriterEffect getTypewriterEffect() {
      return this.typewriterEffect;
   }

   public void loadConfig() {
      if (!this.configFile.exists()) {
         this.plugin.saveResource("text.yml", false);
      }

      this.config = org.orvencraft.uicore.config.ConfigFiles.loadStrict(this.configFile);
   }

   public void reloadConfig() {
      new ArrayList<>(this.activeGroups.keySet()).forEach(this::clearPlayerDisplays);
      this.typewriterEffect.cleanupAll();
      this.loadConfig();
   }

   public void showTextDisplays(Player player, String menuKey) {
      if (player == null || menuKey == null || !this.config.contains("text-displays")) {
         return;
      }
      this.clearPlayerDisplays(player.getUniqueId());
      ConfigurationSection displays = this.config.getConfigurationSection("text-displays");
      if (displays == null) {
         return;
      }
      for (String key : displays.getKeys(false)) {
         String path = "text-displays." + key;
         if (!this.config.getStringList(path + ".menus").contains(menuKey)) {
            continue;
         }
         String content = TextContentUtils.normalizeLineBreaks(this.config.getString(path + ".content", ""));
         List<String> contents = this.config.isList(path + ".content")
            ? this.config.getStringList(path + ".content")
            : Collections.singletonList(content);
         this.createTextDisplays(
            player,
            contents,
            this.config.getDouble(path + ".scale", 1.0),
            this.config.getDouble(path + ".position.x", 0.0),
            this.config.getDouble(path + ".position.y", 0.0),
            this.config.getDouble(path + ".position.z", 0.0),
            this.config.getDouble(path + ".tilt.x", 0.0),
            this.config.getDouble(path + ".tilt.y", 0.0),
            this.config.getDouble(path + ".tilt.z", 0.0),
            this.config.getBoolean(path + ".animation.enabled", !"none".equalsIgnoreCase(this.config.getString(path + ".animation.type", "none"))),
            this.config.getString(path + ".animation.type", "none"),
            this.config.getDouble(path + ".animation.speed", 1.0),
            this.config.getDouble(path + ".animation.amplitude", 0.25),
            this.config.getDouble(path + ".animation.phase-offset", 0.0),
            this.config.getDouble(path + ".animation.spacing", 0.0),
            this.config.getString(path + ".alignment", "center"),
            this.config.getInt(path + ".duration", 30) * 20,
            this.config.getBoolean(path + ".typewriter.enabled", false),
            Math.max(1, this.config.getInt(path + ".typewriter.speed", 2)),
            Math.max(0, this.config.getInt(path + ".typewriter.start-delay", 0)),
            this.config.getString(path + ".typewriter.cursor-char", "|")
         );
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
      List<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
      Location baseLoc = player.getLocation();
      double yOffset = 0.0;
      if ("center".equals(alignment)) {
         yOffset = -(contents.size() - 1) * spacing / 2.0;
      } else if ("right".equals(alignment)) {
         yOffset = -(contents.size() - 1) * spacing;
      }

      ActiveTextGroup group = new ActiveTextGroup(displays, tasks);
      UUID playerId = player.getUniqueId();
      this.activeGroups.computeIfAbsent(playerId, ignored -> new CopyOnWriteArrayList<>()).add(group);
      try {
         for (int i = 0; i < contents.size(); i++) {
            Component line = UIcorePlugin.renderDisplayComponent(player, contents.get(i));
            Location location = baseLoc.clone().add(new Vector(x, y + yOffset + (contents.size() - 1 - i) * spacing, z));
            this.scheduleConfiguredTextDisplay(
               player, playerId, group, location, line, "text-display-" + i, scale, 0.0, 0.0, 0.0,
               typewriterEnabled, typewriterSpeed, typewriterStartDelay, typewriterCursorChar,
               animEnabled, animType, animSpeed, animAmplitude, animPhaseOffset, i
            );
         }
         ScheduledTask durationTask = UIcorePlugin.foliaLib
            .scheduling()
            .entitySpecificScheduler(player)
            .runDelayed(() -> this.clearTextGroup(playerId, group), () -> this.clearTextGroup(playerId, group), Math.max(1, duration));
         group.addTask(durationTask);
      } catch (RuntimeException exception) {
         this.clearTextGroup(playerId, group);
         throw exception;
      }
   }

   public void createTextDisplayWithLayout(Player player, String text, Location loc, float scale, MenuLayout layout) {
      if (player != null && loc != null && layout != null) {
         Component parsedText = layout.renderStyledComponent(player, text, false, false, null);
         World world = loc.getWorld();
         if (world != null) {
            ActiveTextGroup group = new ActiveTextGroup(new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>());
            UUID playerId = player.getUniqueId();
            this.activeGroups.computeIfAbsent(playerId, ignored -> new CopyOnWriteArrayList<>()).add(group);
            try {
               this.scheduleConfiguredTextDisplay(
                  player, playerId, group, loc.clone(), parsedText, layout.key, layout.getTextSize(), layout.tiltX, layout.tiltY, layout.tiltZ,
                  layout.isTypewriterEnabled(), layout.getTypewriterSpeed(), layout.getTypewriterStartDelay(), layout.getTypewriterCursorChar(),
                  false, "none", 0.0, 0.0, 0.0, 0
               );
            } catch (RuntimeException exception) {
               this.clearTextGroup(playerId, group);
               throw exception;
            }
         }
      }
   }

   public void completeTypewriterEffects(UUID playerId) {
      forEachActiveDisplay(playerId, ad -> {
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(ad.display).run(() -> {
            if (ad.display.isValid() && this.typewriterEffect.isRunning(ad.display)) {
               this.typewriterEffect.completeEffect(ad.display);
            }
         }, () -> this.typewriterEffect.cancelEffect(ad.display.getUniqueId()));
      });
   }

   private void scheduleConfiguredTextDisplay(
      Player player, UUID playerId, ActiveTextGroup group, Location location, Component text, String name, double scale,
      double tiltX, double tiltY, double tiltZ, boolean typewriterEnabled, int typewriterSpeed, int typewriterStartDelay,
      String typewriterCursor, boolean animationEnabled, String animationType, double animationSpeed, double animationAmplitude,
      double animationPhaseOffset, int index
   ) {
      World world = location.getWorld();
      if (world == null || !group.isActive()) {
         return;
      }
      ScheduledTask spawnTask = UIcorePlugin.foliaLib.scheduling().regionSpecificScheduler(location).run(() -> {
         if (!group.isActive()) {
            return;
         }
         TextDisplay display = world.spawn(location, TextDisplay.class, created -> created.setVisibleByDefault(false));
         ActiveTextDisplay entry = new ActiveTextDisplay(display, location.clone(), index);
         if (!group.addDisplay(entry)) {
            display.remove();
            return;
         }
         try {
            UIcorePlugin.plugin.tagMenuEntity(display);
            display.setPersistent(false);
            display.addScoreboardTag("uicore_text_display");
            if (typewriterEnabled) {
               display.setText(typewriterCursor);
               this.typewriterEffect.applyEffect(display, text, typewriterSpeed, typewriterStartDelay, typewriterCursor);
            } else {
               display.text(text);
            }
            display.setCustomName(name);
            display.setCustomNameVisible(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setDefaultBackground(false);
            display.setShadowed(true);
            display.setBillboard(Billboard.CENTER);
            UIcorePlugin.plugin.configureMenuEntityVisibility(player, display);
            display.setInterpolationDuration(2);
            Transformation oldTransformation = display.getTransformation();
            display.setTransformation(new Transformation(
               oldTransformation.getTranslation(),
               oldTransformation.getLeftRotation().rotationYXZ((float) Math.toRadians(tiltX), (float) Math.toRadians(tiltY), (float) Math.toRadians(tiltZ)),
               new Vector3f((float) scale, (float) scale, (float) scale), oldTransformation.getRightRotation()
            ));
            this.applyAnimation(display, animationEnabled, animationType, animationSpeed, animationAmplitude, animationPhaseOffset, index, group);
            ScheduledTask showTask = UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(
               () -> { if (group.isActive() && player.isOnline()) player.showEntity(UIcorePlugin.plugin, display); },
               () -> { }
            );
            group.addTask(showTask);
         } catch (RuntimeException exception) {
            this.plugin.getLogger().warning("Could not create text display '" + name + "': " + exception.getMessage());
            this.clearTextGroup(playerId, group);
         }
      });
      group.addTask(spawnTask);
   }

   public void cancelTypewriterEffects(UUID playerId) {
      forEachActiveDisplay(playerId, ad -> {
         this.typewriterEffect.cancelEffect(ad.display.getUniqueId());
      });
   }

   private void applyAnimation(
      final TextDisplay display,
      boolean enabled,
      String animType,
      final double speed,
      double amplitude,
      final double phaseOffset,
      final int index,
      ActiveTextGroup group
   ) {
      if (!enabled || animType == null || "none".equalsIgnoreCase(animType)) {
         return;
      }
      double safeSpeed = Math.max(0.01, speed);
      double safeAmplitude = Math.max(0.0, amplitude);
      double step = safeSpeed * 0.05;
      ScheduledTask scheduled = switch (animType.toLowerCase()) {
         case "rotate" -> scheduleRotation(display, step);
         case "__old-float-y", "__old-up-down", "float-y", "up-down" -> scheduleFloat(display, step, safeAmplitude, phaseOffset * index, true);
         case "__old-left-right", "float-x", "left-right" -> scheduleFloat(display, step, safeAmplitude, phaseOffset * index, false);
         default -> null;
      };
      if (scheduled != null) {
         group.addTask(scheduled);
      }
   }

   private ScheduledTask scheduleRotation(TextDisplay display, double step) {
      float[] angle = {0.0F};
      ScheduledTask[] taskReference = new ScheduledTask[1];
      ScheduledTask scheduled = UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(() -> {
         if (!display.isValid()) {
            if (taskReference[0] != null) taskReference[0].cancel();
            return;
         }
         angle[0] += (float) step;
         Transformation oldTransformation = display.getTransformation();
         display.setTransformation(new Transformation(
            oldTransformation.getTranslation(), oldTransformation.getLeftRotation().rotationYXZ(0.0F, angle[0], 0.0F),
            oldTransformation.getScale(), oldTransformation.getRightRotation()
         ));
      }, null, 1L, 1L);
      taskReference[0] = scheduled;
      return scheduled;
   }

   private ScheduledTask scheduleFloat(TextDisplay display, double step, double amplitude, double phase, boolean vertical) {
      Vector3f[] baseTranslation = {null};
      long[] tick = {0L};
      ScheduledTask[] taskReference = new ScheduledTask[1];
      ScheduledTask scheduled = UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(display).runAtFixedRate(() -> {
         if (!display.isValid()) {
            if (taskReference[0] != null) taskReference[0].cancel();
            return;
         }
         Transformation oldTransformation = display.getTransformation();
         if (baseTranslation[0] == null) {
            baseTranslation[0] = new Vector3f(oldTransformation.getTranslation());
         }
         float offset = (float) (Math.sin(++tick[0] * step + phase) * amplitude);
         Vector3f base = baseTranslation[0];
         Vector3f translation = vertical
            ? new Vector3f(base.x, base.y + offset, base.z)
            : new Vector3f(base.x + offset, base.y, base.z);
         display.setTransformation(new Transformation(
            translation, oldTransformation.getLeftRotation(), oldTransformation.getScale(), oldTransformation.getRightRotation()
         ));
      }, null, 1L, 1L);
      taskReference[0] = scheduled;
      return scheduled;
   }

   public void clearPlayerDisplays(UUID playerId) {
      List<ActiveTextGroup> groups = this.activeGroups.remove(playerId);
      if (groups != null) {
         for (ActiveTextGroup group : groups) {
            clearTextGroupResources(group);
         }
      }
   }

   public void cleanup() {
      new ArrayList<>(this.activeGroups.keySet()).forEach(this::clearPlayerDisplays);
      this.typewriterEffect.cleanupAll();
   }

   private void clearTextGroup(UUID playerId, ActiveTextGroup group) {
      List<ActiveTextGroup> groups = this.activeGroups.get(playerId);
      if (groups == null || !groups.remove(group)) {
         return;
      }
      clearTextGroupResources(group);
      if (groups.isEmpty()) {
         this.activeGroups.remove(playerId, groups);
      }
   }

   private void clearTextGroupResources(ActiveTextGroup group) {
      if (!group.deactivate()) {
         return;
      }
      for (ScheduledTask task : group.tasks) {
         try { task.cancel(); }
         catch (RuntimeException exception) { this.plugin.getLogger().warning("Text task cleanup failed: " + exception.getMessage()); }
      }
      for (ActiveTextDisplay entry : group.displays) {
         try { this.typewriterEffect.cancelEffect(entry.display.getUniqueId()); }
         catch (RuntimeException exception) { this.plugin.getLogger().warning("Typewriter cleanup failed: " + exception.getMessage()); }
         org.orvencraft.uicore.scheduler.CleanupTasks.entity(this.plugin, entry.display,
            () -> { if (entry.display.isValid()) entry.display.remove(); }
         );
      }
      group.tasks.clear(); group.displays.clear();
   }

   private void forEachActiveDisplay(UUID playerId, java.util.function.Consumer<ActiveTextDisplay> action) {
      List<ActiveTextGroup> groups = this.activeGroups.get(playerId);
      if (groups != null) {
         groups.forEach(group -> group.displays.forEach(action));
      }
   }

   public boolean showTextDisplayById(Player player, String textId) {
      if (!this.config.contains("text-displays." + textId)) {
         return false;
      } else {
         ConfigurationSection section = this.config.getConfigurationSection("text-displays." + textId);
         List<String> contents = section.isList("content")
            ? section.getStringList("content").stream().map(TextContentUtils::normalizeLineBreaks).toList()
            : Collections.singletonList(TextContentUtils.normalizeLineBreaks(section.getString("content", "")));
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

   private static class ActiveTextGroup {
      final List<ActiveTextDisplay> displays;
      final List<ScheduledTask> tasks;
      private final AtomicBoolean active = new AtomicBoolean(true);

      ActiveTextGroup(List<ActiveTextDisplay> displays, List<ScheduledTask> tasks) {
         this.displays = displays;
         this.tasks = tasks;
      }

      boolean isActive() {
         return this.active.get();
      }

      boolean addDisplay(ActiveTextDisplay display) {
         if (!this.active.get()) {
            return false;
         }
         this.displays.add(display);
         if (this.active.get()) {
            return true;
         }
         this.displays.remove(display);
         return false;
      }

      void addTask(ScheduledTask task) {
         if (task == null) {
            return;
         }
         if (!this.active.get()) {
            task.cancel();
            return;
         }
         this.tasks.add(task);
         if (!this.active.get() && this.tasks.remove(task)) {
            task.cancel();
         }
      }

      boolean deactivate() {
         return this.active.compareAndSet(true, false);
      }
   }
}
