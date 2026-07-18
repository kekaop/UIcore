package com.cmenu.ui;

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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Billboard;
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

public class ItemDisplayManager implements Listener {
   private final JavaPlugin plugin;
   private final File configFile;
   private FileConfiguration config;
   private final List<PlayerDisplayPair> activeDisplays = Collections.synchronizedList(new ArrayList<>());
   private final List<PlayerTaskPair> displayTasks = Collections.synchronizedList(new ArrayList<>());
   private final Map<Player, String> playerActiveItems = new ConcurrentHashMap<>();
   private final Map<Player, Pig> ridingPig = new ConcurrentHashMap<>();

   public ItemDisplayManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.configFile = new File(plugin.getDataFolder(), "items.yml");
      this.loadConfig();
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public void loadConfig() {
      if (!this.configFile.exists()) {
         this.plugin.saveResource("items.yml", false);
      }

      this.config = YamlConfiguration.loadConfiguration(this.configFile);
   }

   public void reloadConfig() {
      Map<Player, String> temp = new HashMap<>(this.playerActiveItems);
      this.config = YamlConfiguration.loadConfiguration(this.configFile);
      temp.forEach((p, id) -> {
         this.hideItem(p);
         if (p.isOnline()) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.showItem(p, id), 2L);
         }
      });
   }

   private void setTeleportDurationSafe(ItemDisplay display, int duration) {
      try {
         Method method = ItemDisplay.class.getMethod("setTeleportDuration", int.class);
         method.invoke(display, duration);
      } catch (NoSuchMethodException var4) {
      } catch (Exception var5) {
         this.plugin.getLogger().warning("无法设置 ItemDisplay 的 teleport duration: " + var5.getMessage());
      }
   }

   public boolean showItem(Player player, String itemId) {
      this.hideItem(player);
      if (!this.config.contains("display-items." + itemId)) {
         player.sendMessage(ChatColor.RED + "物品ID不存在: " + itemId);
         return false;
      } else {
         try {
            String path = "display-items." + itemId;
            Material material = Material.valueOf(this.config.getString(path + ".material", "DIAMOND"));
            int cmd = this.config.getInt(path + ".custom-model-data", 0);
            float scale = (float)this.config.getDouble(path + ".scale", 0.3);
            double xOffset = this.config.getDouble(path + ".offset.x", 0.0);
            double yOffset = this.config.getDouble(path + ".offset.y", 0.0);
            double zOffset = this.config.getDouble(path + ".offset.z", 2.0);
            boolean glow = this.config.getBoolean(path + ".glow", true);
            boolean rotate = this.config.getBoolean(path + ".rotate", false);
            float rotateSpeed = (float)this.config.getDouble(path + ".rotate-speed", 1.0);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null && cmd > 0) {
               meta.setCustomModelData(cmd);
               item.setItemMeta(meta);
            }

            Location eye = player.getEyeLocation();
            Vector dir = eye.getDirection().normalize();
            Location loc = eye.clone().add(dir.multiply(zOffset)).add(0.0, yOffset, 0.0).add(this.getRightVector(eye).multiply(xOffset));
            ItemDisplay display = (ItemDisplay)player.getWorld().spawn(loc, ItemDisplay.class);
            display.setItemStack(item);
            display.setGlowing(glow);
            display.setViewRange(100.0F);
            display.setPersistent(false);
            display.setBillboard(Billboard.FIXED);
            this.setTeleportDurationSafe(display, 2);
            this.setScale(
               display, scale * (float)CursorMenuPlugin.cursorSizeX, scale * (float)CursorMenuPlugin.cursorSizeY, scale * (float)CursorMenuPlugin.cursorSizeZ
            );
            this.activeDisplays.add(new PlayerDisplayPair(player, display, itemId));
            this.playerActiveItems.put(player, itemId);
            int posTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
               if (player.isOnline() && display.isValid()) {
                  Location newEye = player.getEyeLocation();
                  Vector newDir = newEye.getDirection().normalize();
                  Location newLoc = newEye.clone().add(newDir.multiply(zOffset)).add(0.0, yOffset, 0.0).add(this.getRightVector(newEye).multiply(xOffset));
                  display.teleport(newLoc);
               }
            }, 0L, 1L).getTaskId();
            int rotTaskId = -1;
            if (rotate) {
               float radPerTick = rotateSpeed * 0.0174533F;
               rotTaskId = Bukkit.getScheduler()
                  .runTaskTimer(
                     this.plugin,
                     () -> {
                        if (player.isOnline() && display.isValid()) {
                           Transformation tr = display.getTransformation();
                           display.setTransformation(
                              new Transformation(
                                 tr.getTranslation(), tr.getLeftRotation().rotateAxis(radPerTick, 0.0F, 1.0F, 0.0F), tr.getScale(), tr.getRightRotation()
                              )
                           );
                        }
                     },
                     0L,
                     1L
                  )
                  .getTaskId();
            }

            this.displayTasks.add(new PlayerTaskPair(player, posTaskId, rotTaskId));
            player.sendMessage(ChatColor.GREEN + "已显示物品: " + itemId);
            this.mountPig(player);
            return true;
         } catch (Exception var25) {
            player.sendMessage(ChatColor.RED + "显示失败: " + var25.getMessage());
            this.plugin.getLogger().warning("显示物品失败: " + var25.getMessage());
            return false;
         }
      }
   }

   public void hideItem(Player player) {
      this.dismountPig(player);
      this.displayTasks.removeIf(pair -> {
         if (pair.player.equals(player)) {
            Bukkit.getScheduler().cancelTask(pair.positionTaskId);
            if (pair.rotationTaskId != -1) {
               Bukkit.getScheduler().cancelTask(pair.rotationTaskId);
            }

            return true;
         } else {
            return false;
         }
      });
      this.activeDisplays.removeIf(pair -> {
         if (pair.player.equals(player)) {
            if (pair.display.isValid()) {
               pair.display.remove();
            }

            return true;
         } else {
            return false;
         }
      });
      this.playerActiveItems.remove(player);
   }

   private Vector getRightVector(Location loc) {
      float yawRad = (float)Math.toRadians(loc.getYaw());
      return new Vector(Math.cos(yawRad), 0.0, Math.sin(yawRad)).normalize();
   }

   private void setScale(ItemDisplay display, float scaleX, float scaleY, float scaleZ) {
      if (hasNewScaleApi()) {
         Transformation oldTr = display.getTransformation();
         Transformation newTr = new Transformation(
            oldTr.getTranslation(), oldTr.getLeftRotation(), new Vector3f(scaleX, scaleY, scaleZ), oldTr.getRightRotation()
         );
         display.setTransformation(newTr);
      } else {
         try {
            Method m = ItemDisplay.class.getMethod("setScale", float.class);
            m.invoke(display, Math.max(scaleX, Math.max(scaleY, scaleZ)));
         } catch (Exception var7) {
         }
      }
   }

   private static boolean hasNewScaleApi() {
      try {
         ItemDisplay.class.getMethod("getTransformation");
         return true;
      } catch (NoSuchMethodException var1) {
         return false;
      }
   }

   public List<String> getAllItemIds() {
      return this.config.contains("display-items") ? new ArrayList<>(this.config.getConfigurationSection("display-items").getKeys(false)) : new ArrayList<>();
   }

   private void mountPig(Player player) {
      if (!(player.getVehicle() instanceof Pig pig && this.ridingPig.get(player) == pig)) {
         if (!player.isInsideVehicle()) {
            this.dismountPig(player);
            Location loc = player.getLocation();
            Pig pigx = (Pig)player.getWorld().spawn(loc, Pig.class, p -> {
               p.setAI(false);
               p.setAware(false);
               p.setSilent(true);
               p.setInvulnerable(true);
               p.setCollidable(false);
               p.setGravity(false);
               p.setInvisible(true);
            });
            this.ridingPig.put(player, pigx);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (pig.isValid() && player.isOnline()) {
                  pig.addPassenger(player);
               }
            }, 1L);
         }
      }
   }

   private void dismountPig(Player player) {
      Pig pig = this.ridingPig.remove(player);
      if (pig != null && pig.isValid()) {
         pig.removePassenger(player);
         pig.remove();
      }
   }

   public String getPlayerActiveItemId(Player player) {
      return this.playerActiveItems.getOrDefault(player, "");
   }

   @EventHandler
   public void onVehicleExit(VehicleExitEvent e) {
      if (e.getExited() instanceof Player p && this.ridingPig.containsKey(p) && e.getVehicle().equals(this.ridingPig.get(p))) {
         this.hideItem(p);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent e) {
      this.hideItem(e.getPlayer());
   }
}
