package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SensorGroupsSerializer implements JsonSerializer<SensorGroups> {

  private static Gson gson;

  static {
    gson = new GsonBuilder()
      .registerTypeAdapter(SensorGroup.class, new SensorGroupSerializer())
      .create();
  }

  @Override
  public JsonElement serialize(SensorGroups src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray result = new JsonArray();
    src.forEach(g -> result.add(gson.toJsonTree(g)));
    return result;
  }

}
