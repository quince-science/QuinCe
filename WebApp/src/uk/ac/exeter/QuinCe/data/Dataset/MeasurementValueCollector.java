package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * MeasurementValueCollectors are responsible for generating all the
 * {@link MeasurementValue} objects for a {@link Measurement} object.
 */
public interface MeasurementValueCollector {

  /**
   * Collect together all the {@link SensorValue}s required to perform the data
   * reduction for a given {@link Measurement}.
   *
   * <p>
   * One {@link MeasurementValue} is required for each of the
   * {@link SensorType}s required for the data reduction. However, a
   * {@link MeasurementValue} may not be directly analagous to a single
   * {@link SensorValue} due to factors including but not limited to:
   * </p>
   *
   * <ul>
   * <li>Interpolation due to sensors not all taking measurements at the same
   * time.</li>
   * <li>Interpolation performed to recover data around values that have been
   * QCed as {@link Flag#QUESTIONABLE} or {@link Flag#BAD}.</li>
   * <li>Averaging of data for sensors that take measurements in groups between
   * periods of sleep.</li>
   * </ul>
   *
   * <p>
   * Implementations of this interface are free to determine how the collection
   * is performed according to needs of the sensor concerned.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} taking the measurements.
   * @param dataSet
   *          The {@link DataSet} being processed.
   * @param variable
   *          The {@link Variable} whose data reduction is being prepared.
   * @param allMeasurements
   *          The complete set of {@link Measurement}s in the dataset.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s in the dataset.
   * @param conn
   *          A database connection.
   * @param measurement
   *          The {@link Measurement} being processed.
   * @return The values to be used in the data reduction.
   * @throws MeasurementValueCollectorException
   *           If the values cannot be collected.
   */
  public Collection<MeasurementValue> collectMeasurementValues(
    Instrument instrument, DataSet dataSet, Variable variable,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn, Measurement measurement)
    throws MeasurementValueCollectorException;
}
