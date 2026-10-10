package org.orvencraft.uicore.section;

import org.orvencraft.uicore.layout.MenuLayout;
import org.orvencraft.uicore.npc.NPCMenuSettings;
import org.orvencraft.uicore.schematic.SchematicPlacement;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
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
   private volatile Definition definition = new Definition(Map.of(), Map.of(), Map.of());
   private final Map<String, MenuLayout> materializedSlots;
   public NPCMenuSettings npcSettings;
   public MenuPrivacySettings privacy = MenuPrivacySettings.DEFAULT;
   public List<SchematicPlacement> schematics = List.of();
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
      boolean autoCommandsEnabled
   ) {
      this.distance = distance;
      this.world = world;
      this.cameraX = cameraX;
      this.cameraY = cameraY;
      this.cameraZ = cameraZ;
      this.yaw = yaw;
      this.pitch = pitch;
      this.materializedSlots = new ConcurrentHashMap<>();
      this.permission = permission;
      this.autoCommands = autoCommands;
      this.autoCommandDelays = autoCommandDelays;
      this.autoCommandsEnabled = autoCommandsEnabled;
   }

   public synchronized void add(String key, MenuLayout layout) {
      Map<String, MenuLayout> updated = new LinkedHashMap<>(definition.layouts());
      updated.put(key, layout);
      definition = new Definition(updated, definition.templates(), definition.slots());
   }

   public synchronized boolean addIfAbsent(String key, MenuLayout layout) {
      if (definition.layouts().containsKey(key)) return false;
      add(key, layout);
      return true;
   }

   public synchronized boolean edit(String key, UnaryOperator<MenuLayout> editor) {
      MenuLayout current = get(key);
      if (current == null) return false;
      MenuLayout replacement = java.util.Objects.requireNonNull(editor.apply(current));
      // A reentrant editor may itself change this element. Never silently overwrite it.
      if (get(key) != current) throw new IllegalStateException("Element changed during editor callback: " + key);
      add(key, replacement);
      return true;
   }

   public synchronized MenuLayout remove(String key) {
      Map<String, MenuLayout> updated = new LinkedHashMap<>(definition.layouts());
      MenuLayout removed = updated.remove(key);
      definition = new Definition(updated, definition.templates(), definition.slots());
      return removed;
   }

   public boolean contains(String key) {
      return definition.layouts().containsKey(key);
   }

   public synchronized void addTemplate(String key, MenuLayout template) {
      Map<String, MenuLayout> updated = new LinkedHashMap<>(definition.templates());
      updated.put(key, template);
      definition = new Definition(definition.layouts(), updated, definition.slots());
      this.materializedSlots.clear();
   }

   public synchronized void addSlot(String key, MenuSlot slot) {
      Map<String, MenuSlot> updated = new LinkedHashMap<>(definition.slots());
      updated.put(key, slot);
      definition = new Definition(definition.layouts(), definition.templates(), updated);
      this.materializedSlots.clear();
   }

   public boolean hasTemplate(String key) {
      return key != null && definition.templates().containsKey(key);
   }

   public boolean hasSlot(String key) {
      return key != null && definition.slots().containsKey(key);
   }

   public MenuLayout materializeSlot(String menuId, String slotId, String templateId) {
      return materializeSlot(menuId, slotId, templateId, definition);
   }

   public synchronized MenuLayout materializeSlot(String menuId, String slotId, String templateId, Definition snapshot) {
      MenuSlot slot = snapshot.slots().get(slotId);
      MenuLayout template = snapshot.templates().get(templateId);
      if (slot == null || template == null) {
         return null;
      }

      String cacheKey = slotId + "\u0000" + templateId;
      if (snapshot != definition) return slot.materialize(menuId, templateId, template, this.world);
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
         this.autoCommandsEnabled
      );
      copy.key = this.key;
      copy.npcSettings = this.npcSettings;
      copy.privacy = this.privacy;
      copy.schematics = this.schematics;
      copy.positionType = this.positionType;
      copy.playerOffsetRight = this.playerOffsetRight;
      copy.playerOffsetUp = this.playerOffsetUp;
      copy.playerOffsetForward = this.playerOffsetForward;
      copy.playerYawOffset = this.playerYawOffset;
      copy.playerPitchOffset = this.playerPitchOffset;
      copy.definition = definition;
      return copy;
   }

   /**
    * Runtime player-relative sections only own camera coordinates.  Their menu
    * definition remains the canonical section definition and must be refreshed
    * after a global API mutation.
    */
   public synchronized void syncDefinitionFrom(Section definition) {
      if (definition == null || definition == this) {
         return;
      }

      Definition snapshot = definition.definition();
      if (this.definition != snapshot) {
         this.definition = snapshot;
         this.materializedSlots.clear();
      }
      this.npcSettings = definition.npcSettings;
      this.privacy = definition.privacy;
      this.schematics = definition.schematics;
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
      return definition.layouts().get(key);
   }

   /** One immutable, ordered view of all menu definitions for a render or API read. */
   public Definition definition() { return definition; }

   public record Definition(Map<String, MenuLayout> layouts, Map<String, MenuLayout> templates, Map<String, MenuSlot> slots) {
      public Definition {
         layouts = Collections.unmodifiableMap(new LinkedHashMap<>(layouts));
         templates = Collections.unmodifiableMap(new LinkedHashMap<>(templates));
         slots = Collections.unmodifiableMap(new LinkedHashMap<>(slots));
      }
   }
}
