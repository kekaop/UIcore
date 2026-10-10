package org.orvencraft.uicore.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CursorRotationTrackerTest {
   @Test
   void dragRebaseKeepsCursorAtLastPosition() {
      CursorRotationTracker tracker = new CursorRotationTracker();
      assertEquals(15.0, tracker.update(15F, 0F, -90, 90));
      tracker.rebase(65F);
      assertEquals(15.0, tracker.update(65F, 0F, -90, 90));
      assertEquals(20.0, tracker.update(70F, 0F, -90, 90));
   }

   @Test
   void hostileFiniteYawCompletesInSeparateProcess() throws Exception {
      java.nio.file.Path javaBin = java.nio.file.Path.of(System.getProperty("java.home"), "bin");
      String javaExecutable = javaBin.resolve(java.nio.file.Files.isExecutable(javaBin.resolve("java")) ? "java" : "java.exe").toString();
      Process process = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"),
         YawProbe.class.getName()).redirectErrorStream(true).start();
      try {
         assertTrue(process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS), "Yaw normalization must terminate");
         assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes()));
      } finally { process.destroyForcibly(); }
   }

   public static class YawProbe {
      public static void main(String[] args) {
         CursorRotationTracker tracker = new CursorRotationTracker();
         for (float value : new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, 1e30F, -1e30F, 0F}) {
            double result = tracker.update(value, -value, -180.0, 180.0);
            if (!Double.isFinite(result) || result < -180 || result > 180) throw new AssertionError(result);
            CursorRotationTracker.rotationChanged(value, 0F, -value, 0F);
         }
      }
   }

   @Test
   void invalidRotationsDoNotMutateTrackerOrTriggerUpdates() {
      CursorRotationTracker tracker = new CursorRotationTracker();
      assertEquals(10.0, tracker.update(10F, 0F, -90, 90));
      assertEquals(10.0, tracker.update(Float.NaN, 0F, -90, 90));
      assertEquals(10.0, tracker.update(Float.POSITIVE_INFINITY, 0F, -90, 90));
      assertEquals(11.0, tracker.update(11F, 0F, -90, 90));
      assertFalse(CursorRotationTracker.rotationChanged(0, 0, Float.POSITIVE_INFINITY, 0));
      assertFalse(CursorRotationTracker.rotationChanged(0, 0, 0, Float.NaN));
   }
   @Test
   void crossingWrappedYawDoesNotJumpToOppositeEdge() {
      CursorRotationTracker tracker = new CursorRotationTracker();

      assertEquals(89.0, tracker.update(179.0F, 90.0F, -90.0, 90.0), 1.0E-6);
      assertEquals(90.0, tracker.update(-179.0F, 90.0F, -90.0, 90.0), 1.0E-6);
      assertEquals(89.0, tracker.update(180.0F, 90.0F, -90.0, 90.0), 1.0E-6);
   }

   @Test
   void yawStaysClampedAndImmediatelyMovesBackFromEdge() {
      CursorRotationTracker tracker = new CursorRotationTracker();

      assertEquals(0.0, tracker.update(0.0F, 0.0F, -90.0, 90.0), 1.0E-6);
      assertEquals(90.0, tracker.update(150.0F, 0.0F, -90.0, 90.0), 1.0E-6);
      assertEquals(80.0, tracker.update(140.0F, 0.0F, -90.0, 90.0), 1.0E-6);
   }

   @Test
   void positionalMoveDoesNotCountAsRotationFallback() {
      assertFalse(CursorRotationTracker.rotationChanged(20.0F, -5.0F, 20.0F, -5.0F));
      assertTrue(CursorRotationTracker.rotationChanged(20.0F, -5.0F, 20.1F, -5.0F));
      assertTrue(CursorRotationTracker.rotationChanged(20.0F, -5.0F, 20.0F, -4.9F));
   }
}
