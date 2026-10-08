package org.orvencraft.uicore.api.extension;

/** Closing a registration removes its handler from UIcore. */
@FunctionalInterface
public interface ExtensionRegistration extends AutoCloseable {
   @Override
   void close();
}
