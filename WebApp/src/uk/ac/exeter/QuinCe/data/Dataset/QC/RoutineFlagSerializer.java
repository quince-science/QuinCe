package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * JSON serializer for {@link RoutineFlag} instances.
 *
 * <p>
 * At the time of writing, deserializations are not interested in the
 * {@code routineName}, although we do store it for provenance purposes and it
 * may become useful in future. For now, though, deserialization is performed by
 * the {@link FlagSerializer} class which automatically detects this
 * serialization format.
 * </p>
 *
 * @see FlagSerializer
 */
public class RoutineFlagSerializer implements JsonSerializer<RoutineFlag> {

  @Override
  public JsonElement serialize(RoutineFlag src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject json = new JsonObject();
    json.addProperty("routineName", src.getRoutineName());
    json.addProperty("flagValue", src.getValue());

    return json;
  }
}
