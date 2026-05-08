package uk.ac.exeter.QuinCe.web.datasets.SensorOffsets;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.SensorOffset;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * JSON Serializer for {@link SensorOffset} objects. Note that this serializer
 * is only for use by the {@link SensorOffsetsBean}, and assumes a great deal
 * from the bean's context.
 */
public class SensorOffsetSerializer implements JsonSerializer<SensorOffset> {

  private SensorOffsetsBean sourceBean;

  protected SensorOffsetSerializer(SensorOffsetsBean sourceBean) {
    this.sourceBean = sourceBean;
  }

  @Override
  public JsonElement serialize(SensorOffset src, Type typeOfSrc,
    JsonSerializationContext context) {

    // If the plot data has not yet been loaded in the bean, we just return
    // null. Things will get updated later on.
    JsonElement result = JsonNull.INSTANCE;

    if (null != sourceBean.getPlotData()) {
      LocalDateTime secondTime = src.getTime();
      LocalDateTime firstTime = DateTimeUtils
        .longToDate(DateTimeUtils.dateToLong(secondTime) - src.getOffset());

      SensorOffsetsPlotData plotData = sourceBean.getPlotData();

      JsonObject json = new JsonObject();
      json.addProperty("firstTime", DateTimeUtils.dateToLong(firstTime));

      Double firstValue = plotData.getFirstSeriesValue(firstTime);
      if (firstValue.isNaN()) {
        json.add("firstValue", JsonNull.INSTANCE);
      } else {
        json.addProperty("firstValue", firstValue);
      }

      json.addProperty("secondTime", DateTimeUtils.dateToLong(secondTime));

      Double secondValue = plotData.getSecondSeriesValue(secondTime);
      if (secondValue.isNaN()) {
        json.add("secondValue", JsonNull.INSTANCE);
      } else {
        json.addProperty("secondValue", secondValue);
      }

      json.addProperty("offset", src.getOffset());

      result = json;
    }

    return result;
  }
}
