package org.orvencraft.uicore.section;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SectionRuntimeDefinitionTest {
   @Test
   void runtimeCameraCopyReceivesLaterCanonicalDefinitionChanges() {
      Section canonical = section();
      canonical.layouts.put("before", null);
      canonical.templates.put("template", null);
      canonical.slots.put("slot", null);
      Section runtime = canonical.copyWithCamera("runtime-world", 20, 30, 40, 90, 0);

      canonical.layouts.remove("before");
      canonical.layouts.put("after", null);
      canonical.templates.clear();
      canonical.templates.put("replacement-template", null);
      canonical.slots.clear();
      canonical.slots.put("replacement-slot", null);
      runtime.syncDefinitionFrom(canonical);

      assertFalse(runtime.layouts.containsKey("before"));
      assertTrue(runtime.layouts.containsKey("after"));
      assertTrue(runtime.templates.containsKey("replacement-template"));
      assertTrue(runtime.slots.containsKey("replacement-slot"));
      assertTrue(runtime.world.equals("runtime-world"));
   }

   private static Section section() {
      return new Section(1.5, "world", 1, 2, 3, 0, 0, "", List.of(), List.of(), false);
   }
}
