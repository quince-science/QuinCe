package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * The default implementation of the {@link MeasurementValuesCollector}.
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

      List<MeasurementValue> result = new ArrayList<MeasurementValue>();

      for (SensorType sensorType : variable
        .getAllSensorTypes(!dataSet.fixedPosition())) {

        result.add(MeasurementValueCalculatorFactory.calculateMeasurementValue(
          instrument, dataSet, referenceValue, variable, sensorType,
          allMeasurements, allSensorValues, conn));
      }

      return result;
    } catch (Exception e) {
      throw new MeasurementValueCollectorException(e);
    }
  }

  private SensorValuesListValue getReferenceValue(Instrument instrument,
    Variable variable, Measurement measurement,
    DatasetSensorValues allSensorValues)
    throws SensorValuesListException, RunTypeCategoryException {

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

    if (null != runType) {

      // Assume only 1 run type column
      long runTypeColumn = instrument.getSensorAssignments()
        .getColumnIds(SensorType.RUN_TYPE_SENSOR_TYPE).get(0);
      SensorValuesList runTypeValues = allSensorValues
        .getColumnValues(runTypeColumn);
      referenceValuesListValue = runTypeValues.getValue(measurement.getTime(),
        false);

    } else {
      long coreSensorTypeColumn = instrument.getSensorAssignments()
        .getColumnIds(variable.getCoreSensorType()).get(0);
      SensorValuesList coreSensorValues = allSensorValues
        .getColumnValues(coreSensorTypeColumn);
      referenceValuesListValue = coreSensorValues
        .getValue(measurement.getTime(), false);
    }

    return referenceValuesListValue;
  }
}
