package org.orvencraft.uicore.api.extension;

import org.bukkit.plugin.Plugin;

public interface DataService {
   @FunctionalInterface
   interface Provider {
      Object resolve(ExtensionContext context) throws Exception;
   }

   default ExtensionRegistration register(Plugin owner, String id, Provider provider) {
      return this.register(owner, id, 1L, provider);
   }
   ExtensionRegistration register(Plugin owner, String id, long minRefreshTicks, Provider provider);
   boolean isRegistered(String id);
}
