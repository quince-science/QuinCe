package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * {@link MeasurementValueCollector} for 4H-Jena CONTROS CO₂ sensors.
 *
 * <p>
 * Due to the special zero behaviours (zero before sleep, zero after sleep
 * etc.), the automatic detection of PERIODIC/CONTINUOUS modes (see
 * {@link SensorValuesList}) cannot be used. Every record from the sensor must
 * be treated as an individual measurement, and thus PERIODIC mode does not
 * work. Therefore, for the sensor's own {@link SensorType}s we always get each
 * value directly and do not rely on the {@link SensorValuesList}'s internal
 * algorithms. This approach also precludes interpolating around values with QC
 * flags applied.
 * </p>
 */
public class ControsPco2MeasurementValueCollector
  implements MeasurementValueCollector {

  /**
   * The names of the {@link SensorType}s recorded by the CONTROS sensor.
   */
  private static List<String> CONTROS_SENSOR_TYPES;

  static {
    CONTROS_SENSOR_TYPES = new ArrayList<String>();
    CONTROS_SENSOR_TYPES.add("Gas Stream Temperature");
    CONTROS_SENSOR_TYPES.add("Cell Gas Pressure");
    CONTROS_SENSOR_TYPES.add("Membrane Pressure");
    CONTROS_SENSOR_TYPES.add("Raw Detector Signal");
    CONTROS_SENSOR_TYPES.add("Reference Signal");
    CONTROS_SENSOR_TYPES.add("Zero Mode");
    CONTROS_SENSOR_TYPES.add("Flush Mode");
    CONTROS_SENSOR_TYPES.add("Runtime");
  }

  @Override
  public Collection<MeasurementValue> collectMeasurementValues(
    Instrument instrument, DataSet dataSet, Variable variable,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn, Measurement measurement)
    throws MeasurementValueCollectorException {

    try {

      // Convenience variable to make code shorter
      LocalDateTime time = measurement.getTime();

      List<MeasurementValue> result = new ArrayList<MeasurementValue>();

      long referenceColumnId = instrument.getSensorAssignments()
        .getColumnIds("Raw Detector Signal").get(0);

      SensorValuesListValue referenceValue = allSensorValues
        .getColumnValues(referenceColumnId).getValue(time, time, time, false);

      for (SensorType sensorType : variable
        .getAllSensorTypes(!dataSet.fixedPosition())) {

        if (CONTROS_SENSOR_TYPES.contains(sensorType.getShortName())) {
          try {
            long columnId = instrument.getSensorAssignments()
              .getColumnIds(sensorType).get(0);
            SensorValuesList sensorValuesList = allSensorValues
              .getColumnValues(columnId);

            result.add(new MeasurementValue(sensorType,
              sensorValuesList.getValue(time, time, time, false)));
          } catch (SensorValuesListException e) {
            throw new MeasurementValueCalculatorException(
              "Error getting Pro Oceanus value");
          }
        } else {
          // Values from sensors other than the CONTROS can be interpolated or
          // whatever.
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
}
