package org.orvencraft.uicore.extension.builtin;

import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.extension.ExtensionRegistration;
import org.orvencraft.uicore.extension.ExtensionManager;

/** Read-only Vault values for menus. The economy provider can change at runtime. */
final class VaultIntegration {
   private VaultIntegration() {}

   static List<ExtensionRegistration> register(UIcorePlugin plugin, ExtensionManager extensions) {
      return List.of(
         extensions.data().register(plugin, "vault:balance", 20L, context -> {
            Economy economy = economy();
            return economy == null ? null : economy.getBalance(context.player());
         }),
         extensions.data().register(plugin, "vault:formatted-balance", 20L, context -> {
            Economy economy = economy();
            return economy == null ? null : economy.format(economy.getBalance(context.player()));
         }),
         extensions.conditions().register(plugin, "vault:has-balance", context -> {
            Double amount = amount(context.arguments().get("amount"));
            Economy economy = economy();
            return amount != null && economy != null && economy.has(context.player(), amount);
         })
      );
   }

   private static Economy economy() {
      RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
      Economy economy = provider == null ? null : provider.getProvider();
      return economy != null && economy.isEnabled() ? economy : null;
   }

   static Double amount(Object raw) {
      if (!(raw instanceof Number) && !(raw instanceof String)) return null;
      try {
         double value = Double.parseDouble(String.valueOf(raw));
         return Double.isFinite(value) && value >= 0.0 ? value : null;
      } catch (NumberFormatException exception) {
         return null;
      }
   }
}
