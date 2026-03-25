package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

public class VariableCascadeDeserializer
  implements JsonDeserializer<VariableCascades> {

  private final SensorType sensorType;

  protected VariableCascadeDeserializer(SensorType sensorType) {
    this.sensorType = sensorType;
  }

  @Override
  public VariableCascades deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    VariableCascades result = new VariableCascades();

    try {
      JsonObject jsonObj = json.getAsJsonObject();

      for (String schemeName : jsonObj.keySet()) {

        FlagScheme flagScheme = Instrument.getFlagScheme(schemeName);

        JsonArray cascadesArray = jsonObj.get(schemeName).getAsJsonArray();

        for (JsonElement cascadeElem : cascadesArray) {
          JsonArray cascade = cascadeElem.getAsJsonArray();

          if (cascade.size() != 2) {
            throw new JsonParseException("Invalid cascade definition");
          }

          int trigger = cascade.get(0).getAsInt();
          int outcome = cascade.get(1).getAsInt();

          result.add(flagScheme, sensorType, trigger, outcome);
        }

      }
    } catch (Exception e) {
      throw new JsonParseException(e);
    }

    return result;
  }
}
