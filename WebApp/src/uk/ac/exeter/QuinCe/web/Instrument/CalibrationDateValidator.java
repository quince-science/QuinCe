package uk.ac.exeter.QuinCe.web.Instrument;

/**
 * An validation class to check that a sensor calibration date does not
 * clash with an existing calibration. 
 * 
 * @author Steve Jones
 *
 */
public class CalibrationDateValidator extends InstrumentDateValidator {
	
	/**
	 * Specifies that the {@code sensor_calibration} table should be searched for clashing dates.
	 */
	@Override
	public String getTable() {
		return "sensor_calibration";
	}

	/**
	 * Specifies that the {@code calibration_date} field should be searched for clashing dates. 
	 */
	@Override
	public String getField() {
		return "calibration_date";
	}

	/**
	 * Returns the message to be displayed if the calibration date is already in use.
	 */
	@Override
	public String getErrorMessage() {
		return "A calibration with the specified date already exists";
	}

}
