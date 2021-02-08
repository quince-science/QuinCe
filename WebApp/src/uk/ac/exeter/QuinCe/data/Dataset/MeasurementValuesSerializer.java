package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagSerializer;

public class MeasurementValuesSerializer
  implements JsonSerializer<HashMap<Long, MeasurementValue>> {

  private static final Gson gson;

  static {
    gson = new GsonBuilder()
      .registerTypeAdapter(Flag.class, new FlagSerializer()).create();
  }

  @Override
  public JsonElement serialize(HashMap<Long, MeasurementValue> src,
    Type typeOfSrc, JsonSerializationContext context) {

    JsonObject json = new JsonObject();

    for (Map.Entry<Long, MeasurementValue> entry : src.entrySet()) {

      MeasurementValue value = entry.getValue();

      JsonObject valueJson = new JsonObject();

      // Sensor Value IDs
      JsonArray sensorValueIds = new JsonArray(
        value.getSensorValueIds().size());
      value.getSensorValueIds().forEach(sensorValueIds::add);
      valueJson.add("svids", sensorValueIds);

      // Supporting Sensor Value IDs
      JsonArray supportingSensorValueIds = new JsonArray(
        value.getSupportingSensorValueIds().size());
      value.getSupportingSensorValueIds()
        .forEach(supportingSensorValueIds::add);
      valueJson.add("suppids", supportingSensorValueIds);

      // Calculated value
      valueJson.add("value", new JsonPrimitive(value.getCalculatedValue()));

      // Flag
      valueJson.add("flag", gson.toJsonTree(value.getQcFlag()));

      // QC Comments
      JsonArray qcComments = new JsonArray(value.getQcMessages().size());
      value.getQcMessages().forEach(qcComments::add);
      valueJson.add("qcComments", qcComments);

      json.add(String.valueOf(entry.getKey()), valueJson);
    }

    return json;
  }
}
