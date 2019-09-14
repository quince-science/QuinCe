package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exceptions thrown when handling calibrations
 * 
 * @author Steve Jones
 *
 */
public class CalibrationException extends RuntimeException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 4181728012550073515L;

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

}
