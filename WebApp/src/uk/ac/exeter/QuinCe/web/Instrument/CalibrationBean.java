package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.ParameterException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling calibrations
 *
 * @author Steve Jones
 *
 */
public abstract class CalibrationBean extends BaseManagedBean {

  /**
   * The database ID of the calibration currently being edited
   *
   * <p>
   * For new calibrations, this will be
   * {@link DatabaseUtils#NO_DATABASE_RECORD}.
   * </p>
   */
  private long selectedCalibrationId = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The database ID of the current instrument
   */
  protected long instrumentId;

  /**
   * The name of the current instrument
   */
  private String instrumentName;

  /**
   * The calibration database handler
   */
  private CalibrationDB dbInstance = null;

  /**
   * The currently defined calibrations
   */
  private TreeMap<String, List<Calibration>> calibrations = null;

  /**
   * The calibration target names lookup
   */
  private Map<String, String> calibrationTargets = null;

  /**
   * The newly entered calibration
   */
  private Calibration calibration;

  /**
   * Empty constructor
   */
  public CalibrationBean() {
  }

  /**
   * Initialise the bean
   *
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
        dbInstance = getDbInstance();
        loadCalibrations();
        calibration = initNewCalibration();
      } catch (Exception e) {
        nav = internalError(e);
      }
    }

    return nav;
  }

  /**
   * Get the instrument's database ID
   *
   * @return The instrument ID
   */
  public long getInstrumentId() {
    return instrumentId;
  }

  /**
   * Set the database ID of the instrument
   *
   * @param instrumentId
   *          The instrument ID
   */
  public void setInstrumentId(long instrumentId) {
    this.instrumentId = instrumentId;
  }

  /**
   * Get the instrument name
   *
   * @return The instrument name
   */
  public String getInstrumentName() {
    return instrumentName;
  }

  /**
   * Set the instrument name
   *
   * @param instrumentName
   *          The instrument name
   */
  public void setInstrumentName(String instrumentName) {
    this.instrumentName = instrumentName;
  }

  /**
   * Get the navigation string that will navigate to the list of calibrations
   *
   * @return The list navigation string
   */
  protected abstract String getPageNavigation();

  /**
   * Get a list of all possible targets for the calibration type
   *
   * @return The targets
   * @throws Exception
   *           If the list of targets cannot be retrieved
   */
  public Map<String, String> getTargets() throws Exception {
    return calibrationTargets;
  };

  /**
   * Store the entered calibration in the database
   *
   * @return The navigation
   */
  public String saveCalibration() throws Exception {
    // Null means we go back to the page we came from.
    // Will be overridden if there's an error
    String nav = null;

    try {
      if (calibration.getId() == DatabaseUtils.NO_DATABASE_RECORD) {
        addCalibration();
      } else {
        updateCalibration();
      }

      loadCalibrations();
      calibration = initNewCalibration();
    } catch (Exception e) {
      nav = internalError(e);
    }

    return nav;
  }

  /**
   * Add a new calibration
   *
   * @return The navigation string
   * @throws ParameterException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  private void addCalibration()
    throws MissingParamException, DatabaseException, ParameterException {
    if (dbInstance.calibrationExists(getDataSource(), calibration)) {
      setMessage(null,
        "A calibration already exists for this standard at this time");
    } else {
      dbInstance.addCalibration(getDataSource(), calibration);
    }
  }

  private void updateCalibration()
    throws MissingParamException, DatabaseException, ParameterException {

    if (dbInstance.calibrationExists(getDataSource(), calibration)) {
      setMessage(null,
        "A calibration already exists for this standard at this time");
    } else {
      dbInstance.updateCalibration(getDataSource(), calibration);
    }
  }

  /**
   * Get an instance of the database interaction class for the calibrations
   *
   * @return The database interaction instance
   */
  protected abstract CalibrationDB getDbInstance();

  /**
   * Load the most recent calibrations from the database
   *
   * @throws RecordNotFoundException
   *           If any required database records are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws CalibrationException
   *           If the calibrations are internally inconsistent
   * @throws MissingParamException
   *           If any internal calls are missing required parameters
   * @throws InstrumentException
   */
  private void loadCalibrations()
    throws MissingParamException, CalibrationException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    calibrations = dbInstance.getCalibrations(getDataSource(), instrumentId);
    calibrationTargets = dbInstance.getTargets(getDataSource(), instrumentId);
  }

  /**
   * Get the calibration type for the calibrations being edited
   *
   * @return The calibration type
   */
  protected abstract String getCalibrationType();

  /**
   * Get the human-readable calibration type for the calibrations being edited
   *
   * @return The human-readable calibration type
   */
  public abstract String getHumanReadableCalibrationType();

  /**
   * Individual targets are represented as groups on the page. Get the JSON for
   * these groups
   *
   * @return The targets JSON
   */
  public String getUsedTargetsJson() throws Exception {
    JSONArray groups = new JSONArray();

    int counter = 0;

    for (String target : calibrations.keySet()) {
      JSONObject group = new JSONObject();
      group.put("id", counter);
      group.put("order", counter);
      group.put("content", getTargets().get(target));

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
   *
   * @return The calibrations JSON
   */
  public String getCalibrationsJson() {
    JSONArray items = new JSONArray();

    int groupId = 0;
    for (String key : calibrations.keySet()) {

      for (Calibration calibration : calibrations.get(key)) {
        JSONObject calibrationJson = new JSONObject();
        calibrationJson.put("id", calibration.getId());
        calibrationJson.put("type", "box");
        calibrationJson.put("target", key);
        calibrationJson.put("group", groupId);
        calibrationJson.put("start",
          DateTimeUtils.toIsoDate(calibration.getDeploymentDate()));
        calibrationJson.put("content",
          calibration.getHumanReadableCoefficients());
        calibrationJson.put("title",
          calibration.getHumanReadableCoefficients());

        JSONArray coefficients = new JSONArray();
        for (CalibrationCoefficient coefficient : calibration
          .getCoefficients()) {

          coefficients.put(coefficient.getValue());
        }
        calibrationJson.put("coefficients", coefficients);

        items.put(calibrationJson);
      }

      groupId++;
    }

    return items.toString();
  }

  /**
   * Get the label to use for the calibration target
   *
   * @return The target label
   */
  public abstract String getTargetLabel();

  /**
   * Get the label used to describe the coefficients
   *
   * @return The coefficients label
   */
  public String getCoefficientsLabel() {
    return "Coefficients";
  }

  public Calibration getCalibration() {
    return calibration;
  }

  public long getSelectedCalibrationId() {
    return selectedCalibrationId;
  }

  public void setSelectedCalibrationId(long selectedCalibrationId) {
    this.selectedCalibrationId = selectedCalibrationId;
  }

  public void loadSelectedCalibration() {
    if (selectedCalibrationId == DatabaseUtils.NO_DATABASE_RECORD) {
      calibration = initNewCalibration();
    } else {

      for (List<Calibration> calibs : calibrations.values()) {
        for (Calibration c : calibs) {
          if (c.getId() == selectedCalibrationId) {
            calibration = c;
            break;
          }
        }
      }
    }
  }
}
