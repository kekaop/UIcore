package space.arim.morepaperlib.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import space.arim.morepaperlib.MorePaperLib;

public final class MorePaperLibAdventure {
   private final MorePaperLib morePaperLib;
   private final boolean hasMethodKick;
   private final boolean hasMethodDisallow;

   public MorePaperLibAdventure(MorePaperLib morePaperLib) {
      this.morePaperLib = morePaperLib;
      this.hasMethodKick = morePaperLib.methodExists(Player.class, "kick", Component.class);
      this.hasMethodDisallow = morePaperLib.methodExists(AsyncPlayerPreLoginEvent.class, "disallow", Result.class, Component.class);
   }

   public MorePaperLib getMorePaperLib() {
      return this.morePaperLib;
   }

   public void kickPlayer(Player player, Component message) {
      if (this.hasMethodKick) {
         player.kick(message);
      } else {
         player.kickPlayer(LegacyComponentSerializer.legacySection().serialize(message));
      }
   }

   public void disallowPreLoginEvent(AsyncPlayerPreLoginEvent event, Result result, Component message) {
      if (this.hasMethodDisallow) {
         event.disallow(result, message);
      } else {
         event.disallow(result, LegacyComponentSerializer.legacySection().serialize(message));
      }
   }
}
