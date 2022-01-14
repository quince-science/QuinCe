package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationDateException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationTargetException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
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
   * The instrument whose calibrations are being edited
   */
  protected Instrument instrument;

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
  private TreeMap<DataSet, Boolean> affectedDatasets = null;

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
    if (null == instrument) {
      nav = internalError(new MissingParamException("instrumentId"));
      ok = false;
    }

    if (ok) {
      try {
        datasets = DataSetDB.getDataSets(getDataSource(), instrument.getId(),
          true);
        dbInstance = getDbInstance();
        loadCalibrations();
        affectedDatasets = null;
        calibration = initNewCalibration();
      } catch (Exception e) {
        e.printStackTrace();
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
    return (null == instrument ? -1 : instrument.getId());
  }

  /**
   * Set the database ID of the instrument
   *
   * @param instrumentId
   *          The instrument ID
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public void setInstrumentId(long instrumentId) throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    if (instrumentId > 0) {
      this.instrument = InstrumentDB.getInstrument(getDataSource(),
        instrumentId);
    }
  }

  /**
   * Get the instrument name
   *
   * @return The instrument name
   */
  public String getInstrumentName() {
    return (null == instrument ? null : instrument.getName());
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
   */
  public Map<String, String> getTargets() {
    return calibrationTargets;
  };

  /**
   * Store the entered calibration in the database.
   *
   * @return The navigation.
   * @throws InvalidCalibrationEditException
   *           If the edit action is not recognised.
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
        throw new InvalidCalibrationEditException(
          "Unrecognised action " + editAction);
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
  protected Class<? extends Job> getReprocessJobClass() {
    return ExtractDataSetJob.class;
  }

  /**
   * Store a new calibration in the database.
   *
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws ParameterException
   *           If any parameters are missing or invalid.
   */
  private void addCalibration() throws DatabaseException, ParameterException {
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
   * Load the most recent calibrations from the database.
   *
   * @throws RecordNotFoundException
   *           If any required database records are missing.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws CalibrationException
   *           If the calibrations are internally inconsistent.
   * @throws MissingParamException
   *           If any internal calls are missing required parameters.
   * @throws InstrumentException
   *           If the instrument calibration details cannot be retrieved.
   */
  private void loadCalibrations()
    throws MissingParamException, CalibrationException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    calibrationTargets = dbInstance.getTargets(getDataSource(), instrument);
  }

  private TreeMap<String, List<Calibration>> getCalibrations()
    throws MissingParamException, DatabaseException {
    return dbInstance.getCalibrations(getDataSource(), instrument);
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
   * these groups.
   *
   * @return The targets JSON.
   * @throws DatabaseException
   * @throws JSONException
   * @throws MissingParamException
   */
  public String getUsedTargetsJson()
    throws MissingParamException, JSONException, DatabaseException {
    JSONArray groups = new JSONArray();

    int counter = 0;

    for (String target : getCalibrations().keySet()) {
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

    return StringUtils.javascriptString(groups.toString());
  }

  /**
   * Generate a new, empty {@link Calibration} object.
   *
   * @return The new object.
   */
  protected abstract Calibration initNewCalibration();

  /**
   * Get the JSON for the individual calibrations.
   *
   * @return The calibrations JSON.
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public String getCalibrationsJson()
    throws MissingParamException, DatabaseException {
    JSONArray items = new JSONArray();

    TreeMap<String, List<Calibration>> calibrations = getCalibrations();

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

    return StringUtils.javascriptString(items.toString());
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

  public void loadSelectedCalibration()
    throws RecordNotFoundException, MissingParamException, DatabaseException {

    calibration = null;

    if (selectedCalibrationId == DatabaseUtils.NO_DATABASE_RECORD) {
      calibration = initNewCalibration();
    } else {

      calibration = getCalibration(selectedCalibrationId);

      // The calibration wasn't found
      if (null == calibration) {
        throw new RecordNotFoundException(instrument.getName(), "calibration",
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
   * <p>
   * The algorithm for this is nasty. Your best bet to understanding it is to
   * examine the test suite in {@code CalibrationBeanEditCalibrationsTest} and
   * its subclasses/configuration files. There is documentation alongside them
   * that explains what should happen in different situations.
   * </p>
   *
   * @throws InvalidCalibrationEditException
   *           If the specified calibration details are invalid.
   * @throws RecordNotFoundException
   *           If the specified calibration does not exist.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws MissingParamException
   *           If any required parameters are missing.
   */
  public void calcAffectedDataSets() throws InvalidCalibrationEditException,
    RecordNotFoundException, InvalidCalibrationTargetException,
    InvalidCalibrationDateException, MissingParamException, DatabaseException {

    // Make sure all the parameters are present and valid
    checkEditedCalibration();

    String calibrationTarget = calibration.getTarget();

    // Get a new copy of the calibrations setup. We don't use the bean's copy
    // because this is a what-if method and we don't want the results to be
    // kept.
    List<Calibration> testCalibrations = dbInstance
      .getCalibrations(getDataSource(), instrument).get(calibrationTarget);
    if (null == testCalibrations) {
      testCalibrations = new ArrayList<Calibration>(0);
    }

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

    boolean calibrationMoved;

    if (editAction == ADD_ACTION || editAction == DELETE_ACTION) {
      calibrationMoved = false;
    } else {
      // We're editing. If we're editing the time, then we have to do the hard
      // algorithm.
      LocalDateTime newTime = calibration.getDeploymentDate();
      LocalDateTime oldTime = editedCalibration.getDeploymentDate();

      calibrationMoved = !newTime.equals(oldTime);
    }

    if (!calibrationMoved) {
      calcAffectedDatasetsSimple(testCalibrations);
    } else {
      calcAffectedDatasetsMovedCalibration(testCalibrations);
    }

    // Set or clear the "cannot be reprocessed" message
    if (getAffectedDatasetsStatus() < 0) {
      setMessage(getComponentID("affectedDatasets"),
        "This change cannot be completed because some datasets cannot be reprocessed.");
    }
  }

  private void calcAffectedDatasetsSimple(List<Calibration> testCalibrations)
    throws MissingParamException, DatabaseException {

    LocalDateTime[] surroundingCalibrations = getSurroundingCalibrations(
      testCalibrations, calibration.getDeploymentDate());

    LocalDateTime datasetSearchStart;

    if (changeAffectsDatasetsAfterOnly()) {
      datasetSearchStart = calibration.getDeploymentDate();
    } else {
      datasetSearchStart = surroundingCalibrations[0];
    }

    LocalDateTime datasetSearchEnd = surroundingCalibrations[1];

    List<DataSet> datasetsToTest = DataSetDB.getDatasetsBetweenDates(
      getDataSource(), instrument.getId(), datasetSearchStart,
      datasetSearchEnd);

    // If we deleted the first calibration, any datasets starting before then
    // can't be recalculated.
    LocalDateTime firstValidTime = LocalDateTime.MIN;

    if (editAction == DELETE_ACTION && dbInstance.priorCalibrationRequired()) {
      if (testCalibrations.get(0).getId() == calibration.getId()) {
        if (testCalibrations.size() == 1) {
          firstValidTime = LocalDateTime.MAX;
        } else {
          firstValidTime = testCalibrations.get(1).getDeploymentDate();
        }
      }
    }

    this.affectedDatasets = new TreeMap<DataSet, Boolean>();

    for (DataSet dataset : datasetsToTest) {
      this.affectedDatasets.put(dataset,
        !dataset.getStart().isBefore(firstValidTime));
    }

  }

  private void calcAffectedDatasetsMovedCalibration(
    List<Calibration> testCalibrations)
    throws MissingParamException, DatabaseException {

    LocalDateTime originalTime = getCalibration(calibration.getId())
      .getDeploymentDate();
    LocalDateTime newTime = calibration.getDeploymentDate();

    LocalDateTime[] originalSurroundingCalibrations = getSurroundingCalibrations(
      testCalibrations, originalTime);

    LocalDateTime beforeOriginal = originalSurroundingCalibrations[0];
    LocalDateTime afterOriginal = originalSurroundingCalibrations[1];

    LocalDateTime[] newSurroundingCalibrations = getSurroundingCalibrations(
      testCalibrations, newTime);

    LocalDateTime beforeNew = newSurroundingCalibrations[0];
    LocalDateTime afterNew = newSurroundingCalibrations[1];

    List<DataSet> affectedByOriginalGone = DataSetDB.getDatasetsBetweenDates(
      getDataSource(), instrument.getId(), beforeOriginal, afterOriginal);

    LocalDateTime afterAffectedStartTime = changeAffectsDatasetsAfterOnly()
      ? newTime
      : beforeNew;

    List<DataSet> affectedByNewPosition = DataSetDB.getDatasetsBetweenDates(
      getDataSource(), instrument.getId(), afterAffectedStartTime, afterNew);

    // Combine the two sets of datasets
    TreeSet<DataSet> datasetsToTest = new TreeSet<DataSet>(
      affectedByOriginalGone);
    datasetsToTest.addAll(affectedByNewPosition);

    LocalDateTime safeDatasetStartTime = LocalDateTime.MIN;

    if (dbInstance.priorCalibrationRequired()
      && newTime.isAfter(originalTime)) {

      // See if we've left any datasets without a calibration

      if (null == beforeOriginal) {
        if (newTime.isBefore(afterOriginal)) {
          safeDatasetStartTime = newTime;
        } else {
          safeDatasetStartTime = afterOriginal;
        }
      }
    }

    this.affectedDatasets = new TreeMap<DataSet, Boolean>();

    for (DataSet dataset : datasetsToTest) {

      // We know which datasets are potentially affected.
      // Depending on where the calibration was moved, it may not actually need
      // to be reprocessed.

      boolean reprocess = true;

      if (originalTime.isBefore(dataset.getStart())
        && newTime.isBefore(dataset.getStart())
        && (null == afterOriginal || newTime.isBefore(afterOriginal))
        && (null == beforeOriginal || newTime.isAfter(beforeOriginal))
        && changeAffectsDatasetsAfterOnly()) {
        reprocess = false;
      }

      if (originalTime.isAfter(dataset.getStart())
        && newTime.isAfter(dataset.getStart())
        && (null == beforeOriginal || newTime.isAfter(beforeOriginal))
        && (null == afterOriginal || newTime.isBefore(afterOriginal))
        && changeAffectsDatasetsAfterOnly()) {
        reprocess = false;
      }

      if (newTime.isBefore(dataset.getStart())
        && (null == beforeOriginal || newTime.isBefore(beforeOriginal))) {

        LocalDateTime[] datasetSurroundingCalibrations = getSurroundingCalibrations(
          testCalibrations, dataset.getStart());

        if (null != datasetSurroundingCalibrations[0]
          && newTime.isBefore(datasetSurroundingCalibrations[0])
          && (null != beforeOriginal
            && beforeOriginal.isBefore(dataset.getStart())
            && datasetSurroundingCalibrations[0].isEqual(beforeOriginal))
          && changeAffectsDatasetsAfterOnly()) {

          reprocess = false;
        }
      }

      if (newTime.isAfter(dataset.getStart())
        && originalTime.isAfter(dataset.getStart())
        && changeAffectsDatasetsAfterOnly()) {
        reprocess = false;
      }

      if (reprocess) {
        this.affectedDatasets.put(dataset,
          !dataset.getStart().isBefore(safeDatasetStartTime));
      }
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
   * Check that the parameters passed to {@link #getAffectedDatasets()} are
   * valid.
   *
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
   * @throws DatabaseException
   * @throws MissingParamException
   */
  private Calibration getCalibration(long calibrationId)
    throws MissingParamException, DatabaseException {
    Calibration result = null;

    for (List<Calibration> calibrations : getCalibrations().values()) {
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

  /**
   * Indicates whether changing a calibration will only affect the datasets
   * after that calibration, or also the ones before it (up to the next
   * calibration before or after it).
   *
   * This applies only for added or edited calibrations. Deleting a calibration
   * will always affect datasets before and after it.
   *
   * The default implementation states that only datasets after the changed
   * calibration should be affected.
   *
   * @return {@code true} if only datasets after the changed calibration should
   *         be affected; {@code false} if datasets before and after should be
   *         affected.
   */
  protected boolean changeAffectsDatasetsAfterOnly() {
    return true;
  }
}
