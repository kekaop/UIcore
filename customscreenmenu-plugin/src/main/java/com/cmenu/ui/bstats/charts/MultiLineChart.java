package com.cmenu.ui.bstats.charts;

import com.cmenu.ui.bstats.json.JsonObjectBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class MultiLineChart extends CustomChart {
   private final Callable<Map<String, Integer>> callable;

   public MultiLineChart(String chartId, Callable<Map<String, Integer>> callable) {
      super(chartId);
      this.callable = callable;
   }

   @Override
   protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
      JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
      Map<String, Integer> map = this.callable.call();
      if (map != null && !map.isEmpty()) {
         boolean allSkipped = true;

         for (Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() != 0) {
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
