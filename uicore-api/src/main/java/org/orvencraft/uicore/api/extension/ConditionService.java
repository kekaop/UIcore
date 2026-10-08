package org.orvencraft.uicore.api.extension;

import org.bukkit.plugin.Plugin;

public interface ConditionService {
   @FunctionalInterface
   interface Predicate {
      boolean test(ExtensionContext context) throws Exception;
   }

   ExtensionRegistration register(Plugin owner, String id, Predicate predicate);
   boolean isRegistered(String id);
}
