package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class xCO2InGasWithStandardsCalculator extends ValueCalculator {

  private SensorType xco2SensorType;

  private SensorType xh2oSensorType;

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
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception {

    Double result;

    CalibrationSet calibrationSet = ExternalStandardDB.getInstance()
      .getMostRecentCalibrations(conn,
        measurementValues.getInstrument().getDatabaseId(),
        measurementValues.getMeasurement().getTime());

    Map<Long, SensorValue> sensorValues = getSensorValues(measurementValues,
      xco2SensorType, conn);

    ArrayList<MeasurementValue> xco2Values = measurementValues
      .get(xco2SensorType);

    if (dryingRequired(measurementValues.getInstrument())) {

      ArrayList<MeasurementValue> xh2oValues = measurementValues
        .get(xh2oSensorType);
      if (xh2oValues.size() != xco2Values.size()) {
        throw new ValueCalculatorException(xh2oSensorType,
          "Mismatched size of CO2 and xH2O values");
      }

      result = dryingCalculation(measurementValues.getMeasurement().getTime(),
        measurementValues.getInstrument(), allMeasurements, measurementValues,
        xco2Values, xh2oValues, sensorValues, calibrationSet, conn);
    } else {
      result = nonDryingCalculation(
        measurementValues.getMeasurement().getTime(),
        measurementValues.getInstrument(), allMeasurements, measurementValues,
        xco2Values, sensorValues, calibrationSet, conn);
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
    MeasurementValues measurementValues, ArrayList<MeasurementValue> xco2Values,
    ArrayList<MeasurementValue> xh2oValues, Map<Long, SensorValue> sensorValues,
    CalibrationSet calibrationSet, Connection conn)
    throws ValueCalculatorException {

    MutableDouble sum = new MutableDouble(0);
    MutableInt count = new MutableInt(0);

    for (int i = 0; i < xco2Values.size(); i++) {

      MeasurementValue xh2oValue = xh2oValues.get(i);
      MeasurementValue xco2Value = xco2Values.get(i);

      SensorValue xh2oPrior = sensorValues.get(xh2oValue.getPrior());
      SensorValue xco2Prior = sensorValues.get(xco2Value.getPrior());

      double xh2oPriorCalibratedValue = calibrate(instrument, allMeasurements,
        measurementValues, xh2oSensorType, xh2oPrior.getTime(),
        xh2oPrior.getDoubleValue(), calibrationSet, conn);

      double xco2PriorValue = xco2Prior.getDoubleValue();
      double xco2PriorDriedValue = dry(xco2PriorValue,
        xh2oPriorCalibratedValue);

      double xco2PriorCalibratedValue = calibrate(instrument, allMeasurements,
        measurementValues, xco2SensorType, xco2Prior.getTime(),
        xco2PriorDriedValue, calibrationSet, conn);

      if (!xco2Value.hasPost()) {
        sum.add(xco2PriorCalibratedValue);
      } else {
        SensorValue xh2oPost = sensorValues.get(xh2oValue.getPost());
        SensorValue xco2Post = sensorValues.get(xco2Value.getPost());

        double xh2oPostCalibratedValue = calibrate(instrument, allMeasurements,
          measurementValues, xh2oSensorType, xh2oPost.getTime(),
          xh2oPost.getDoubleValue(), calibrationSet, conn);

        double xco2PostValue = xco2Post.getDoubleValue();
        double xco2PostDriedValue = dry(xco2PostValue, xh2oPostCalibratedValue);

        double xco2PostCalibratedValue = calibrate(instrument, allMeasurements,
          measurementValues, xco2SensorType, xco2Post.getTime(),
          xco2PostDriedValue, calibrationSet, conn);

        sum.add(interpolate(xco2Prior.getTime(), xco2PriorCalibratedValue,
          xco2Post.getTime(), xco2PostCalibratedValue, measurementTime));
      }

      count.increment();
    }

    return mean(sum, count);
  }

  private Double nonDryingCalculation(LocalDateTime measurementTime,
    Instrument instrument, Map<String, ArrayList<Measurement>> allMeasurements,
    MeasurementValues measurementValues, ArrayList<MeasurementValue> xco2Values,
    Map<Long, SensorValue> sensorValues, CalibrationSet calibrationSet,
    Connection conn) throws ValueCalculatorException {

    MutableDouble sum = new MutableDouble(0);
    MutableInt count = new MutableInt(0);

    for (MeasurementValue measurementValue : xco2Values) {

      SensorValue prior = sensorValues.get(measurementValue.getPrior());

      double priorCalibratedValue = calibrate(instrument, allMeasurements,
        measurementValues, xco2SensorType, prior, calibrationSet, conn);

      if (!measurementValue.hasPost()) {
        sum.add(priorCalibratedValue);
      } else {
        SensorValue post = sensorValues.get(measurementValue.getPost());

        double postCalibratedValue = calibrate(instrument, allMeasurements,
          measurementValues, xco2SensorType, post, calibrationSet, conn);

        sum.add(interpolate(prior.getTime(), priorCalibratedValue,
          post.getTime(), postCalibratedValue, measurementTime));
      }

      count.increment();
    }

    return mean(sum, count);
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
    MeasurementValues measurementValues, SensorType sensorType,
    SensorValue sensorValue, CalibrationSet calibrationSet, Connection conn)
    throws ValueCalculatorException {

    return calibrate(instrument, allMeasurements, measurementValues, sensorType,
      sensorValue.getTime(), sensorValue.getDoubleValue(), calibrationSet,
      conn);
  }

  private double calibrate(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    MeasurementValues measurementValues, SensorType sensorType,
    LocalDateTime time, double value, CalibrationSet calibrationSet,
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

        if (concentration > 0.0) {

          CalibrationTimeValue priorValue = getPriorCalibrationValue(instrument,
            allMeasurements, target, time, sensorType, conn);
          CalibrationTimeValue postValue = getPostCalibrationValue(instrument,
            allMeasurements, target, time, sensorType, conn);

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
    } catch (

    Exception e) {
      throw new ValueCalculatorException(sensorType,
        "Error while calculating calibrated value", e);
    }

    return calibratedValue;
  }

  private CalibrationTimeValue getPriorCalibrationValue(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements, String target,
    LocalDateTime start, SensorType sensorType, Connection conn)
    throws Exception {

    int rangeStart = -1;
    int rangeEnd = -1;

    int searchIndex = Collections.binarySearch(measurementTimes, start);

    // Search for the closest measurement before this with the target run type
    boolean foundTarget = false;
    while (!foundTarget && searchIndex > 0) {
      searchIndex--;
      if (timeOrderedMeasurements.get(searchIndex).getRunType()
        .equals(target)) {
        rangeEnd = searchIndex + 1; // Sublist call below is exclusive
        foundTarget = true;
      }
    }

    if (foundTarget) {
      // Now keep searching until the run type changes
      boolean runTypeChanged = false;
      while (!runTypeChanged && searchIndex > 0) {
        searchIndex--;
        if (!timeOrderedMeasurements.get(searchIndex).getRunType()
          .equals(target)) {
          runTypeChanged = true;

          // This record is different, so the next is the true range start
          rangeStart = searchIndex + 1;
        }
      }

      if (!runTypeChanged) {
        rangeStart = 0;
      }
    }

    List<Measurement> priorRunMeasurements;
    if (rangeStart == -1 || rangeEnd == -1) {
      priorRunMeasurements = new ArrayList<Measurement>(0);
    } else {
      priorRunMeasurements = timeOrderedMeasurements.subList(rangeStart,
        rangeEnd);
    }

    return calculateCalibrationValue(sensorType, priorRunMeasurements,
      instrument, allMeasurements, conn);
  }

  private CalibrationTimeValue getPostCalibrationValue(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements, String target,
    LocalDateTime start, SensorType sensorType, Connection conn)
    throws Exception {

    int rangeStart = -1;
    int rangeEnd = -1;

    int searchIndex = Collections.binarySearch(measurementTimes, start);

    // Search for the closest measurement before this with the target run type
    boolean foundTarget = false;
    while (!foundTarget && searchIndex < timeOrderedMeasurements.size()) {
      searchIndex++;
      if (timeOrderedMeasurements.get(searchIndex).getRunType()
        .equals(target)) {
        rangeEnd = searchIndex;
        foundTarget = true;
      }
    }

    if (foundTarget) {
      // Now keep searching until the run type changes
      boolean runTypeChanged = false;
      while (!runTypeChanged && searchIndex < timeOrderedMeasurements.size()) {
        searchIndex++;
        if (!timeOrderedMeasurements.get(searchIndex).getRunType()
          .equals(target)) {
          runTypeChanged = true;

          // This record is different, so the previous is the true range end.
          // But sublist ends are exclusive, so actually we use the current
          // index. I hate this.
          rangeEnd = searchIndex;
        }
      }

      if (!runTypeChanged) {
        rangeStart = timeOrderedMeasurements.size();
      }
    }

    List<Measurement> postRunMeasurements;
    if (rangeStart == -1 || rangeEnd == -1) {
      postRunMeasurements = new ArrayList<Measurement>(0);
    } else {
      postRunMeasurements = timeOrderedMeasurements.subList(rangeStart,
        rangeEnd);
    }

    return calculateCalibrationValue(sensorType, postRunMeasurements,
      instrument, allMeasurements, conn);
  }

  private CalibrationTimeValue calculateCalibrationValue(SensorType sensorType,
    List<Measurement> runMeasurements, Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception {
    DefaultValueCalculator calculator = new DefaultValueCalculator(sensorType);

    MutableDouble sum = new MutableDouble(0);
    MutableInt count = new MutableInt(0);

    for (Measurement runMeasurement : runMeasurements) {
      MeasurementValues runMeasurementValues = new MeasurementValues(instrument,
        runMeasurement);
      sum.add(
        calculator.calculateValue(runMeasurementValues, allMeasurements, conn));
    }

    return new CalibrationTimeValue(
      runMeasurements.get(runMeasurements.size() - 1).getTime(),
      mean(sum, count));
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
    LocalDateTime time;
    Double value;

    CalibrationTimeValue(LocalDateTime time, Double value) {
      this.time = time;
      this.value = value;
    }
  }
}
