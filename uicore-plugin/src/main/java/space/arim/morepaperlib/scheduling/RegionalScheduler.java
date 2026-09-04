package space.arim.morepaperlib.scheduling;

import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RegionalScheduler extends SchedulerBase {
   ScheduledTask runDelayed(Runnable var1, long var2);

   void runDelayed(Consumer<ScheduledTask> var1, long var2);

   ScheduledTask runAtFixedRate(Runnable var1, long var2, long var4);

   void runAtFixedRate(Consumer<ScheduledTask> var1, long var2, long var4);

   default AttachedScheduler asAttachedScheduler() {
      class Transformation implements AttachedScheduler {
         @Nullable
         @Override
         public ScheduledTask run(Runnable command, Runnable alternateIfRemoved) {
            return RegionalScheduler.this.run(command);
         }

         @Override
         public boolean run(Consumer<ScheduledTask> command, Runnable alternateIfRemoved) {
            RegionalScheduler.this.run(command);
            return true;
         }

         @Nullable
         @Override
         public ScheduledTask runDelayed(Runnable command, Runnable alternateIfRemoved, long delay) {
            return RegionalScheduler.this.runDelayed(command, delay);
         }

         @Override
         public boolean runDelayed(Consumer<ScheduledTask> command, Runnable alternateIfRemoved, long delay) {
            RegionalScheduler.this.runDelayed(command, delay);
            return true;
         }

         @Nullable
         @Override
         public ScheduledTask runAtFixedRate(Runnable command, Runnable alternateIfRemoved, long initialDelay, long period) {
            return RegionalScheduler.this.runAtFixedRate(command, initialDelay, period);
         }

         @Override
         public boolean runAtFixedRate(Consumer<ScheduledTask> command, Runnable alternateIfRemoved, long initialDelay, long period) {
            RegionalScheduler.this.runAtFixedRate(command, initialDelay, period);
            return true;
         }
      }

      return new Transformation();
   }
}
