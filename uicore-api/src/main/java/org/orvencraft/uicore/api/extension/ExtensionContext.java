package org.orvencraft.uicore.api.extension;

import java.util.Map;
import java.util.Objects;
import org.bukkit.entity.Player;

/** Context for an extension invoked while the player owns the current server region. */
public final class ExtensionContext {
   private final Player player;
   private final String menuId;
   private final String elementId;
   private final Map<String, Object> arguments;
   private boolean cancelled;

   public ExtensionContext(Player player, String menuId, String elementId, Map<String, Object> arguments) {
      this.player = Objects.requireNonNull(player, "player");
      this.menuId = Objects.requireNonNull(menuId, "menuId");
      this.elementId = Objects.requireNonNull(elementId, "elementId");
      this.arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
   }

   public Player player() { return this.player; }
   public String menuId() { return this.menuId; }
   public String elementId() { return this.elementId; }
   public Map<String, Object> arguments() { return this.arguments; }
   public String argument(String key) {
      Object value = this.arguments.get(key);
      return value == null ? null : String.valueOf(value);
   }
   public void cancel() { this.cancelled = true; }
   public boolean isCancelled() { return this.cancelled; }
}
