package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * The default implementation of {@link MeasurementValueCalculator}.
 *
 * <p>
 * Applies interpolation based on quality flags, and standard calibration as
 * needed.
 * </p>
 *
 * @author stevej
 *
 */
public class DefaultMeasurementValueCalculator
  extends MeasurementValueCalculator {

  public static final String STANDARDS_COUNT_PROPERTY = "stdcount";

  // TODO Need limits on how far interpolation goes before giving up.

  @Override
  public MeasurementValue calculate(Instrument instrument,
    Measurement measurement, SensorType sensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    try {
      // TODO #1128 This currently assumes only one sensor for each SensorType.
      // This will have to change eventually.

      // Get the file column for the sensor type
      long columnId = instrument.getSensorAssignments().getColumnIds(sensorType)
        .get(0);

      // If the required SensorType is core, we do not search for interpolated
      // values - the core sensor type defines the measurement so must be used.
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      SearchableSensorValuesList sensorValues = allSensorValues
        .getColumnValues(columnId);

      MeasurementValue result = new MeasurementValue(sensorType);

      if (sensorConfig.isCoreSensor(sensorType)) {
        SensorValue sensorValue = sensorValues.get(measurement.getTime());
        if (null != sensorValue) {
          result.addSensorValue(sensorValue, true);
          result.setCalculatedValue(sensorValue.getDoubleValue());
        }
      } else {

        // Otherwise we get the closest GOOD (or best available quality) values
        // we can, interpolating where required.
        List<SensorValue> valuesToUse = sensorValues
          .getWithInterpolation(measurement.getTime(), true);

        switch (valuesToUse.size()) {
        case 0: {
          // We should not use a value here
          result.setCalculatedValue(Double.NaN);
          break;
        }
        case 1: {
          // Value from exact time - use it directly
          result.addSensorValue(valuesToUse.get(0), true);
          result.setCalculatedValue(valuesToUse.get(0).getDoubleValue());
          break;
        }
        case 2: {
          result.addSensorValues(valuesToUse, true);
          result.setCalculatedValue(SensorValue.interpolate(valuesToUse.get(0),
            valuesToUse.get(1), measurement.getTime()));
          break;
        }
        default: {
          throw new MeasurementValueCalculatorException(
            "Invalid number of values in search result");
        }
        }
      }

      if (sensorType.hasInternalCalibration()) {
        calibrate(instrument, measurement, sensorType, result, allMeasurements,
          sensorValues, conn);
      }

      return result;
    } catch (DatabaseException e) {
      throw new MeasurementValueCalculatorException(
        "Error getting sensor value details", e);
    }
  }

  private void calibrate(Instrument instrument, Measurement measurement,
    SensorType sensorType, MeasurementValue value,
    DatasetMeasurements allMeasurements,
    SearchableSensorValuesList sensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    if (!value.getCalculatedValue().isNaN()) {

      try {
        CalibrationSet calibrationSet = ExternalStandardDB.getInstance()
          .getMostRecentCalibrations(conn, instrument, measurement.getTime());

        value.setProperty(STANDARDS_COUNT_PROPERTY,
          String.valueOf(calibrationSet.size()));

        // Get the standards closest to the measured value, with their
        // concentrations.
        Map<String, Double> closestStandards = calibrationSet
          .getClosestStandards(sensorType, value.getCalculatedValue());

        if (closestStandards.size() > 0) {
          // For each external standard target, calculate the offset from the
          // external standard at the measurement time.
          //
          // We get the offset at the prior and post measurements of that
          // standard,
          // and then interpolate to get the offset at the measurement time.
          Map<String, Double> standardOffsets = new HashMap<String, Double>();

          for (Map.Entry<String, Double> standard : closestStandards
            .entrySet()) {
            String target = standard.getKey();
            double standardConcentration = standard.getValue();

            List<SensorValue> priorCalibrationValues = getPriorCalibrationValues(
              measurement.getTime(), allMeasurements, target, sensorValues);

            List<SensorValue> postCalibrationValues = getPostCalibrationValues(
              measurement.getTime(), allMeasurements, target, sensorValues);

            LocalDateTime priorTime = null;
            Double priorOffset = null;

            if (priorCalibrationValues.size() > 0) {
              priorTime = SensorValue.getMeanTime(priorCalibrationValues,
                false);
              priorOffset = SensorValue.getMeanValue(priorCalibrationValues)
                - standardConcentration;
              value.addSupportingSensorValues(priorCalibrationValues);
            }

            LocalDateTime postTime = null;
            Double postOffset = null;

            if (postCalibrationValues.size() > 0) {
              postTime = SensorValue.getMeanTime(postCalibrationValues, false);
              postOffset = SensorValue.getMeanValue(postCalibrationValues)
                - standardConcentration;
              value.addSupportingSensorValues(postCalibrationValues);
            }

            Double interpolated = Calculators.interpolate(priorTime,
              priorOffset, postTime, postOffset, measurement.getTime());

            if (null == interpolated) {
              interpolated = Double.NaN;
            }

            standardOffsets.put(target, interpolated);
          }

          // If all the standards are for the concentration (which happens for
          // moisture in gas standards because they're all 100% dry) then we'll
          // only get one offset. But that's fine because the offset in the
          // sensors will measure the same regardless of which standard we're
          // using.
          if (standardOffsets.size() == 1) {
            Double offset = standardOffsets.values().iterator().next();
            value.setCalculatedValue(value.getCalculatedValue() - offset);
          } else {

            // Make a regression of the offsets to calculate the offset at the
            // measurement time
            SimpleRegression regression = new SimpleRegression(true);
            for (String target : standardOffsets.keySet()) {
              regression.addData(
                calibrationSet.getCalibrationValue(target,
                  value.getSensorType().getShortName()),
                standardOffsets.get(target));
            }

            double calibrationOffset = regression
              .predict(value.getCalculatedValue());

            // Now apply the offset to the measured value.
            // TODO #732/#410 Add excessive calibration adjustment check to this
            // method - it will set the flag on the MeasurementValue. Needs to
            // be
            // defined per sensor type.
            value.setCalculatedValue(
              value.getCalculatedValue() - calibrationOffset);
          }
        }
      } catch (Exception e) {
        throw new MeasurementValueCalculatorException(
          "Error while calculating calibrated value", e);
      }
    }
  }

  /**
   * Get the closest GOOD calibration value before a given time.
   *
   * @param startTime
   *          The start time.
   * @param measurements
   *          The list of measurements from which to select are value. Assumed
   *          to be correct for the desired calibration target.
   * @param sensorValues
   *          The list of sensor values for the desired data column.
   * @return The SensorValue for the calibration measurement.
   */
  private List<SensorValue> getPriorCalibrationValues(LocalDateTime startTime,
    DatasetMeasurements allMeasurements, String target,
    SearchableSensorValuesList sensorValues) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    // Work out where we're starting in the list of measurements for the target
    // standard.
    // The result of this search should be negative because our base measurement
    // will not be in the calibration run type. But we handle the case where
    // it's positive just in case. (See documentation for binarySearch.)

    List<Measurement> targetMeasurements = allMeasurements
      .getMeasurements(Measurement.GENERIC_RUN_TYPE_VARIABLE, target);

    int startPoint = Collections.binarySearch(targetMeasurements,
      Measurement.dummyTimeMeasurement(startTime), Measurement.TIME_COMPARATOR);

    if (startPoint >= 0) {
      startPoint--;
    } else {
      startPoint = (startPoint * -1) - 2;
    }

    // If the start point is still negative, then there will be no prior
    // calibrations
    if (startPoint >= 0) {
      int searchPoint = startPoint;
      while (searchPoint >= 0) {
        LocalDateTime testTime = targetMeasurements.get(searchPoint).getTime();
        SensorValue testValue = sensorValues.get(testTime);
        if (null != testValue && testValue.getUserQCFlag().isGood()) {
          result.add(testValue);
          break;
        }

        searchPoint--;
      }

      // Now we've found the closest measurement, find others from the same run
      // type sequence.
      TreeSet<Measurement> runMeasurements = allMeasurements
        .getMeasurementsInSameRun(Measurement.GENERIC_RUN_TYPE_VARIABLE,
          targetMeasurements.get(startPoint));

      for (Measurement measurement : runMeasurements) {
        SensorValue valueCandidate = sensorValues.get(measurement.getTime());
        if (null != valueCandidate && valueCandidate.getUserQCFlag().isGood()) {
          result.add(valueCandidate);
        }
      }
    }

    return result;
  }

  /**
   * Get the closest GOOD calibration value after a given time.
   *
   * @param startTime
   *          The start time.
   * @param measurements
   *          The list of measurements from which to select are value. Assumed
   *          to be correct for the desired calibration target.
   * @param sensorValues
   *          The list of sensor values for the desired data column.
   * @return The SensorValue for the calibration measurement.
   */
  private List<SensorValue> getPostCalibrationValues(LocalDateTime startTime,
    DatasetMeasurements allMeasurements, String target,
    SearchableSensorValuesList sensorValues) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    // Work out where we're starting in the list of measurements for the target
    // standard.
    // The result of this search should be negative because our base measurement
    // will not be in the calibration run type. But we handle the case where
    // it's positive just in case. (See documentation for binarySearch.)

    List<Measurement> targetMeasurements = allMeasurements
      .getMeasurements(Measurement.GENERIC_RUN_TYPE_VARIABLE, target);

    int startPoint = Collections.binarySearch(targetMeasurements,
      Measurement.dummyTimeMeasurement(startTime), Measurement.TIME_COMPARATOR);

    if (startPoint >= 0) {
      startPoint++;
    } else {
      startPoint = (startPoint * -1) - 1;
    }

    if (startPoint < targetMeasurements.size()) {
      int searchPoint = startPoint;
      while (searchPoint >= 0 && searchPoint < targetMeasurements.size()) {
        LocalDateTime testTime = targetMeasurements.get(searchPoint).getTime();
        SensorValue testValue = sensorValues.get(testTime);
        if (null != testValue && testValue.getUserQCFlag().isGood()) {
          result.add(testValue);
          break;
        }

        searchPoint--;
      }

      // Now we've found the closest measurement, find others from the same run
      // type sequence.
      TreeSet<Measurement> runMeasurements = allMeasurements
        .getMeasurementsInSameRun(Measurement.GENERIC_RUN_TYPE_VARIABLE,
          targetMeasurements.get(startPoint));

      for (Measurement measurement : runMeasurements) {
        SensorValue valueCandidate = sensorValues.get(measurement.getTime());
        if (null != valueCandidate && valueCandidate.getUserQCFlag().isGood()) {
          result.add(valueCandidate);
        }
      }
    }

    return result;
  }
}
