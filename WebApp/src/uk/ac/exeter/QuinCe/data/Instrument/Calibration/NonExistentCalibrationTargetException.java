package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exception for an attempt to reference a calibration target that doesn't
 * exist.
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class NonExistentCalibrationTargetException extends Exception {

  public NonExistentCalibrationTargetException(String target) {
    super("Calibration target '" + target + "' does not exist");
  }

}
