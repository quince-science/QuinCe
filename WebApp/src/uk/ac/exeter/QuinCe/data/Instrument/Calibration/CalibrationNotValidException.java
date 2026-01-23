package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exception for handling invalid calibrations, generally because a sensor
 * calibration is to old, or it is from the future!
 */
@SuppressWarnings("serial")
public class CalibrationNotValidException extends CalibrationException {

  /**
   * @param message
   *          The error message.
   */
  public CalibrationNotValidException(String message) {
    super(message);
  }
}
