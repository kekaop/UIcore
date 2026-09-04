package space.arim.morepaperlib.scheduling;

final class StandardFoliaDetection implements FoliaDetection {
   private static final boolean IS_FOLIA;
   static final StandardFoliaDetection INSTANCE = new StandardFoliaDetection();

   private StandardFoliaDetection() {
   }

   @Override
   public boolean isUsingFolia() {
      return IS_FOLIA;
   }

   static {
      boolean isFolia;
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         isFolia = true;
      } catch (ClassNotFoundException var2) {
         isFolia = false;
      }

      IS_FOLIA = isFolia;
   }
}
