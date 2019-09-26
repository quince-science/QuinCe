package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exception for unrecognised calibration types
 * 
 * @author Steve Jones
 *
 */
public class UnrecognisedCalibrationTypeException extends CalibrationException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 7615133288119877695L;

  /**
   * Simple constructor
   * 
   * @param type
   *          The unrecognised type
   */
  public UnrecognisedCalibrationTypeException(String type) {
    super("Unrecognised calibration type '" + type + "'");
  }
}
