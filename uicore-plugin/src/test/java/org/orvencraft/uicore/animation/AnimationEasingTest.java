package org.orvencraft.uicore.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimationEasingTest {
   @Test
   void clampsProgressAndKeepsLinearFallback() {
      assertEquals(0.0, AnimationEasing.apply("linear", -1.0));
      assertEquals(0.5, AnimationEasing.apply("unknown", 0.5));
      assertEquals(1.0, AnimationEasing.apply("linear", 2.0));
   }

   @Test
   void easeOutBackOvershootsAndFinishesAtTarget() {
      assertTrue(AnimationEasing.apply("ease-out-back", 0.7) > 1.0);
      assertEquals(1.0, AnimationEasing.apply("ease-out-back", 1.0), 0.000001);
   }

   @Test
   void easeInCubicAcceleratesOutgoingMotion() {
      assertEquals(0.125, AnimationEasing.apply("ease-in-cubic", 0.5), 0.000001);
   }
}
