package uk.ac.exeter.QuinCe.data.Calculation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Abstract class for data reduction calculators
 * @author Steve Jones
 *
 */
public abstract class DataReductionCalculator {

  /**
   * The CalculationDB instance for the calculator
   */
  protected CalculationDB db;

  /**
   * The set of calibration data to be used for the calculations
   */
  protected CalibrationDataSet calibrations;

  /**
   * The external standards for the data set
   */
  private CalibrationSet externalStandards;

  /**
   * The base constructor - sets up things that
   * are universal to all calculators
   * @throws CalculatorException If the set of calibration records is empty
   */
  protected DataReductionCalculator(CalibrationSet externalStandards, CalibrationDataSet calibrations) throws CalculatorException {
    if (null == calibrations || calibrations.size() == 0) {
      throw new CalculatorException("No calibration records supplied to calculator");
    }

    db = getDbInstance();
    this.calibrations = calibrations;
    this.externalStandards = externalStandards;
  }

  /**
   * Get an instance of the {@link CalculationDB} for the
   * calculator
   */
  protected abstract CalculationDB getDbInstance();

  /**
   * Perform the data reduction calculation for a given measurement,
   * and store the results in the database.
   * @param measurement The measurement
   * @return The calculated values
   * @throws CalculatorException if the calculation cannot be completed
   */
  public abstract Map<String, Double> performDataReduction(DataSetRawDataRecord measurement) throws CalculatorException;

  // TODO Document this properly
  /**
   * Apply external standards calibration to a sensor value
   *
   * @param recordDate The date of the record from which the sensor value was taken
   * @param sensorName The name of the sensor
   * @param originalValue The sensor value to be calibrated
   * @param ignoreZero Indicates whether or not the zero standard should be ignored
   * @return The calibrated sensor value
   * @throws CalculatorException If there are not sufficient standard measurements
   */
  protected double applyExternalStandards(LocalDateTime recordDate, String sensorName, double originalValue, boolean ignoreZero) throws CalculatorException {

    double calibratedValue;

    try {

      // For each external standard target, calculate the offset from the external
      // standard at the record date
      Map<String, Double> standardMeasurements = new HashMap<String, Double>();
      for (String target : externalStandards.getTargets()) {
        double concentration = externalStandards.getCalibrationValue(target, sensorName);
        if (!ignoreZero || concentration > 0.0) {
          DataSetRawDataRecord priorCalibration = calibrations.getCalibrationBefore(recordDate, target);
          DataSetRawDataRecord postCalibration = calibrations.getCalibrationAfter(recordDate, target);
          standardMeasurements.put(target, calculateStandardValueAtDate(recordDate, target, sensorName, priorCalibration, postCalibration));
        }
      }

      // Make a regression of the offsets to calculate the offset at the measured concentration
      SimpleRegression regression = new SimpleRegression(true);
      for (String target : standardMeasurements.keySet()) {
        regression.addData(standardMeasurements.get(target), externalStandards.getCalibrationValue(target, sensorName));
      }

      calibratedValue = regression.predict(originalValue);

    } catch (Exception e) {
      if (e instanceof CalculatorException) {
        throw (CalculatorException) e;
      } else {
        throw new CalculatorException(e);
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
  private double calculateStandardValueAtDate(LocalDateTime date, String target, String sensorName, DataSetRawDataRecord priorCalibration, DataSetRawDataRecord postCalibration) throws RecordNotFoundException {

    double result;

    if (null == priorCalibration && null == postCalibration) {
      throw new RecordNotFoundException("No calibrations found for external standard '" + target + "'");
    } else if (null == priorCalibration) {
      result = postCalibration.getSensorValue(sensorName);
    } else if (null == postCalibration) {
      result = priorCalibration.getSensorValue(sensorName);
    } else {
        double priorMeasuredValue = priorCalibration.getSensorValue(sensorName);
        double postMeasuredValue = postCalibration.getSensorValue(sensorName);
        SimpleRegression regression = new SimpleRegression(true);
        regression.addData(DateTimeUtils.dateToLong(priorCalibration.getDate()), priorMeasuredValue);
        regression.addData(DateTimeUtils.dateToLong(postCalibration.getDate()), postMeasuredValue);
        result = regression.predict(DateTimeUtils.dateToLong(date));
    }

    return result;
  }
}
