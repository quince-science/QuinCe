package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.DateColumnGroupedSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A DataReducer will perform all data reduction calculations
 * for a given variable. The output from the data reduction is
 * an instance of the DataReductionRecord class
 *
 * @author Steve Jones
 *
 */
public abstract class DataReducer {

  /**
   * All the measurements from the current data set
   */
  protected List<Measurement> allMeasurements;

  /**
   * A local copy of the complete set of sensor values for the current data set
   */
  protected DateColumnGroupedSensorValues groupedSensorValues;

  /**
   * The internal calibrations for the current data set
   */
  protected CalibrationSet calibrationSet;

  /**
   * Cache of searched record times prior to starting point
   */
  private HashMap<String, LocalDateTime> previousSearchTimes;

  /**
   * Cache of searched record times after starting point
   */
  private HashMap<String, LocalDateTime> nextSearchTimes;

  public DataReducer(List<Measurement> allMeasurements,
      DateColumnGroupedSensorValues groupedSensorValues,
      CalibrationSet calibrationSet) {

    this.allMeasurements = allMeasurements;
    this.groupedSensorValues = groupedSensorValues;
    this.calibrationSet = calibrationSet;

    previousSearchTimes = new HashMap<String, LocalDateTime>();
    nextSearchTimes = new HashMap<String, LocalDateTime>();
  }

  /**
   * Perform the data reduction and set up the QC flags
   * @param instrument The instrument that took the measurement
   * @param measurement The measurement
   * @param sensorValues The measurement's sensor values
   * @param allMeasurements All measurements for the data set
   * @return The data reduction result
   */
  public DataReductionRecord performDataReduction(Instrument instrument,
      Measurement measurement, Map<SensorType, CalculationValue> sensorValues) throws Exception {

    DataReductionRecord record = new DataReductionRecord(measurement);
    doCalculation(instrument, measurement, sensorValues, record);

    List<SensorType> missingParameters = getMissingParameters(instrument.getSensorAssignments(), sensorValues);
    if (missingParameters.size() > 0) {
      makeMissingParameterRecord(record, missingParameters);
    } else {
      doCalculation(instrument, measurement, sensorValues, record);
    }

    applyQCFlags(instrument.getSensorAssignments(), measurement.getVariable(), sensorValues, record);

    return record;
  }

  /**
   * Perform the data reduction calculations
   * @param instrument The instrument that took the measurement
   * @param measurement The measurement
   * @param sensorValues The measurement's sensor values
   * @param allMeasurements All measurements for the data set
   * @param record The data reduction result
   */
  protected abstract void doCalculation(Instrument instrument,
      Measurement measurement, Map<SensorType, CalculationValue> sensorValues,
      DataReductionRecord record)
      throws Exception;

  /**
   * Set the QC flag on a record based on the flags of the sensor values.
   * The flag logic is in {@link DataReductionRecord}.
   * @param instrumentAssignments The sensor assignments for the instrument
   * @param variable The variable being processed
   * @param sensorValues The sensor values
   * @param record The target record
   * @throws SensorTypeNotFoundException If the sensor config is invalid
   * @throws DataReductionException If the QC cannot be applied
   * @throws SensorConfigurationException If the sensor config is invalid
   */
  private void applyQCFlags(SensorAssignments instrumentAssignments, InstrumentVariable variable, Map<SensorType, CalculationValue> sensorValues, DataReductionRecord record) throws SensorTypeNotFoundException, DataReductionException, SensorConfigurationException {
    for (SensorType sensorType : getRequiredSensorTypes(instrumentAssignments)) {
      CalculationValue value = sensorValues.get(sensorType);
      Flag cascadeFlag = variable.getCascade(sensorType, value.getQCFlag(), instrumentAssignments);
      record.setQc(cascadeFlag, value.getQCMessages());
    }
  }

  /**
   * Set a data reduction record's state for a missing required parameter
   * @param record The record
   * @param missingParameterName The name of the missing parameter
   */
  protected void makeMissingParameterRecord(
      DataReductionRecord record, List<SensorType> missingTypes) {

    List<String> qcMessages = new ArrayList<String>(missingTypes.size());

    for (String parameter : getCalculationParameterNames()) {
      record.put(parameter, Double.NaN);
    }

    for (SensorType type : missingTypes) {
      qcMessages.add("Missing " + type.getName());
    }

    record.setQc(Flag.NO_QC, qcMessages);
  }

