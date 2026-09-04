package org.orvencraft.uicore.api;

public final class MenuMutationResult {
   private final boolean success;
   private final String message;
   private final int refreshedViewers;

   private MenuMutationResult(boolean success, String message, int refreshedViewers) {
      this.success = success;
      this.message = message;
      this.refreshedViewers = refreshedViewers;
   }

   public static MenuMutationResult success(String message, int refreshedViewers) {
      return new MenuMutationResult(true, message, refreshedViewers);
   }

   public static MenuMutationResult failure(String message) {
      return new MenuMutationResult(false, message, 0);
   }

   public boolean success() {
      return this.success;
   }

   public String message() {
      return this.message;
   }

   public int refreshedViewers() {
      return this.refreshedViewers;
   }
}
