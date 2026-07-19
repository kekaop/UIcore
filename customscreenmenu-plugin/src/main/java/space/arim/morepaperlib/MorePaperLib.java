package space.arim.morepaperlib;

import java.util.Objects;
import org.bukkit.plugin.Plugin;
import space.arim.morepaperlib.commands.CommandRegistration;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

public class MorePaperLib {
   private final Plugin plugin;

   public MorePaperLib(Plugin plugin) {
      this.plugin = Objects.requireNonNull(plugin, "plugin");
   }

   public Plugin getPlugin() {
      return this.plugin;
   }

   public boolean methodExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
      try {
         clazz.getMethod(methodName, parameterTypes);
         return true;
      } catch (NoSuchMethodException var5) {
         return false;
      }
   }

   public CommandRegistration commandRegistration() {
      return new CommandRegistration(this);
   }

   public GracefulScheduling scheduling() {
      return new GracefulScheduling(this);
   }
}
