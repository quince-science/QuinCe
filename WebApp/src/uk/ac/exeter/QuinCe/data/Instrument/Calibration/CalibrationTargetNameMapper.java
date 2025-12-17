package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Utility class to map internal calibration targets to human-readable values.
 *
 * <p>
 * Some targets for {@link Calibration} objects are stored as codes so that they
 * can be linked to parts of an instrument, dataset, or other 'internal' items.
 * For example, coefficients for specific sensors have their target set to the
 * sensor's database ID. This is not helpful to display to the user, so
 * instances of this interface can be used to map those values to human-readable
 * strings.
 * </p>
 */
public interface CalibrationTargetNameMapper {

  /**
   * Map the specified calibration target to its human-readable form.
   *
   * @param target
   *          The calibration target.
   * @return The human-readable version of the target.
   */
  public String map(String target);
}
