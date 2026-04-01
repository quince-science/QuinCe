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
 */
public class FlagSerializer
  implements JsonSerializer<Flag>, JsonDeserializer<Flag> {

  /**
   * The {@link FlagScheme} to use for parsing and translating {@link Flag}s.
   */
  private FlagScheme flagScheme;

  /**
   * Constructor.
   *
   * @param flagScheme
   *          The {@link FlagScheme} to use for parsing and translating
   *          {@link Flag}s.
   */
  public FlagSerializer(FlagScheme flagScheme) {
    this.flagScheme = flagScheme;
  }

  @Override
  public JsonElement serialize(Flag src, Type typeOfSrc,
    JsonSerializationContext context) {

    return new JsonPrimitive(src.getValue());
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
        result = flagScheme.getFlag(flagValueElement.getAsInt());
      } else {
        result = flagScheme.getFlag(json.getAsInt());
      }
    } catch (FlagException e) {
      throw new JsonParseException("Invalid flag value");
    }

    return result;

  }
}
