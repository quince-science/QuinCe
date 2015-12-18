package uk.ac.exeter.QuinCe.web.Instrument;

public class CalibrationDateValidator extends InstrumentDateValidator {
	
	@Override
	public String getTable() {
		return "sensor_calibration";
	}

	@Override
	public String getField() {
		return "calibration_date";
	}
	
	@Override
	public String getErrorMessage() {
		return "A calibration with the specified date already exists";
	}

}
