package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * An object to calculate {@link MeasurementValue} objects.
 */
public abstract class MeasurementValueCalculator {

  /**
   * Calculate a {@link MeasurementValue} for a given {@link Measurement} and
   * {@link SensorType}.
   *
   * <p>
   * This acquires the correct {@link MeasurementValueCalculator} for the
   * {@link SensorType} and uses it to calculate the required
   * {@link MeasurementValue}.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} for which measurements are being
   *          calculated.
   * @param dataSet
   *          The {@link DataSet} for which measurements are being calculated.
   * @param timeReference
   *          A {link SensorValuesListValue} containing details of the time
   *          period to be considered when building the {@link MeasuremntValue}.
   * @param variable
   *          The {@link Variable} for the current {@link Measurement}.
   * @param requiredSensorType
   *          The {@link SensorType} for which a value is required.
   * @param allMeasurements
   *          The complete set of {@link Measurements} for the current
   *          {@link DataSet}.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s for the current
   *          {@link DataSet}.
   * @param conn
   *          A database connection.
   * @return The calculated {@link MeasurementValue}.
   * @throws MeasurementValueCalculatorException
   *           If the value cannot be constructed.
   */
  public abstract MeasurementValue calculate(Instrument instrument,
    DataSet dataSet, SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException;

  /**
   * Retrieve all the {@link SensorValue} objects used to create a given
   * collection of {@link MeasurementValue}s.
   *
   * <p>
   * Note that supporting {@link SensorValue}s (see
   * {@link MeasurementValue#supportingSensorValueIds}) are not included.
   * </p>
   *
   * @param measurementValues
   *          The {@link MeasurementValue}s for which {@link SensorValue}s are
   *          required.
   * @param allSensorValues
   *          The complete set of {@link SensorValue} objects for the dataset.
   * @return The used {@link SensorValue}s.
   */
  protected static List<SensorValue> getSensorValues(
    Collection<MeasurementValue> measurementValues,
    DatasetSensorValues allSensorValues) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    measurementValues.forEach(m -> m.getSensorValueIds()
      .forEach(s -> result.add(allSensorValues.getById(s))));

    return result;
  }

  /**
   * Retrieve all the {@link SensorValue} objects used to create the given
   * {@link MeasurementValue}s.
   *
   * <p>
   * Note that supporting {@link SensorValue}s (see
   * {@link MeasurementValue#supportingSensorValueIds}) are not included.
   * </p>
   *
   * @param allSensorValuexs
   *          The complete set of {@link SensorValue} objects for the dataset.
   * @param measurementValues
   *          The {@link MeasurementValue}s for which {@link SensorValue}s are
   *          required.
   * @return The used {@link SensorValue}s.
   */
  protected static List<SensorValue> getSensorValues(
    DatasetSensorValues allSensorValues,
    MeasurementValue... measurementValues) {
    return getSensorValues(Arrays.asList(measurementValues), allSensorValues);
  }
}
