package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.files.LocateMeasurementsJob;

/**
 * Instance of the {@link CalibrationBean} for editing
 * {@link CalculationCoefficient}s.
 */
@ManagedBean
@SessionScoped
public class CalculationCoefficientsBean extends CalibrationBean {

  /**
   * The navigation string for the external standards list.
   */
  private static final String NAV_LIST = "calculation_coefficients";

  /**
   * Empty constructor.
   *
   * <p>
   * Required for JUnit tests.
   * </p>
   */
  public CalculationCoefficientsBean() {
    super();
  }

  @Override
  protected String getPageNavigation() {
    return NAV_LIST;
  }

  @Override
  protected Class<? extends Job> getReprocessJobClass() {
    return LocateMeasurementsJob.class;
  }

  @Override
  protected CalibrationDB getDbInstance() {
    return CalculationCoefficientDB.getInstance();
  }

  @Override
  protected String getCalibrationType() {
    return CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE;
  }

  @Override
  public String getHumanReadableCalibrationType() {
    return "Calculation Coefficient";
  }

  @Override
  protected Calibration initNewCalibration(long id, LocalDateTime date) {
    return new CalculationCoefficient(getCurrentInstrument(), id, date);
  }

  @Override
  public String getTargetLabel() {
    return "Coefficient";
  }

  @Override
  public String getCalibrationName() {
    return "Coefficient";
  }

  @Override
  protected boolean changeAffectsDatasetsAfterOnly() {
    return false;
  }
}
