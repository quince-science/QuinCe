package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationFactory;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationDateException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationTargetException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.ParameterException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling calibrations
 *
 * @author Steve Jones
 *
 */
public abstract class CalibrationBean extends BaseManagedBean {

  /**
   * Code for the action to add a new calibration
   */
  public static final int ADD_ACTION = 1;

  /**
   * Code for the action to edit a calibration
   */
  public static final int EDIT_ACTION = 0;

  /**
   * Code for the action to delete a calibration
   */
  public static final int DELETE_ACTION = -1;

  /**
   * Status flag indicating that no {@link DataSet}s are affected by the current
   * edit.
   *
   * @see #getAffectedDatasetsStatus()
   */
  private static final int NO_AFFECTED_DATASETS = 0;

  /**
   * Status flag indicating that some {@link DataSet}s are affected by the
   * current edit, and they can all be reprocessed.
   *
   * @see #getAffectedDatasetsStatus()
   */
  private static final int AFFECTED_DATASETS_OK = 1;

  /**
   * Status flag indicating that some {@link DataSet}s are affected by the
   * current edit, but at least one cannot be reprocessed.
   *
   * @see #getAffectedDatasetsStatus()
   */
  private static final int AFFECTED_DATASETS_FAIL = -1;

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
   * The calibration edit action
   */
  private int editAction = EDIT_ACTION;

  /**
   * The {@link DataSet}s that will be affected by the current edit action
   */
  private Map<DataSet, Boolean> affectedDatasets = null;

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
        datasets = DataSetDB.getDataSets(getDataSource(), instrumentId, true);
        dbInstance = getDbInstance();
        loadCalibrations();
        affectedDatasets = null;
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
  public Map<String, String> getTargets() {
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
      switch (editAction) {
      case ADD_ACTION: {
        addCalibration();
        break;
      }
      case EDIT_ACTION: {
        updateCalibration();
        break;
      }
      case DELETE_ACTION: {
        deleteCalibration();
        break;
      }
      default: {
        throw new Exception("Unrecognised action " + editAction);
      }
      }

      loadCalibrations();
      calibration = initNewCalibration();

      // Trigger reprocessing for all affected datasets
      if (null != affectedDatasets) {
        for (DataSet dataSet : affectedDatasets.keySet()) {
          Class<? extends Job> reprocessJobClass = getReprocessJobClass();

          DataSetDB.setDatasetStatus(getDataSource(), dataSet.getId(),
            getReprocessStatus());
          Properties jobProperties = new Properties();

          // See GitHub Issue #1369
          jobProperties.setProperty(AutoQCJob.ID_PARAM,
            String.valueOf(dataSet.getId()));
          JobManager.addJob(getDataSource(), getUser(),
            reprocessJobClass.getCanonicalName(), jobProperties);

        }
      }
    } catch (Exception e) {
      nav = internalError(e);
    }

