package org.orvencraft.uicore.animation;

import java.util.Locale;

public final class AnimationEasing {
   private static final double BACK_OVERSHOOT = 1.70158;

   private AnimationEasing() {
   }

   public static double apply(String name, double progress) {
      double t = Math.max(0.0, Math.min(1.0, progress));
      String easing = name == null ? "linear" : name.trim().toLowerCase(Locale.ROOT);
      return switch (easing) {
         case "ease-in-cubic" -> t * t * t;
         case "ease-out-cubic" -> 1.0 - Math.pow(1.0 - t, 3.0);
         case "ease-in-out-cubic" -> t < 0.5
            ? 4.0 * t * t * t
            : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
         case "ease-out-back" -> {
            double shifted = t - 1.0;
            double coefficient = BACK_OVERSHOOT + 1.0;
            yield 1.0 + coefficient * shifted * shifted * shifted + BACK_OVERSHOOT * shifted * shifted;
         }
         default -> t;
      };
   }
}
