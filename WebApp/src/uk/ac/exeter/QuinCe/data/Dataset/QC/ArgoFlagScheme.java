package uk.ac.exeter.QuinCe.data.Dataset.QC;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

public class ArgoFlagScheme extends AbstractFlagScheme {

  /**
   * Singleton instance of this scheme.
   */
  private static ArgoFlagScheme instance = null;

  protected ArgoFlagScheme() throws FlagException {
    super();
    registerGoodFlag(new Flag(2, "Good", 'G', 2, true, false, 2));
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
    // TODO Auto-generated method stub
    return null;
  }

}
