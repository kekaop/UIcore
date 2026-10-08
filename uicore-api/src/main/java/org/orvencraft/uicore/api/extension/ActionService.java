package org.orvencraft.uicore.api.extension;

import org.bukkit.plugin.Plugin;

public interface ActionService {
   @FunctionalInterface
   interface Handler {
      void execute(ExtensionContext context) throws Exception;
   }

   ExtensionRegistration register(Plugin owner, String id, Handler handler);
   boolean isRegistered(String id);
}
