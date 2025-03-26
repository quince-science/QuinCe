package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;

/**
 * {@link MeasurementValueCollector} for Pro Oceanus CO₂ sensors.
 *
 * <p>
 * The reference value must be the run type value ({@code a m} or {@code w m}),
 * which should represent a group of measurements in PERIODIC mode (see
 * {@link SensorValuesList}). The values from the Pro Oceanus will be retrieved
 * from the list directly based on the time period of the reference value
 * without interpolation. Other required values will be retrieved as normal
 * variables.
 * </p>
 *
 * <p>
 * Sensor offset groups will not be applied to the Pro Oceanus values. However,
 * the target {@link Measurement} object will still have its time adjusted to
 * the earliest sensor group in the {@link DataReductionJob}.
 * </p>
 *
 * @see SensorValuesList
 * @see DataReductionJob
 */
public class ProOceanusMeasurementValueCollector
  implements MeasurementValueCollector {

  /**
   * The names of the {@link SensorType}s measured directly by the Pro Oceanus
   * sensor. These values must not be interpolated, while others can be.
   */
  private static List<String> INTERNAL_SENSOR_TYPES;

  static {
    INTERNAL_SENSOR_TYPES = new ArrayList<String>();
    INTERNAL_SENSOR_TYPES.add("Cell Gas Pressure");
    INTERNAL_SENSOR_TYPES.add("Humidity Pressure");
    INTERNAL_SENSOR_TYPES.add("ProOceanus Current Count");
    INTERNAL_SENSOR_TYPES.add("ProOceanus Zero Count");
    INTERNAL_SENSOR_TYPES.add("xCO₂ (wet, no standards)");
  }

  @Override
  public Collection<MeasurementValue> collectMeasurementValues(
    Instrument instrument, DataSet dataSet, Variable variable,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn, Measurement measurement)
    throws MeasurementValueCollectorException {

    try {
      SensorValuesListValue referenceValue = getReferenceValue(instrument,
        measurement, allSensorValues);

      List<MeasurementValue> result = new ArrayList<MeasurementValue>();

      for (SensorType sensorType : variable
        .getAllSensorTypes(!dataSet.fixedPosition())) {

        if (INTERNAL_SENSOR_TYPES.contains(sensorType.getShortName())) {

          try {
            long columnId = instrument.getSensorAssignments()
              .getColumnIds(sensorType).get(0);
            SensorValuesList sensorValuesList = allSensorValues
              .getColumnValues(columnId);

            SensorValuesListOutput value = sensorValuesList
              .getValue(referenceValue, true);

            result.add(new MeasurementValue(sensorType, value));
          } catch (SensorValuesListException e) {
            throw new MeasurementValueCalculatorException(
              "Error getting Pro Oceanus value");
          }
        } else {
          result.add(MeasurementValueCalculatorFactory
            .calculateMeasurementValue(instrument, dataSet, referenceValue,
              variable, sensorType, allMeasurements, allSensorValues, conn));
        }
      }

      return result;
    } catch (Exception e) {
      throw new MeasurementValueCollectorException(e);
    }
  }

  private SensorValuesListValue getReferenceValue(Instrument instrument,
    Measurement measurement, DatasetSensorValues allSensorValues)
    throws SensorValuesListException {

    long runTypeColumn = instrument.getSensorAssignments()
      .getColumnIds(SensorType.RUN_TYPE_SENSOR_TYPE).get(0);
    SensorValuesList runTypeValues = allSensorValues
      .getColumnValues(runTypeColumn);
    runTypeValues.allowStringValuesToDefineGroups(true);
    return runTypeValues.getValue(measurement.getTime(), false);
  }
}
