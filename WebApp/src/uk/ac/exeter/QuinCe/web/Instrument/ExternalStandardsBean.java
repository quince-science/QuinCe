package uk.ac.exeter.QuinCe.web.Instrument;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandard;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;

/**
 * Bean for external standards
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ExternalStandardsBean extends CalibrationBean {

  /**
   * The navigation string for the external standards list
   */
  private static final String NAV_LIST = "external_standards";

  /**
   * The external standard being edited by the user
   */
  private ExternalStandard enteredStandard = null;

  /**
   * The external standards database utility class
   */
  private ExternalStandardDB db = null;

  /**
   * Constructor
   */
  public ExternalStandardsBean() {
    super();
    db = ExternalStandardDB.getInstance();
  }

  @Override
  protected String getListNavigation() {
    return NAV_LIST;
  }

  @Override
  protected void createEnteredCalibration() {
    enteredStandard = new ExternalStandard(instrumentId);
  }

  @Override
  public ExternalStandard getEnteredCalibration() {
    return enteredStandard;
  }

  @Override
  protected CalibrationDB getDbInstance() {
    return db;
  }

  @Override
  protected String getCalibrationType() {
    return ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE;
  }
}
