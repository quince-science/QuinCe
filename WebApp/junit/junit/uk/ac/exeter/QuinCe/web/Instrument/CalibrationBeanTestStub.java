package junit.uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
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

  private CalibrationDB stubDbInstance;

  private boolean affectFollowingOnly;

  /**
   * Package-protected constructor.
   *
   * @param dbInstance
   *          The {@link CalibrationDB} to use.
   */
  protected CalibrationBeanTestStub(CalibrationDB dbInstance,
    boolean affectFollowingOnly) {
    this.stubDbInstance = dbInstance;
    this.affectFollowingOnly = affectFollowingOnly;
  }

  @Override
  protected String getPageNavigation() {
    return "NAV";
  }

  @Override
  protected CalibrationDB getDbInstance() {
    return stubDbInstance;
  }

  @Override
  protected String getCalibrationType() {
    return stubDbInstance.getCalibrationType();
  }

  @Override
  public String getHumanReadableCalibrationType() {
    return stubDbInstance.getCalibrationType();
  }

  @Override
  protected Calibration initNewCalibration(LocalDateTime date) {
    return new CalibrationTestStub(instrument, date);
  }

  @Override
  public String getTargetLabel() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected int getReprocessStatus() {
    return DataSet.STATUS_SENSOR_QC;
  }

  @Override
  protected Class<? extends Job> getReprocessJobClass() {
    return AutoQCJob.class;
  }

  @Override
  protected boolean changeAffectsDatasetsAfterOnly() {
    return affectFollowingOnly;
  }
}
