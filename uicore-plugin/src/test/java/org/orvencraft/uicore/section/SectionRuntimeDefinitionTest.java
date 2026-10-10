package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.orvencraft.uicore.layout.MenuLayout;
import org.junit.jupiter.api.Test;

class SectionRuntimeDefinitionTest {
   @Test
   void runtimeCameraCopyReceivesLaterCanonicalDefinitionChanges() {
      Section canonical = section();
      canonical.add("before", layout(0));
      canonical.addTemplate("template", layout(1));
      Section runtime = canonical.copyWithCamera("runtime-world", 20, 30, 40, 90, 0);
      Section.Definition initial = runtime.definition();

      canonical.remove("before");
      canonical.add("after", layout(2));
      canonical.addTemplate("replacement-template", layout(3));
      runtime.syncDefinitionFrom(canonical);

      assertTrue(initial.layouts().containsKey("before"));
      assertFalse(initial.layouts().containsKey("after"));
      assertFalse(runtime.contains("before"));
      assertTrue(runtime.contains("after"));
      assertTrue(runtime.hasTemplate("replacement-template"));
      assertEquals("runtime-world", runtime.world);
      assertEquals(20, runtime.cameraX);
      assertThrows(UnsupportedOperationException.class, () -> runtime.definition().layouts().clear());
      assertThrows(UnsupportedOperationException.class, () -> runtime.definition().templates().clear());
      assertThrows(UnsupportedOperationException.class, () -> runtime.definition().slots().clear());
   }

   @Test
   void concurrentWritersAndCameraReadersKeepStableOrderedSnapshots() throws Exception {
      Section canonical = section();
      canonical.add("first", layout(0));
      Section.Definition initial = canonical.definition();
      ExecutorService executor = Executors.newFixedThreadPool(4);
      CountDownLatch start = new CountDownLatch(1);
      List<Future<?>> tasks = new ArrayList<>();
      try {
         for (int worker = 0; worker < 2; worker++) {
            final int id = worker;
            tasks.add(executor.submit(() -> {
               await(start);
               for (int index = 0; index < 200; index++) {
                  String key = id + "-" + index;
                  canonical.add(key, layout(index));
                  if (index % 2 == 0) canonical.remove(key);
               }
            }));
         }
         for (int worker = 0; worker < 2; worker++) {
            tasks.add(executor.submit(() -> {
               await(start);
               Section runtime = canonical.copyWithCamera("runtime", 1, 2, 3, 0, 0);
               for (int index = 0; index < 400; index++) {
                  runtime.syncDefinitionFrom(canonical);
                  Section.Definition snapshot = runtime.definition();
                  assertEquals("first", snapshot.layouts().keySet().iterator().next());
                  snapshot.layouts().forEach((key, value) -> assertNotNull(value));
                  assertEquals(snapshot.layouts().size(), new ArrayList<>(snapshot.layouts().entrySet()).size());
               }
            }));
         }
         start.countDown();
         for (Future<?> task : tasks) task.get(10, TimeUnit.SECONDS);
         assertEquals(201, canonical.definition().layouts().size());
         assertEquals(List.of("first"), List.copyOf(initial.layouts().keySet()));
      } finally { executor.shutdownNow(); }
   }

   @Test
   void concurrentAddsAndEditsDoNotOverwriteEachOther() throws Exception {
      Section canonical = section();
      AtomicInteger additions = new AtomicInteger();
      ExecutorService executor = Executors.newFixedThreadPool(4);
      try {
         List<Callable<Void>> adds = new ArrayList<>();
         for (int i = 0; i < 4; i++) adds.add(() -> {
            if (canonical.addIfAbsent("counter", layout(0))) additions.incrementAndGet();
            return null;
         });
         for (Future<Void> result : executor.invokeAll(adds, 10, TimeUnit.SECONDS)) result.get();
         assertEquals(1, additions.get());
         List<Callable<Void>> edits = new ArrayList<>();
         for (int i = 0; i < 4; i++) edits.add(() -> {
            for (int j = 0; j < 100; j++) assertTrue(canonical.edit("counter", old -> layout(old.x + 1)));
            return null;
         });
         for (Future<Void> result : executor.invokeAll(edits, 10, TimeUnit.SECONDS)) result.get(10, TimeUnit.SECONDS);
         assertEquals(400, canonical.get("counter").x);
         Section.Definition beforeFailure = canonical.definition();
         assertThrows(IllegalArgumentException.class, () -> canonical.edit("counter", old -> { throw new IllegalArgumentException("bad edit"); }));
         assertSame(beforeFailure, canonical.definition());
      } finally { executor.shutdownNow(); }
   }

   private static void await(CountDownLatch latch) {
      try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
      catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new AssertionError(exception); }
   }

   private static MenuLayout layout(double x) {
      return new MenuLayout("test", "test", List.of(), false, x, 0, 0, false, false, null, false, List.of(), 0, 0, 0, "", false, "", "");
   }

   private static Section section() {
      return new Section(1.5, "world", 1, 2, 3, 0, 0, "", List.of(), List.of(), false);
   }
}
