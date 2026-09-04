package org.orvencraft.uicore.section;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.npc.NPCMenuSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Section {
   public String key;
   public final List<String> autoCommands;
   public final List<Long> autoCommandDelays;
   public final boolean autoCommandsEnabled;
   public double distance;
   public String world;
   public double cameraX;
   public double cameraY;
   public double cameraZ;
   public float yaw;
   public float pitch;
   public final String permission;
   public final boolean wasdEnabled;
   public LinkedHashMap<String, MenuLayout> layouts;
   public final LinkedHashMap<String, MenuLayout> templates;
   public final LinkedHashMap<String, MenuSlot> slots;
   private final Map<String, MenuLayout> materializedSlots;
   public NPCMenuSettings npcSettings;
   public MenuPositionType positionType = MenuPositionType.FIXED;
   public double playerOffsetRight;
   public double playerOffsetUp;
   public double playerOffsetForward;
   public float playerYawOffset;
   public float playerPitchOffset;

   public Section(
      double distance,
      String world,
      double cameraX,
      double cameraY,
      double cameraZ,
      float yaw,
      float pitch,
      String permission,
      List<String> autoCommands,
      List<Long> autoCommandDelays,
      boolean autoCommandsEnabled,
      boolean wasdEnabled
   ) {
      this.distance = distance;
      this.world = world;
      this.cameraX = cameraX;
      this.cameraY = cameraY;
      this.cameraZ = cameraZ;
      this.yaw = yaw;
      this.pitch = pitch;
      this.layouts = new LinkedHashMap<>();
      this.templates = new LinkedHashMap<>();
      this.slots = new LinkedHashMap<>();
      this.materializedSlots = new ConcurrentHashMap<>();
      this.permission = permission;
      this.autoCommands = autoCommands;
      this.autoCommandDelays = autoCommandDelays;
      this.autoCommandsEnabled = autoCommandsEnabled;
      this.wasdEnabled = wasdEnabled;
   }

   public Section(
      double distance,
      String world,
      double cameraX,
      double cameraY,
      double cameraZ,
      float yaw,
      float pitch,
      String permission,
      List<String> autoCommands,
      List<Long> autoCommandDelays,
      boolean autoCommandsEnabled
   ) {
      this(distance, world, cameraX, cameraY, cameraZ, yaw, pitch, permission, autoCommands, autoCommandDelays, autoCommandsEnabled, true);
   }

   public void add(String key, MenuLayout layout) {
      this.layouts.put(key, layout);
   }

   public MenuLayout remove(String key) {
      return this.layouts.remove(key);
   }

   public boolean contains(String key) {
      return this.layouts.containsKey(key);
   }

   public void addTemplate(String key, MenuLayout template) {
      this.templates.put(key, template);
      this.materializedSlots.clear();
   }

   public void addSlot(String key, MenuSlot slot) {
      this.slots.put(key, slot);
      this.materializedSlots.clear();
   }

   public boolean hasTemplate(String key) {
      return key != null && this.templates.containsKey(key);
   }

   public boolean hasSlot(String key) {
      return key != null && this.slots.containsKey(key);
   }

   public MenuLayout materializeSlot(String menuId, String slotId, String templateId) {
      MenuSlot slot = this.slots.get(slotId);
      MenuLayout template = this.templates.get(templateId);
      if (slot == null || template == null) {
         return null;
      }

      String cacheKey = slotId + "\u0000" + templateId;
      return this.materializedSlots.computeIfAbsent(cacheKey, ignored -> slot.materialize(menuId, templateId, template, this.world));
   }

   public Section copyWithCamera(String world, double cameraX, double cameraY, double cameraZ, float yaw, float pitch) {
      Section copy = new Section(
         this.distance,
         world,
         cameraX,
         cameraY,
         cameraZ,
         yaw,
         pitch,
         this.permission,
         this.autoCommands,
         this.autoCommandDelays,
         this.autoCommandsEnabled,
         this.wasdEnabled
      );
      copy.key = this.key;
      copy.npcSettings = this.npcSettings;
      copy.positionType = this.positionType;
      copy.playerOffsetRight = this.playerOffsetRight;
      copy.playerOffsetUp = this.playerOffsetUp;
      copy.playerOffsetForward = this.playerOffsetForward;
      copy.playerYawOffset = this.playerYawOffset;
      copy.playerPitchOffset = this.playerPitchOffset;
      copy.layouts.putAll(this.layouts);
      copy.templates.putAll(this.templates);
      copy.slots.putAll(this.slots);
      return copy;
   }

   public boolean isPlayerRelative() {
      return this.positionType == MenuPositionType.PLAYER;
   }

   public Section resolveFromPlayerLocation(Location playerLocation) {
      if (playerLocation == null || playerLocation.getWorld() == null) {
         return this;
      }

      Vector forward = playerLocation.getDirection().normalize();
      Vector right = new Vector(-forward.getZ(), 0.0, forward.getX());
      if (right.lengthSquared() < 1.0E-8) {
         double yaw = Math.toRadians(playerLocation.getYaw());
         Vector horizontalForward = new Vector(-Math.sin(yaw), 0.0, Math.cos(yaw));
         right = new Vector(-horizontalForward.getZ(), 0.0, horizontalForward.getX());
      }
      right.normalize();
      Vector up = forward.getCrossProduct(right).multiply(-1.0).normalize();

      Location camera = playerLocation.clone()
         .add(right.multiply(this.playerOffsetRight))
         .add(up.multiply(this.playerOffsetUp))
         .add(forward.multiply(this.playerOffsetForward));
      return this.copyWithCamera(
         camera.getWorld().getName(),
         camera.getX(),
         camera.getY(),
         camera.getZ(),
         playerLocation.getYaw() + this.playerYawOffset,
         playerLocation.getPitch() + this.playerPitchOffset
      );
   }

   public MenuLayout get(String key) {
      return this.layouts.get(key);
   }
}
