package org.orvencraft.uicore.api;

import java.util.Map;

public final class MenuElementSnapshot {
   private final String menuId;
   private final String elementId;
   private final String type;
   private final String text;
   private final double x;
   private final double y;
   private final double z;
   private final double textSize;
   private final Map<String, Object> configValues;

   public MenuElementSnapshot(
      String menuId,
      String elementId,
      String type,
      String text,
      double x,
      double y,
      double z,
      double textSize,
      Map<String, Object> configValues
   ) {
      this.menuId = menuId;
      this.elementId = elementId;
      this.type = type;
      this.text = text;
      this.x = x;
      this.y = y;
      this.z = z;
      this.textSize = textSize;
      this.configValues = ConfigurationCopy.immutableMap(configValues);
   }

   public String menuId() {
      return this.menuId;
   }

   public String elementId() {
      return this.elementId;
   }

   public String type() {
      return this.type;
   }

   public String text() {
      return this.text;
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public double z() {
      return this.z;
   }

   public double textSize() {
      return this.textSize;
   }

   public Map<String, Object> configValues() {
      return this.configValues;
   }
}
