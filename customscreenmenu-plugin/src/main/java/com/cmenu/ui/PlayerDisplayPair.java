package com.cmenu.ui;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

class PlayerDisplayPair {
   final Player player;
   final ItemDisplay display;
   final String itemId;

   PlayerDisplayPair(Player p, ItemDisplay d, String id) {
      this.player = p;
      this.display = d;
      this.itemId = id;
   }
}
