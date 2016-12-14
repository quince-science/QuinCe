package uk.ac.exeter.QuinCe.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the calibration coefficients for a single sensor
 * @author Steve Jones
 *
 */
public class CalibrationCoefficients implements Comparable<SensorCode> {

	/**
	 * The code for the sensor. The sensor code
	 * can be generated using the generateSensorCode method
	 */
	private SensorCode sensorCode;
	
	/**
	 * The intercept of the fitted calibration curve
	 */
	private double intercept = 0.0;
	
	/**
	 * The linear coefficient of the fitted calibration curve
	 */
	private double x = 1.0;
	
	/**
	 * The x^2 coefficient of the fitted calibration curve
	 */
	private double x2 = 0.0;
	
	/**
	 * The x^3 coefficient of the fitted calibration curve
	 */
	private double x3 = 0.0;
	
	/**
	 * The x^4 coefficient of the fitted calibration curve
	 */
	private double x4 = 0.0;
	
	/**
	 * The x^5 coefficient of the fitted calibration curve
	 */
	private double x5 = 0.0;
	
	/////////// *** METHODS *** ////////////////////
	
	/**
	 * Creates a coefficients object with the specified sensor code and name.
	 * The coefficients are initialised to a linear 1:1 relationship
	 * @param sensorCode The sensor code
	 * @param sensorName The sensor name
	 */
	public CalibrationCoefficients(SensorCode sensorCode) {
		this.sensorCode = sensorCode;
	}
	
	/**
	 * Construct a list of coefficients objects for all the sensors attached to a given instrument
	 * @param instrument The instrument
	 * @return A list of coefficients objects
	 */
	public static List<CalibrationCoefficients> initCalibrationCoefficients(Instrument instrument) {
		List<CalibrationCoefficients> result = new ArrayList<CalibrationCoefficients>();
		
		if (instrument.hasIntakeTemp1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 1, instrument)));
		}
		
		if (instrument.hasIntakeTemp2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 2, instrument)));
		}
		
		if (instrument.hasIntakeTemp3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 3, instrument)));
		}
		
		if (instrument.hasSalinity1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_SALINITY, 1, instrument)));
		}
		
		if (instrument.hasSalinity2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_SALINITY, 2, instrument)));
		}
		
		if (instrument.hasSalinity3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_SALINITY, 3, instrument)));
		}
		
		if (instrument.hasEqt1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQT, 1, instrument)));
		}
		
		if (instrument.hasEqt2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQT, 2, instrument)));
		}
		
		if (instrument.hasEqt3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQT, 3, instrument)));
		}
		
		if (instrument.hasEqp1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQP, 1, instrument)));
		}
		
		if (instrument.hasEqp2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQP, 2, instrument)));
		}
		
		if (instrument.hasEqp3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQP, 3, instrument)));
		}
		
		return result;
	}
	
	public static CalibrationCoefficients findSensorCoefficients(List<CalibrationCoefficients> coefficients, SensorCode code) {
		CalibrationCoefficients result = null;
		
		for (CalibrationCoefficients coeffs : coefficients) {
			if (coeffs.compareTo(code) == 0) {
				result = coeffs;
				break;
			}
		}
		
		return result;
	}

	/**
	 * CalibrationCoefficients objects are the same if their sensor codes are the same
	 */
	@Override
	public int compareTo(SensorCode o) {
		return sensorCode.compareTo(o);
	}
	
	/////////// *** GETTERS AND SETTERS *** //////////////

	/**
	 * Returns the sensor code
	 * @return The sensor code
	 */
	public SensorCode getSensorCode() {
		return sensorCode;
	}

	/**
	 * Returns the intercept of the fitted calibration curve
	 * @return The intercept of the fitted calibration curve
	 */
	public double getIntercept() {
		return intercept;
	}

	/**
	 * Sets the intercept of the fitted calibration curve
	 * @param intercept The intercept of the fitted calibration curve
	 */
	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}

	/**
	 * Returns the linear coefficient of the fitted calibration curve
	 * @return The linear coefficient of the fitted calibration curve
	 */
	public double getX() {
		return x;
	}

	/**
	 * Sets the linear coefficient of the fitted calibration curve
	 * @param x The linear coefficient of the fitted calibration curve
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Returns the x^2 coefficient of the fitted calibration curve
	 * @return The x^2 coefficient of the fitted calibration curve
	 */
	public double getX2() {
		return x2;
	}

	/**
	 * Sets the x^2 coefficient of the fitted calibration curve
	 * @param x2 The x^2 coefficient of the fitted calibration curve
	 */
	public void setX2(double x2) {
		this.x2 = x2;
	}

	/**
	 * Returns the x^3 coefficient of the fitted calibration curve
	 * @return The x^3 coefficient of the fitted calibration curve
	 */
	public double getX3() {
		return x3;
	}

	/**
	 * Sets the x^3 coefficient of the fitted calibration curve
	 * @param x3 The x^3 coefficient of the fitted calibration curve
	 */
	public void setX3(double x3) {
		this.x3 = x3;
	}

	/**
	 * Returns the x^4 coefficient of the fitted calibration curve
	 * @return The x^4 coefficient of the fitted calibration curve
	 */
	public double getX4() {
		return x4;
	}

	/**
	 * Sets the x^4 coefficient of the fitted calibration curve
	 * @param x4 The x^4 coefficient of the fitted calibration curve
	 */
	public void setX4(double x4) {
		this.x4 = x4;
	}

	/**
	 * Returns the x^5 coefficient of the fitted calibration curve
	 * @return The x^5 coefficient of the fitted calibration curve
	 */
	public double getX5() {
		return x5;
	}

	/**
	 * Sets the x^5 coefficient of the fitted calibration curve
	 * @param x5 The x^5 coefficient of the fitted calibration curve
	 */
	public void setX5(double x5) {
		this.x5 = x5;
	}
}
