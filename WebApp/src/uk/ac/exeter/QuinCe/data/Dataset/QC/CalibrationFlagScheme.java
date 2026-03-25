package uk.ac.exeter.QuinCe.data.Dataset.QC;

/**
 * This interface is for instances of {@link FlagScheme} that implement QC flags
 * related to calibration.
 */
public interface CalibrationFlagScheme extends FlagScheme {

  /**
   * Get a {@link Flag} that indicates a value has not been calibrated.
   * 
   * @return A flag indicating that a value has not been calibrated.
   */
  public Flag getNotCalibratedFlag();
}
