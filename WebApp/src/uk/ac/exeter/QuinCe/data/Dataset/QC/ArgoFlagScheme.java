package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

public class ArgoFlagScheme extends AbstractFlagScheme {

  /**
   * {@link Flag} indicating that a value is probably good.
   */
  public static final Flag PROBABLY_GOOD_FLAG = new Flag(2, "Probably Good",
    'P', 10, true, false, 2);

  /**
   * {@link Flag} indicating that a value has been estimated.
   */
  public static final Flag ESTIMATED_VALUE_FLAG = new Flag(8, "Estimated Value",
    'E', 20, false, false, 8);

  /**
   * {@link Flag} indicating that a value has been estimated.
   */
  public static final Flag VALUE_CHANGED_FLAG = new Flag(5, "Value Changed",
    'C', 30, false, false, 5);

  /**
   * {@link Flag} indicating that a value is bad but may be correctable.
   */
  public static final Flag BAD_CORRECTABLE_FLAG = new Flag(3,
    "Bad, potentially correctable", 'b', 40, true, false, 3);

  /**
   * {@link Flag} indicating that a value is bad and not correctable.
   */
  public static final Flag BAD_FLAG = new Flag(4, "Bad", 'B', 50, true, false,
    4);

  /**
   * Singleton instance of this scheme.
   */
  private static ArgoFlagScheme instance = null;

  protected ArgoFlagScheme() throws FlagException {
    super();
    registerGoodFlag(new Flag(1, "Good", 'G', 2, true, false, 1));
    registerFlag(PROBABLY_GOOD_FLAG);
    registerFlag(ESTIMATED_VALUE_FLAG);
    registerFlag(VALUE_CHANGED_FLAG);
    registerFlag(BAD_CORRECTABLE_FLAG);
    registerFlag(BAD_FLAG);
  }

  public static ArgoFlagScheme getInstance() throws FlagException {

    if (null == instance) {
      instance = new ArgoFlagScheme();
    }

    return instance;
  }

  @Override
  public int getBasis() {
    return Instrument.BASIS_ARGO;
  }

  @Override
  public String getName() {
    return "Argo";
  }

  @Override
  public Flag getBadFlag() {
    return BAD_FLAG;
  }

  @Override
  public List<Flag> getPlotHighlightFlags() {
    List<Flag> result = new ArrayList<Flag>();
    result.add(getGoodFlag());
    result.add(PROBABLY_GOOD_FLAG);
    result.add(ESTIMATED_VALUE_FLAG);
    result.add(VALUE_CHANGED_FLAG);
    result.add(BAD_CORRECTABLE_FLAG);
    result.add(BAD_FLAG);
    return result;
  }
}
