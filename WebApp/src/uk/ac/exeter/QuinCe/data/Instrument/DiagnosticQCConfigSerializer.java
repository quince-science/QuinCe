package uk.ac.exeter.QuinCe.data.Instrument;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;

public class DiagnosticQCConfigSerializer
  implements JsonSerializer<DiagnosticQCConfig> {

  protected static final String RANGE_MIN_KEY = "rangeMin";

  protected static final String RANGE_MAX_KEY = "rangeMax";

  protected static final String RUN_TYPES_KEY = "affectedRunTypes";

  @Override
  public JsonElement serialize(DiagnosticQCConfig src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject json = new JsonObject();

    for (Map.Entry<SensorAssignment, DiagnosticSensorQCConfig> entry : src
      .entrySet()) {

      JsonObject sensorConfigJson = new JsonObject();

      DiagnosticSensorQCConfig sensorConfig = entry.getValue();

      if (!sensorConfig.isEmpty()) {
        if (null != sensorConfig.getRangeMin()) {
          sensorConfigJson.add(RANGE_MIN_KEY,
            new JsonPrimitive(sensorConfig.getRangeMin()));
        }
        if (null != sensorConfig.getRangeMax()) {
          sensorConfigJson.add(RANGE_MAX_KEY,
            new JsonPrimitive(sensorConfig.getRangeMax()));
        }

        if (sensorConfig.anyRunTypeAssigned()) {
          JsonObject affectedRunTypesJson = new JsonObject();

          sensorConfig.getAllAffectedRunTypes().entrySet().forEach(e -> {
            JsonArray runTypes = new JsonArray();
            e.getValue().forEach(runTypes::add);
            affectedRunTypesJson.add(String.valueOf(e.getKey().getDatabaseId()),
              runTypes);
          });

          sensorConfigJson.add(RUN_TYPES_KEY, affectedRunTypesJson);
        }

        json.add(String.valueOf(entry.getKey().getDatabaseId()),
          sensorConfigJson);
      }
    }

    // TODO Auto-generated method stub
    return json;
  }

}
