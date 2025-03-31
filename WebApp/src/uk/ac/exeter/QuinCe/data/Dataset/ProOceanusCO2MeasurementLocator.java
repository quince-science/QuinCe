package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Measurement locator for Pro Oceanus CO₂ sensors.
 *
 * <p>
 * This handles both marine and atmospheric variables (use the
 * {@link DummyMeasurementLocator} for the atmospheric variable).
 * </p>
 *
 * <p>
 * A flushing time is often not required for Pro Oceanus sensors. However, if
 * one is set the flushing is applied when either:
 * </p>
 * <ul>
 * <li>The value of the Zero A/D column changes</li>
 * <li>The run mode changes (W M) or (A M)</li>
 * </ul>
 * <p>
 * If both of these happen at the same time, the flushing periods are combined.
 * Flushing flags are applied to the CO₂ column.
 * </p>
 *
 * <p>
 * Measurements are located after the flushing flags have been applied. If the
 * sensor is not measuring continuously, the CO₂ values should be detected in
 * PERIODIC mode (see {@link SensorValuesList}), so there will be one
 * measurement per period of {@code W M} and {@code A M} readings.
 * </p>
 */
public class ProOceanusCO2MeasurementLocator extends MeasurementLocator {

  // Run types are converted to lower case

  /**
   * Indicates that the sensor is taking water measurements.
   */
  public static final String WATER_MODE = "w m";

  /**
   * Indicates that the sensor is taking atmospheric measurements.
   */
  public static final String ATM_MODE = "a m";

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset, DatasetSensorValues sensorValues)
    throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable waterVar = sensorConfig
        .getInstrumentVariable("Pro Oceanus CO₂ Water");
      Variable atmVar = sensorConfig
        .getInstrumentVariable("Pro Oceanus CO₂ Atmosphere");

      HashMap<Long, String> waterRunTypes = new HashMap<Long, String>();
      waterRunTypes.put(waterVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);

      HashMap<Long, String> atmRunTypes = new HashMap<Long, String>();
      atmRunTypes.put(atmVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);

      SensorType zeroCountType = sensorConfig
        .getSensorType("ProOceanus Zero Count");
      SensorType co2Type = sensorConfig
        .getSensorType("xCO₂ (wet, no standards)");
      SensorValuesList runTypes = sensorValues.getRunTypes();

      // Assume one column of each type
      long zeroCountColumn = instrument.getSensorAssignments()
        .getColumnIds(zeroCountType).get(0);
      long co2Column = instrument.getSensorAssignments().getColumnIds(co2Type)
        .get(0);

      // Loop through all the rows, examining the zero/run type columns to
      // locate flushing values.
      List<SensorValue> flaggedSensorValues = new ArrayList<SensorValue>();

      SensorValuesList co2Values = sensorValues.getColumnValues(co2Column);

      // First, the zero values
      SensorValuesList zeroValues = sensorValues
        .getColumnValues(zeroCountColumn);

      if (instrument.getIntProperty(Instrument.PROP_PRE_FLUSHING_TIME) > 0) {

        String lastZero = "";
        for (SensorValue zero : zeroValues.getRawValues()) {
          String newZero = zero.getValue();
          if (!newZero.equals(lastZero)) {
            LocalDateTime flushingStart = zero.getTime();
            LocalDateTime flushingEnd = flushingStart.plusSeconds(
              instrument.getIntProperty(Instrument.PROP_PRE_FLUSHING_TIME));

            for (SensorValue flushingCO2 : co2Values.getRawValues(flushingStart,
              flushingEnd)) {
              flushingCO2.setUserQC(Flag.FLUSHING, "Flushing");
              flaggedSensorValues.add(flushingCO2);
            }
          }

          lastZero = newZero;
        }

        // Now do the same for run types
        String lastRunType = "";
        for (SensorValue runType : runTypes.getRawValues()) {
          String newRunType = runType.getValue();
          if (!newRunType.equals(lastRunType)) {
            LocalDateTime flushingStart = runType.getTime();
            LocalDateTime flushingEnd = flushingStart.plusSeconds(
              instrument.getIntProperty(Instrument.PROP_PRE_FLUSHING_TIME));

            for (SensorValue flushingCO2 : co2Values.getRawValues(flushingStart,
              flushingEnd)) {
              flushingCO2.setUserQC(Flag.FLUSHING, "Flushing");
              flaggedSensorValues.add(flushingCO2);
            }
          }

          lastRunType = newRunType;
        }

        // Store the updated flags
        DataSetDataDB.storeSensorValues(conn, flaggedSensorValues);

        // Force the CO2 output values to be recalculated now that
        // some values have had their flags changed.
        co2Values.resetOutput();
      }

      // Now we construct measurements based on the remaining run types.
      List<Measurement> measurements = new ArrayList<Measurement>();

      runTypes.allowStringValuesToDefineGroups(true);
      for (SensorValuesListValue runType : runTypes.getValues()) {
        /*
         * Null run types can happen if data is coming from multiple files (eg
         * TSG data).
         */
        if (null != runType) {

          SensorValuesListValue co2Value = co2Values.getValue(runType.getTime(),
            false);

          // We only make measurements for non-flushing CO2 values
          if (null != co2Value) {
            if (runType.getStringValue().equals(WATER_MODE)) {
              if (instrument.hasVariable(waterVar)) {
                measurements.add(new Measurement(dataset.getId(),
                  runType.getNominalTime(), waterRunTypes));
              }
            } else if (runType.getStringValue().equals(ATM_MODE)) {
              if (instrument.hasVariable(atmVar)) {
                measurements.add(new Measurement(dataset.getId(),
                  runType.getNominalTime(), atmRunTypes));
              }
            } else {
              throw new MeasurementLocatorException(
                "Unrecognised ProOceanus mode '" + runType.getStringValue()
                  + "'");
            }
          }
        }
      }

      return measurements;
    } catch (Exception e) {
      if (e instanceof MeasurementLocatorException) {
        throw (MeasurementLocatorException) e;
      } else {
        throw new MeasurementLocatorException(e);
      }
    }
  }
}
