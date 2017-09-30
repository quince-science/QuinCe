package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Factory for creating {@link Calibration} objects
 * @author Steve Jones
 *
 */
public class CalibrationFactory {

	/**
	 * The package containing calibration classes
	 */
	private static final String CALIBRATION_PACKAGE = "uk.ac.exeter.QuinCe.data.Instrument.Calibration";
	
	/**
	 * Create a calibration object. The specific type of the calibration object is dependent on
	 * the parameters passed in.
	 * 
	 * @param calibrationType The high-level calibration type
	 * @param calibrationClass The class of the desired calibration object
	 * @param instrumentId The instrument to which the calibration applies
	 * @param deploymentDate The deployment date (may be null)
	 * @param target The target (sensor, gas standard etc) of the calibration
	 * @param coefficients The calibration coefficients
	 * @return The Calibration object
	 */
	public static Calibration createCalibration(String calibrationType, String calibrationClass, long instrumentId, LocalDateTime deploymentDate, String target, List<Double> coefficients) {
		Calibration result;
		
		switch (calibrationType) {
		case GasStandardDB.GAS_STANDARD_CALIBRATION_TYPE: {
			try {
				result = new GasStandard(instrumentId, target, deploymentDate, coefficients);
			} catch (CalibrationException e) {
				throw e;
			} catch (Exception e) {
				throw new CalibrationException(e); 
			}
			break;
		}
		case SensorCalibrationDB.SENSOR_CALIBRATION_TYPE: {
			
			try {
				String fullClass = CALIBRATION_PACKAGE + "." + calibrationClass;
				Class<?> clazz = Class.forName(fullClass);
				
				Constructor<?> constructor = clazz.getConstructor(long.class, String.class, LocalDateTime.class, List.class);
				result = (Calibration) constructor.newInstance(instrumentId, target, deploymentDate, coefficients);
			} catch (CalibrationException e) {
				throw e;
			} catch (Exception e) {
				throw new CalibrationException(e); 
			}
			
			break;
		}
		default: {
			throw new UnrecognisedCalibrationTypeException(calibrationType);
		}
		}
		
		return result;
	}
}
