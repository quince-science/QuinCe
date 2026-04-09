package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class Y2AxisPlotValueSerializer implements JsonSerializer<PlotValue> {

  @Override
  public JsonElement serialize(PlotValue src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray json = new JsonArray();

    // X value
    if (src.xIsTime()) {
      json.add(DateTimeUtils.toIsoDate(src.getXTime()));
    } else {
      Double x = src.getXDouble();
      if (null == x || x.isNaN()) {
        json.add(JsonNull.INSTANCE);
      } else {
        json.add(src.getXDouble());
      }
    }

    // Data point ID
    json.add(src.getId());

    // Dummy placeholder for the Y1 value.
    // Dygraphs gets confused if this is not here.
    json.add(JsonNull.INSTANCE);

    // Y2 axis value
    if (!src.hasY2()) {

      // Highlight flags - we don't include NEEDED
      for (int i = 0; i < src.getFlagScheme().getPlotHighlightFlags()
        .size(); i++) {
        json.add(JsonNull.INSTANCE);
      }

      // The Y2 value
      json.add(JsonNull.INSTANCE);
    } else {
      // Flags
      for (Flag highlightFlag : src.getFlagScheme().getPlotHighlightFlags()) {
        if (src.getFlag2().equals(highlightFlag)) {
          json.add(src.getY2());
        } else {
          json.add(JsonNull.INSTANCE);
        }
      }

      // The Y2 value
      if (src.isGhost2()) {
        // We don't show ghosts in the Y2 series
        json.add(JsonNull.INSTANCE);
      } else {
        json.add(src.getY2());
      }
    }

    return json;
  }
}
