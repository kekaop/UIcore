package org.orvencraft.uicore.bstats.charts;

import org.orvencraft.uicore.bstats.json.JsonObjectBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class AdvancedBarChart extends CustomChart {
   private final Callable<Map<String, int[]>> callable;

   public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
      super(chartId);
      this.callable = callable;
   }

   @Override
   protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
      JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
      Map<String, int[]> map = this.callable.call();
      if (map != null && !map.isEmpty()) {
         boolean allSkipped = true;

         for (Entry<String, int[]> entry : map.entrySet()) {
            if (entry.getValue().length != 0) {
               allSkipped = false;
               valuesBuilder.appendField(entry.getKey(), entry.getValue());
            }
         }

         return allSkipped ? null : new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
      } else {
         return null;
      }
   }
}
