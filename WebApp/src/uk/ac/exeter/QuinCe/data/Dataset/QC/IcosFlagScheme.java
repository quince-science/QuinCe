package uk.ac.exeter.QuinCe.data.Dataset.QC;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * ICOS QC Flag scheme.
 * 
 * <p>
 * This is based on SOCAT's QC flagging scheme, with some extra flags for
 * QuinCe-specific functions.
 * </p>
 */
public class IcosFlagScheme extends AbstractFlagScheme
  implements CalibrationFlagScheme {

  /**
   * {@link Flag} indicating that a value has not been calibrated.
   */
  private static final Flag NO_CALIBRATION_FLAG = new Flag(1, "Not calibrated",
    'C', 1, false, false, 1);

  /**
   * {@link Flag} indicating that a value is <i>Bad</i>.
   */
  private static final Flag BAD_FLAG = new Flag(4, "Bad", 'B', 4, true, true,
    4);

  /**
   * {@link Flag} indicating that a value is <i>Bad</i>.
   */
  public static final Flag QUESTIONABLE_FLAG = new Flag(3, "Questionable", 'Q',
    3, true, true, 3);

  /**
   * Base constructor.
   * 
   * @throws FlagException
   *           If the scheme cannot be created.
   */
  protected IcosFlagScheme() throws FlagException {
    super();
    registerGoodFlag(new Flag(2, "Good", 'G', 2, true, false, 2));
    registerFlag(QUESTIONABLE_FLAG);
    registerFlag(BAD_FLAG);
    registerFlag(NO_CALIBRATION_FLAG);
  }

  /**
   * Singleton instance of this scheme.
   */
  private static IcosFlagScheme instance = null;

  /**
   * Get the singleton instance of this scheme.
   * 
   * @return The singleton instance of this scheme.
   * @throws FlagException
   *           If the instance cannot be constructed.
   */
  public static IcosFlagScheme getInstance() throws FlagException {

    if (null == instance) {
      instance = new IcosFlagScheme();
    }

    return instance;
  }

  @Override
  public Flag getNotCalibratedFlag() {
    return NO_CALIBRATION_FLAG;
  }

  @Override
  public Flag getBadFlag() {
    return BAD_FLAG;
  }

  @Override
  public int getBasis() {
    return Instrument.BASIS_TIME;
  }

  @Override
  public String getName() {
    return "ICOS";
  }
}
