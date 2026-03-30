package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class FlagPlotValueSerializer implements JsonSerializer<PlotValue> {

  private boolean plotHasY2;

  protected FlagPlotValueSerializer(boolean plotHasY2) {
    this.plotHasY2 = plotHasY2;
  }

  @Override
  public JsonElement serialize(PlotValue src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray json = new JsonArray();

    if (src.xIsTime()) {
      json.add(DateTimeUtils.toIsoDate(src.getXTime()));
    } else {
      json.add(src.getXDouble());
    }

    if (!src.hasY()) {
      // No Y value, so all flags are null

      // The NEEDED flag
      json.add(JsonNull.INSTANCE);

      // And the plot highlight flags
      for (int i = 0; i < src.getFlagScheme().getPlotHighlightFlags()
        .size(); i++) {
        json.add(JsonNull.INSTANCE);
      }
    } else {

      // Drop the value in the relevant column according to its flag.
      if (src.getFlag().equals(FlagScheme.NEEDED_FLAG)) {
        json.add(src.getY());
      } else {
        json.add(JsonNull.INSTANCE);
      }

      for (Flag highlightFlag : src.getFlagScheme().getPlotHighlightFlags()) {
        if (src.getFlag().equals(highlightFlag)) {
          json.add(src.getY());
        } else {
          json.add(JsonNull.INSTANCE);
        }
      }
    }

    // Placeholder for the Y2 value if we're doing a dual-axis plot. I
    // don't remember why it's needed.
    if (plotHasY2) {
      json.add(JsonNull.INSTANCE);
    }

    return json;
  }

}
