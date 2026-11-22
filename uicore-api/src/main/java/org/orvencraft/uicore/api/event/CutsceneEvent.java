package org.orvencraft.uicore.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.orvencraft.uicore.api.cutscene.CutsceneSnapshot;

/**
 * Playback events run on the player's owning scheduler, which may be a Folia region thread.
 * A PLUGIN_DISABLED abort runs on the shutdown thread: listeners must use their own enabled
 * plugin's entity scheduler before accessing player state during shutdown.
 */
public abstract class CutsceneEvent extends Event {
   private final Player player;
   private final CutsceneSnapshot snapshot;

   protected CutsceneEvent(Player player, CutsceneSnapshot snapshot) {
      super(!Bukkit.isPrimaryThread());
      this.player = player;
      this.snapshot = snapshot;
   }

   public Player getPlayer() { return this.player; }
   public String getCutsceneId() { return this.snapshot.id(); }
   public CutsceneSnapshot getSnapshot() { return this.snapshot; }
}
