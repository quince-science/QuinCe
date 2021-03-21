package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * JSON serializer for QC flags - just stores the numeric value
 *
 * @author stevej
 *
 */
public class FlagSerializer
  implements JsonSerializer<Flag>, JsonDeserializer<Flag> {

  @Override
  public JsonElement serialize(Flag src, Type typeOfSrc,
    JsonSerializationContext context) {

    return new JsonPrimitive(src.getFlagValue());
  }

  @Override
  public Flag deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    Flag result;

    try {
      // Handle JSON from RoutineFlag instances. Simply ignore the detail and
      // get the flag value

      if (json.isJsonObject()) {
        JsonElement flagValueElement = json.getAsJsonObject().get("flagValue");
        result = new Flag(flagValueElement.getAsInt());
      } else {
        result = new Flag(json.getAsInt());
      }
    } catch (InvalidFlagException e) {
      throw new JsonParseException("Invalid flag value");
    }

    return result;

  }
}
