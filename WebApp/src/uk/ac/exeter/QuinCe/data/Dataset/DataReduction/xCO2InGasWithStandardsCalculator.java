package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class xCO2InGasWithStandardsCalculator extends ValueCalculator {

  private final SensorType xco2SensorType;

  private final SensorType xh2oSensorType;

  private ArrayList<Measurement> timeOrderedMeasurements;

  private ArrayList<LocalDateTime> measurementTimes;

  public xCO2InGasWithStandardsCalculator() throws SensorTypeNotFoundException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
    this.xco2SensorType = sensorConfig.getSensorType("xCO₂ (with standards)");
    this.xh2oSensorType = sensorConfig.getSensorType("xH₂O (with standards)");
  }

  @Override
  public Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer, Connection conn)
    throws Exception {

    Double result;

    CalibrationSet calibrationSet = ExternalStandardDB.getInstance()
      .getMostRecentCalibrations(conn,
        measurementValues.getInstrument().getDatabaseId(),
        measurementValues.getMeasurement().getTime());

    LinkedHashSet<MeasurementValue> xco2MeasurementValues = measurementValues
      .get(xco2SensorType);

    if (dryingRequired(measurementValues.getInstrument())) {

      LinkedHashSet<MeasurementValue> xh2oMeasurementValues = measurementValues
        .get(xh2oSensorType);
      if (xh2oMeasurementValues.size() != xco2MeasurementValues.size()) {
        throw new ValueCalculatorException(xh2oSensorType,
          "Mismatched size of CO2 and xH2O values");
      }

      result = dryingCalculation(measurementValues.getMeasurement().getTime(),
        measurementValues.getInstrument(), allMeasurements, allSensorValues,
        measurementValues, xco2MeasurementValues, xh2oMeasurementValues,
        calibrationSet, reducer, conn);
    } else {
      result = nonDryingCalculation(
        measurementValues.getMeasurement().getTime(),
        measurementValues.getInstrument(), allMeasurements, allSensorValues,
        measurementValues, xco2MeasurementValues, calibrationSet, reducer,
        conn);
    }

    return result;
  }

  private boolean dryingRequired(Instrument instrument)
    throws ValueCalculatorException {

    List<SensorAssignment> co2Assignments = instrument.getSensorAssignments()
      .get(xco2SensorType);

    // TODO We assume there's only one CO2 sensor. Handle more.
    if (co2Assignments.size() > 1) {
      throw new ValueCalculatorException(xco2SensorType,
        "Cannot handle multiple CO2 sensors yet!");
    }

    SensorAssignment assignment = co2Assignments.get(0);

    return assignment.getDependsQuestionAnswer();
  }

  private Double dryingCalculation(LocalDateTime measurementTime,
    Instrument instrument, Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, MeasurementValues measurementValues,
    LinkedHashSet<MeasurementValue> xco2Values,
    LinkedHashSet<MeasurementValue> xh2oValues, CalibrationSet calibrationSet,
    DataReducer reducer, Connection conn) throws ValueCalculatorException {

    MeanCalculator mean = new MeanCalculator();

    Iterator<MeasurementValue> xco2Iterator = xco2Values.iterator();
    Iterator<MeasurementValue> xh2oIterator = xh2oValues.iterator();

    while (xco2Iterator.hasNext()) {

      MeasurementValue xh2oValue = xh2oIterator.next();
      MeasurementValue xco2Value = xco2Iterator.next();

      SensorValue xh2oPrior = null;
      Double xh2oPriorCalibratedValue = null;
      SensorValue xco2Prior = null;
      Double xco2PriorCalibratedValue = null;

      SensorValue xh2oPost = null;
      Double xh2oPostCalibratedValue = null;
      SensorValue xco2Post = null;
      Double xco2PostCalibratedValue = null;

      if (xh2oValue.hasPrior() && xco2Value.hasPrior()) {
        xh2oPrior = allSensorValues.getById(xh2oValue.getPrior());
        xh2oPriorCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xh2oSensorType,
          xh2oPrior.getTime(), xh2oPrior.getDoubleValue(), calibrationSet,
          false, reducer, conn);

        xco2Prior = allSensorValues.getById(xco2Value.getPrior());
        double xco2PriorValue = xco2Prior.getDoubleValue();
        double xco2PriorDriedValue = dry(xco2PriorValue,
          xh2oPriorCalibratedValue);

        xco2PriorCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xco2SensorType,
          xco2Prior.getTime(), xco2PriorDriedValue, calibrationSet, true,
          reducer, conn);
      }

      if (xh2oValue.hasPost() && xco2Value.hasPost()) {
        xh2oPost = allSensorValues.getById(xh2oValue.getPost());
        xh2oPostCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xh2oSensorType,
          xh2oPost.getTime(), xh2oPost.getDoubleValue(), calibrationSet, false,
          reducer, conn);

        xco2Post = allSensorValues.getById(xco2Value.getPost());
        double xco2PostValue = xco2Post.getDoubleValue();
        double xco2PostDriedValue = dry(xco2PostValue, xh2oPostCalibratedValue);

        xco2PostCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xco2SensorType,
          xco2Post.getTime(), xco2PostDriedValue, calibrationSet, true, reducer,
          conn);
      }

      if (null != xco2PriorCalibratedValue && null != xco2PostCalibratedValue) {
        mean.add(interpolate(xco2Prior.getTime(), xco2PriorCalibratedValue,
          xco2Post.getTime(), xco2PostCalibratedValue, measurementTime));
      } else if (null != xco2PriorCalibratedValue) {
        mean.add(xco2PriorCalibratedValue);
      } else if (null != xco2PostCalibratedValue) {
        mean.add(xco2PostCalibratedValue);
      }
    }

    return mean.mean();
  }

  private Double nonDryingCalculation(LocalDateTime measurementTime,
    Instrument instrument, Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, MeasurementValues measurementValues,
    LinkedHashSet<MeasurementValue> xco2Values, CalibrationSet calibrationSet,
    DataReducer reducer, Connection conn) throws ValueCalculatorException {

    MeanCalculator mean = new MeanCalculator();

    for (MeasurementValue measurementValue : xco2Values) {

      SensorValue prior = null;
      Double priorCalibratedValue = null;
      SensorValue post = null;
      Double postCalibratedValue = null;

      if (measurementValue.hasPrior()) {
        prior = allSensorValues.getById(measurementValue.getPrior());
        priorCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xco2SensorType, prior,
          calibrationSet, true, reducer, conn);
      }

      if (measurementValue.hasPost()) {
        post = allSensorValues.getById(measurementValue.getPost());
        postCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xco2SensorType, post,
          calibrationSet, true, reducer, conn);
      }

      if (null != priorCalibratedValue && null != postCalibratedValue) {
        mean.add(interpolate(prior.getTime(), priorCalibratedValue,
          post.getTime(), postCalibratedValue, measurementTime));
      } else if (null != priorCalibratedValue) {
        mean.add(priorCalibratedValue);
      } else if (null != postCalibratedValue) {
        mean.add(postCalibratedValue);
      }
    }

    return mean.mean();
  }

  /**
   * Calculate dried CO2 using a moisture measurement
   *
   * @param co2
   *          The measured CO2 value
   * @param xH2O
   *          The moisture value
   * @return The 'dry' CO2 value
   */
  private double dry(Double co2, Double xH2O) {
    return co2 / (1.0 - (xH2O / 1000));
  }

  private double calibrate(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, MeasurementValues measurementValues,
    SensorType sensorType, SensorValue sensorValue,
    CalibrationSet calibrationSet, boolean ignoreZero, DataReducer reducer,
    Connection conn) throws ValueCalculatorException {

    return calibrate(instrument, allMeasurements, allSensorValues,
      measurementValues, sensorType, sensorValue.getTime(),
      sensorValue.getDoubleValue(), calibrationSet, ignoreZero, reducer, conn);
  }

  private double calibrate(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, MeasurementValues measurementValues,
    SensorType sensorType, LocalDateTime time, double value,
    CalibrationSet calibrationSet, boolean ignoreZero, DataReducer reducer,
    Connection conn) throws ValueCalculatorException {

    // TODO Add excessive calibration adjustment check to this method -
    // it will set the flag on the CalculationValue

    Double calibratedValue;

    // Combine all the measurements into a single time stream if we haven't
    // already done it
    if (null == timeOrderedMeasurements) {
      makeTimeOrderedMeasurements(allMeasurements);
    }

    try {
      // For each external standard target, calculate the offset from the
      // external
      // standard at the record date
      Map<String, Double> standardMeasurements = new HashMap<String, Double>();
      for (String target : calibrationSet.getTargets().keySet()) {

        double concentration = calibrationSet.getCalibrationValue(target,
          sensorType.getName());

        if (concentration > 0.0 || !ignoreZero) {

          CalibrationTimeValue priorValue = getPriorCalibrationValue(instrument,
            allMeasurements, allSensorValues, target, time, sensorType, reducer,
            conn);
          CalibrationTimeValue postValue = getPostCalibrationValue(instrument,
            allMeasurements, allSensorValues, target, time, sensorType, reducer,
            conn);

          standardMeasurements.put(target,
            interpolate(priorValue.time, priorValue.value, postValue.time,
              postValue.value, measurementValues.getMeasurement().getTime()));
        }
      }

      // Make a regression of the offsets to calculate the offset at the
      // measured concentration
      SimpleRegression regression = new SimpleRegression(true);
      for (String target : standardMeasurements.keySet()) {
        regression.addData(standardMeasurements.get(target),
          calibrationSet.getCalibrationValue(target, sensorType.getName()));
      }

      calibratedValue = regression.predict(value);
    } catch (Exception e) {
      throw new ValueCalculatorException(sensorType,
        "Error while calculating calibrated value", e);
    }

    return calibratedValue;
  }

  private CalibrationTimeValue getPriorCalibrationValue(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, String target, LocalDateTime start,
    SensorType sensorType, DataReducer reducer, Connection conn)
    throws Exception {

    MeanCalculator calibrationValues = new MeanCalculator();
    LocalDateTime priorTime = null;

    int searchIndex = Collections.binarySearch(measurementTimes, start);

    // Search for the closest measurement before this with the target run type
    // and a GOOD value for the sensor type
    boolean foundTarget = false;

    while (!foundTarget && searchIndex > 0) {
      searchIndex--;

      Measurement measurement = timeOrderedMeasurements.get(searchIndex);

      // Check the run type
      if (measurement.getRunType().equals(target)) {

        // Now get the value for the sensor type
        Double sensorValue = getGoodSensorValue(instrument, allSensorValues,
          sensorType, measurement.getTime());

        if (!sensorValue.isNaN()) {
          calibrationValues.add(sensorValue);
          priorTime = timeOrderedMeasurements.get(searchIndex).getTime();
          foundTarget = true;
        }
      }
    }

    if (foundTarget) {
      // Now keep searching until the run type changes
      boolean runTypeChanged = false;
      while (!runTypeChanged && searchIndex > 0) {
        searchIndex--;

        Measurement measurement = timeOrderedMeasurements.get(searchIndex);

        // Check the run type
        if (!measurement.getRunType().equals(target)) {
          runTypeChanged = true;
        } else {

          // Get the value for the sensor type
          Double sensorValue = getGoodSensorValue(instrument, allSensorValues,
            sensorType, measurement.getTime());

          if (!sensorValue.isNaN()) {
            calibrationValues.add(sensorValue);
          }
        }
      }
    }

    return new CalibrationTimeValue(priorTime, calibrationValues.mean());
  }

  private CalibrationTimeValue getPostCalibrationValue(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, String target, LocalDateTime start,
    SensorType sensorType, DataReducer reducer, Connection conn)
    throws Exception {

    MeanCalculator calibrationValues = new MeanCalculator();
    LocalDateTime postTime = null;

    int searchIndex = Collections.binarySearch(measurementTimes, start);

    // Search for the closest measurement before this with the target run type
    // and a GOOD value for the sensor type
    boolean foundTarget = false;

    if (searchIndex >= 0) {
      while (!foundTarget && searchIndex < timeOrderedMeasurements.size() - 1) {
        searchIndex++;

        Measurement measurement = timeOrderedMeasurements.get(searchIndex);

        // Check the run type
        if (measurement.getRunType().equals(target)) {

          // Now get the value for the sensor type
          Double sensorValue = getGoodSensorValue(instrument, allSensorValues,
            sensorType, measurement.getTime());

          if (!sensorValue.isNaN()) {
            calibrationValues.add(sensorValue);
            postTime = timeOrderedMeasurements.get(searchIndex).getTime();
            foundTarget = true;
          }
        }
      }
    }

    if (foundTarget) {
      // Now keep searching until the run type changes
      boolean runTypeChanged = false;
      while (!runTypeChanged
        && searchIndex < timeOrderedMeasurements.size() - 1) {
        searchIndex++;

        Measurement measurement = timeOrderedMeasurements.get(searchIndex);

        // Check the run type
        if (!measurement.getRunType().equals(target)) {
          runTypeChanged = true;
        } else {

          // Get the value for the sensor type
          Double sensorValue = getGoodSensorValue(instrument, allSensorValues,
            sensorType, measurement.getTime());
          if (!sensorValue.isNaN()) {
            calibrationValues.add(sensorValue);
          }
        }
      }
    }

    return new CalibrationTimeValue(postTime, calibrationValues.mean());
  }

  private Double getGoodSensorValue(Instrument instrument,
    DatasetSensorValues allSensorValues, SensorType sensorType,
    LocalDateTime time) {

    // #1128 This gets all sensors for the given sensor type.
    // Should only get the value for the sensor that's being used in the main
    // measurement

    MeanCalculator mean = new MeanCalculator();

    for (long columnId : instrument.getSensorAssignments()
      .getColumnIds(sensorType)) {

      SensorValue value = allSensorValues.getSensorValue(time, columnId);
      if (null != value && !value.isNaN()
        && (value.getUserQCFlag().equals(Flag.GOOD)
          || value.getUserQCFlag().equals(Flag.ASSUMED_GOOD))) {
        mean.add(value.getDoubleValue());
      }

    }

    return mean.mean();
  }

  private void makeTimeOrderedMeasurements(
    Map<String, ArrayList<Measurement>> allMeasurements) {

    timeOrderedMeasurements = new ArrayList<Measurement>();
    allMeasurements.values().forEach(timeOrderedMeasurements::addAll);

    Collections.sort(timeOrderedMeasurements);

    measurementTimes = new ArrayList<LocalDateTime>(
      timeOrderedMeasurements.size());
    timeOrderedMeasurements.forEach(m -> measurementTimes.add(m.getTime()));
  }

  private class CalibrationTimeValue {
    final LocalDateTime time;
    final Double value;

    CalibrationTimeValue(LocalDateTime time, Double value) {
      this.time = time;
      this.value = value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
  }
}
