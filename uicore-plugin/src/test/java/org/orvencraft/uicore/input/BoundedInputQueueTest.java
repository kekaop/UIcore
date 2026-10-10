package org.orvencraft.uicore.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoundedInputQueueTest {
   @Test
   void retainsOrderAndRejectsEventsBeyondItsHardLimit() {
      BoundedInputQueue<Integer> queue = new BoundedInputQueue<>(3);
      assertTrue(queue.offer(1));
      assertTrue(queue.offer(2));
      assertTrue(queue.offer(3));
      assertFalse(queue.offer(4));
      assertEquals(3, queue.size());
      assertEquals(1, queue.poll());
      assertEquals(2, queue.poll());
      assertEquals(3, queue.poll());
      assertNull(queue.poll());
   }

   @Test
   void canBeClearedAfterAnOverflow() {
      BoundedInputQueue<String> queue = new BoundedInputQueue<>(1);
      assertTrue(queue.offer("start"));
      assertFalse(queue.offer("release"));
      queue.clear();
      assertEquals(0, queue.size());
      assertTrue(queue.offer("next-start"));
   }
}
