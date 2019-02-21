package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;
import java.util.TreeMap;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling calibrations
 * @author Steve Jones
 *
 */
public abstract class CalibrationBean extends BaseManagedBean {

  /**
   * The database ID of the current instrument
   */
  protected long instrumentId;

  /**
   * The name of the current instrument
   */
  private String instrumentName;

  /**
   * The currently defined calibrations
   */
  private TreeMap<String, List<Calibration>> calibrations = null;

  /**
   * The newly entered calibration
   */
  private Calibration newCalibration;

  /**
   * Empty constructor
   */
  public CalibrationBean() {
  }

  /**
   * Initialise the bean
   * @return The navigation string
   */
  public String start() {
    String nav = getPageNavigation();

    boolean ok = true;

    // Get the instrument ID
    if (instrumentId == 0) {
      nav = internalError(new MissingParamException("instrumentId"));
      ok = false;
    }

    if (ok) {
      if (null == instrumentName || instrumentName.trim().length() == 0) {
        nav = internalError(new MissingParamException("instrumentName"));
        ok = false;
      }
    }

    if (ok) {
      try {
        loadCalibrations();
        newCalibration = initNewCalibration();
      } catch (Exception e) {
        nav = internalError(e);
      }
    }

    return nav;
  }

  /**
   * Get the instrument's database ID
   * @return The instrument ID
   */
  public long getInstrumentId() {
    return instrumentId;
  }

  /**
   * Set the database ID of the instrument
   * @param instrumentId The instrument ID
   */
  public void setInstrumentId(long instrumentId) {
    this.instrumentId = instrumentId;
  }

  /**
   * Get the instrument name
   * @return The instrument name
   */
  public String getInstrumentName() {
    return instrumentName;
  }

  /**
   * Set the instrument name
   * @param instrumentName The instrument name
   */
  public void setInstrumentName(String instrumentName) {
    this.instrumentName = instrumentName;
  }

  /**
   * Get the navigation string that will navigate to
   * the list of calibrations
   * @return The list navigation string
   */
  protected abstract String getPageNavigation();

  /**
   * Get a list of all possible targets for the calibration type
   * @return The targets
   * @throws Exception If the list of targets cannot be retrieved
   */
  public List<String> getTargets() throws Exception {
    return getDbInstance().getTargets(getDataSource(), instrumentId);
  };

  /**
   * Store the entered calibration in the database
   * @return The navigation
   */
  public String addCalibration() {
    // Null means we go back to the page we came from.
    // Will be overridden if there's an error
    String nav = null;

    try {
      if (getDbInstance().calibrationExists(getDataSource(), getNewCalibration())) {
        setMessage(null, "A calibration already exists for this standard at this time");
      } else {
        getDbInstance().addCalibration(getDataSource(), getNewCalibration());
        loadCalibrations();
        newCalibration = initNewCalibration();
      }
    } catch (Exception e) {
      nav = internalError(e);
    }

    return nav;
  }

  /**
   * Get an instance of the database interaction class for
   * the calibrations
   * @return The database interaction instance
   */
  protected abstract CalibrationDB getDbInstance();

  /**
   * Load the most recent calibrations from the database
   * @throws RecordNotFoundException If any required database records are missing
   * @throws DatabaseException If a database error occurs
   * @throws CalibrationException If the calibrations are internally inconsistent
   * @throws MissingParamException If any internal calls are missing required parameters
   */
  private void loadCalibrations() throws MissingParamException, CalibrationException, DatabaseException, RecordNotFoundException {
    calibrations = getDbInstance().getCalibrations(getDataSource(), instrumentId);
  }

  /**
   * Get the calibration type for the calibrations being edited
   * @return The calibration type
   */
  protected abstract String getCalibrationType();

  /**
   * Get the human-readable calibration type for the calibrations being edited
   * @return The human-readable calibration type
   */
  public abstract String getHumanReadableCalibrationType();

  /**
   * Individual targets are represented as groups on the page.
   * Get the JSON for these groups
   * @return The targets JSON
   */
  public String getTargetsJson() {
    JSONArray groups = new JSONArray();

    int counter = 0;

    for (String target : calibrations.keySet()) {
      JSONObject group = new JSONObject();
      group.put("id", counter);
      group.put("order", counter);
      group.put("content", target);

      groups.put(group);
      counter++;
    }

    return groups.toString();
  }

  /**
   * Generate a new, empty calibration
   */
  protected abstract Calibration initNewCalibration();

  /**
   * Get the JSON for the individual calibrations
   * @return The calibrations JSON
   */
  public String getCalibrationsJson() {
    JSONArray items = new JSONArray();

    int groupId = 0;
    for (String key: calibrations.keySet()) {

      for (Calibration calibration : calibrations.get(key)) {
        JSONObject calibrationJson = new JSONObject();
        calibrationJson.put("type", "box");
        calibrationJson.put("group", groupId);
        calibrationJson.put("start", DateTimeUtils.toJsonDate(calibration.getDeploymentDate()));
        calibrationJson.put("content", calibration.getHumanReadableCoefficients());
        calibrationJson.put("title", calibration.getHumanReadableCoefficients());

        items.put(calibrationJson);
      }

      groupId++;
    }

    return items.toString();
  }

  /**
   * Get the new calibration deployment details
   * @return The new calibration
   */
  public Calibration getNewCalibration() {
    return newCalibration;
  }

  /**
   * Get the label to use for the calibration target
   * @return The target label
   */
  public abstract String getTargetLabel();

  /**
   * Get the label used to describe the coefficients
   * @return The coefficients label
   */
  public String getCoefficientsLabel() {
    return "Coefficients";
  }
}
