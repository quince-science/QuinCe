package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exceptions thrown when handling calibrations
 */
@SuppressWarnings("serial")
public class CalibrationException extends Exception {

  /**
   * Constructor for a simple error message
   *
   * @param message
   *          The error message
   */
  public CalibrationException(String message) {
    super(message);
  }

  /**
   * Constructor wrapper for an internal error while processing calibrations
   *
   * @param cause
   *          The error
   */
  public CalibrationException(Throwable cause) {
    super("Error while processing calibrations", cause);
  }

  public CalibrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
