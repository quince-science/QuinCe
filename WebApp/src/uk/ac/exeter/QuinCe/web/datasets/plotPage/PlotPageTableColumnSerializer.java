package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PlotPageTableColumnSerializer
  implements JsonSerializer<PlotPageTableColumn> {

  @Override
  public JsonElement serialize(PlotPageTableColumn src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject json = new JsonObject();

    if (NumberUtils.isCreatable(src.getValue())) {
      json.addProperty("value", Double.parseDouble(src.getValue()));
    } else {
      json.addProperty("value", src.getValue());
    }

    json.addProperty("used", src.getUsed());
    json.addProperty("qcFlag", src.getQcFlag().getFlagValue());
    json.addProperty("qcMessage", src.getQcMessage());
    json.addProperty("flagNeeded", src.getFlagNeeded());

    return json;
  }

}
