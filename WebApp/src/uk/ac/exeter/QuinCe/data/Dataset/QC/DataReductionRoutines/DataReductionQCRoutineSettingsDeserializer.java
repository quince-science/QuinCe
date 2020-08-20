package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReductionRoutines;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;

public class DataReductionQCRoutineSettingsDeserializer
  implements JsonDeserializer<DataReductionQCRoutineSettings> {

  private final SensorsConfiguration sensorConfig;

  protected DataReductionQCRoutineSettingsDeserializer(
    SensorsConfiguration sensorConfig) {
    this.sensorConfig = sensorConfig;
  }

  @Override
  public DataReductionQCRoutineSettings deserialize(JsonElement json,
    Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    DataReductionQCRoutineSettings result = new DataReductionQCRoutineSettings();

    JsonObject jsonObject = json.getAsJsonObject();

    // Extract the flagged sensors
    if (!jsonObject.has("flagged_sensors")) {
      throw new JsonParseException("Missing flagged_sensors entry");
    }

    JsonArray sensorList = jsonObject.get("flagged_sensors").getAsJsonArray();

    sensorList.forEach(s -> {
      try {
        result.addFlaggedSensor(sensorConfig.getSensorType(s.getAsString()));
      } catch (SensorTypeNotFoundException e) {
        throw new JsonParseException(
          "Unrecognised Sensor Type '" + s.getAsString() + "'");
      }
    });

    /**
     * Extract options
     */
    if (!jsonObject.has("options")) {
      throw new JsonParseException("Missing flagged_sensors entry");
    }

    JsonObject options = jsonObject.get("options").getAsJsonObject();
    for (Map.Entry<String, JsonElement> entry : options.entrySet()) {
      result.addOption(entry.getKey(), entry.getValue().getAsString());
    }

    return result;
  }

}
