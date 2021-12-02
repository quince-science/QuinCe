package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class SensorOffsetsSerializer implements JsonSerializer<SensorOffsets> {

  protected static final String TIME_NAME = "time";

  protected static final String OFFSET_NAME = "offset";

  @Override
  public JsonElement serialize(SensorOffsets src, Type typeOfSrc,
    JsonSerializationContext context) {

    LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>> map = src.getMap();

    JsonObject json = new JsonObject();

    for (Map.Entry<SensorGroupPair, TreeSet<SensorOffset>> entry : map
      .entrySet()) {

      SensorGroupPair groupPair = entry.getKey();

      JsonArray offsets = new JsonArray();

      entry.getValue().forEach(o -> {
        JsonObject offsetObject = new JsonObject();
        offsetObject.addProperty(TIME_NAME,
          DateTimeUtils.dateToLong(o.getTime()));
        offsetObject.addProperty(OFFSET_NAME, o.getOffset());
        offsets.add(offsetObject);
      });

      json.add(String.valueOf(groupPair.getId()), offsets);

    }

    return json;
  }
}
