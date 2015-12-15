package uk.ac.exeter.QuinCe.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the calibration coefficients for a single sensor
 * @author Steve Jones
 *
 */
public class CalibrationCoefficients {

	/**
	 * The code for the sensor. The sensor code
	 * can be generated using the generateSensorCode method
	 */
	private SensorCode sensorCode;
	
	/**
	 * The human-readable name of the sensor
	 */
	private String sensorName;
	
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
	public CalibrationCoefficients(SensorCode sensorCode, String sensorName) {
		this.sensorCode = sensorCode;
		this.sensorName = sensorName;
	}
	
	/**
	 * Construct a list of coefficients objects for all the sensors attached to a given instrument
	 * @param instrument The instrument
	 * @return A list of coefficients objects
	 */
	public static List<CalibrationCoefficients> initCalibrationCoefficients(Instrument instrument) {
		List<CalibrationCoefficients> result = new ArrayList<CalibrationCoefficients>();
		
		if (instrument.hasIntakeTemp1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 1), instrument.getLongIntakeTempName1()));
		}
		
		if (instrument.hasIntakeTemp2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 2), instrument.getLongIntakeTempName2()));
		}
		
		if (instrument.hasIntakeTemp3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 3), instrument.getLongIntakeTempName3()));
		}
		
		if (instrument.hasSalinity1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_SALINITY, 1), instrument.getLongSalinityName1()));
		}
		
		if (instrument.hasSalinity2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_SALINITY, 2), instrument.getLongSalinityName2()));
		}
		
		if (instrument.hasSalinity3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_SALINITY, 3), instrument.getLongSalinityName3()));
		}
		
		if (instrument.hasEqt1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQT, 1), instrument.getLongEqtName1()));
		}
		
		if (instrument.hasEqt2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQT, 2), instrument.getLongEqtName2()));
		}
		
		if (instrument.hasEqt3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQT, 3), instrument.getLongEqtName3()));
		}
		
		if (instrument.hasEqp1()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQP, 1), instrument.getLongEqpName1()));
		}
		
		if (instrument.hasEqp2()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQP, 2), instrument.getLongEqpName2()));
		}
		
		if (instrument.hasEqp3()) {
			result.add(new CalibrationCoefficients(new SensorCode(SensorCode.TYPE_EQP, 3), instrument.getLongEqpName3()));
		}
		
		return result;
	}
	
	
	/////////// *** GETTERS AND SETTERS *** //////////////

	/**
	 * Returns the sensor code
	 * @return The sensor code
	 */
	public String getSensorCode() {
		return sensorCode.toString();
	}

	/**
	 * Sets the sensor code
	 * @param sensorCode The sensor code
	 */
	public void setSensorCode(String sensorCode) {
		this.sensorCode = new SensorCode(sensorCode);
	}

	/**
	 * Returns the sensor name
	 * @return The sensor name
	 */
	public String getSensorName() {
		return sensorName;
	}

	/**
	 * Sets the sensor code
	 * @param sensorCode The sensor code
	 */
	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
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
