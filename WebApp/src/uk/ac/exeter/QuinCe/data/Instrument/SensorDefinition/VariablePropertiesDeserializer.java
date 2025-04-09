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

public class VariablePropertiesDeserializer
  implements JsonDeserializer<VariableProperties> {

  @Override
  public VariableProperties deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    JsonObject jsonObj = json.getAsJsonObject();

    String runType = null;
    if (jsonObj.has("runType")) {
      runType = jsonObj.get("runType").getAsString();
    }

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

    return new VariableProperties(runType, coefficients,
      dependsQuestionAnswers);
  }

}
