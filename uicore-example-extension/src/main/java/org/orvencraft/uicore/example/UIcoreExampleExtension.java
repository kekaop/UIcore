package org.orvencraft.uicore.example;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import org.orvencraft.uicore.api.UIcoreApi;
import org.orvencraft.uicore.api.UIcoreProvider;
import org.orvencraft.uicore.api.MenuElementSpec;
import org.orvencraft.uicore.api.extension.ExtensionRegistration;

/** Demonstrates owner-scoped registrations available from both Java and YAML. */
public final class UIcoreExampleExtension extends JavaPlugin {
   private final List<ExtensionRegistration> registrations = new ArrayList<>();

   @Override
   public void onEnable() {
      UIcoreApi api = UIcoreProvider.get();
      if (api == null) throw new IllegalStateException("UIcore API service is unavailable");

      this.registrations.add(api.actions().register(this, "example:hello", context -> {
         String recipient = context.argument("target");
         context.player().sendMessage("Hello from UIcore" + (recipient == null ? "!" : ", " + recipient + "!"));
      }));
      this.registrations.add(api.data().register(this, "example:balance", context -> 100));
      this.registrations.add(api.data().register(this, "example:item-material", context -> "EMERALD"));
      this.registrations.add(api.data().register(this, "example:highlighted", context -> true));
      this.registrations.add(api.conditions().register(this, "example:has-permission", context -> {
         String permission = context.argument("permission");
         return permission != null && context.player().hasPermission(permission);
      }));
      this.registrations.add(api.components().register(this, "example:profile-card", context -> List.of(
         MenuElementSpec.builder("title").text("<gold>" + context.parameter("title")).position(0, 0.3, 0).build(),
         MenuElementSpec.builder("balance").text("Balance: {example:balance}").position(0, 0, 0).build(),
         MenuElementSpec.builder("hello").text("<green>Say hello").position(0, -0.3, 0)
            .action("example:hello", java.util.Map.of("target", context.player().getName())).build()
      )));
   }

   @Override
   public void onDisable() {
      for (ExtensionRegistration registration : this.registrations) registration.close();
      this.registrations.clear();
   }
}
