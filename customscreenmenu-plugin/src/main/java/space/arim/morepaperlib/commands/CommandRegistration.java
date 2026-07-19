package space.arim.morepaperlib.commands;

import java.lang.reflect.Field;
import java.util.Map;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import space.arim.morepaperlib.FeatureFailedException;
import space.arim.morepaperlib.MorePaperLib;

public final class CommandRegistration {
   private final MorePaperLib morePaperLib;
   private final boolean hasMethodGetCommandMap;
   private final boolean hasMethodGetKnownCommands;

   public CommandRegistration(MorePaperLib morePaperLib) {
      this.morePaperLib = morePaperLib;
      this.hasMethodGetCommandMap = morePaperLib.methodExists(Server.class, "getCommandMap");
      this.hasMethodGetKnownCommands = morePaperLib.methodExists(SimpleCommandMap.class, "getKnownCommands");
   }

   public CommandMap getServerCommandMap() {
      Server server = this.morePaperLib.getPlugin().getServer();
      return this.hasMethodGetCommandMap ? server.getCommandMap() : this.getFieldValue(server.getClass(), server, "commandMap", CommandMap.class);
   }

   public Map<String, Command> getCommandMapKnownCommands(SimpleCommandMap simpleCommandMap) {
      return this.hasMethodGetKnownCommands
         ? simpleCommandMap.getKnownCommands()
         : this.getFieldValue(SimpleCommandMap.class, simpleCommandMap, "knownCommands", Map.class);
   }

   <T, F> F getFieldValue(Class<? extends T> clazz, T object, String fieldName, Class<F> fieldType) {
      Field field;
      try {
         field = clazz.getDeclaredField(fieldName);
      } catch (SecurityException | NoSuchFieldException var9) {
         throw new FeatureFailedException("Unable to find field " + fieldName, var9);
      }

      Object fieldValue;
      try {
         field.setAccessible(true);
         fieldValue = field.get(object);
      } catch (IllegalArgumentException | IllegalAccessException | SecurityException var8) {
         throw new FeatureFailedException("Unable to access field " + fieldName, var8);
      }

      if (!fieldType.isInstance(fieldValue)) {
         throw new FeatureFailedException("Field " + fieldName + " not an instance of " + fieldType.getName());
      } else {
         return fieldType.cast(fieldValue);
      }
   }
}
