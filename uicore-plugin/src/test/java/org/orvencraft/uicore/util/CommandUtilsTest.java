package org.orvencraft.uicore.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandUtilsTest {
   @Test
   void queuedCommandCannotEscapeAClosedOrReplacedSession() {
      var generation = new java.util.concurrent.atomic.AtomicLong(7);
      var active = new java.util.concurrent.atomic.AtomicBoolean(true);
      var executed = new java.util.concurrent.atomic.AtomicInteger();
      Runnable queued = CommandUtils.guardSession(() -> active.get() && generation.get() == 7, executed::incrementAndGet);
      active.set(false);
      queued.run();
      assertEquals(0, executed.get(), "Close cancels pending console work");
      active.set(true);
      generation.set(8);
      queued.run();
      assertEquals(0, executed.get(), "Reopening cannot revive an old command");
      CommandUtils.guardSession(() -> active.get() && generation.get() == 8, executed::incrementAndGet).run();
      assertEquals(1, executed.get(), "The current visit still executes commands");
   }
   @Test
   void parsesExplicitInlineDelay() {
      CommandUtils.DelayedCommand parsed = CommandUtils.parseDelayedCommand("<delay=60>[message] Ready", 20L);
      assertEquals(60L, parsed.delayTicks());
      assertEquals("[message] Ready", parsed.command());
   }

   @Test
   void usesElementDelayWhenInlineDelayIsMissing() {
      CommandUtils.DelayedCommand parsed = CommandUtils.parseDelayedCommand("[player] spawn", 12L);
      assertEquals(12L, parsed.delayTicks());
      assertEquals("[player] spawn", parsed.command());
   }

   @Test
   void parsesOnlyTheLeadingActionTagBeforePlaceholderExpansion() {
      CommandUtils.CommandAction action = CommandUtils.parseCommandAction(" [ConSoLe] give %player% [player]");
      assertEquals(CommandUtils.ActionType.CONSOLE, action.type());
      assertEquals("give %player% [player]", action.argument());
   }

   @Test
   void consoleAllowlistMatchesTheCommandLabelAtATokenBoundary() {
      assertTrue(CommandUtils.isAllowedCommand("/give Alex diamond", "give"));
      assertTrue(CommandUtils.isAllowedCommand("give Alex diamond", "give Alex"));
      assertFalse(CommandUtils.isAllowedCommand("giveaway Alex", "give"));
      assertFalse(CommandUtils.isAllowedCommand("give Alex diamond", "give Bob"));
   }
   @Test
   void deniedLogDoesNotRevealLoginArgumentsOrControlCharacters() {
      String logged = CommandUtils.sanitizeForLog("login top-secret\nforged-line");
      assertFalse(logged.contains("top-secret"));
      assertFalse(logged.contains("\n"));
      assertFalse(CommandUtils.sanitizeForLog("some_plugin arbitrary-secret").contains("arbitrary-secret"));
   }
   @Test
   void messageBuiltinsAreResolvedWhileInputTokensRemainLiteral() {
      org.bukkit.entity.Player player = (org.bukkit.entity.Player)java.lang.reflect.Proxy.newProxyInstance(
         org.bukkit.entity.Player.class.getClassLoader(), new Class<?>[]{org.bukkit.entity.Player.class},
         (proxy, method, args) -> switch (method.getName()) {
            case "getName", "getDisplayName" -> "Viewer";
            case "getUniqueId" -> new java.util.UUID(0, 1);
            case "getGameMode" -> org.bukkit.GameMode.CREATIVE;
            case "getLocation" -> null;
            case "getLevel", "getTotalExperience", "getFoodLevel" -> 7;
            case "getHealth", "getMaxHealth" -> 20D;
            case "isOp", "isOnline", "isSneaking", "isFlying" -> true;
            default -> throw new UnsupportedOperationException(method.getName());
         });
      String resolved = CommandUtils.replaceBuiltinPlaceholders(player,
         "%player_name% %player_gamemode% %player_level% %player_is_flying% {input.password}", false);
      assertEquals("Viewer CREATIVE 7 true {input.password}", resolved);
   }
}
