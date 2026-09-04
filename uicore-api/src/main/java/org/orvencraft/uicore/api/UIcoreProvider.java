package org.orvencraft.uicore.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class UIcoreProvider {
   private UIcoreProvider() {
   }

   public static UIcoreApi get() {
      RegisteredServiceProvider<UIcoreApi> provider = Bukkit.getServicesManager().getRegistration(UIcoreApi.class);
      return provider == null ? null : provider.getProvider();
   }
}
