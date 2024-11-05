package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;

public class PlotPageTableValueSerializer
  implements JsonSerializer<PlotPageTableValue> {

  private DatasetSensorValues allSensorValues;

  public PlotPageTableValueSerializer(DatasetSensorValues allSensorValues) {
    this.allSensorValues = allSensorValues;
  }

  @Override
  public JsonElement serialize(PlotPageTableValue src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject json = new JsonObject();

    if (null == src || src.isNull()) {
      json.add("value", JsonNull.INSTANCE);
      json.add("qcFlag", JsonNull.INSTANCE);
      json.add("qcMessage", JsonNull.INSTANCE);
      json.add("flagNeeded", JsonNull.INSTANCE);
      json.add("type", JsonNull.INSTANCE);
    } else {
      json.addProperty("value", src.getValue());
      json.addProperty("qcFlag", src.getQcFlag(allSensorValues).getFlagValue());

      if (null == src.getQcMessage(allSensorValues, false)) {
        json.add("qcMessage", JsonNull.INSTANCE);
      } else {
        json.addProperty("qcMessage",
          src.getQcMessage(allSensorValues, true).replaceAll("\\r?\\n", ";"));
      }

      json.addProperty("flagNeeded", src.getFlagNeeded());
      json.addProperty("type", src.getType());
    }

    return json;
  }

}
