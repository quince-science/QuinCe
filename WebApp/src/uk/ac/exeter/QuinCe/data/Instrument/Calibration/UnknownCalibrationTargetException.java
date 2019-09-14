package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exception for unrecognised calibration targets
 * 
 * @author Steve Jones
 *
 */
public class UnknownCalibrationTargetException extends CalibrationException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -2855585676136712015L;

  /**
   * Basic constructor
   * 
   * @param instrumentId
   *          The instrument ID
   * @param type
   *          The calibration type
   * @param target
   *          The unrecognised target
   */
  public UnknownCalibrationTargetException(long instrumentId, String type,
    String target) {
    super("Unrecognised calibration target for instrument ID " + instrumentId
      + ", calibration type " + type + ": " + target);
  }

}
