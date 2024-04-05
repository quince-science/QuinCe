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

    // Y1 axis, if we haven't put in a value already
    json.add(JsonNull.INSTANCE);

    // Y2 axis value
    if (!src.hasY2()) {
      json.add(JsonNull.INSTANCE);
      json.add(JsonNull.INSTANCE);
      json.add(JsonNull.INSTANCE);
      json.add(JsonNull.INSTANCE);
      json.add(JsonNull.INSTANCE);
    } else {

      // BAD value
      if (src.getFlag2().equals(Flag.BAD)) {
        json.add(src.getY2());
      } else {
        json.add(JsonNull.INSTANCE);
      }

      // QUESTIONABLE value
      if (src.getFlag2().equals(Flag.QUESTIONABLE)) {
        json.add(src.getY2());
      } else {
        json.add(JsonNull.INSTANCE);
      }

      // NOT_CALIBRATED value
      if (src.getFlag2().equals(Flag.NOT_CALIBRATED)) {
        json.add(src.getY2());
      } else {
        json.add(JsonNull.INSTANCE);
      }

      // The value
      if (src.isGhost2()) {
        json.add(src.getY2());
        json.add(JsonNull.INSTANCE);
      } else {
        json.add(JsonNull.INSTANCE);
        json.add(src.getY2());
      }
    }

    return json;
  }
}
