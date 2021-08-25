package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SensorGroupSerializer implements JsonSerializer<SensorGroup> {

  @Override
  public JsonElement serialize(SensorGroup src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject group = new JsonObject();
    group.addProperty("name", src.getName());

    JsonArray membersJson = new JsonArray(src.size());
    src.getMembers().forEach(m -> membersJson.add(m.getSensorName()));
    group.add("members", membersJson);

    if (src.hasPrev()) {
      group.addProperty("prevLink", src.getPrevLink().getSensorName());
    } else {
      group.add("prevLink", JsonNull.INSTANCE);
    }

    if (src.hasNext()) {
      group.addProperty("nextLink", src.getNextLink().getSensorName());
    } else {
      group.add("nextLink", JsonNull.INSTANCE);
    }

    return group;
  }
}
