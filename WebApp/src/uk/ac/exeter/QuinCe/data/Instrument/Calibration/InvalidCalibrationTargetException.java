package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

@SuppressWarnings("serial")
public class InvalidCalibrationTargetException extends CalibrationException {

  public InvalidCalibrationTargetException(String target) {
    super("Calibration target '" + target + "' does not exist");
  }

}
