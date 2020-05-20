package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class MainPlotValueSerializer implements JsonSerializer<PlotValue> {

  @Override
  public JsonElement serialize(PlotValue src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray json = new JsonArray();

    if (src.xIsTime()) {
      json.add(DateTimeUtils.toIsoDate(src.getXTime()));
    } else {
      json.add(src.getXDouble());
    }

    json.add(src.getId());
    if (src.isGhost()) {
      json.add(src.getY());
      json.add(JsonNull.INSTANCE);
    } else {
      json.add(JsonNull.INSTANCE);
      json.add(src.getY());
    }

    return json;
  }

}
