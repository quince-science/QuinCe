package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationDateException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationTargetException;
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
   * The datasets defined for the instrument.
   *
   * <p>
   * These are shown in the timeline and used to determine which datasets are
   * affected by an edit.
   * </p>
   */
  private List<DataSet> datasets;

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
        datasets = DataSetDB.getDataSets(getDataSource(), instrumentId);
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

    JSONObject group = new JSONObject();
    group.put("id", "Datasets");
    group.put("order", counter);
    group.put("content", "Datasets");
    groups.put(group);

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

    // Add the datasets
    for (DataSet dataset : datasets) {
      JSONObject datasetJson = new JSONObject();
      datasetJson.put("id", getTimelineId(dataset));
      datasetJson.put("type", "range");
      datasetJson.put("group", "Datasets");
      datasetJson.put("start", DateTimeUtils.toIsoDate(dataset.getStart()));
      datasetJson.put("end", DateTimeUtils.toIsoDate(dataset.getEnd()));
      datasetJson.put("content", dataset.getName());
      datasetJson.put("title", dataset.getName());
      items.put(datasetJson);
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

  /**
   * Generate the Timeline ID for a dataset.
   *
   * <p>
   * We can't simply use the dataset's ID because that may clash with a
   * calibration's ID.
   * </p>
   *
   * @param dataset
   *          The dataset whose ID is to be generated
   * @return The dataset's timeline ID
   */
  private String getTimelineId(DataSet dataset) {
    return "DS-" + dataset.getId();
  }

  /**
   * Determine which {@link DataSet}s will be affected by editing a given
   * calibration.
   *
   * <p>
   * The result of the method will be a map of affected {@link DataSet}s with
   * {@code boolean}s indicating whether or not that {@link DataSet} can be
   * reprocessed. If, for example, a {@link DataSet} is left without a leading
   * calibration before it, then there is no way to apply a calibration and
   * therefore it cannot be reprocessed.
   * </p>
   *
   * <p>
   * The calibration being edited is specified by its database ID in
   * {@code editedCalibration}. If a new calibration is being created, this
   * should be negative.
   * </p>
   *
   * <p>
   * The new calibration details are given by its time and target. If only the
   * coefficients for the calibration have changed, there will still be affected
   * {@link DataSet}s. However, passing the (unchanged) {@code newTime} and
   * {@code newTarget} will still cause the method to perform the correct
   * checks. If the {@code newTime} and {@code newTarget} are both {@code null},
   * the method will assume that the specified calibration is to be deleted.
   * </p>
   *
   * <p>
   * If one of {@code newTime} or {@code newTarget} but not the other, an
   * Exception will be thrown. If {@code newTime} and {@code newTarget} are both
   * {@code null} (implying a deleted calibration) but the
   * {@code editCalibration} is negative, an exception will be thrown.
   * </p>
   *
   * @param editedCalibrationId
   *          The database ID of the calibration being edited; negative if this
   *          is a new calibration.
   * @param newTime
   *          The new calibration time.
   * @param newTarget
   *          The new calibration target.
   * @return The affected {@link DataSet}s.
   * @throws InvalidCalibrationEditException
   *           If the specified calibration details are invalid.
   * @throws RecordNotFoundException
   *           If the specified calibration does not exist.
   * @throws NonExistentCalibrationTargetException
   *           If the specified calibration target does not exist.
   */
  public Map<DataSet, Boolean> getAffectedDataSets(long editedCalibrationId,
    LocalDateTime newTime, String newTarget)
    throws InvalidCalibrationEditException, RecordNotFoundException,
    InvalidCalibrationTargetException, InvalidCalibrationDateException {

    checkAffectedDataSetsParameters(editedCalibrationId, newTime, newTarget);

    Calibration editedCalibration = null;

    if (editedCalibrationId > 0) {
      editedCalibration = getCalibration(editedCalibrationId);

      if (null == editedCalibration) {
        throw new RecordNotFoundException(
          "Calibration " + editedCalibrationId + " not found");
      }
    }

    return null;
  }

  private void checkAffectedDataSetsParameters(long editedCalibrationId,
    LocalDateTime newTime, String newTarget)
    throws RecordNotFoundException, InvalidCalibrationEditException {

    // A zero calibration ID is invalid
    if (editedCalibrationId == 0) {
      throw new RecordNotFoundException("Invalid calibration ID", "calibration",
        editedCalibrationId);
    }

    // Cannot have only one of newTime and newTarget set
    if (null == newTime && null != newTarget) {
      throw new InvalidCalibrationEditException("Missing time");
    }

    if (null != newTime && null == newTarget) {
      throw new InvalidCalibrationEditException("Missing target");
    }

    // Future dates are not allowed
    LocalDateTime now = LocalDateTime.ofInstant(Instant.now(),
      ZoneId.of("UTC"));
    if (null != newTime && newTime.isAfter(now)) {
      throw new InvalidCalibrationDateException();
    }

    // Invalid targets are not allowed
    if (null != newTarget && !calibrationTargets.containsValue(newTarget)) {
      throw new InvalidCalibrationTargetException(newTarget);
    }

    // Cannot have null time and target (implying deleting a calibration) with a
    // negative ID
    if (null == newTime && null == newTarget && editedCalibrationId < 0) {
      throw new InvalidCalibrationEditException(
        "Cannot delete a calbration without an ID");
    }
  }

  /**
   * Find a calibration using its database ID. Returns {@code null} if the
   * calibration is not found.
   *
   * @param calibrationId
   *          The calbration's ID
   * @return The calibration
   */
  private Calibration getCalibration(long calibrationId) {
    Calibration result = null;

    for (List<Calibration> calibrations : calibrations.values()) {
      for (Calibration calibration : calibrations) {
        if (calibration.getId() == calibrationId) {
          result = calibration;
          break;
        }
      }
    }

    return result;

  }
}
