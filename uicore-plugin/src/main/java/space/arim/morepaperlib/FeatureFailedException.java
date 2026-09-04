package space.arim.morepaperlib;

public class FeatureFailedException extends RuntimeException {
   private static final long serialVersionUID = 5038819479673612108L;

   public FeatureFailedException() {
   }

   public FeatureFailedException(Throwable cause) {
      super(cause);
   }

   public FeatureFailedException(String message) {
      super(message);
   }

   public FeatureFailedException(String message, Throwable cause) {
      super(cause);
   }
}
