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
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
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

      SensorValue xh2oPrior = allSensorValues.getById(xh2oValue.getPrior());
      SensorValue xco2Prior = allSensorValues.getById(xco2Value.getPrior());

      double xh2oPriorCalibratedValue = calibrate(instrument, allMeasurements,
        allSensorValues, measurementValues, xh2oSensorType, xh2oPrior.getTime(),
        xh2oPrior.getDoubleValue(), calibrationSet, false, reducer, conn);

      double xco2PriorValue = xco2Prior.getDoubleValue();
      double xco2PriorDriedValue = dry(xco2PriorValue,
        xh2oPriorCalibratedValue);

      double xco2PriorCalibratedValue = calibrate(instrument, allMeasurements,
        allSensorValues, measurementValues, xco2SensorType, xco2Prior.getTime(),
        xco2PriorDriedValue, calibrationSet, true, reducer, conn);

      if (!xco2Value.hasPost()) {
        mean.add(xco2PriorCalibratedValue);
      } else {
        SensorValue xh2oPost = allSensorValues.getById(xh2oValue.getPost());
        SensorValue xco2Post = allSensorValues.getById(xco2Value.getPost());

        double xh2oPostCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xh2oSensorType,
          xh2oPost.getTime(), xh2oPost.getDoubleValue(), calibrationSet, false,
          reducer, conn);

        double xco2PostValue = xco2Post.getDoubleValue();
        double xco2PostDriedValue = dry(xco2PostValue, xh2oPostCalibratedValue);

        double xco2PostCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xco2SensorType,
          xco2Post.getTime(), xco2PostDriedValue, calibrationSet, true, reducer,
          conn);

        mean.add(interpolate(xco2Prior.getTime(), xco2PriorCalibratedValue,
          xco2Post.getTime(), xco2PostCalibratedValue, measurementTime));
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

      SensorValue prior = allSensorValues.getById(measurementValue.getPrior());

      double priorCalibratedValue = calibrate(instrument, allMeasurements,
        allSensorValues, measurementValues, xco2SensorType, prior,
        calibrationSet, true, reducer, conn);

      if (!measurementValue.hasPost()) {
        mean.add(priorCalibratedValue);
      } else {
        SensorValue post = allSensorValues.getById(measurementValue.getPost());

        double postCalibratedValue = calibrate(instrument, allMeasurements,
          allSensorValues, measurementValues, xco2SensorType, post,
          calibrationSet, true, reducer, conn);

        mean.add(interpolate(prior.getTime(), priorCalibratedValue,
          post.getTime(), postCalibratedValue, measurementTime));
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

          String sensorValueSearchIdPrefix = generateSearchId(allSensorValues);

          CalibrationTimeValue priorValue = getPriorCalibrationValue(instrument,
            allMeasurements, allSensorValues, target, time, sensorType, reducer,
            sensorValueSearchIdPrefix, conn);
          CalibrationTimeValue postValue = getPostCalibrationValue(instrument,
            allMeasurements, allSensorValues, target, time, sensorType, reducer,
            sensorValueSearchIdPrefix, conn);

          // TODO This shouldn't be needed - see GitHub issue #1653
          allSensorValues.destroySearchesWithPrefix(sensorValueSearchIdPrefix);

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

  private String generateSearchId(DatasetSensorValues allSensorValues) {
    boolean ok = false;
    String idBase = "xCO2InGasWithStandardsCalculator";

    String result = null;

    while (!ok) {
      int random = ThreadLocalRandom.current().nextInt(0, 5000 + 1);
      if (!allSensorValues.containsSearchWithPrefix(idBase + random)) {
        result = idBase + random;
        ok = true;
      }
    }

    return result;
  }

  private CalibrationTimeValue getPriorCalibrationValue(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, String target, LocalDateTime start,
    SensorType sensorType, DataReducer reducer, String searchIdPrefix,
    Connection conn) throws Exception {

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
      instrument, allMeasurements, allSensorValues, reducer, searchIdPrefix,
      conn);
  }

  private CalibrationTimeValue getPostCalibrationValue(Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, String target, LocalDateTime start,
    SensorType sensorType, DataReducer reducer, String searchIdPrefix,
    Connection conn) throws Exception {

    int rangeStart = -1;
    int rangeEnd = -1;

    int searchIndex = Collections.binarySearch(measurementTimes, start);

    // Search for the closest measurement after this with the target run type
    boolean foundTarget = false;

    if (searchIndex >= 0) {
      searchIndex++;
      while (!foundTarget && searchIndex < timeOrderedMeasurements.size()) {
        if (timeOrderedMeasurements.get(searchIndex).getRunType()
          .equals(target)) {
          rangeStart = searchIndex;
          rangeEnd = searchIndex;
          foundTarget = true;
        }
        searchIndex++;
      }
    }

    if (foundTarget) {
      // Now keep searching until the run type changes
      boolean runTypeChanged = false;
      while (!runTypeChanged
        && searchIndex < timeOrderedMeasurements.size() - 1) {
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
    if (rangeStart < 0 || rangeEnd < 0 || rangeStart >= rangeEnd) {
      postRunMeasurements = new ArrayList<Measurement>(0);
    } else {
      postRunMeasurements = timeOrderedMeasurements.subList(rangeStart,
        rangeEnd);
    }

    return calculateCalibrationValue(sensorType, postRunMeasurements,
      instrument, allMeasurements, allSensorValues, reducer, searchIdPrefix,
      conn);
  }

  private CalibrationTimeValue calculateCalibrationValue(SensorType sensorType,
    List<Measurement> runMeasurements, Instrument instrument,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer,
    String searchIdPrefix, Connection conn) throws Exception {

    DefaultValueCalculator calculator = new DefaultValueCalculator(sensorType);

    CalibrationTimeValue result = null;

    if (runMeasurements.size() == 0) {
      result = new CalibrationTimeValue();
    } else {
      MeanCalculator mean = new MeanCalculator();

      for (Measurement runMeasurement : runMeasurements) {
        MeasurementValues runMeasurementValues = new MeasurementValues(
          instrument, runMeasurement, searchIdPrefix);

        runMeasurementValues.loadSensorValues(allSensorValues, sensorType);

        mean.add(calculator.calculateValue(runMeasurementValues,
          allMeasurements, allSensorValues, reducer, conn));
      }

      result = new CalibrationTimeValue(
        runMeasurements.get(runMeasurements.size() - 1).getTime(), mean.mean());
    }

    return result;
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

    CalibrationTimeValue() {
      this.time = null;
      this.value = null;
    }
  }
}
