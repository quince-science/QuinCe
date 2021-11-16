package uk.ac.exeter.QuinCe.web.datasets.SensorOffsets;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
   * The name of the first series.
   */
  private final String series1Name;

  /**
   * The name of the second series.
   */
  private final String series2Name;

  /**
   * The internal data structure.
   */
  private TreeMap<LocalDateTime, Tuple> data;

  protected TimeSeriesPlotData(String series1Name,
    List<SensorValue> series1Points, String series2Name,
    List<SensorValue> series2Points) {

    this.series1Name = series1Name;
    this.series2Name = series2Name;

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

  public String getCSV() {
    StringBuilder result = new StringBuilder();

    result.append("Date/Time,");
    result.append(series1Name);
    result.append(',');
    result.append(series2Name);
    result.append('\n');

    for (Map.Entry<LocalDateTime, Tuple> entry : data.entrySet()) {
      result.append(DateTimeUtils.formatDateTime(entry.getKey()));
      result.append(',');
      result.append(entry.getValue().getFirst());
      result.append(',');
      result.append(entry.getValue().getSecond());
      result.append('\n');
    }

    return result.toString();
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
