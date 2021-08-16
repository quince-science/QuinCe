package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for transient instrument operations, e.g. listing instruments.
 *
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class InstrumentListBean extends BaseManagedBean {

  /**
   * The name of the session attribute that holds the ID of the currently
   * selected instrument
   */
  public static final String ATTR_CURRENT_INSTRUMENT = "currentInstrument";

  /**
   * The navigation for the calibrations page
   */
  public static final String PAGE_CALIBRATIONS = "calibrations";

  /**
   * The navigation for the standards page
   */
  public static final String PAGE_STANDARDS = "standards";

  /**
   * The navigation to the instrument list
   */
  protected static final String PAGE_INSTRUMENT_LIST = "instrument_list";

  /**
   * The list of instruments
   */
  private List<Instrument> instrumentList = null;

  /**
   * The ID of the instrument chosen from the instrument list
   */
  private Instrument chosenInstrument = null;

  /**
   * Initialises the bean by pre-loading the list of instruments
   */
  public void init() {
    // Load the instrument list
    try {
      instrumentList = InstrumentDB
        .getInstrumentList(ServletUtils.getDBDataSource(), getUser());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Returns a list of the instruments owned by the current user
   *
   * @return The instruments owned by the current user
   */
  public List<Instrument> getInstrumentList() {
    if (null == instrumentList) {
      init();
    }

    return instrumentList;
  }

  /**
   * View the calibrations list page for the chosen instrument
   *
   * @return The calibrations list page navigation
   */
  public String viewCalibrations() {
    return PAGE_CALIBRATIONS;
  }

  /**
   * View the external standards list page for the chosen instrument
   *
   * @return The external standards list page navigation
   */
  public String viewStandards() {
    return PAGE_STANDARDS;
  }

  /**
   * Returns to the instrument list
   *
   * @return The navigation string for the instrument list
   */
  public String viewInstrumentList() {
    return InstrumentListBean.PAGE_INSTRUMENT_LIST;
  }

  ///////////////// *** GETTERS AND SETTERS *** ///////////////

  /**
   * Returns the ID of the instrument chosen from the instrument list
   *
   * @return The instrument ID
   */
  public long getChosenInstrument() {
    long result = -1;

    if (null != chosenInstrument) {
      result = chosenInstrument.getId();
    }

    return result;
  }

  /**
   * Sets the ID of the instrument chosen from the instrument list
   *
   * @param chosenInstrument
   *          The instrument ID
   */
  public void setChosenInstrument(long chosenInstrument) {
    for (Instrument instrument : instrumentList) {
      if (instrument.getId() == chosenInstrument) {
        this.chosenInstrument = instrument;
        setCurrentInstrumentId(instrument.getId());
        break;
      }
    }
  }
}