  /**
   * Get the calculation parameters generated by the reducer, in
   * display order
   * @return The calculation parameters
   */
  protected abstract List<String> getCalculationParameterNames();

  /**
   * Get the list of SensorTypes required by this data reducer. This takes
   * the minimum list of sensor types (or parent types) and determines the
   * actual required types according to the sensor types assigned to the
   * instrument and their dependents.
   *
   * @param instrumentAssignments The sensor types assigned to the instrument
   * @param sensorTypeNames The names of the bare minimum sensor types
   * @return The complete list of required SensorType objects
   * @throws SensorTypeNotFoundException
   */
  protected Set<SensorType> getRequiredSensorTypes(
    SensorAssignments instrumentAssignments) throws DataReductionException, SensorTypeNotFoundException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    List<SensorType> sensorTypes = sensorConfig.getSensorTypes(getRequiredTypeStrings());

    Set<SensorType> result = new HashSet<SensorType>(sensorTypes.size());

    try {
        for (SensorType baseSensorType : sensorTypes) {

          if (sensorConfig.isParent(baseSensorType)) {
            Set<SensorType> childSensorTypes = sensorConfig.getChildren(baseSensorType);
            if (!addAnySensorTypesAndDependsOn(result, childSensorTypes, instrumentAssignments)) {
              throw new DataReductionException(
                "No assignments present for children of Sensor Type "
                + baseSensorType.getName() + " or their dependents");
            }
          } else {
            if (!addSensorTypeAndDependsOn(result, baseSensorType, instrumentAssignments)) {
              throw new DataReductionException(
                "No assignments present for Sensor Type "
                + baseSensorType.getName() + " or its dependents");
            }
          }
        }
    } catch (SensorTypeNotFoundException e) {
      throw new DataReductionException("Named sensor type not found", e);
    } catch (SensorConfigurationException e) {
      throw new DataReductionException("Invalid sensor configuration detected", e);
    }

