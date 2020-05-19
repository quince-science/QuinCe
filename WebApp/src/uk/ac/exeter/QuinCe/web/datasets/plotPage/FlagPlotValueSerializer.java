package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class FlagPlotValueSerializer implements JsonSerializer<PlotValue> {

  @Override
  public JsonElement serialize(PlotValue src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray json = new JsonArray();

    json.add(src.getX());

    // Bad
    if (src.getFlag().equals(Flag.BAD)) {
      json.add(src.getY());
    } else {
      json.add(JsonNull.INSTANCE);
    }

    // Questionable
    if (src.getFlag().equals(Flag.QUESTIONABLE)) {
      json.add(src.getY());
    } else {
      json.add(JsonNull.INSTANCE);
    }

    // Needed
    if (src.getFlag().equals(Flag.NEEDED)) {
      json.add(src.getY());
    } else {
      json.add(JsonNull.INSTANCE);
    }

    return json;
  }

}
