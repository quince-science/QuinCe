package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Exception for unrecognised calibration targets
 */
@SuppressWarnings("serial")
public class UnknownCalibrationTargetException extends CalibrationException {

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
