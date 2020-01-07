package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

@SuppressWarnings("serial")
public class InvalidCalibrationDateException extends CalibrationException {

  public InvalidCalibrationDateException() {
    super("Calibration date cannot be in the future");
  }

}
