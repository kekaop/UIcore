package com.cmenu.ui.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class CustomScreenMenuProvider {
   private CustomScreenMenuProvider() {
   }

   public static CustomScreenMenuApi get() {
      RegisteredServiceProvider<CustomScreenMenuApi> provider = Bukkit.getServicesManager().getRegistration(CustomScreenMenuApi.class);
      return provider == null ? null : provider.getProvider();
   }
}
