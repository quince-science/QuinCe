package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;

/**
 * Gson Serializer to perform a minimal serialization of {@link ArgoCoordinate}
 * objects.
 *
 * <p>
 * The serializer encodes only the {@link ArgoCoordinate}'s database ID.
 * </p>
 */
public class CoordinateIdSerializer implements JsonSerializer<ArgoCoordinate> {

  @Override
  public JsonElement serialize(ArgoCoordinate src, Type typeOfSrc,
    JsonSerializationContext context) {

    return new JsonPrimitive(src.getId());
  }
}
