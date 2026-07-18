package space.arim.morepaperlib.scheduling;

import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface AttachedScheduler {
   @Nullable
   ScheduledTask run(Runnable var1, Runnable var2);

   boolean run(Consumer<ScheduledTask> var1, Runnable var2);

   @Nullable
   ScheduledTask runDelayed(Runnable var1, Runnable var2, long var3);

   boolean runDelayed(Consumer<ScheduledTask> var1, Runnable var2, long var3);

   @Nullable
   ScheduledTask runAtFixedRate(Runnable var1, Runnable var2, long var3, long var5);

   boolean runAtFixedRate(Consumer<ScheduledTask> var1, Runnable var2, long var3, long var5);
}
