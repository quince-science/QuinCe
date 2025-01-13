package uk.ac.exeter.QuinCe.web.datasets.SensorOffsets;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;

import uk.ac.exeter.QuinCe.data.Dataset.SensorOffsets;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Data structure for building main time series plot data for the Sensor Offsets
 * page.
 */
public class TimeSeriesPlotData {

  /**
   * The internal data structure.
   */
  private TreeMap<LocalDateTime, Tuple> data;

  private List<SensorValue> series2;

  protected TimeSeriesPlotData(SensorValuesList series1Points,
    SensorValuesList series2Points) {

    data = new TreeMap<LocalDateTime, Tuple>();

    // Extract the two series into the Tuple map.
    processSeries1(series1Points);
    processSeries2(series2Points);

    // Store the second series separately - we need it to apply offsets to.
    this.series2 = series2Points.getRawValues();
  }

  private void processSeries1(SensorValuesList points) {
    points.getRawValues().stream().filter(p -> p.getUserQCFlag().isGood())
      .forEach(p -> {
        if (!p.getDoubleValue().isNaN()) {
          Tuple tuple = new Tuple();
          tuple.setFirst(p.getDoubleValue());
          data.put(p.getTime(), tuple);
        }
      });
  }

  private void processSeries2(SensorValuesList points) {
    points.getRawValues().stream().filter(p -> p.getUserQCFlag().isGood())
      .forEach(p -> {
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

  protected String getArray(SensorOffsets sensorOffsets,
    SensorGroupPair groupPair) {

    List<SensorValue> offsetsApplied = sensorOffsets.applyOffsets(groupPair,
      series2);

    TreeMap<LocalDateTime, Tuple> dataWithOffset = new TreeMap<LocalDateTime, Tuple>(
      data);

    // Add the offsets to the copied map
    offsetsApplied.forEach(o -> {
      if (dataWithOffset.containsKey(o.getTime())) {

        Tuple oldTuple = dataWithOffset.get(o.getTime());

        Tuple newTuple = new Tuple(oldTuple);
        newTuple.setOffsetSecond(o.getDoubleValue());
        dataWithOffset.put(o.getTime(), newTuple);
      } else {
        Tuple tuple = new Tuple();
        tuple.setOffsetSecond(o.getDoubleValue());
        dataWithOffset.put(o.getTime(), tuple);
      }
    });

    JsonArray json = new JsonArray();

    for (Map.Entry<LocalDateTime, Tuple> entry : dataWithOffset.entrySet()) {
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

      if (entry.getValue().getOffsetSecond().isNaN()) {
        entryArray.add(JsonNull.INSTANCE);
      } else {
        entryArray.add(entry.getValue().getOffsetSecond());
      }

      json.add(entryArray);
    }

    return json.toString();
  }

  protected Double getFirstSeriesValue(LocalDateTime time) {
    Tuple tuple = data.get(time);
    return null == tuple ? Double.NaN : tuple.getFirst();
  }

  protected Double getSecondSeriesValue(LocalDateTime time) {
    Tuple tuple = data.get(time);
    return null == tuple ? Double.NaN : tuple.getSecond();
  }

  /**
   * A simple Tuple of two Double values
   */
  private class Tuple {

    private Double first = Double.NaN;

    private Double second = Double.NaN;

    private Double offsetSecond = Double.NaN;

    protected Tuple() {
      // Blank constructor
    }

    protected Tuple(Tuple source) {
      this.first = source.first;
      this.second = source.second;
      this.offsetSecond = source.offsetSecond;
    }

    protected void setFirst(Double first) {
      this.first = first;
    }

    protected void setSecond(Double second) {
      this.second = second;
    }

    protected void setOffsetSecond(Double offsetSecond) {
      this.offsetSecond = offsetSecond;
    }

    private Double getFirst() {
      return first;
    }

    private Double getSecond() {
      return second;
    }

    private Double getOffsetSecond() {
      return offsetSecond;
    }
  }
}
