package org.orvencraft.uicore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.orvencraft.uicore.api.cutscene.CutsceneSnapshot;

public final class CutsceneStartEvent extends CutsceneEvent {
   private static final HandlerList HANDLERS = new HandlerList();
   public CutsceneStartEvent(Player player, CutsceneSnapshot snapshot) {
      super(player, snapshot);
   }
   @Override public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}

