package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class MenuPositionTypeTest {
   @Test
   void parsesFixedAndPlayerAliases() {
      assertEquals(MenuPositionType.FIXED, MenuPositionType.parse(null));
      assertEquals(MenuPositionType.FIXED, MenuPositionType.parse("fixed"));
      assertEquals(MenuPositionType.PLAYER, MenuPositionType.parse("player"));
      assertEquals(MenuPositionType.PLAYER, MenuPositionType.parse("player-relative"));
      assertEquals(MenuPositionType.PLAYER, MenuPositionType.parse("in_front_of_player"));
   }

   @Test
   void resolvesOffsetsInPlayerViewAxes() {
      World world = (World)Proxy.newProxyInstance(
         World.class.getClassLoader(),
         new Class<?>[]{World.class},
         (proxy, method, args) -> "getName".equals(method.getName()) ? "test-world" : defaultValue(method.getReturnType())
      );
      Section section = new Section(1.5, "unused", 0, 0, 0, 0, 0, "", List.of(), List.of(), false);
      section.positionType = MenuPositionType.PLAYER;
      section.playerOffsetRight = 1.0;
      section.playerOffsetUp = 2.0;
      section.playerOffsetForward = 3.0;
      section.playerYawOffset = 10.0F;
      section.playerPitchOffset = 5.0F;

      Section resolved = section.resolveFromPlayerLocation(new Location(world, 10.0, 20.0, 30.0, 0.0F, 0.0F));

      assertEquals("test-world", resolved.world);
      assertEquals(9.0, resolved.cameraX, 1.0E-6);
      assertEquals(22.0, resolved.cameraY, 1.0E-6);
      assertEquals(33.0, resolved.cameraZ, 1.0E-6);
      assertEquals(10.0F, resolved.yaw, 1.0E-6F);
      assertEquals(5.0F, resolved.pitch, 1.0E-6F);
   }

   private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) {
         return null;
      }
      if (type == boolean.class) {
         return false;
      }
      if (type == char.class) {
         return '\0';
      }
      if (type == byte.class) {
         return (byte)0;
      }
      if (type == short.class) {
         return (short)0;
      }
      if (type == int.class) {
         return 0;
      }
      if (type == long.class) {
         return 0L;
      }
      if (type == float.class) {
         return 0.0F;
      }
      return 0.0D;
   }
}
