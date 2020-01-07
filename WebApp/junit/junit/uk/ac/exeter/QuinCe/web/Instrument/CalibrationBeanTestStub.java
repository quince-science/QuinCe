package junit.uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;

/**
 * {@link CalibrationBean} test stub for {@link CalibrationBeanTest}.
 *
 * <p>
 * This uses the {@link ExternalStandardDB} as its backing database instance.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class CalibrationBeanTestStub extends CalibrationBean {

  /**
   * Package-protected constructor
   */
  protected CalibrationBeanTestStub() {

  }

  @Override
  protected String getPageNavigation() {
    return "NAV";
  }

  @Override
  protected CalibrationDB getDbInstance() {
    return ExternalStandardDB.getInstance();
  }

  @Override
  protected String getCalibrationType() {
    return getDbInstance().getCalibrationType();
  }

  @Override
  public String getHumanReadableCalibrationType() {
    return getDbInstance().getCalibrationType();
  }

  @Override
  protected Calibration initNewCalibration() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getTargetLabel() {
    // TODO Auto-generated method stub
    return null;
  }

}
