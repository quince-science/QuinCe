package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson serializer for a NewInstrumentFileSet. Only includes the info required
 * in the front end.
 * 
 * @author stevej
 *
 */
public class NewInstrumentFileSetSerializer
  implements JsonSerializer<NewInstrumentFileSet> {

  @Override
  public JsonElement serialize(NewInstrumentFileSet src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject files = new JsonObject();

    for (int i = 0; i < src.size(); i++) {
      FileDefinitionBuilder file = src.get(i);

      JsonObject fileInfo = new JsonObject();
      fileInfo.addProperty("headerLines", file.getHeaderLines());

      files.add(file.getFileDescription(), fileInfo);
    }

    return files;
  }
}
