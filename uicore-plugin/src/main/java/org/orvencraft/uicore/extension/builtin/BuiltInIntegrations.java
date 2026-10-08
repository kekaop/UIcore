package org.orvencraft.uicore.extension.builtin;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.extension.ExtensionRegistration;
import org.orvencraft.uicore.extension.ExtensionManager;

/** Optional integrations shipped inside UIcore's JAR. */
public final class BuiltInIntegrations implements Listener {
   private final UIcorePlugin plugin;
   private final ExtensionManager extensions;
   private List<ExtensionRegistration> vault = List.of();
   private List<ExtensionRegistration> luckPerms = List.of();

   public BuiltInIntegrations(UIcorePlugin plugin, ExtensionManager extensions) {
      this.plugin = plugin;
      this.extensions = extensions;
   }

   public void start() {
      this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
      this.updateVault();
      this.updateLuckPerms();
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onPluginEnable(PluginEnableEvent event) {
      if (event.getPlugin().getName().equals("Vault")) this.updateVault();
      if (event.getPlugin().getName().equals("LuckPerms")) this.updateLuckPerms();
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onPluginDisable(PluginDisableEvent event) {
      if (event.getPlugin().getName().equals("Vault")) {
         close(this.vault);
         this.vault = List.of();
      }
      if (event.getPlugin().getName().equals("LuckPerms")) {
         close(this.luckPerms);
         this.luckPerms = List.of();
      }
   }

   private void updateVault() {
      if (!this.vault.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("Vault")) return;
      try {
         this.vault = VaultIntegration.register(this.plugin, this.extensions);
         this.plugin.getLogger().info("Built-in Vault integration enabled");
      } catch (RuntimeException | LinkageError exception) {
         this.plugin.getLogger().warning("Built-in Vault integration unavailable: " + exception.getClass().getSimpleName());
      }
   }

   private void updateLuckPerms() {
      if (!this.luckPerms.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
      try {
         this.luckPerms = LuckPermsIntegration.register(this.plugin, this.extensions);
         this.plugin.getLogger().info("Built-in LuckPerms integration enabled");
      } catch (RuntimeException | LinkageError exception) {
         this.plugin.getLogger().warning("Built-in LuckPerms integration unavailable: " + exception.getClass().getSimpleName());
      }
   }

   private static void close(List<ExtensionRegistration> registrations) {
      for (ExtensionRegistration registration : registrations) registration.close();
   }

   public void stop() {
      close(this.vault);
      close(this.luckPerms);
      this.vault = List.of();
      this.luckPerms = List.of();
   }
}
