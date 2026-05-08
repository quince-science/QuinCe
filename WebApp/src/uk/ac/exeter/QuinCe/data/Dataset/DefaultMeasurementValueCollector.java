package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * The default implementation of the {@link MeasurementValueCollector}.
 *
 * This collects the {@link MeasurementValue}s for all the {@link Variable}'s
 * required {@link SensorType}s according to the time specified in the supplied
 * reference {@link SensorValuesListValue}.
 */
public class DefaultMeasurementValueCollector
  implements MeasurementValueCollector {

  public Collection<MeasurementValue> collectMeasurementValues(
    Instrument instrument, DataSet dataSet, Variable variable,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn, Measurement measurement)
    throws MeasurementValueCollectorException {

    try {
      SensorValuesListValue referenceValue = getReferenceValue(instrument,
        variable, measurement, allSensorValues);

      if (null == referenceValue) {
        throw new MeasurementValueCollectorException(
          "Failed to get referenceValue for measurement at "
            + measurement.getCoordinate());
      }

      List<MeasurementValue> result = new ArrayList<MeasurementValue>();

      for (SensorType sensorType : variable
        .getAllSensorTypes(!dataSet.fixedPosition())) {

        MeasurementValue measurementValue = MeasurementValueCalculatorFactory
          .calculateMeasurementValue(instrument, dataSet, referenceValue,
            variable, sensorType, allMeasurements, allSensorValues, conn);

        if (null != measurementValue) {
          result.add(measurementValue);
        }
      }

      return result;
    } catch (MeasurementValueCollectorException e) {
      throw e;
    } catch (Exception e) {
      throw new MeasurementValueCollectorException(e);
    }
  }

  /**
   * Obtain the reference value for a {@link Measurement}.
   *
   * <p>
   * The reference value defines the base point for collecting values to be used
   * in data reduction for a given {@link Measurement}. At the most basic level,
   * this is the {@link SensorValue} of the core {@link SensorType} measured at
   * the time of the {@link Measurement}. Values for all the other required
   * {@link SensorType}s for the data reduction are collected based on the
   * timestamp of the 'core' {@link SensorValue}.
   * </p>
   *
   * <p>
   * For instruments that sleep for periods between taking measurements (see
   * {@link SensorValuesList#getMeasurementMode()}), each group of measurements
   * is averaged. Therefore the reference value will encompass the time period
   * of the waking period. From this, the other values collected in
   * {@link #collectMeasurementValues(Instrument, DataSet, Variable, DatasetMeasurements, DatasetSensorValues, Connection, Measurement)}
   * will be selected as the mean of all the {@link SensorValue}s that fall
   * within that time period.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} taking the measurements.
   * @param variable
   *          The {@link Variable} being processed.
   * @param measurement
   *          The {@link Measurement} currently being processed.
   * @param allSensorValues
   *          The set of all {@link SensorValue}s in the current dataset.
   * @return The reference value for the {@link Measurement}.
   * @throws SensorValuesListException
   *           If an error occurs while accessing the {@link SensorValue}s.
   * @throws RunTypeCategoryException
   *           If the run type for the measurement cannot be established.
   * @throws RecordNotFoundException
   */
  private SensorValuesListValue getReferenceValue(Instrument instrument,
    Variable variable, Measurement measurement,
    DatasetSensorValues allSensorValues) throws SensorValuesListException,
    RunTypeCategoryException, RecordNotFoundException {

    /*
     * Get the Run Type for this variable if it has one
     */
    String runType = null;

    if (instrument.isRunTypeForVariable(variable,
      measurement.getRunType(variable))) {
      runType = measurement.getRunType(variable);
    } else if (instrument.isRunTypeForVariable(variable,
      measurement.getRunType(Measurement.RUN_TYPE_DEFINES_VARIABLE))) {
      runType = measurement.getRunType(Measurement.RUN_TYPE_DEFINES_VARIABLE);
    }

    /*
     * The sensor values used for a given measurement will depend on the
     * measurement mode of the instrument as a whole. To find this, we get the
     * measurement mode of either the Run Type column (if the Variable uses run
     * types) or the core sensor type.
     *
     * We retrieve the SensorValuesListValue of the chosen column, which gives a
     * start and end time for which we must retrieve the values for all
     * SensorTypes. For CONTINUOUS mode measurements the start and end times
     * will be identical (meaning we retrieve one record) or for PERIODIC mode
     * we will get a range of times.
     */

    SensorValuesListValue referenceValuesListValue;

    if (null != runType && !Measurement.isNonColumnRunType(runType)) {

      // Assume only 1 run type column
      long runTypeColumn = instrument.getSensorAssignments()
        .getColumnIds(SensorType.RUN_TYPE_SENSOR_TYPE).get(0);
      SensorValuesList runTypeValues = allSensorValues
        .getColumnValues(runTypeColumn);
      referenceValuesListValue = runTypeValues
        .getValue(measurement.getCoordinate(), false);

    } else {
      long coreSensorTypeColumn = instrument.getSensorAssignments()
        .getColumnIds(variable.getCoreSensorType()).get(0);
      SensorValuesList coreSensorValues = allSensorValues
        .getColumnValues(coreSensorTypeColumn);
      referenceValuesListValue = coreSensorValues
        .getValue(measurement.getCoordinate(), false);
    }

    return referenceValuesListValue;
  }
}
