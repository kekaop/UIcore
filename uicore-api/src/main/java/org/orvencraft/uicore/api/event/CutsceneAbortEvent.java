package org.orvencraft.uicore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.orvencraft.uicore.api.cutscene.CutsceneSnapshot;

public final class CutsceneAbortEvent extends CutsceneEvent {
   private static final HandlerList HANDLERS = new HandlerList();
   private final org.orvencraft.uicore.api.cutscene.CutsceneAbortReason reason;
   private final String diagnostic;
   public CutsceneAbortEvent(Player player, CutsceneSnapshot snapshot, org.orvencraft.uicore.api.cutscene.CutsceneAbortReason reason) {
      this(player, snapshot, reason, "");
   }
   public CutsceneAbortEvent(Player player, CutsceneSnapshot snapshot, org.orvencraft.uicore.api.cutscene.CutsceneAbortReason reason, String diagnostic) {
      super(player, snapshot); this.reason = reason;
      this.diagnostic = diagnostic;
   }
   public org.orvencraft.uicore.api.cutscene.CutsceneAbortReason getReason() { return this.reason; }
   public String getDiagnostic() { return this.diagnostic; }
   @Override public HandlerList getHandlers() { return HANDLERS; }
   public static HandlerList getHandlerList() { return HANDLERS; }
}

