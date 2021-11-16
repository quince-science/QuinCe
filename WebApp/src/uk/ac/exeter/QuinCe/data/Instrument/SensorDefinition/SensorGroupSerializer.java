package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;
import java.util.List;

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

    List<String> memberNames = src.getMemberNames();
    JsonArray members = new JsonArray(memberNames.size());
    memberNames.forEach(members::add);
    group.add("members", members);

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
