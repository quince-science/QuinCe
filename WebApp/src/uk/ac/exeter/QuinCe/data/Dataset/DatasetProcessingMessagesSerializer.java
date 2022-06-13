package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DatasetProcessingMessagesSerializer
  implements JsonSerializer<DatasetProcessingMessages>,
  JsonDeserializer<DatasetProcessingMessages> {

  @Override
  public JsonElement serialize(DatasetProcessingMessages src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject json = new JsonObject();

    for (Map.Entry<String, List<String>> entry : src.entrySet()) {
      JsonArray messageArray = new JsonArray();
      entry.getValue().forEach(s -> messageArray.add(s));
      json.add(entry.getKey(), messageArray);
    }

    return json;
  }

  @Override
  public DatasetProcessingMessages deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    DatasetProcessingMessages result = new DatasetProcessingMessages();

    JsonObject jsonObj = json.getAsJsonObject();
    for (String module : jsonObj.keySet()) {
      JsonArray messages = jsonObj.get(module).getAsJsonArray();
      messages.forEach(m -> result.addMessage(module, m.getAsString()));
    }

    return result;
  }
}
