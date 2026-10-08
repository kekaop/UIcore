package org.orvencraft.uicore.api.extension;

import java.util.List;
import org.bukkit.plugin.Plugin;
import org.orvencraft.uicore.api.MenuElementSpec;

/** Registers components composed from ordinary UIcore elements. */
public interface ComponentService {
   @FunctionalInterface
   interface Factory {
      List<MenuElementSpec> create(ComponentContext context) throws Exception;
   }

   ExtensionRegistration register(Plugin owner, String id, Factory factory);
   boolean isRegistered(String id);
}
