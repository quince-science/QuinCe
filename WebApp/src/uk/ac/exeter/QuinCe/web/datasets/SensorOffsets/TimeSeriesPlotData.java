package uk.ac.exeter.QuinCe.web.datasets.SensorOffsets;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Data structure for building main time series plot data for the Sensor Offsets
 * page.
 *
 * @author Steve Jones
 *
 */
public class TimeSeriesPlotData {

  /**
   * The internal data structure.
   */
  private TreeMap<LocalDateTime, Tuple> data;

  protected TimeSeriesPlotData(List<SensorValue> series1Points,
    List<SensorValue> series2Points) {

    data = new TreeMap<LocalDateTime, Tuple>();
    processSeries1(series1Points);
    processSeries2(series2Points);
  }

  private void processSeries1(List<SensorValue> points) {
    points.stream().filter(p -> p.getUserQCFlag().isGood()).forEach(p -> {
      if (!p.getDoubleValue().isNaN()) {
        Tuple tuple = new Tuple();
        tuple.setFirst(p.getDoubleValue());
        data.put(p.getTime(), tuple);
      }
    });
  }

  private void processSeries2(List<SensorValue> points) {
    points.stream().filter(p -> p.getUserQCFlag().isGood()).forEach(p -> {
      if (!p.getDoubleValue().isNaN()) {
        if (data.containsKey(p.getTime())) {
          data.get(p.getTime()).setSecond(p.getDoubleValue());
        } else {
          Tuple tuple = new Tuple();
          tuple.setSecond(p.getDoubleValue());
          data.put(p.getTime(), tuple);
        }
      }
    });
  }

  public String getArray() {

    JsonArray json = new JsonArray();

    for (Map.Entry<LocalDateTime, Tuple> entry : data.entrySet()) {
      JsonArray entryArray = new JsonArray();
      entryArray.add(DateTimeUtils.dateToLong(entry.getKey()));

      if (entry.getValue().getFirst().isNaN()) {
        entryArray.add(JsonNull.INSTANCE);
      } else {
        entryArray.add(entry.getValue().getFirst());
      }

      if (entry.getValue().getSecond().isNaN()) {
        entryArray.add(JsonNull.INSTANCE);
      } else {
        entryArray.add(entry.getValue().getSecond());
      }

      json.add(entryArray);
    }

    return json.toString();
  }

  protected Double getFirstSeriesValue(LocalDateTime time) {
    return data.get(time).getFirst();
  }

  protected Double getSecondSeriesValue(LocalDateTime time) {
    return data.get(time).getSecond();
  }

  /**
   * A simple Tuple of two Double values
   *
   * @author Steve Jones
   *
   */
  private class Tuple {

    private Double first = Double.NaN;

    private Double second = Double.NaN;

    protected void setFirst(Double first) {
      this.first = first;
    }

    protected void setSecond(Double second) {
      this.second = second;
    }

    private Double getFirst() {
      return first;
    }

    private Double getSecond() {
      return second;
    }
  }
}
