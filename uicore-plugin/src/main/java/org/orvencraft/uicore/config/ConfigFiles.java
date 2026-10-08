package org.orvencraft.uicore.config;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Strict parsing shared by startup and reload; never substitutes empty defaults on errors. */
public final class ConfigFiles {
   private ConfigFiles() { }
   public static YamlConfiguration loadStrict(File file) {
      YamlConfiguration config = new YamlConfiguration();
      try { config.load(file); }
      catch (IOException | InvalidConfigurationException exception) {
         throw new IllegalArgumentException("Invalid configuration " + file.getName(), exception);
      }
      for (String path : config.getKeys(true)) {
         Object value = config.get(path);
         if (value instanceof Number number && (!Double.isFinite(number.doubleValue()) || Math.abs(number.doubleValue()) > 30_000_000D))
            throw new IllegalArgumentException(file.getName() + ": invalid numeric value at " + path);
         if (value instanceof String text && text.length() > 16384)
            throw new IllegalArgumentException(file.getName() + ": excessive text at " + path);
      }
      return config;
   }
}
