package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class VariableAttributesDeserializer
  implements JsonDeserializer<VariableAttributes> {

  private static final String TYPE_ELEMENT = "type";

  private static final String NAME_ELEMENT = "name";

  private static final String ENUM_VALUES_ELEMENT = "values";

  @Override
  public VariableAttributes deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    VariableAttributes result = new VariableAttributes();

    for (Map.Entry<String, JsonElement> entry : ((JsonObject) json)
      .entrySet()) {
      String id = entry.getKey();

      JsonObject details = (JsonObject) entry.getValue();
      String type = details.get(TYPE_ELEMENT).getAsString();
      String description = details.get(NAME_ELEMENT).getAsString();

      List<String> enumValues = null;

      if (type.equals(VariableAttributes.ENUM_TYPE)) {
        if (!details.has(ENUM_VALUES_ELEMENT)) {
          throw new JsonParseException("Missing ENUM values element");
        }

        enumValues = ((JsonArray) details.get(ENUM_VALUES_ELEMENT)
          .getAsJsonArray()).asList().stream().map(e -> e.getAsString())
          .toList();
      }

      try {
        result.add(new VariableAttribute(id, type, description, enumValues));
      } catch (InvalidVariableAttributeException e) {
        throw new JsonParseException(e);
      }
    }

    return result;
  }

}
