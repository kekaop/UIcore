package org.orvencraft.uicore.npc;

import org.bukkit.configuration.ConfigurationSection;

public record NPCMenuSettings(
   Boolean enabled,
   Double offsetX,
   Double offsetY,
   Double offsetZ,
   Float yawOffset,
   Float pitch,
   Boolean autoRotateEnabled,
   Float autoRotateSpeed,
   Boolean syncSkin,
   Boolean syncEquipment,
   Boolean showName,
   String nameFormat,
   String customModel
) {
   public static NPCMenuSettings from(ConfigurationSection section) {
      if (section == null) {
         return null;
      }
      ConfigurationSection offset = section.getConfigurationSection("offset");
      ConfigurationSection rotate = section.getConfigurationSection("auto-rotate");
      ConfigurationSection sync = section.getConfigurationSection("sync");
      ConfigurationSection name = section.getConfigurationSection("name-display");
      ConfigurationSection model = section.getConfigurationSection("custom-model");
      String modelName = model == null ? section.getString("custom-model", null) : model.getString("model", model.getString("name", null));
      if (model != null && !model.getBoolean("enabled", true)) {
         modelName = null;
      }

      return new NPCMenuSettings(
         readBoolean(section, "enabled"),
         readDouble(offset, "x"),
         readDouble(offset, "y"),
         readDouble(offset, "z"),
         readFloat(section, "yaw-offset"),
         readFloat(section, "pitch"),
         readBoolean(rotate, "enabled"),
         readFloat(rotate, "speed"),
         readBoolean(sync, "skin"),
         readBoolean(sync, "equipment"),
         readBoolean(name, "show"),
         name == null ? null : name.getString("format", null),
         blankToNull(modelName)
      );
   }

   private static Boolean readBoolean(ConfigurationSection section, String key) {
      return section != null && section.contains(key) ? section.getBoolean(key) : null;
   }

   private static Double readDouble(ConfigurationSection section, String key) {
      return section != null && section.contains(key) ? section.getDouble(key) : null;
   }

   private static Float readFloat(ConfigurationSection section, String key) {
      return section != null && section.contains(key) ? (float)section.getDouble(key) : null;
   }

   private static String blankToNull(String value) {
      return value == null || value.isBlank() ? null : value.trim();
   }
}
