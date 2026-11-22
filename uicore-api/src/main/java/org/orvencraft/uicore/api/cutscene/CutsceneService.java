package org.orvencraft.uicore.api.cutscene;

import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;

/**
 * Controls the player's current menu cutscene. Requests are dispatched onto the player's scheduler.
 * Control results report whether a request can be queued; choice acceptance is confirmed by its event.
 * Configuration changes affect new sessions only. Event callbacks may safely close or replace a menu.
 */
public interface CutsceneService {
   CutsceneStartResult start(Player player, String cutsceneId);
   boolean pause(Player player);
   boolean resume(Player player);
   boolean choose(Player player, String optionId);
   boolean skip(Player player);
   boolean stop(Player player);
   Optional<CutsceneSnapshot> state(Player player);
   Set<String> ids();
}
