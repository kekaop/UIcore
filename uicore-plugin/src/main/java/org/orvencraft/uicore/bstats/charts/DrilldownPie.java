package org.orvencraft.uicore.bstats.charts;

import org.orvencraft.uicore.bstats.json.JsonObjectBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class DrilldownPie extends CustomChart {
   private final Callable<Map<String, Map<String, Integer>>> callable;

   public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
      super(chartId);
      this.callable = callable;
   }

   @Override
   public JsonObjectBuilder.JsonObject getChartData() throws Exception {
      JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
      Map<String, Map<String, Integer>> map = this.callable.call();
      if (map != null && !map.isEmpty()) {
         boolean reallyAllSkipped = true;

         for (Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
            JsonObjectBuilder valueBuilder = new JsonObjectBuilder();
            boolean allSkipped = true;

            for (Entry<String, Integer> valueEntry : map.get(entryValues.getKey()).entrySet()) {
               valueBuilder.appendField(valueEntry.getKey(), valueEntry.getValue());
               allSkipped = false;
            }

            if (!allSkipped) {
               reallyAllSkipped = false;
               valuesBuilder.appendField(entryValues.getKey(), valueBuilder.build());
            }
         }

         return reallyAllSkipped ? null : new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
      } else {
         return null;
      }
   }
}
