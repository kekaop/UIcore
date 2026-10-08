package org.orvencraft.uicore;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import space.arim.morepaperlib.scheduling.ScheduledTask;

public class ItemDisplayManager implements Listener {
   private final JavaPlugin plugin;
   private final File configFile;
   private FileConfiguration config;
   private final List<PlayerDisplayPair> activeDisplays = Collections.synchronizedList(new ArrayList<>());
   private final Map<Player, List<Runnable>> displayTasks = new ConcurrentHashMap<>();
   private final Map<Player, String> playerActiveItems = new ConcurrentHashMap<>();
   private final Map<Player, Pig> ridingPig = new ConcurrentHashMap<>();
   private final Map<Player, Long> displayGenerations = new ConcurrentHashMap<>();

   public ItemDisplayManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.configFile = new File(plugin.getDataFolder(), "items.yml");
      this.loadConfig();
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public void loadConfig() {
      if (!this.configFile.exists()) this.plugin.saveResource("items.yml", false);
      this.config = org.orvencraft.uicore.config.ConfigFiles.loadStrict(this.configFile);
   }

   public void reloadConfig() {
      Map<Player, String> activeItems = new HashMap<>(this.playerActiveItems);
      this.config = org.orvencraft.uicore.config.ConfigFiles.loadStrict(this.configFile);
      activeItems.forEach((player, itemId) -> {
         this.hideItem(player);
         this.runPlayerDelayed(player, () -> this.showItemOnPlayerThread(player, itemId), 2L);
      });
   }

   public boolean showItem(Player player, String itemId) {
      if (player == null || itemId == null || !this.config.contains("display-items." + itemId)) {
         if (player != null) this.runPlayer(player, () -> player.sendMessage(ChatColor.RED + "Item display id does not exist: " + itemId));
         return false;
      }
      this.runPlayer(player, () -> this.showItemOnPlayerThread(player, itemId));
      return true;
   }

   private void showItemOnPlayerThread(Player player, String itemId) {
      this.hideItemOnPlayerThread(player);
      try {
         String path = "display-items." + itemId;
         Material material = Material.valueOf(this.config.getString(path + ".material", "DIAMOND"));
         int customModelData = this.config.getInt(path + ".custom-model-data", 0);
         float scale = (float) this.config.getDouble(path + ".scale", 0.3D);
         double xOffset = this.config.getDouble(path + ".offset.x", 0.0D);
         double yOffset = this.config.getDouble(path + ".offset.y", 0.0D);
         double zOffset = this.config.getDouble(path + ".offset.z", 2.0D);
         boolean glow = this.config.getBoolean(path + ".glow", true);
         boolean rotate = this.config.getBoolean(path + ".rotate", false);
         float rotateSpeed = (float) this.config.getDouble(path + ".rotate-speed", 1.0D);
         ItemStack item = new ItemStack(material);
         ItemMeta meta = item.getItemMeta();
         if (meta != null && customModelData > 0) {
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
         }

         Location displayLocation = this.displayLocation(player.getEyeLocation(), xOffset, yOffset, zOffset);
         long generation = this.displayGenerations.merge(player, 1L, Long::sum);
         this.playerActiveItems.put(player, itemId);
         this.runRegion(displayLocation, () -> this.spawnDisplay(player, itemId, generation, displayLocation, item, scale, glow, rotate, rotateSpeed, xOffset, yOffset, zOffset));
         this.mountPig(player, generation);
         player.sendMessage(ChatColor.GREEN + "Showing item display: " + itemId);
      } catch (Exception exception) {
         player.sendMessage(ChatColor.RED + "Could not show item: " + exception.getMessage());
         this.plugin.getLogger().warning("Could not show item: " + exception.getMessage());
      }
   }