    return nav;
  }

  /**
   * Get the status to set on a {@link DataSet} that is being reprocessed.
   *
   * @return The new status.
   */
  protected abstract int getReprocessStatus();

  /**
   * Get the job class for reprocessing a {@link DataSet}.
   *
   * @return The reprocessing job class.
   */
  protected abstract Class<? extends Job> getReprocessJobClass();

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

  private void deleteCalibration()
    throws MissingParamException, DatabaseException {
    dbInstance.deleteCalibration(getDataSource(), calibration.getId());
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
      group.put("id", StringUtils.tabToSpace(target));
      group.put("order", counter);
      group.put("content", StringUtils.tabToSpace(getTargets().get(target)));

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

    for (String key : calibrations.keySet()) {

      for (Calibration calibration : calibrations.get(key)) {
        JSONObject calibrationJson = new JSONObject();
        calibrationJson.put("id", calibration.getId());
        calibrationJson.put("type", "box");
        calibrationJson.put("target", StringUtils.tabToSpace(key));
        calibrationJson.put("group", StringUtils.tabToSpace(key));
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

  public void loadSelectedCalibration() throws RecordNotFoundException {

    calibration = null;

    if (selectedCalibrationId == DatabaseUtils.NO_DATABASE_RECORD) {
      calibration = initNewCalibration();
    } else {

      for (List<Calibration> calibs : calibrations.values()) {
        for (Calibration c : calibs) {
          if (c.getId() == selectedCalibrationId) {
            calibration = CalibrationFactory.clone(c);
            break;
          }
        }
      }

      // The calibration wasn't found
      if (null == calibration) {
        throw new RecordNotFoundException(instrumentName, "calibration",
          selectedCalibrationId);
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
   * @throws DatabaseException
   * @throws MissingParamException
   * @throws NonExistentCalibrationTargetException
   *           If the specified calibration target does not exist.
   */
  public void calcAffectedDataSets() throws InvalidCalibrationEditException,
    RecordNotFoundException, InvalidCalibrationTargetException,
    InvalidCalibrationDateException, MissingParamException, DatabaseException {

    // Get a new copy of the calibrations setup. We don't use the bean's copy
    // because this is a what-if method and we don't want the results to be
    // kept.
    TreeMap<String, List<Calibration>> testCalibrations = dbInstance
      .getCalibrations(getDataSource(), instrumentId);

    // Get the existing calibration to be edited, if required
    Calibration editedCalibration = null;

    // If we're editing an existing calibration, get it
    if (calibration.getId() > 0) {

      editedCalibration = getCalibration(calibration.getId());

      if (null == editedCalibration) {
        throw new RecordNotFoundException(
          "Calibration " + calibration.getId() + " not found");
      }
    }

    // Make sure all the parameters are present and valid
    checkEditedCalibration();

    Map<DataSet, Boolean> result = new HashMap<DataSet, Boolean>();

    // If we're editing an existing calibration...
    if (editAction != ADD_ACTION) {

      // Work out which datasets will be affected by removing the calibration
      // from its current location

      LocalDateTime[] surroundingCalibrations = getSurroundingCalibrations(
        testCalibrations.get(editedCalibration.getTarget()),
        editedCalibration.getDeploymentDate());

      // Get the datasets that are encompassed by the prev and next dates
      // If there's no previous date, includes all datasets before the next date
      // If there's no next date, includes all datasets after the start date
      // If neither date is set, includes all datasets
      List<DataSet> affectedDatasets = DataSetDB.getDatasetsBetweenDates(
        getDataSource(), instrumentId, surroundingCalibrations[0],
        surroundingCalibrations[1]);

      // Add them to the result. If there's no previous date, the boolean is
      // False because it can't be reprocessed
      for (DataSet dataSet : affectedDatasets) {
        boolean canBeReprocessed = true;
        if (dbInstance.priorCalibrationRequired()
          && null == surroundingCalibrations[0]) {

          canBeReprocessed = false;
        }

        result.put(dataSet, canBeReprocessed);
      }

      // Now remove the calibration from the test calibrations so the next
      // stage can see what happens when we add it in its new location
      testCalibrations.get(editedCalibration.getTarget())
        .remove(editedCalibration);

    }

    // If we're adding a new calibration (which includes editing an existing one
    // - it's treated as a delete then add)...
    if (editAction == EDIT_ACTION || editAction == ADD_ACTION) {

      // Get the calibrations either side of the new calibration date
      List<Calibration> newTargetCalibrations = testCalibrations
        .get(calibration.getTarget());
      if (null == newTargetCalibrations) {
        newTargetCalibrations = new ArrayList<Calibration>(0);
      }

      LocalDateTime[] surroundingCalibrations = getSurroundingCalibrations(
        newTargetCalibrations, calibration.getDeploymentDate());

      // Get the datasets covered by the before/after calbration range
      List<DataSet> affectedDatasets = DataSetDB.getDatasetsBetweenDates(
        getDataSource(), instrumentId, surroundingCalibrations[0],
        surroundingCalibrations[1]);

      // Add them to the result.
      for (DataSet dataSet : affectedDatasets) {

        boolean canBeReprocessed = true;

        // If (a) prior calibrations are required, (b) the new calibration is
        // not before the dataset, and (c) there are no other calibrations
        // before the dataset, it can't be reprocessed.
        if (dbInstance.priorCalibrationRequired()
          && dataSet.getStart().isBefore(calibration.getDeploymentDate())
          && (null == testCalibrations.get(calibration.getTarget())
            || testCalibrations.get(calibration.getTarget()).size() == 0
            || !testCalibrations.get(calibration.getTarget()).get(0)
              .getDeploymentDate().isBefore(dataSet.getStart()))) {

          canBeReprocessed = false;
        }

        // If the dataset is already flagged as affected, then we need to see
        // if the target has been changed. If so, then a False canBeReprocessed
        // flag will always override the flag we've just calculated.
        if (null != editedCalibration
          && !editedCalibration.getTarget().equals(calibration.getTarget())) {

          if (result.containsKey(dataSet) && result.get(dataSet) == false) {
            canBeReprocessed = false;
          }

        }

        result.put(dataSet, canBeReprocessed);
      }
    }

    this.affectedDatasets = result;

    // Set or clear the "cannot be reprocessed" message
    if (getAffectedDatasetsStatus() < 0) {
      setMessage(getComponentID("affectedDatasets"),
        "This change cannot be completed because some datasets cannot be reprocessed.");
    }
  }

  /**
   * Get the {@link DataSet}s that will be affected by the current edit action,
   * along with a flag specifying whether each {@link DataSet} can be
   * recalculated once the edit is complete.
   *
   * @return The affected {@link DataSet}s.
   */
  public Map<DataSet, Boolean> getAffectedDatasets() {
    return affectedDatasets;
  }

  /**
   * Dummy method allowing the front end to call a {@code set} method. Does
   * nothing.
   *
   * <p>
   * This only exists because the front end form has a lot of dynamically
   * generated inputs that can't be referenced by name, so the whole form has to
   * be submitted.
   * </p>
   *
   * @param count
   *          The count (ignored)
   */
  public void setAffectedDatasetsStatus(int count) {
    ; // Do nothing
  }

  /**
   * Get the status of the {@link DataSet}s affected by the current edit.
   * Returns one of:
   *
   * <ul>
   * <li>{@link #NO_AFFECTED_DATASETS}: No datasets are affected.</li>
   * <li>{@link #AFFECTED_DATASETS_OK}: Some datasets are affected and all can
   * be reprocessed.</li>
   * <li>{@link #AFFECTED_DATASETS_FAIL}: Some datasets are affected but at
   * least one cannot be reprocessed.</li>
   * </ul>
   *
   *
   * @return The status of the affected {@link DataSet}s.
   */
  public int getAffectedDatasetsStatus() {

    int result = NO_AFFECTED_DATASETS;

    if (null != affectedDatasets && affectedDatasets.size() > 0) {
      result = AFFECTED_DATASETS_OK;
      for (Boolean datasetOk : affectedDatasets.values()) {
        if (!datasetOk) {
          result = AFFECTED_DATASETS_FAIL;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get the calibrations before and after a given date from a list of
   * calibrations.
   *
   * <p>
   * Returns a two-element array of {@code [before, after]}. If there are no
   * calibrations before or after the given date, the corresponding array entry
   * will be {@code null}.
   * </p>
   *
   * <p>
   * The date searches are exclusive, so a date that equals the {@code baseDate}
   * will not be included in the output.
   * </p>
   *
   * @param searchCalibrations
   *          The calibrations to search.
   * @param baseDate
   *          The date on which to base the search.
   * @return The preceding and following calibration dates.
   */
  private LocalDateTime[] getSurroundingCalibrations(
    List<Calibration> searchCalibrations, LocalDateTime baseDate) {

    // Get the dates of the previous and next calibrations for the same
    // target, if they exist
    LocalDateTime before = null;
    LocalDateTime after = null;

    for (int i = 0; i < searchCalibrations.size(); i++) {

      LocalDateTime calibrationTime = searchCalibrations.get(i)
        .getDeploymentDate();

      if (calibrationTime.isBefore(baseDate)) {
        before = calibrationTime;
      } else if (calibrationTime.isAfter(baseDate)) {
        after = calibrationTime;
        break; // We've found everything we need so stop
      }
    }

    return new LocalDateTime[] { before, after };
  }

  /**
   * Check that the parameters passed to
   * {@link #getAffectedDataSets(long, LocalDateTime, String)} are valid.
   *
   * @param editedCalibrationId
   *          The edited calibration ID
   * @param newTime
   *          The new calibration time
   * @param newTarget
   *          The new calibration target
   * @throws RecordNotFoundException
   *           If the edited calibration does not exist
   * @throws InvalidCalibrationEditException
   *           If the new calibration details are invalid.
   */
  private void checkEditedCalibration()
    throws RecordNotFoundException, InvalidCalibrationEditException {

    // A zero calibration ID is invalid
    if (calibration.getId() == 0) {
      throw new RecordNotFoundException("Invalid calibration ID", "calibration",
        calibration.getId());
    }

    // Cannot have only one of newTime and newTarget set
    if (null == calibration.getDeploymentDate()
      && null != calibration.getTarget()) {
      throw new InvalidCalibrationEditException("Missing time");
    }

    if (null != calibration.getDeploymentDate()
      && null == calibration.getTarget()) {
      throw new InvalidCalibrationEditException("Missing target");
    }

    // Future dates are not allowed
    LocalDateTime now = LocalDateTime.ofInstant(Instant.now(),
      ZoneId.of("UTC"));
    if (null != calibration.getDeploymentDate()
      && calibration.getDeploymentDate().isAfter(now)) {
      throw new InvalidCalibrationDateException();
    }

    // Invalid targets are not allowed
    if (null != calibration.getTarget()
      && !calibrationTargets.containsKey(calibration.getTarget())) {
      throw new InvalidCalibrationTargetException(calibration.getTarget());
    }

    // Cannot have null time and target (implying deleting a calibration) with a
    // negative ID
    if (null == calibration.getDeploymentDate()
      && null == calibration.getTarget() && calibration.getId() < 0) {
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

  @Override
  public String getFormName() {
    return "deploymentForm";
  }

  public String getCalibrationName() {
    return "Calibration";
  }

  public int getEditAction() {
    return editAction;
  }

  public void setEditAction(int editAction) {
    this.editAction = editAction;
  }

  public String getCalibrationTargetName() {
    String result = null;

    if (null != calibration && null != calibration.getTarget()) {
      result = getTargets().get(calibration.getTarget());
    }

    return result;
  }
}
