package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class SensorGroupDeserializer implements JsonDeserializer<SensorGroup> {

  private SensorAssignments sensorAssignments;

  protected SensorGroupDeserializer(SensorAssignments sensorAssignments) {
    this.sensorAssignments = sensorAssignments;
  }

  @Override
  public SensorGroup deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    JsonObject jsonObject = json.getAsJsonObject();

    String name = jsonObject.get("name").getAsString();

    SensorGroup group = new SensorGroup(name);
    JsonArray members = jsonObject.getAsJsonArray("members");
    Iterator<JsonElement> memberIterator = members.iterator();

    try {
      while (memberIterator.hasNext()) {
        group.addAssignment(sensorAssignments
          .getBySensorName(memberIterator.next().getAsString()));
      }

      if (jsonObject.has("nextLink")) {
        group.setNextLink(sensorAssignments
          .getBySensorName(jsonObject.get("nextLink").getAsString()));
      }

      if (jsonObject.has("prevLink")) {
        group.setPrevLink(sensorAssignments
          .getBySensorName(jsonObject.get("prevLink").getAsString()));
      }
    } catch (Exception e) {
      throw new JsonParseException(e);
    }

    return group;
  }
}