   private void spawnDisplay(Player player, String itemId, long generation, Location location, ItemStack item, float scale, boolean glow, boolean rotate,
                             float rotateSpeed, double xOffset, double yOffset, double zOffset) {
      if (!this.isCurrent(player, itemId, generation) || location.getWorld() == null) return;
      ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class);
      display.getScheduler().run(this.plugin, ignored -> {
         if (!this.isCurrent(player, itemId, generation)) {
            display.remove();
            return;
         }
         display.setItemStack(item);
         display.setGlowing(glow);
         display.setViewRange(100.0F);
         display.setPersistent(false);
         display.addScoreboardTag("uicore_item_display");
         display.setBillboard(Billboard.FIXED);
         this.setTeleportDurationSafe(display, 2);
         this.setScale(display, scale * (float) UIcorePlugin.cursorSizeX, scale * (float) UIcorePlugin.cursorSizeY, scale * (float) UIcorePlugin.cursorSizeZ);
         this.activeDisplays.add(new PlayerDisplayPair(player, display, itemId));
         this.startDisplayTasks(player, display, itemId, generation, rotate, rotateSpeed, xOffset, yOffset, zOffset);
      }, () -> { });
   }

   private void startDisplayTasks(Player player, ItemDisplay display, String itemId, long generation, boolean rotate, float rotateSpeed,
                                  double xOffset, double yOffset, double zOffset) {
      List<Runnable> tasks = this.displayTasks.computeIfAbsent(player, ignored -> Collections.synchronizedList(new ArrayList<>()));
      ScheduledTask positionTask = UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).runAtFixedRate(
         () -> {
            if (!this.isCurrent(player, itemId, generation) || !display.isValid()) return;
            Location target = this.displayLocation(player.getEyeLocation(), xOffset, yOffset, zOffset);
            display.getScheduler().run(this.plugin, ignored -> {
               if (this.isCurrent(player, itemId, generation) && display.isValid()) UIcorePlugin.moveMenuEntity(display, target);
            }, () -> { });
         }, () -> { }, 1L, 1L);
      if (positionTask != null) tasks.add(positionTask::cancel);
      if (rotate) {
         float radiansPerTick = rotateSpeed * 0.0174533F;
         var rotationTask = display.getScheduler().runAtFixedRate(this.plugin, ignored -> {
            if (!this.isCurrent(player, itemId, generation) || !display.isValid()) return;
            Transformation transformation = display.getTransformation();
            display.setTransformation(new Transformation(
               transformation.getTranslation(), transformation.getLeftRotation().rotateAxis(radiansPerTick, 0.0F, 1.0F, 0.0F),
               transformation.getScale(), transformation.getRightRotation()));
         }, () -> { }, 1L, 1L);
         if (rotationTask != null) tasks.add(rotationTask::cancel);
      }
   }

   public void hideItem(Player player) {
      if (player != null) org.orvencraft.uicore.scheduler.CleanupTasks.entity(this.plugin, player, () -> this.hideItemOnPlayerThread(player));
   }

   private void hideItemOnPlayerThread(Player player) {
      this.displayGenerations.merge(player, 1L, Long::sum);
      this.dismountPig(player);
      List<Runnable> tasks = this.displayTasks.remove(player);
      if (tasks != null) tasks.forEach(Runnable::run);
      synchronized (this.activeDisplays) {
         this.activeDisplays.removeIf(pair -> {
            if (!pair.player.equals(player)) return false;
            org.orvencraft.uicore.scheduler.CleanupTasks.entity(this.plugin, pair.display, () -> {
               if (pair.display.isValid()) pair.display.remove();
            });
            return true;
         });
      }
      this.playerActiveItems.remove(player);
   }

   private void mountPig(Player player, long generation) {
      if (player.isInsideVehicle()) return;
      this.dismountPig(player);
      Location location = player.getLocation();
      this.runRegion(location, () -> {
         String itemId = this.playerActiveItems.get(player);
         if (!this.isCurrent(player, itemId, generation) || location.getWorld() == null) return;
         Pig pig = location.getWorld().spawn(location, Pig.class, created -> {
            created.setAI(false);
            created.setAware(false);
            created.setSilent(true);
            created.setInvulnerable(true);
            created.setCollidable(false);
            created.setGravity(false);
            created.setInvisible(true);
            created.setPersistent(false);
            created.addScoreboardTag("uicore_item_display_mount");
         });
         this.ridingPig.put(player, pig);
         pig.getScheduler().runDelayed(this.plugin, ignored -> {
            if (this.isCurrent(player, itemId, generation) && pig.isValid()) pig.addPassenger(player);
            else if (pig.isValid()) pig.remove();
         }, () -> { }, 1L);
      });
   }

   private void dismountPig(Player player) {
      Pig pig = this.ridingPig.remove(player);
      if (pig != null && pig.equals(player.getVehicle())) player.leaveVehicle();
      if (pig != null) org.orvencraft.uicore.scheduler.CleanupTasks.entity(this.plugin, pig, () -> {
         if (pig.isValid()) pig.remove();
      });
   }

   private Location displayLocation(Location eye, double xOffset, double yOffset, double zOffset) {
      Vector direction = eye.getDirection().normalize();
      return eye.clone().add(direction.multiply(zOffset)).add(0.0D, yOffset, 0.0D).add(this.getRightVector(eye).multiply(xOffset));
   }

   private boolean isCurrent(Player player, String itemId, long generation) {
      return generation == this.displayGenerations.getOrDefault(player, 0L) && itemId != null && itemId.equals(this.playerActiveItems.get(player));
   }

   private void runPlayer(Player player, Runnable task) {
      UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).run(task, () -> { });
   }

   private void runPlayerDelayed(Player player, Runnable task, long delay) {
      UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task, () -> { }, delay);
   }

   private void runRegion(Location location, Runnable task) {
      if (location.getWorld() != null) UIcorePlugin.foliaLib.scheduling().regionSpecificScheduler(location).run(task);
   }

   private void setTeleportDurationSafe(ItemDisplay display, int duration) {
      try {
         Method method = ItemDisplay.class.getMethod("setTeleportDuration", int.class);
         method.invoke(display, duration);
      } catch (NoSuchMethodException ignored) {
      } catch (Exception exception) {
         this.plugin.getLogger().warning("Could not set ItemDisplay teleport duration: " + exception.getMessage());
      }
   }

   private Vector getRightVector(Location location) {
      float yawRadians = (float) Math.toRadians(location.getYaw());
      return new Vector(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians)).normalize();
   }

   private void setScale(ItemDisplay display, float scaleX, float scaleY, float scaleZ) {
      Transformation oldTransformation = display.getTransformation();
      display.setTransformation(new Transformation(oldTransformation.getTranslation(), oldTransformation.getLeftRotation(),
         new Vector3f(scaleX, scaleY, scaleZ), oldTransformation.getRightRotation()));
   }

   public List<String> getAllItemIds() {
      return this.config.contains("display-items") ? new ArrayList<>(this.config.getConfigurationSection("display-items").getKeys(false)) : new ArrayList<>();
   }

   public String getPlayerActiveItemId(Player player) {
      return this.playerActiveItems.getOrDefault(player, "");
   }

   @EventHandler
   public void onVehicleExit(VehicleExitEvent event) {
      if (event.getExited() instanceof Player player && this.ridingPig.containsKey(player) && event.getVehicle().equals(this.ridingPig.get(player))) {
         this.hideItem(player);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.hideItem(event.getPlayer());
   }
}
