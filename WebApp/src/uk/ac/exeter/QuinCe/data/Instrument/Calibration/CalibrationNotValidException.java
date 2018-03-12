/**
 *
 */
package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exception for handling invalid calibrations, generally because a sensor
 * calibration is to old, or it is from the future!
 *
 * @author Jonas F. Henriksen
 *
 */
public class CalibrationNotValidException extends CalibrationException {

  private static final long serialVersionUID = -5155257714267450088L;

  /**
   * @param message
   */
  public CalibrationNotValidException(String message) {
    super(message);
  }
}
