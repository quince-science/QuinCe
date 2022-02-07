package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class SensorOffsetsDeserializer
  implements JsonDeserializer<SensorOffsets> {

  private Instrument instrument;

  public SensorOffsetsDeserializer(Instrument instrument) {
    this.instrument = instrument;
  }

  @Override
  public SensorOffsets deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    // Initialize the SensorOffsets object
    SensorOffsets sensorOffsets = new SensorOffsets(
      instrument.getSensorGroups());

    for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject()
      .entrySet()) {

      // Get the group pair
      SensorGroupPair groupPair;
      try {
        groupPair = instrument.getSensorGroups()
          .getGroupPair(Integer.parseInt(entry.getKey()));
      } catch (Exception e) {
        throw new JsonParseException(e);
      }

      // Loop through the offsets
      entry.getValue().getAsJsonArray().forEach(a -> {

        try {

          JsonObject offsetObject = a.getAsJsonObject();
          LocalDateTime time = DateTimeUtils.longToDate(
            offsetObject.get(SensorOffsetsSerializer.TIME_NAME).getAsLong());
          long offset = offsetObject.get(SensorOffsetsSerializer.OFFSET_NAME)
            .getAsLong();

          sensorOffsets.addOffset(groupPair, time, offset);
        } catch (SensorOffsetsException e) {
          throw new JsonParseException(e);
        }
      });
    }

    return sensorOffsets;
  }
}
