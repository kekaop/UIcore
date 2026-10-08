package org.orvencraft.uicore.api.state;

public interface MenuState {
   <T> T get(StateKey<T> key);
   <T> void set(StateKey<T> key, T value);
   void clear(StateKey<?> key);
}
