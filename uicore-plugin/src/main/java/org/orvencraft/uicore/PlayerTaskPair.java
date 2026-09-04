package org.orvencraft.uicore;

import org.bukkit.entity.Player;

class PlayerTaskPair {
   final Player player;
   final int positionTaskId;
   final int rotationTaskId;

   PlayerTaskPair(Player p, int posId, int rotId) {
      this.player = p;
      this.positionTaskId = posId;
      this.rotationTaskId = rotId;
   }
}
