package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for transient instrument operations, e.g. listing instruments.
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

  private Map<Long, Integer> datasetCounts;

  /**
   * The ID of the instrument chosen from the instrument list
   */
  private Instrument chosenInstrument = null;

  public String start() {
    try {
      datasetCounts = DataSetDB.getDataSetCounts(getDataSource(),
        getInstruments().stream().map(i -> i.getId()).toList());
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      internalError(e);
    }
    return PAGE_INSTRUMENT_LIST;
  }

  /**
   * Returns a list of the instruments owned by the current user
   *
   * @return The instruments owned by the current user
   */
  public List<Instrument> getInstrumentList() {
    return getInstruments();
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
    for (Instrument instrument : getInstruments()) {
      if (instrument.getId() == chosenInstrument) {
        this.chosenInstrument = instrument;
        setCurrentInstrumentId(instrument.getId());
        break;
      }
    }
  }

  public boolean hasDatasets(long instrumentId) {
    return datasetCounts.containsKey(instrumentId)
      && datasetCounts.get(instrumentId) > 0;
  }

  public void deleteInstrument() {
    try {
      InstrumentDB.deleteInstrument(getDataSource(), chosenInstrument.getId());
      initialiseInstruments();
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      internalError(e);
    }

    // We return to the instrument list page
  }
}
