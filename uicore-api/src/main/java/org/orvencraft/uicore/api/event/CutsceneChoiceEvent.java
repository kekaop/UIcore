package org.orvencraft.uicore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.orvencraft.uicore.api.cutscene.CutsceneSnapshot;

public final class CutsceneChoiceEvent extends CutsceneEvent {
   private static final HandlerList HANDLERS = new HandlerList();
   private final String optionId;
   public CutsceneChoiceEvent(Player player, CutsceneSnapshot snapshot, String optionId) {
      super(player, snapshot); this.optionId = optionId;
   }
   public String getOptionId() { return this.optionId; }
   @Override public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}

