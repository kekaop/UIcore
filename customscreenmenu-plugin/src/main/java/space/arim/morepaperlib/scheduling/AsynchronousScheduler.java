package space.arim.morepaperlib.scheduling;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface AsynchronousScheduler extends SchedulerBase, Executor {
   ScheduledTask runDelayed(Runnable var1, Duration var2);

   void runDelayed(Consumer<ScheduledTask> var1, Duration var2);

   ScheduledTask runAtFixedRate(Runnable var1, Duration var2, Duration var3);

   void runAtFixedRate(Consumer<ScheduledTask> var1, Duration var2, Duration var3);
}
