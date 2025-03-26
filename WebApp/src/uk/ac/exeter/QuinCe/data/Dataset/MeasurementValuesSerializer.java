package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagSerializer;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;

public class MeasurementValuesSerializer
  implements JsonSerializer<HashMap<Long, MeasurementValue>>,
  JsonDeserializer<HashMap<Long, MeasurementValue>> {

  private static final Gson gson;

  private static final String SENSOR_VALUE_IDS_KEY = "svids";

  private static final String SUPPORTING_VALUE_IDS_KEY = "suppids";

  private static final String MEMBER_COUNT_KEY = "memberCount";

  private static final String INTERPOLATES_OVER_FLAG_KEY = "interpolatesOverFlag";

  private static final String VALUE_KEY = "value";

  private static final String FLAG_KEY = "flag";

  private static final String QC_COMMENT_KEY = "qcComments";

  private static final String TYPE_KEY = "type";

  private static final String PROPERTIES_KEY = "props";

  private static final Double NAN_VALUE = -999999999.9D;

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
      valueJson.add(SENSOR_VALUE_IDS_KEY, sensorValueIds);

      // Supporting Sensor Value IDs
      JsonArray supportingSensorValueIds = new JsonArray(
        value.getSupportingSensorValueIds().size());
      value.getSupportingSensorValueIds()
        .forEach(supportingSensorValueIds::add);
      valueJson.add(SUPPORTING_VALUE_IDS_KEY, supportingSensorValueIds);

      valueJson.add(MEMBER_COUNT_KEY,
        new JsonPrimitive(value.getMemberCount()));

      valueJson.add(INTERPOLATES_OVER_FLAG_KEY,
        new JsonPrimitive(value.interpolatesAroundFlag()));

      // Calculated value
      JsonPrimitive valuePrimitive;
      if (value.getCalculatedValue().isNaN()) {
        valuePrimitive = new JsonPrimitive(NAN_VALUE);
      } else {
        valuePrimitive = new JsonPrimitive(value.getCalculatedValue());
      }

      valueJson.add(VALUE_KEY, valuePrimitive);

      // Flag
      // We know that this implementation doesn't utilise the
      // DatasetSensorValues parameter
      valueJson.add(FLAG_KEY, gson.toJsonTree(value.getQcFlag(null)));

      // QC Comments
      JsonArray qcComments = new JsonArray(value.getQcMessages().size());
      value.getQcMessages().forEach(qcComments::add);
      valueJson.add(QC_COMMENT_KEY, qcComments);

      json.add(String.valueOf(entry.getKey()), valueJson);

      // Type
      valueJson.addProperty(TYPE_KEY, value.getType());

      // Properties
      valueJson.add(PROPERTIES_KEY, gson.toJsonTree(value.getProperties()));
    }

    return json;
  }

  @Override
  public HashMap<Long, MeasurementValue> deserialize(JsonElement json,
    Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    HashMap<Long, MeasurementValue> result = new HashMap<Long, MeasurementValue>();

    for (Map.Entry<String, JsonElement> entry : ((JsonObject) json)
      .entrySet()) {

      long sensorTypeId = Long.parseLong(entry.getKey());

      MeasurementValue measurementValue = makeMeasurementValue(sensorTypeId,
        (JsonObject) entry.getValue());

      result.put(sensorTypeId, measurementValue);
    }

    return result;
  }

  private MeasurementValue makeMeasurementValue(long sensorTypeId,
    JsonObject json) throws JsonParseException {

    try {
      JsonArray sensorValueIdsElement = json
        .getAsJsonArray(SENSOR_VALUE_IDS_KEY);
      List<Long> sensorValueIds = new ArrayList<Long>(
        sensorValueIdsElement.size());

      sensorValueIdsElement.forEach(e -> sensorValueIds.add(e.getAsLong()));

      JsonArray supportingValueIdsElement = json
        .getAsJsonArray(SUPPORTING_VALUE_IDS_KEY);
      List<Long> supportingValueIds = new ArrayList<Long>(
        supportingValueIdsElement.size());

      supportingValueIdsElement
        .forEach(e -> supportingValueIds.add(e.getAsLong()));

      int memberCount = json.get(MEMBER_COUNT_KEY).getAsInt();

      /*
       * This default is to handle old MeasurementValues that were created
       * before the flag was introduced.
       */
      boolean interpolatesOverFlag = false;
      if (json.has(INTERPOLATES_OVER_FLAG_KEY)) {
        interpolatesOverFlag = json.get(INTERPOLATES_OVER_FLAG_KEY)
          .getAsBoolean();
      }

      Double value = json.get(VALUE_KEY).getAsDouble();
      if (Math.abs(value - NAN_VALUE) < 1) {
        value = Double.NaN;
      }

      Flag flag = gson.fromJson(json.get(FLAG_KEY), Flag.class);

      JsonArray qcCommentsElement = json.getAsJsonArray(QC_COMMENT_KEY);
      HashSet<String> qcComments = new HashSet<String>(
        qcCommentsElement.size());

      qcCommentsElement.forEach(e -> qcComments.add(e.getAsString()));

      char type = json.get(TYPE_KEY).getAsString().charAt(0);

      Properties properties = gson.fromJson(json.get(PROPERTIES_KEY),
        Properties.class);

      return new MeasurementValue(sensorTypeId, sensorValueIds,
        supportingValueIds, memberCount, interpolatesOverFlag, value, flag,
        qcComments, type, properties);
    } catch (SensorTypeNotFoundException e) {
      throw new JsonParseException(e);
    }

  }
}
