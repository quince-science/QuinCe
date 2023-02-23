package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public abstract class MeasurementValueCalculator {

  public abstract MeasurementValue calculate(Instrument instrument,
    DataSet dataSet, Measurement measurement, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException;

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