    return result;
  }

  /**
   * Add a set of Sensor Types to an existing list of Sensor Types, including
   * any dependents
   *
   * @param list The list to which the sensor types are to be added
   * @param typesToAdd The sensor types to add
   * @param instrumentAssignments The instrument's sensor assignments
   * @return {@code true} if at least one Sensor Type is added; {@code false} if
   *         none are added (unless the list is empty)
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   */
  private boolean addAnySensorTypesAndDependsOn(Set<SensorType> list,
    Set<SensorType> typesToAdd, SensorAssignments instrumentAssignments)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    boolean result = false;

    if (typesToAdd.size() == 0) {
      result = true;
    } else {
      for (SensorType add : typesToAdd) {
        if (addSensorTypeAndDependsOn(list, add, instrumentAssignments)) {
          result = true;
        }
      }
    }

    return result;
  }

  /**
   * Add a Sensor Type to an existing list of Sensor Types, including
   * any dependents
   *
   * @param list The list to which the sensor types are to be added
   * @param typesToAdd The sensor types to add
   * @param instrumentAssignments The instrument's sensor assignments
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   */
  private boolean addSensorTypeAndDependsOn(Set<SensorType> list,
    SensorType typeToAdd, SensorAssignments instrumentAssignments)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    boolean result = true;

    if (!instrumentAssignments.isAssigned(typeToAdd)) {
      result = false;
    } else {
      list.add(typeToAdd);
      SensorType dependsOn = instrumentAssignments.getDependsOn(typeToAdd);
      if (null != dependsOn) {
        if (!addSensorTypeAndDependsOn(list, dependsOn, instrumentAssignments)) {
          result = false;
        }
      }
    }

    return result;
  }

  /**
   * See if any required values are NaN in the supplied set of values. If there
   * are NaNs, make the record a blank and return {@code true}.
   * @param record The record being processed
   * @param values The calculation values
   * @param requiredTypes The required sensor types
   * @return
   * @throws SensorTypeNotFoundException
   * @throws DataReductionException
   */
  private List<SensorType> getMissingParameters(SensorAssignments instrumentAssignments, Map<SensorType, CalculationValue> values) throws SensorTypeNotFoundException, DataReductionException {

    List<SensorType> missingTypes = new ArrayList<SensorType>();

    for (SensorType type : getRequiredSensorTypes(instrumentAssignments)) {
      CalculationValue value = values.get(type);
      if (null == value || value.isNaN()) {
        missingTypes.add(type);
      }
    }

    return missingTypes;
  }

  protected abstract String[] getRequiredTypeStrings();

  /**
   * Apply external standards calibration to a sensor value
   *
   * @param recordDate The date of the record from which the sensor value was taken
   * @param sensorName The name of the sensor
   * @param calculationValue The sensor value to be calibrated
   * @param ignoreZero Indicates whether or not the zero standard should be ignored
   * @return The calibrated sensor value
   * @throws DataReductionException If there are not sufficient standard measurements
   */
  protected Double applyValueCalibration(Measurement measurement, SensorType sensorType, CalculationValue originalValue, boolean ignoreZero) throws DataReductionException {
    return applyValueCalibration(measurement, sensorType, originalValue.getValue(), ignoreZero);
  }

  /**
   * Apply external standards calibration to a sensor value
   *
   * @param recordDate The date of the record from which the sensor value was taken
   * @param sensorName The name of the sensor
   * @param calculationValue The sensor value to be calibrated
   * @param ignoreZero Indicates whether or not the zero standard should be ignored
   * @return The calibrated sensor value
   * @throws DataReductionException If there are not sufficient standard measurements
   */
  protected Double applyValueCalibration(Measurement measurement, SensorType sensorType, Double originalValue, boolean ignoreZero) throws DataReductionException {

    // TODO Add excessive calibration adjustment check to this method -
    // it will set the flag on the CalculationValue

    Double calibratedValue;

    // Get the before and after measurements for each run type
    try {

      // For each external standard target, calculate the offset from the external
      // standard at the record date
      Map<String, Double> standardMeasurements = new HashMap<String, Double>();
      for (String target : calibrationSet.getTargets().keySet()) {
        double concentration = calibrationSet.getCalibrationValue(target, sensorType.getName());
        if (!ignoreZero || concentration > 0.0) {

          PrevNextTimes surroundingTimes = getSurroundingTimes(measurement, target, sensorType);

          CalculationValue priorCalibrationValue = null;
          if (null != surroundingTimes.prev) {
            priorCalibrationValue = CalculationValue.get(measurement, sensorType,
              groupedSensorValues.get(surroundingTimes.prev).get(sensorType));
          }

          CalculationValue postCalibrationValue = null;
          if (null != surroundingTimes.next) {
            postCalibrationValue = CalculationValue.get(measurement, sensorType,
              groupedSensorValues.get(surroundingTimes.next).get(sensorType));
          }

          standardMeasurements.put(target, calculateStandardValueAtDate(measurement.getTime(), target, surroundingTimes.prev, priorCalibrationValue, surroundingTimes.next, postCalibrationValue));
        }
      }

      // Make a regression of the offsets to calculate the offset at the measured concentration
      SimpleRegression regression = new SimpleRegression(true);
      for (String target : standardMeasurements.keySet()) {
        regression.addData(standardMeasurements.get(target), calibrationSet.getCalibrationValue(target, sensorType.getName()));
      }

      calibratedValue = regression.predict(originalValue);

    } catch (Exception e) {
      if (e instanceof DataReductionException) {
        throw (DataReductionException) e;
      } else {
        throw new DataReductionException("Error while applying internal calibration", e);
      }
    }

    return calibratedValue;
  }

  /**
   * Calculate the measured external standard value at a given date between
   * two calibration measurements.
   *
   * One of the calibrations can be {@code null}, in which case the
   * value that of the calibration measurement that is
   * not {@null}. If both measurements are {@code null} an error is thrown.
   *
   * @param date The target date for which the value is to be calculated
   * @param target The name of the external standard
   * @param sensorName The name of the sensor whose offset is being calculated
   * @param priorCalibration The calibration measurement prior to the {@code date}
   * @param postCalibration The calibration measurement after the {@code date}
   * @return The value at the specified date
   * @throws RecordNotFoundException If both calibration measurements are {@code null}.
   */
  private Double calculateStandardValueAtDate(LocalDateTime date, String target, LocalDateTime priorCalibrationTime, CalculationValue priorCalibration, LocalDateTime postCalibrationTime, CalculationValue postCalibration) throws RecordNotFoundException {

    Double result;

    if (null == priorCalibration && null == postCalibration) {
      throw new RecordNotFoundException("No calibrations found for external standard '" + target + "'");
    } else if (null == priorCalibration) {
      result = postCalibration.getValue();
    } else if (null == postCalibration) {
      result = priorCalibration.getValue();
    } else {
        double priorMeasuredValue = priorCalibration.getValue();
        double postMeasuredValue = postCalibration.getValue();
        SimpleRegression regression = new SimpleRegression(true);
        regression.addData(DateTimeUtils.dateToLong(priorCalibrationTime), priorMeasuredValue);
        regression.addData(DateTimeUtils.dateToLong(postCalibrationTime), postMeasuredValue);
        result = regression.predict(DateTimeUtils.dateToLong(date));
    }

    return result;
  }


  private PrevNextTimes getSurroundingTimes(Measurement start, String runType, SensorType sensorType) {

    PrevNextTimes result = null;
    PrevNextTimes cached = new PrevNextTimes(previousSearchTimes.get(runType), nextSearchTimes.get(runType));
    boolean searchRequired = false;

    if (null == cached.prev && null == cached.next) {
      searchRequired = true;
    } else if (null != cached.prev && start.getTime().isBefore(cached.prev)) {
      searchRequired = true;
    } else if (null != cached.next && start.getTime().isAfter(cached.next)) {
      searchRequired = true;
    }

    if (!searchRequired) {
      result = cached;
    } else {
      LocalDateTime prev = getPreviousTime(start, runType, sensorType);
      LocalDateTime next = getNextTime(start, runType, sensorType);
      result = new PrevNextTimes(prev, next);

      previousSearchTimes.put(runType, prev);
      nextSearchTimes.put(runType, next);
    }

    return result;
  }


  /**
   * Get the last measurement prior to the specified date with the
   * specified run type. Returns {@code null} if there is no matching record
   * @param time The time
   * @param runType The run type
   * @return The previous measurement
   */
  private LocalDateTime getPreviousTime(Measurement start, String runType, SensorType sensorType) {

    // TODO This and getNextTime can be refactored together

    LocalDateTime result = null;

    int i = allMeasurements.indexOf(start) - 1;
    while (null == result && i >= 0) {
      Measurement currentMeasurement = allMeasurements.get(i);
      if (currentMeasurement.getRunType().equals(runType)) {
        Map<SensorType, List<SensorValue>> measurementData =
          groupedSensorValues.get(currentMeasurement.getTime());

        CalculationValue value = CalculationValue.get(start, sensorType, measurementData.get(sensorType));
        if (!value.isNaN()) {
          result = currentMeasurement.getTime();
        }
      }

      i--;
    }

    return result;
  }

  /**
   * Get the first measurement after the specified date with the
   * specified run type. Returns {@code null} if there is no matching record
   * @param time The time
   * @param runType The run type
   * @return The previous measurement
   */
  private LocalDateTime getNextTime(Measurement start, String runType, SensorType sensorType) {
    LocalDateTime result = null;

    int i = allMeasurements.indexOf(start) + 1;
    while (null == result && i < allMeasurements.size()) {
      Measurement currentMeasurement = allMeasurements.get(i);
      if (currentMeasurement.getRunType().equals(runType)) {
        Map<SensorType, List<SensorValue>> measurementData =
          groupedSensorValues.get(currentMeasurement.getTime());

        CalculationValue value = CalculationValue.get(start, sensorType, measurementData.get(sensorType));
        if (!value.isNaN()) {
          result = currentMeasurement.getTime();
        }
      }

      i++;
    }

    return result;
  }

  protected SensorType getSensorType(String sensorTypeName) throws SensorTypeNotFoundException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    return sensorConfig.getSensorType(sensorTypeName);
  }

  protected Double getValue(Map<SensorType, CalculationValue> sensorValues, String sensorTypeName) throws SensorTypeNotFoundException {
    SensorType sensorType = getSensorType(sensorTypeName);
    return sensorValues.get(sensorType).getValue();
  }

  private class PrevNextTimes {
    LocalDateTime prev;
    LocalDateTime next;

    private PrevNextTimes(LocalDateTime prev, LocalDateTime next) {
      this.prev = prev;
      this.next = next;
    }
  }
}
