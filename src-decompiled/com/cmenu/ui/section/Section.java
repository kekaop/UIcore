package com.cmenu.ui.section;

import com.cmenu.ui.layout.MenuLayout;
import java.util.LinkedHashMap;
import java.util.List;

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
      copy.layouts.putAll(this.layouts);
      return copy;
   }

   public MenuLayout get(String key) {
      return this.layouts.get(key);
   }
}
