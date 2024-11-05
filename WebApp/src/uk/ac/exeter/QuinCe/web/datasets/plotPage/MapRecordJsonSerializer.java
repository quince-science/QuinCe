package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Renders a {@link MapRecord} to a GeoJSON Point feature.
 */
public class MapRecordJsonSerializer implements JsonSerializer<MapRecord> {

  public static final int VALUE = 0;

  public static final int FLAG = 1;

  public static final int FLAG_IGNORE_NEEDED = 2;

  public static final int SELECTION = 3;

  private int type;

  private DatasetSensorValues allSensorValues;

  public MapRecordJsonSerializer(int type,
    DatasetSensorValues allSensorValues) {
    this.type = type;
    this.allSensorValues = allSensorValues;
  }

  @Override
  public JsonElement serialize(MapRecord src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject json = new JsonObject();

    json.addProperty("type", "Feature");

    JsonObject geometry = new JsonObject();
    geometry.addProperty("type", "Point");

    JsonArray coordinates = new JsonArray();
    coordinates.add(src.position.getLongitude());
    coordinates.add(src.position.getLatitude());

    geometry.add("coordinates", coordinates);

    json.add("geometry", geometry);

    JsonObject properties = new JsonObject();

    switch (type) {
    case VALUE: {
      properties.addProperty("type", VALUE);
      properties.addProperty("rowID", src.id);
      properties.addProperty("value", StringUtils.formatNumber(src.getValue()));
      break;
    }
    case FLAG: {
      properties.addProperty("type", FLAG);
      properties.addProperty("flag",
        src.getFlag(allSensorValues, false).getFlagValue());
      break;
    }
    case FLAG_IGNORE_NEEDED: {
      properties.addProperty("type", FLAG);
      properties.addProperty("flag",
        src.getFlag(allSensorValues, true).getFlagValue());
      break;
    }
    case SELECTION: {
      properties.addProperty("type", SELECTION);
      break;
    }
    }

    json.add("properties", properties);

    return json;
  }

}
