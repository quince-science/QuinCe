package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class ReferenceValueSerializer
  implements JsonSerializer<TreeMap<LocalDateTime, Double>> {

  @Override
  public JsonElement serialize(TreeMap<LocalDateTime, Double> src,
    Type typeOfSrc, JsonSerializationContext context) {

    JsonArray array = new JsonArray();

    for (Map.Entry<LocalDateTime, Double> entry : src.entrySet()) {
      JsonObject entryJson = new JsonObject();
      entryJson.addProperty("date", DateTimeUtils.dateToLong(entry.getKey()));
      entryJson.addProperty("value", entry.getValue());
      array.add(entryJson);
    }

    return array;
  }
}
