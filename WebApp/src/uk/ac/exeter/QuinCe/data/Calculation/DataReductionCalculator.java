package uk.ac.exeter.QuinCe.data.Calculation;

import java.time.LocalDateTime;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

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
	 * @throws CalculatorException if the calculation cannot be completed
	 */
	public abstract void performDataReduction(DataSetRawDataRecord measurement) throws CalculatorException;
	
	// TODO Document this properly
	/**
	 * Apply external standards calibration to a sensor value
	 * 
	 * @param recordDate The date of the record from which the sensor value was taken
	 * @param sensorName The name of the sensor
	 * @param originalValue The sensor value to be calibrated
	 * @return The calibrated sensor value
	 * @throws CalculatorException If there are not sufficient standard measurements
	 */
	protected double applyExternalStandards2d(LocalDateTime recordDate, String sensorName, double originalValue) throws CalculatorException {

		double calibratedValue;
		
		try {
			// Get the standards with concentrations either side of the measured value
			String standardBelow = externalStandards.getCalibrationBelow(originalValue, true);
			if (null == standardBelow) {
				standardBelow = externalStandards.getCalibrationAbove(originalValue, true);
			} if (null == standardBelow) {
				throw new CalculatorException("No external standards defined");
			}
			
			double standardBelowValue = externalStandards.getCalibrationValue(standardBelow, sensorName);
			
			String standardAbove = externalStandards.getCalibrationAbove(standardBelowValue, false);
			if (null == standardAbove) {
				// Maybe we're above the highest standard. So look for a standard beneath the "below" standard.
				String standardBeneathBelow = externalStandards.getCalibrationBelow(standardBelowValue, false);
				
				if (null != standardBeneathBelow) {
					standardAbove = standardBelow;
					standardBelow = standardBeneathBelow;
					standardBelowValue = externalStandards.getCalibrationValue(standardBelow, sensorName);
				} else {
					throw new CalculatorException("Need more than one external standard");
				}
			}
			
			double standardAboveValue = externalStandards.getCalibrationValue(standardAbove, sensorName);
			
			// Get the calibration measurements for the above two standards taken immediately before the measurement.
			// If there is no measurement before, take the measurement immediately afterwards - we will do a linear
			// extrapolation from that and the following standard.
			DataSetRawDataRecord priorCalibrationBelow = calibrations.getCalibrationBefore(recordDate, standardBelow);
			if (null == priorCalibrationBelow) {
				priorCalibrationBelow = calibrations.getCalibrationAfter(recordDate, standardBelow);
			} if (null == priorCalibrationBelow) {
				throw new CalculatorException("No external standard measurements for " + standardBelow);
			}
			
			DataSetRawDataRecord priorCalibrationAbove = calibrations.getCalibrationBefore(recordDate, standardAbove);
			if (null == priorCalibrationAbove) {
				priorCalibrationAbove = calibrations.getCalibrationAfter(recordDate, standardAbove);
			} if (null == priorCalibrationAbove) {
				throw new CalculatorException("No external standard measurements for " + standardBelow);
			}
			
			// Get the calibration measurements immediately after the measurements retrieve above.
			// This will give us the two sets of calibrations we need for the adjustments
			DataSetRawDataRecord postCalibrationBelow = calibrations.getCalibrationAfter(priorCalibrationBelow.getDate(), standardBelow);
			if (null == postCalibrationBelow) {
				// Maybe we're after the last calibration record. So look for one before the "prior" record
				DataSetRawDataRecord calibrationBeforePrior = calibrations.getCalibrationBefore(priorCalibrationBelow.getDate(), standardBelow);
				
				if (null != calibrationBeforePrior) {
					postCalibrationBelow = priorCalibrationBelow;
					priorCalibrationBelow = calibrationBeforePrior;
				} else {
					throw new CalculatorException("Need more than one external standard measurement for " + standardBelow);
				}
			}
	
			DataSetRawDataRecord postCalibrationAbove = calibrations.getCalibrationAfter(priorCalibrationAbove.getDate(), standardAbove);
			if (null == postCalibrationAbove) {
				// Maybe we're after the last calibration record. So look for one before the "prior" record
				DataSetRawDataRecord calibrationBeforePrior = calibrations.getCalibrationBefore(priorCalibrationAbove.getDate(), standardAbove);
				
				if (null != calibrationBeforePrior) {
					postCalibrationAbove = priorCalibrationAbove;
					priorCalibrationAbove = calibrationBeforePrior;
				} else {
					throw new CalculatorException("Need more than one external standard measurement for " + standardAbove);
				}
			}
			
			// Get the offset of the measurements from the true standard values
			double priorCalibrationBelowOffset = standardBelowValue - priorCalibrationBelow.getSensorValue(sensorName);
			long priorCalibrationBelowTime = DateTimeUtils.dateToLong(priorCalibrationBelow.getDate());
			
			double postCalibrationBelowOffset = standardBelowValue - postCalibrationBelow.getSensorValue(sensorName);
			long postCalibrationBelowTime = DateTimeUtils.dateToLong(postCalibrationBelow.getDate());
			
			double priorCalibrationAboveOffset = standardAboveValue - priorCalibrationAbove.getSensorValue(sensorName);
			long priorCalibrationAboveTime = DateTimeUtils.dateToLong(priorCalibrationAbove.getDate());
			
			double postCalibrationAboveOffset = standardAboveValue - postCalibrationAbove.getSensorValue(sensorName);
			long postCalibrationAboveTime = DateTimeUtils.dateToLong(postCalibrationAbove.getDate());
			
			// Calculate the offsets of the standards at the time of the measurement 
			double belowOffset = calculateOffsetAtValue(priorCalibrationBelowTime, priorCalibrationBelowOffset, postCalibrationBelowTime, postCalibrationBelowOffset, DateTimeUtils.dateToLong(recordDate));
			double aboveOffset = calculateOffsetAtValue(priorCalibrationAboveTime, priorCalibrationAboveOffset, postCalibrationAboveTime, postCalibrationAboveOffset, DateTimeUtils.dateToLong(recordDate));
			
			// Calculate the final offsets to the standards
			calibratedValue = originalValue + calculateOffsetAtValue(standardBelowValue, belowOffset, standardAboveValue, aboveOffset, originalValue);
		} catch (Exception e) {
			if (e instanceof CalculatorException) {
				throw (CalculatorException) e;
			} else {
				throw new CalculatorException(e);
			}
		}
		
		return calibratedValue;
	}

	// TODO Document this properly
	// TODO A lot of this code can be refactored and shared with the 2d version.
	/**
	 * Apply external standards calibration to a sensor value
	 * 
	 * @param recordDate The date of the record from which the sensor value was taken
	 * @param sensorName The name of the sensor
	 * @param originalValue The sensor value to be calibrated
	 * @return The calibrated sensor value
	 * @throws CalculatorException If there are not sufficient standard measurements
	 */
	protected double applyExternalStandards1d(LocalDateTime recordDate, String sensorName, double originalValue) throws CalculatorException {

		double calibratedValue;
		
		try {
			// Get the standards with concentrations either side of the measured value
			String standard = externalStandards.getCalibrationBelow(originalValue, true);
			if (null == standard) {
				standard = externalStandards.getCalibrationAbove(originalValue, true);
			} if (null == standard) {
				throw new CalculatorException("No external standards defined");
			}
			
			double standardValue = externalStandards.getCalibrationValue(standard, sensorName);
			
			// Get the calibration measurements for the above two standards taken immediately before the measurement.
			// If there is no measurement before, take the measurement immediately afterwards - we will do a linear
			// extrapolation from that and the following standard.
			DataSetRawDataRecord priorCalibration = calibrations.getCalibrationBefore(recordDate, standard);
			if (null == priorCalibration) {
				priorCalibration = calibrations.getCalibrationAfter(recordDate, standard);
			} if (null == priorCalibration) {
				throw new CalculatorException("No external standard measurements for " + standard);
			}
			
			// Get the calibration measurements immediately after the measurements retrieve above.
			// This will give us the two sets of calibrations we need for the adjustments
			DataSetRawDataRecord postCalibration = calibrations.getCalibrationAfter(priorCalibration.getDate(), standard);
			if (null == postCalibration) {
				// Maybe we're after the last calibration record. So look for one before the "prior" record
				DataSetRawDataRecord calibrationBeforePrior = calibrations.getCalibrationBefore(priorCalibration.getDate(), standard);
				
				if (null != calibrationBeforePrior) {
					postCalibration = priorCalibration;
					priorCalibration = calibrationBeforePrior;
				} else {
					throw new CalculatorException("Need more than one external standard measurement for " + standard);
				}
			}
	
			// Get the offset of the measurements from the true standard values
			double priorCalibrationOffset = standardValue - priorCalibration.getSensorValue(sensorName);
			long priorCalibrationTime = DateTimeUtils.dateToLong(priorCalibration.getDate());
			
			double postCalibrationOffset = standardValue - postCalibration.getSensorValue(sensorName);
			long postCalibrationTime = DateTimeUtils.dateToLong(postCalibration.getDate());
			
			// Calculate the offsets of the standards at the time of the measurement 
			double offset = calculateOffsetAtValue(priorCalibrationTime, priorCalibrationOffset, postCalibrationTime, postCalibrationOffset, DateTimeUtils.dateToLong(recordDate));
			
			// Calculate the final offsets to the standards
			calibratedValue = originalValue + offset;
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
	 * Calculate the offset to be applied to a value based on offsets from two known values
	 * using linear regression
	 * @param lowValue The true lower value
	 * @param lowOffset The offset of the system from the lower value
	 * @param highValue The true higher value
	 * @param highOffset The offset of the system from the higher value
	 * @param targetValue The values whose offset is to be calculated
	 * @return The calculated offset
	 */
	private double calculateOffsetAtValue(double lowValue, double lowOffset, double highValue, double highOffset, double targetValue) {
		SimpleRegression regression = new SimpleRegression(true);
		regression.addData(lowValue, lowOffset);
		regression.addData(highValue, highOffset);
		return regression.predict(targetValue);
	}
}
