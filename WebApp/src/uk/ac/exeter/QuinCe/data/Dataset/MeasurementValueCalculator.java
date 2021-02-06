package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public abstract class MeasurementValueCalculator {

  public abstract MeasurementValue calculate(Instrument instrument,
    Measurement measurement, SensorType sensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException;

  protected Double interpolate(SensorValue prior, SensorValue post,
    LocalDateTime measurementTime) {

    Double result = null;

    if (null != prior && null != post) {
      double x0 = DateTimeUtils.dateToLong(prior.getTime());
      double y0 = prior.getDoubleValue();
      double x1 = DateTimeUtils.dateToLong(post.getTime());
      double y1 = post.getDoubleValue();
      result = interpolate(x0, y0, x1, y1,
        DateTimeUtils.dateToLong(measurementTime));
    } else if (null != prior) {
      result = prior.getDoubleValue();
    } else if (null != post) {
      result = post.getDoubleValue();
    }

    return result;
  }

  protected Double interpolate(LocalDateTime time0, Double y0,
    LocalDateTime time1, Double y1, LocalDateTime measurementTime) {
    Double result = null;

    if (null != y0 && null != y1) {
      double x0 = DateTimeUtils.dateToLong(time0);
      double x1 = DateTimeUtils.dateToLong(time1);
      result = interpolate(x0, y0, x1, y1,
        DateTimeUtils.dateToLong(measurementTime));
    } else if (null != y0) {
      result = y0;
    } else if (null != y1) {
      result = y1;
    }

    return result;
  }

  protected double interpolate(double x0, double y0, double x1, double y1,
    double x) {

    return (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0);
  }

  protected static List<SensorValue> getSensorValues(
    Collection<MeasurementValue> measurementValues,
    DatasetSensorValues allSensorValues) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    measurementValues.forEach(m -> m.getSensorValueIds()
      .forEach(s -> result.add(allSensorValues.getById(s))));

    return result;
  }

  protected static List<SensorValue> getSensorValues(
    DatasetSensorValues allSensorValues,
    MeasurementValue... measurementValues) {
    return getSensorValues(Arrays.asList(measurementValues), allSensorValues);
  }

  protected static int getMemberCount(Collection<MeasurementValue> values) {
    return values.stream().mapToInt(x -> x.getMemberCount()).sum();
  }

  protected static int getMemberCount(MeasurementValue... values) {
    return getMemberCount(Arrays.asList(values));
  }
}
