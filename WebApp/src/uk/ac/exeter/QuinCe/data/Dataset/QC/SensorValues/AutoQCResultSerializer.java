package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;

public class AutoQCResultSerializer
  implements JsonSerializer<AutoQCResult>, JsonDeserializer<AutoQCResult> {

  private final FlagScheme flagScheme;

  protected AutoQCResultSerializer(FlagScheme flagScheme) {
    this.flagScheme = flagScheme;
  }

  @Override
  public JsonElement serialize(AutoQCResult src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray result = new JsonArray();

    for (RoutineFlag flag : src) {
      JsonObject flagJson = new JsonObject();
      flagJson.addProperty("routineName", flag.getRoutineName());
      flagJson.addProperty("requiredValue", flag.getRequiredValue());
      flagJson.addProperty("actualValue", flag.getActualValue());
      flagJson.addProperty("flagValue", flag.getValue());

      result.add(flagJson);
    }

    return result;
  }

  @Override
  public AutoQCResult deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    AutoQCResult result = new AutoQCResult(flagScheme);

    for (JsonElement element : json.getAsJsonArray()) {
      JsonObject flagObj = element.getAsJsonObject();

      String routineName = flagObj.get("routineName").getAsString();

      String requiredValue = "";
      if (flagObj.has("requiredValue")) {
        requiredValue = flagObj.get("requiredValue").getAsString();
      }

      String actualValue = "";
      if (flagObj.has("actualValue")) {
        actualValue = flagObj.get("actualValue").getAsString();
      }

      Flag flag = flagScheme.getFlag(flagObj.get("flagValue").getAsInt());

      result.add(new RoutineFlag(flagScheme, routineName, flag, requiredValue,
        actualValue));
    }

    return result;
  }
}
