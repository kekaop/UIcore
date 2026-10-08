package org.orvencraft.uicore.extension.builtin;

import java.util.List;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.extension.ExtensionRegistration;
import org.orvencraft.uicore.extension.ExtensionManager;

/** Context-aware, cached LuckPerms values for the online player. */
final class LuckPermsIntegration {
   private LuckPermsIntegration() {}

   static List<ExtensionRegistration> register(UIcorePlugin plugin, ExtensionManager extensions) {
      return List.of(
         extensions.data().register(plugin, "luckperms:primary-group", 20L, context -> {
            User user = user(context.player());
            return user == null ? null : user.getPrimaryGroup();
         }),
         extensions.data().register(plugin, "luckperms:prefix", 20L, context -> {
            CachedMetaData meta = meta(context.player());
            return meta == null ? null : meta.getPrefix();
         }),
         extensions.data().register(plugin, "luckperms:suffix", 20L, context -> {
            CachedMetaData meta = meta(context.player());
            return meta == null ? null : meta.getSuffix();
         }),
         extensions.conditions().register(plugin, "luckperms:in-group", context -> {
            String group = context.argument("group");
            User user = user(context.player());
            if (group == null || group.isBlank() || user == null) return false;
            return user.getInheritedGroups(queryOptions(context.player())).stream()
               .anyMatch(inherited -> inherited.getName().equalsIgnoreCase(group));
         }),
         extensions.conditions().register(plugin, "luckperms:meta-equals", context -> {
            String key = context.argument("key");
            String expected = context.argument("value");
            CachedMetaData meta = meta(context.player());
            return key != null && !key.isBlank() && expected != null && meta != null
               && expected.equals(meta.getMetaValue(key));
         })
      );
   }

   private static User user(Player player) {
      return LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
   }

   private static QueryOptions queryOptions(Player player) {
      return LuckPermsProvider.get().getContextManager().getQueryOptions(player);
   }

   private static CachedMetaData meta(Player player) {
      User user = user(player);
      return user == null ? null : user.getCachedData().getMetaData(queryOptions(player));
   }
}
