package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.lang3.StringUtils;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
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

  private static final int SHARE_REMOVE = -1;

  private static final int SHARE_ADD = 1;

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

  private List<BeanInstrument> filteredInstruments;

  private LinkedHashMap<Long, String> userNames;

  /**
   * The ID of the instrument whose ownership is being edited.
   *
   * <p>
   * Note that this is <i>not</i> the ID of the owner.
   * </p>
   */
  private long ownershipInstrId = -1L;

  /**
   * The ID of the instrument chosen from the instrument list
   */
  private BeanInstrument chosenInstrument = null;

  private int shareAction = Integer.MIN_VALUE;

  private String shareEmail = null;

  private long shareId = Long.MIN_VALUE;

  public String start() {
    try {
      userNames = UserDB.getUserNames(getDataSource());
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
  public List<BeanInstrument> getInstrumentList() {
    return getInstruments().stream().map(i -> new BeanInstrument(i, getUser()))
      .toList();
  }

  public List<BeanInstrument> getFilteredInstruments() {
    return filteredInstruments;
  }

  public void setFilteredInstruments(List<BeanInstrument> filteredInstruments) {
    this.filteredInstruments = filteredInstruments;
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
    for (BeanInstrument instrument : getInstrumentList()) {
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

  /**
   * Filter method for the DataTable.
   *
   * @param value
   * @param filter
   * @param locale
   * @return
   */
  public boolean filter(Object value, Object filter, Locale locale) {
    String filterText = (filter == null) ? ""
      : filter.toString().trim().toLowerCase();

    if (filterText.length() == 0) {
      return true;
    }

    Instrument instrument = (Instrument) value;
    return instrument.getOwner().getSurname().toLowerCase().contains(filterText)
      || instrument.getOwner().getGivenName().toLowerCase().contains(filterText)
      || instrument.getPlatformName().toLowerCase().contains(filterText)
      || instrument.getName().toLowerCase().contains(filterText);
  }

  public String getOwnerName() {
    return ownershipInstrId == -1L ? ""
      : userNames
        .get(getInstrument(ownershipInstrId).getOwner().getDatabaseID());
  }

  public long getOwnershipInstrId() {
    return ownershipInstrId;
  }

  public void setOwnershipInstrId(long id) {
    ownershipInstrId = id;
  }

  public String getOwnerInstrumentName() {
    return ownershipInstrId == -1L ? ""
      : getInstrument(ownershipInstrId).getDisplayName();
  }

  public List<UserTableEntry> getSharedUsers() {
    return ownershipInstrId == -1L ? new ArrayList<UserTableEntry>()
      : getInstrument(ownershipInstrId).getSharedWith().stream()
        .map(id -> new UserTableEntry(id, userNames.get(id))).toList();
  }

  public void setShareAction(int action) {
    this.shareAction = action;
  }

  public int getShareAction() {
    return shareAction;
  }

  public String getShareEmail() {
    return shareEmail;
  }

  public void setShareEmail(String email) {
    this.shareEmail = email;
  }

  public void saveShare() {

    ajaxOK = true;

    try {
      switch (shareAction) {
      case SHARE_ADD: {
        if (StringUtils.isEmpty(shareEmail)) {
          setMessage("shareForm:shareEmail", "An email address is required");
          ajaxOK = false;
        } else {

          User shareUser = UserDB.getUser(getDataSource(), shareEmail);

          if (null == shareUser) {
            setMessage("shareForm:shareEmail",
              "There is no user with the specified email address");
            ajaxOK = false;
          } else {
            InstrumentDB.addUserShare(getDataSource(),
              getInstrument(ownershipInstrId), shareUser);
            break;
          }
        }
      }
      case SHARE_REMOVE: {
        User shareUser = UserDB.getUser(getDataSource(), shareId);
        InstrumentDB.removeUserShare(getDataSource(),
          getInstrument(ownershipInstrId), shareUser);
        break;
      }
      default: {
        throw new IllegalArgumentException("Invalid share action");
      }
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      setMessage("shareForm:shareEmail", "Error adding share");
      ajaxOK = false;
    }
  }

  public void setShareId(long id) {
    this.shareId = id;
  }

  public long getShareId() {
    return shareId;
  }

  public String transferOwnership() {

    try {
      User newOwner = UserDB.getUser(getDataSource(), shareId);
      if (null != newOwner) {
        InstrumentDB.setOwner(getDataSource(), getInstrument(ownershipInstrId),
          newOwner);
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }

    return PAGE_INSTRUMENT_LIST;
  }

  @Override
  public String getFormName() {
    return "instrumentListForm";
  }

  public record UserTableEntry(long id, String name) {

    public long getId() {
      return id();
    }

    public String getName() {
      return name();
    }
  }
}
