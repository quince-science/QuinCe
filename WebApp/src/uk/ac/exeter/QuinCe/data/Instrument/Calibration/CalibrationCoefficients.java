package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorCode;

/**
 * <p>Each sensor for an instrument must have calibration data associated with it.
 * Calibrations for each sensor should be made on a regular basis.</p>
 * 
 * <p>Calibrations are stored as coefficients of a curve that is used to convert the 'raw'
 * measured value to the calibrated value. This is commonly a linear adjustment, but this class allows
 * for calibration curves of up to a fifth order polynomial. If any coefficients are unused in a calibration,
 * they can be set to zero.</p>
 * 
 * <p>This class holds a single set of calibration coefficients for a single sensor.</p>

 * @author Steve Jones
 */
public class CalibrationCoefficients implements Comparable<SensorCode> {

	/**
	 * The identifying code for the sensor.
	 */
	private SensorCode sensorCode;
	
	/**
	 * The intercept of the calibration curve.
	 */
	private double intercept = 0.0;
	
	/**
	 * The linear coefficient of the calibration curve.
	 */
	private double x = 1.0;
	
	/**
	 * The second order polynomial coefficient of the calibration curve.
	 */
	private double x2 = 0.0;
	
	/**
	 * The third order polynomial coefficient of the calibration curve.
	 */
	private double x3 = 0.0;
	
	/**
	 * The fourth order polynomial coefficient of the calibration curve.
	 */
	private double x4 = 0.0;
	
	/**
	 * The fifth order polynomial coefficient of the calibration curve.
	 */
	private double x5 = 0.0;
	
	/////////// *** METHODS *** ////////////////////
	
	/**
	 * <p>Creates a set of calibration coefficients for a sensor, identified
	 * by its {@code SensorCode}.</p>
	 * 
	 * <p>The coefficients are initialised to a linear 1:1 relationship, implying
	 * that no calibration adjustments are required.</p>
	 * 
	 * @param sensorCode The {@code SensorCode} identifying the sensor.
	 */
	public CalibrationCoefficients(SensorCode sensorCode) {
		this.sensorCode = sensorCode;
	}
	
	/**
	 * Construct a list of coefficients objects for all the sensors attached to a given instrument.
	 * All objects will have the default linear 1:1 relationship.
	 * 
	 * @param instrument The instrument for whose sensors the {@code CalibrationCoefficients} objects will be created.
	 * @return The coefficients objects for all sensors attached to the instrument.
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
	
	/**
	 * Search a list of {@code CalibrationCoefficients} objects for an object matching the supplied {@code SensorCode}.
	 * 
	 * If none of the objects matches the {@code SensorCode}, the method returns {@code null}.
	 * 
	 * @param coefficients The list of {@code CalibrationCoefficients} objects to be searched.
	 * @param code The {@code SensorCode} for the desired sensor.
	 * @return The {@code CalibrationCoefficients} object for the supplied {@code SensorCode}, or {@code null} if no matching coefficients can be found.
	 */
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
	 * <b>N.B.</b> Comparison of {@code CalibrationCoefficients} objects is only performed on the {@code SensorCode}; the coefficients themselves are not compared.
	 */
	@Override
	public int compareTo(SensorCode o) {
		return sensorCode.compareTo(o);
	}
	
	/////////// *** GETTERS AND SETTERS *** //////////////

	/**
	 * Get the {@code SensorCode} of the sensor that this object refers to.
	 * @return The {@code SensorCode}
	 */
	public SensorCode getSensorCode() {
		return sensorCode;
	}

	/**
	 * Get the intercept of the calibration curve.
	 * @return The intercept of the calibration curve.
	 */
	public double getIntercept() {
		return intercept;
	}

	/**
	 * Set the intercept of the calibration curve.
	 * @param intercept The intercept.
	 */
	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}

	/**
	 * Get the linear coefficient of the calibration curve.
	 * @return The coefficient.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Set the linear coefficient of the calibration curve.
	 * @param x The coefficient.
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Get the second order polynomial coefficient of the calibration curve.
	 * @return The coefficient.
	 */
	public double getX2() {
		return x2;
	}

	/**
	 * Set the second order polynomial coefficient of the calibration curve.
	 * @param x2 The coefficient.
	 */
	public void setX2(double x2) {
		this.x2 = x2;
	}

	/**
	 * Get the third order polynomial coefficient of the calibration curve.
	 * @return The coefficient.
	 */
	public double getX3() {
		return x3;
	}

	/**
	 * Set the third order polynomial coefficient of the calibration curve.
	 * @param x3 The coefficient.
	 */
	public void setX3(double x3) {
		this.x3 = x3;
	}

	/**
	 * Get the fourth order polynomial coefficient of the calibration curve.
	 * @return The coefficient.
	 */
	public double getX4() {
		return x4;
	}

	/**
	 * Set the fourth order polynomial coefficient of the calibration curve.
	 * @param x4 The coefficient.
	 */
	public void setX4(double x4) {
		this.x4 = x4;
	}

	/**
	 * Get the fifth order polynomial coefficient of the calibration curve.
	 * @return The coefficient.
	 */
	public double getX5() {
		return x5;
	}

	/**
	 * Set the fifth order polynomial coefficient of the calibration curve.
	 * @param x5 The coefficient.
	 */
	public void setX5(double x5) {
		this.x5 = x5;
	}
}
