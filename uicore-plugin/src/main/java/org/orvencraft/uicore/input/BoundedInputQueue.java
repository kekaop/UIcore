package org.orvencraft.uicore.input;

import java.util.ArrayDeque;
import java.util.Objects;

/** A small FIFO queue with a hard capacity for cross-thread input handoff. */
public final class BoundedInputQueue<T> {
   private final int capacity;
   private final ArrayDeque<T> values = new ArrayDeque<>();

   public BoundedInputQueue(int capacity) {
      if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
      this.capacity = capacity;
   }

   public synchronized boolean offer(T value) {
      Objects.requireNonNull(value, "value");
      if (values.size() >= capacity) return false;
      values.addLast(value);
      return true;
   }

   public synchronized T poll() {
      return values.pollFirst();
   }

   public synchronized void clear() {
      values.clear();
   }

   public synchronized int size() {
      return values.size();
   }
}
