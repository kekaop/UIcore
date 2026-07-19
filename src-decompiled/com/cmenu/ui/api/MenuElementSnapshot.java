package com.cmenu.ui.api;

import com.cmenu.ui.layout.MenuLayout;
import java.util.Collections;
import java.util.LinkedHashMap;
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

   public MenuElementSnapshot(String menuId, MenuLayout layout) {
      this.menuId = menuId;
      this.elementId = layout.key;
      this.type = layout.getLayoutType();
      this.text = layout.name;
      this.x = layout.x;
      this.y = layout.y;
      this.z = layout.z;
      this.textSize = layout.getTextSize();
      this.configValues = Collections.unmodifiableMap(new LinkedHashMap<>(layout.copySourceConfig().getValues(true)));
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
