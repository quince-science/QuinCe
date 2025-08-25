package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;

public class VariablePropertiesDeserializer
  implements JsonDeserializer<VariableProperties> {

  private final long variableId;

  private final String variableName;

  public VariablePropertiesDeserializer(long variableId, String variableName) {
    this.variableId = variableId;
    this.variableName = variableName;
  }

  @Override
  public VariableProperties deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    JsonObject jsonObj = json.getAsJsonObject();

    List<String> coefficients = null;
    if (jsonObj.has("coefficients")) {
      JsonArray coefficientsArr = jsonObj.get("coefficients").getAsJsonArray();
      coefficients = new ArrayList<String>(coefficientsArr.size());

      Iterator<JsonElement> iter = coefficientsArr.iterator();
      while (iter.hasNext()) {
        coefficients.add(iter.next().getAsString());
      }
    }

    HashMap<Long, Boolean> dependsQuestionAnswers = null;
    if (jsonObj.has("dependsQuestionAnswers")) {
      dependsQuestionAnswers = new HashMap<Long, Boolean>();

      JsonObject dependsQuestionAnswersObj = jsonObj
        .get("dependsQuestionAnswers").getAsJsonObject();

      for (Map.Entry<String, JsonElement> answerEntry : dependsQuestionAnswersObj
        .entrySet()) {

        Long sensorTypeId = Long.parseLong(answerEntry.getKey());
        Boolean answer = answerEntry.getValue().getAsBoolean();
        dependsQuestionAnswers.put(sensorTypeId, answer);
      }
    }

    List<PresetRunType> presetRunTypes = new ArrayList<PresetRunType>();

    if (jsonObj.has("presetRunTypes")) {
      jsonObj.getAsJsonArray("presetRunTypes").forEach(a -> {
        JsonObject entry = a.getAsJsonObject();

        JsonArray runTypeArray = entry.getAsJsonArray("runType");

        List<String> runTypes = new ArrayList<String>(runTypeArray.size());
        for (JsonElement runType : runTypeArray) {
          runTypes.add(runType.getAsString());
        }

        RunTypeCategory runTypeCategory = null;

        long categoryCode = entry.get("category").getAsLong();

        if (categoryCode == -1L) {
          runTypeCategory = RunTypeCategory.IGNORED;
        } else if (categoryCode == -3L) {
          runTypeCategory = RunTypeCategory.INTERNAL_CALIBRATION;
        } else if (categoryCode == -2L) {
          throw new JsonParseException(
            "ALIAS run type category (-2) is not allowed");
        } else if (categoryCode == variableId) {
          runTypeCategory = new RunTypeCategory(variableId, variableName);
        } else {
          throw new JsonParseException("Invalid category code " + categoryCode);
        }

        presetRunTypes.add(new PresetRunType(runTypes, runTypeCategory));
      });

    }

    return new VariableProperties(coefficients, dependsQuestionAnswers,
      presetRunTypes);
  }

}
