package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.primefaces.shaded.json.JSONArray;
import org.primefaces.shaded.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationDateException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling {@link Calibration} editing.
 *
 * <p>
 * This class contains the central methods for handling edits of
 * {@link Calibration}s (stored in {@link CalibrationEdit} objects). The user
 * can make multiple edits which are collected together to be committed in a
 * single operation. The bean also tracks which {@link DataSet}s will be
 * affected by the edits made, and queue those for recalculation after the edits
 * have been saved to the database.
 * </p>
 *
 * <p>
 * Concrete implementations of this class provide the necessary configuration
 * for how the editing process differs for the different types of
 * {@link Calibration} supported by QuinCe.
 * </p>
 */
public abstract class CalibrationBean extends BaseManagedBean {

  /**
   * The navigation destination after the edits have been committed.
   *
   * <p>
   * The user is returned to the Instruments List page.
   * </p>
   */
  private static final String COMMIT_NAV = "instrument_list";

  /**
   * The database ID of the calibration currently being edited.
   *
   * <p>
   * For new calibrations, this will be
   * {@link DatabaseUtils#NO_DATABASE_RECORD}.
   * </p>
   */
  private long selectedCalibrationId = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The {@link DataSet}s defined for the instrument, with an indication of
   * whether they are affected by the edits that have been made.
   */
  private TreeMap<DataSet, RecalculateStatus> datasets;

  /**
   * The calibration database handler.
   */
  private CalibrationDB dbInstance = null;

  /**
   * The calibration target names lookup.
   *
   * @see CalibrationDB#getTargets(java.sql.Connection,
   *      uk.ac.exeter.QuinCe.data.Instrument.Instrument)
   */
  private Map<String, String> calibrationTargets = null;

  /**
   * The {@link Calibration} that is currently being edited.
   */
  private Calibration editedCalibration;

  /**
   * Indicates whether the edited {@link Calibration} is valid.
   *
   * <p>
   * A {@link Calibration} is valid if all required information has been entered
   * and it does not clash with another calibration).
   * </p>
   */
  private boolean editedCalibrationValid;

  /**
   * The calibration edit action.
   *
   * @see CalibrationEdit
   */
  private int action = CalibrationEdit.EDIT;

  /**
   * The original set of {@link Calibration}s before any edits were performed.
   *
   * <p>
   * This is used to calculate which {@link DataSet}s are affected by the edits
   * that have been made.
   * </p>
   *
   * <p>
   * The {@link Calibration}s are stored as a {@link Map} of
   * {@code <target> -> <Calibrations>}, with each target holding the
   * {@link Calibration}s in time order.
   * </p>
   */
  private TreeMap<String, TreeSet<Calibration>> originalCalibrations;

  /**
   * The edited set of calibrations. This will be changed as edits are made.
   *
   * @see #originalCalibrations
   */
  private TreeMap<String, TreeSet<Calibration>> calibrations;

  /**
   * The database ID of the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} whose calibrations
   * are being edited.
   *
   * <p>
   * This is only used as a temporary store when the bean is initialised. During
   * {@link #start()} this is used to set the current
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} for the session via
   * {@link #setCurrentInstrumentId(long)}.
   * </p>
   */
  private long instrumentId;

  /**
   * The list of edits made in the current bean instance (equating to an editing
   * session).
   *
   * <p>
   * The edits are stored as a {@link Map} of
   * {@code <Calibration ID> -> <edit action>}. This automatically prevents
   * multiple edits being applied for the same {@link Calibration}; a new edit
   * on a {@link Calibration} that has already been edited simply replaces the
   * previous edit. New {@link Calibration}s are given a temporary ID by
   * {@link #generateNewId()}.
   * </p>
   */
  private HashMap<Long, CalibrationEdit> edits;

  /**
   * Empty constructor.
   *
   * <p>
   * Required for JUnit tests.
   * </p>
   */
  public CalibrationBean() {
  }

  /**
   * Initialise the bean.
   *
   * <p>
   * Loads details of the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}'s {@link DataSet}s
   * and existing {@link Calibration}s, and prepares the data structures.
   * </p>
   *
   * @return The navigation string to the calibration edit page.
   */
  public String start() {
    String nav = getPageNavigation();

    try {
      setForceInstrumentReload(true);
      initialiseInstruments();
      setCurrentInstrumentId(instrumentId);
      datasets = new TreeMap<DataSet, RecalculateStatus>();
      DataSetDB.getDataSets(getDataSource(), getCurrentInstrumentId(), true)
        .values().forEach(d -> datasets.put(d, new RecalculateStatus()));

      dbInstance = getDbInstance();

      originalCalibrations = dbInstance.getCalibrations(getDataSource(),
        getCurrentInstrument());

      // We get this twice to ensure we have different Calibration objects
      calibrations = dbInstance.getCalibrations(getDataSource(),
        getCurrentInstrument());

      calibrationTargets = dbInstance.getTargets(getDataSource(),
        getCurrentInstrument());
      editedCalibration = initNewCalibration(generateNewId(), getLastDate());
      edits = new HashMap<Long, CalibrationEdit>();
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      nav = internalError(e);
    }

    return nav;
  }

  /**
   * Get the database ID of the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} whose
   * {@link Calibration}s are being edited.
   *
   * @return The instrument ID.
   */
  public long getInstrumentId() {
    return getCurrentInstrumentId();
  }

  /**
   * Set the database ID of the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} whose
   * {@link Calibration}s are being edited.
   *
   * @param instrumentId
   *          The instrument ID.
   * @throws Exception
   *           If the instrument ID is invalid.
   * @see #instrumentId
   */
  public void setInstrumentId(long instrumentId) throws Exception {
    if (instrumentId > 0) {
      try {
        this.instrumentId = instrumentId;
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
        throw e;
      }
    }
  }

  /**
   * Get the display name of the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} whose
   * {@link Calibration}s are being edited.
   *
   * @return The instrument name.
   */
  public String getInstrumentName() {
    return getCurrentInstrument().getDisplayName();
  }

  /**
   * Get the navigation string that will navigate to the relevant instance of
   * the calibration editing screen.
   *
   * @return The list navigation string.
   */
  protected abstract String getPageNavigation();

  /**
   * Get a list of all possible targets for calibration editing.
   *
   * @return The targets.
   * @see CalibrationDB#getTargets(java.sql.Connection,
   *      uk.ac.exeter.QuinCe.data.Instrument.Instrument)
   */
  public Map<String, String> getTargets() {
    return calibrationTargets;
  };

  /**
   * Get the job class for reprocessing a {@link DataSet} after editing is
   * complete.
   *
   * <p>
   * This defaults to {@link ExtractDataSetJob} (meaning that the
   * {@link DataSet} will be reset and processed from the start), but will be
   * overridden if the edits only affect a part of the processing sequence.
   * </p>
   *
   * @return The reprocessing job class.
   */
  protected Class<? extends Job> getReprocessJobClass() {
    return ExtractDataSetJob.class;
  }

  /**
   * Get an instance of the database interaction class for the calibrations.
   *
   * @return The database interaction instance.
   */
  protected abstract CalibrationDB getDbInstance();

  /**
   * Get the calibration type for the calibrations being edited.
   *
   * @return The calibration type.
   */
  protected abstract String getCalibrationType();

  /**
   * Get the human-readable calibration type for the calibrations being edited.
   *
   * @return The human-readable calibration type.
   */
  public abstract String getHumanReadableCalibrationType();

  /**
   * Individual calibration targets are represented as separate groups on the
   * page timeline. Get the JSON for these groups.
   *
   * @return The targets JSON.
   */
  public String getTargetsJson() {
    JsonArray groups = new JsonArray();

    int counter = 0;

    for (String target : calibrationTargets.keySet()) {
      JsonObject group = new JsonObject();
      group.addProperty("id", StringUtils.tabToSpace(target));
      group.addProperty("order", counter);
      group.addProperty("content",
        StringUtils.tabToSpace(getTargets().get(target)));

      groups.add(group);
      counter++;
    }

    JsonObject group = new JsonObject();
    group.addProperty("id", "Datasets");
    group.addProperty("order", counter);
    group.addProperty("content", "Datasets");
    groups.add(group);

    return groups.toString();
  }

  /**
   * Generate a new, empty {@link Calibration} object with the specified ID and
   * deployment date.
   *
   * @param id
   *          The {@link Calibration}'s ID.
   * @param date
   *          The deployment date.
   * @return The new {@link Calibration}.
   * @throws Exception
   *           If the {@link Calibration} cannot be created.
   */
  protected abstract Calibration initNewCalibration(long id, LocalDateTime date)
    throws Exception;

  /**
   * Get the JSON for the individual calibrations for use on the front end.
   *
   * @return The calibrations JSON.
   */
  public String getTimelineJson() {
    JSONArray items = new JSONArray();

    for (String key : calibrationTargets.keySet()) {

      TreeSet<Calibration> targetCalibrations = calibrations.get(key);

      if (null != targetCalibrations) {
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
    }

    // Add the datasets
    for (Map.Entry<DataSet, RecalculateStatus> entry : datasets.entrySet()) {

      DataSet dataset = entry.getKey();

      JSONObject datasetJson = new JSONObject();
      datasetJson.put("id", getTimelineId(dataset));
      datasetJson.put("type", "range");
      datasetJson.put("group", "Datasets");
      datasetJson.put("start",
        DateTimeUtils.formatDateTime(dataset.getStartTime()));
      datasetJson.put("end",
        DateTimeUtils.formatDateTime(dataset.getEndTime()));
      datasetJson.put("content", dataset.getName());
      datasetJson.put("title", dataset.getName());
      datasetJson.put("className", entry.getValue().getDisplayClass());
      items.put(datasetJson);
    }

    return items.toString();
  }

  /**
   * Get the label to use for the calibration target.
   *
   * @return The target label.
   */
  public abstract String getTargetLabel();

  /**
   * Get the label used to describe the coefficients for the calibrations being
   * edited.
   *
   * @return The coefficients label.
   */
  public String getCoefficientsLabel() {
    return "Coefficients";
  }

  /**
   * Get the {@link Calibration} object that is currently being edited.
   *
   * @return The current {@link Calibration}.
   */
  public Calibration getEditedCalibration() {
    return editedCalibration;
  }

  /**
   * Get the database ID of the {@link Calibration} that the user has selected.
   *
   * @return The selected {@link Calibration}'s ID.
   */
  public long getSelectedCalibrationId() {
    return selectedCalibrationId;
  }

  /**
   * Set the database ID of the {@link Calibration} that the user has selected.
   *
   * @param selectedCalibrationId
   *          The selected {@link Calibration}'s ID.
   */
  public void setSelectedCalibrationId(long selectedCalibrationId) {
    this.selectedCalibrationId = selectedCalibrationId;
  }

  /**
   * Load the details of the {@link Calibration} selected by the user ready for
   * editing.
   *
   * @throws RecordNotFoundException
   *           If the selected {@link Calibration} details cannot be found.
   * @see selectedCalibrationId
   * @see #editedCalibration
   */
  public void loadSelectedCalibration() throws RecordNotFoundException {
    editedCalibration = getCalibration(selectedCalibrationId);

    if (null == editedCalibration) {
      throw new RecordNotFoundException(getCurrentInstrument().getDisplayName(),
        "calibration", selectedCalibrationId);
    }

    setLastDate(editedCalibration.getDeploymentDate());
  }

  /**
   * Initialise a new {@link Calibration} object.
   *
   * @throws Exception
   *           If the new object cannot be created.
   * @see #editedCalibration
   */
  public void newCalibration() throws Exception {
    editedCalibration = initNewCalibration(generateNewId(), getLastDate());
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
   *          The dataset for which an ID is required.
   * @return The dataset's timeline ID.
   */
  private String getTimelineId(DataSet dataset) {
    return "DS-" + dataset.getId();
  }

  /**
   * Check that an edit action is valid.
   *
   * <p>
   * The method returns a list of {@link String} messages for errors caused by
   * the user (e.g. specifying invalid data or creating multiple calibrations
   * for the same target and time).
   * </p>
   *
   * <p>
   * Errors caused by bad coding (e.g. invalid IDs, invalid targets etc.) will
   * be thrown as Exceptions.
   * </p>
   *
   * @param action
   *          The edit action. Must be one of {@link CalibrationEdit#ADD},
   *          {@link CalibrationEdit#EDIT} or {@link CalibrationEdit#DELETE}
   * @param calibration
   *          The edited calibration.
   * @return Any error messages generated during the validation.
   * @throws InvalidCalibrationEditException
   *           If the edited calibration is invalid due to a coding error (as
   *           opposed to a user error).
   */
  private List<String> validateCalibration(int action, Calibration calibration)
    throws InvalidCalibrationEditException {

    List<String> result = new ArrayList<String>();

    // Negative calibration IDs are allowed - they are new calibrations.
    if (calibration.getId() == 0
      || calibration.getId() > 0 && action == CalibrationEdit.ADD) {

      throw new InvalidCalibrationEditException("Invalid calibration ID");
    }

    if (action != CalibrationEdit.DELETE) {

      if (null == calibration.getDeploymentDate()) {
        throw new InvalidCalibrationEditException("Missing time");
      }

      if (null == calibration.getTarget()) {
        throw new InvalidCalibrationEditException("Missing target");
      }

      if (!calibrationTargets.containsKey(calibration.getTarget())) {
        throw new InvalidCalibrationEditException("Invalid target");
      }

      // Future dates are not allowed
      LocalDateTime now = LocalDateTime.ofInstant(Instant.now(),
        ZoneId.of("UTC"));
      if (null != calibration.getDeploymentDate()
        && calibration.getDeploymentDate().isAfter(now)) {
        result.add("Time cannot be in the future");
      }

      // Find target and time. If existing calibration with different id,
      // it's invalid.
      Calibration existingCalibration = findCalibration(calibration.getTarget(),
        calibration.getDeploymentDate());

      if (null != existingCalibration
        && existingCalibration.getId() != calibration.getId()) {
        result.add("A calibration for " + calibration.getTarget() + " at "
          + DateTimeUtils.toIsoDate(calibration.getDeploymentDate())
          + " already exists");
      }
    }

    // Check whether we land in the middle of a DataSet, and whether that
    // matters. (add and edit only)
    if (action != CalibrationEdit.DELETE
      && !dbInstance.allowCalibrationChangeInDataset()
      && isInDataset(calibration.getDeploymentDate())) {
      result.add(getCoefficientsLabel() + " cannot change inside a dataset");
    }

    editedCalibrationValid = result.size() == 0;

    return result;
  }

  /**
   * Locate an existing {@link Calibration} for the specified target and time.
   *
   * <p>
   * Returns {@code null} if there is no matching calibration.
   * </p>
   *
   * @param target
   *          The calibration target.
   * @param timestamp
   *          The calibration time.
   * @return The matching {@link Calibration}, or {@code null} if a matching
   *         calibration cannot be found.
   */
  private Calibration findCalibration(String target, LocalDateTime timestamp) {
    Calibration result = null;

    TreeSet<Calibration> search = calibrations.get(target);
    if (null != search) {
      Optional<Calibration> found = search.stream()
        .filter(c -> c.getDeploymentDate().equals(timestamp)).findAny();

      if (found.isPresent()) {
        result = found.get();
      }
    }

    return result;
  }

  /**
   * Find a {@link Calibration} using its database ID.
   *
   * <p>
   * Returns {@code null} if the calibration is not found.
   * </p>
   *
   * @param calibrationId
   *          The calbration's ID.
   * @return The {@link Calibration}, or {@code null} if there is no matching
   *         calibration.
   */
  private Calibration getCalibration(long calibrationId) {
    Calibration result = null;

    for (TreeSet<Calibration> calibrations : calibrations.values()) {
      for (Calibration calibration : calibrations) {
        if (calibration.getId() == calibrationId) {
          result = calibration.makeCopy();
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

  /**
   * Get the short name for the type of {@link Calibration} being edited.
   *
   * @return The calibration type.
   */
  public String getCalibrationName() {
    return "Calibration";
  }

  /**
   * Get the current edit action.
   *
   * @return The edit action
   * @see CalibrationEdit
   */
  public int getAction() {
    return action;
  }

  /**
   * Set the current edit action.
   *
   * @param action
   *          The edit action
   * @see CalibrationEdit
   */
  public void setAction(int action) {
    this.action = action;
  }

  /**
   * Get the target of the {@link Calibration} that is currently being edited.
   *
   * @return The calibration target.
   */
  public String getEditedCalibrationTargetName() {
    String result = null;

    if (null != editedCalibration && null != editedCalibration.getTarget()) {
      result = getTargets().get(editedCalibration.getTarget());
    }

    return result;
  }

  /**
   * Indicates whether changing a calibration will only affect the datasets
   * after that calibration, or also the ones before it (up to the next
   * calibration before or after it).
   *
   * <p>
   * This functionality exists to support calibrations whose effect on data
   * reduction are interpolated between calibrations.
   * </p>
   *
   * <p>
   * This applies only for added or edited calibrations. Deleting a calibration
   * will always affect datasets before and after it.
   * </p>
   *
   * <p>
   * The default implementation states that only datasets after the changed
   * calibration should be affected.
   * </p>
   *
   * @return {@code true} if only datasets after the changed calibration should
   *         be affected; {@code false} if datasets both before and after should
   *         be affected.
   */
  protected boolean changeAffectsDatasetsAfterOnly() {
    return true;
  }

  /**
   * Store the current calibration edit ready to be saved.
   *
   * <p>
   * This method validates the edit, and then adds it to {@link #edits}.
   * </p>
   *
   * <p>
   * If an error occurs in this method, the user will be directed to the
   * Internal Error page. Otherwise the navigation will be {@code null} so the
   * user is returned to the editing page.
   * </p>
   *
   * @return The navigation result. {@code null} if no errors are encountered.
   */
  public String saveCalibration() {

    String nav = null;

    try {

      List<String> validationMessages = validateCalibration(action,
        editedCalibration);

      if (validationMessages.size() > 0) {
        validationMessages.forEach(m -> setMessage(null, m));
      } else {
        // The edit is valid.
        edits.put(editedCalibration.getId(),
          new CalibrationEdit(action, editedCalibration));

        // Update calibrations in bean and calculate which DataSets have been
        // affected

        switch (action) {
        case CalibrationEdit.ADD: {
          // The list of calibrations for the same target as the one that is
          // being edited (includes the edited calibration pre-edit).
          TreeSet<Calibration> targetCalibrations = calibrations
            .get(editedCalibration.getTarget());

          // Handle the case where we were missing one of the targets in the
          // original data.
          if (null == targetCalibrations) {
            targetCalibrations = new TreeSet<Calibration>();
            calibrations.put(editedCalibration.getTarget(), targetCalibrations);
          }

          targetCalibrations.add(editedCalibration);
          setLastDate(editedCalibration.getDeploymentDate());
          break;
        }
        case CalibrationEdit.DELETE: {
          // The list of calibrations for the same target as the one that is
          // being edited (includes the edited calibration pre-edit).
          TreeSet<Calibration> targetCalibrations = calibrations
            .get(editedCalibration.getTarget());

          // The editedCalibration is an independent copy of the "real" one held
          // in the data structures. Get the real one
          Calibration deletedCalibration = targetCalibrations.stream()
            .filter(c -> c.getId() == editedCalibration.getId()).findFirst()
            .get();

          targetCalibrations.remove(deletedCalibration);
          break;
        }
        case CalibrationEdit.EDIT: {

          // An edit may include a change of calibration target
          // The editedCalibration has the new target, but we need
          // to get the original calibration to make sure we cover the old
          // target
          Calibration originalCalibration = getCalibration(
            editedCalibration.getId());

          calibrations.get(originalCalibration.getTarget())
            .remove(originalCalibration);
          calibrations.get(editedCalibration.getTarget())
            .add(editedCalibration);

          break;
        }
        default: {
          throw new IllegalArgumentException("Unrecognised action");
        }
        }

        calculateAffectedDatasets();
      }

    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      nav = internalError(e);
    }

    return nav;
  }

  /**
   * Determine which {@link DataSet}s are affected by the edits that have been
   * made.
   *
   * <p>
   * The method compares effect of the unedited {@link Calibration}s and the
   * edited {@link Calibration}s on each {@link DataSet} in turn. If the effects
   * are different, then the {@link DataSet} is marked as being affected by the
   * edits.
   * </p>
   *
   * @throws InvalidCalibrationDateException
   *           If any {@link Calibration}s cannot be analysed.
   * @see #originalCalibrations
   * @see #calibrations
   */
  private void calculateAffectedDatasets()
    throws InvalidCalibrationDateException {

    for (DataSet dataset : datasets.keySet()) {
      CalibrationSet originalSet = new CalibrationSet(calibrationTargets,
        dataset.getStartTime(), dataset.getEndTime(), dbInstance,
        originalCalibrations);

      CalibrationSet editedSet = new CalibrationSet(calibrationTargets,
        dataset.getStartTime(), dataset.getEndTime(), dbInstance, calibrations);

      if (!editedSet.hasSameEffect(originalSet)) {
        datasets.get(dataset).set(true,
          !dbInstance.completeSetRequired() || editedSet.hasCompletePrior());
      } else {
        datasets.get(dataset).set(false, editedCalibrationValid);
      }
    }
  }

  /**
   * Generate a unique ID for a new {@link Calibration}.
   *
   * <p>
   * These IDs are temporary, and will be replaced by a database ID when the new
   * {@link Calibration} is saved to the database.
   * </p>
   *
   * @return The temporary ID.
   */
  private long generateNewId() {

    // Sleep for 2 ms to guarantee we get a different value
    try {
      TimeUnit.MILLISECONDS.sleep(2);
    } catch (InterruptedException e) {
      // Noop
    }
    return DateTimeUtils.dateToLong(LocalDateTime.now()) * -1;
  }

  /**
   * Determine whether or not the specified time is within any {@link DataSet}.
   *
   * <p>
   * A time exactly on the start or end of the {@link DataSet} qualifies as
   * being outside the DataSet.
   * </p>
   *
   * @param time
   *          The time.
   * @return {@code true} if any {@link DataSet} encompasses the specified time;
   *         {@code false} if not.
   */
  private boolean isInDataset(LocalDateTime time) {
    return datasets.keySet().stream()
      .anyMatch(d -> !DateTimeUtils.isEqualOrBefore(d.getEndTime(), time)
        && !DateTimeUtils.isEqualOrAfter(d.getStartTime(), time));
  }

  /**
   * Retrieve the {@link DataSet}s that are affected by the current edits, along
   * with a {@code boolean} indicating whether or not the {@link DataSet} can be
   * successfully reprocessed.
   *
   * @return The affected {@link DataSet}s.
   * @see RecalculateStatus
   */
  public TreeMap<Long, Boolean> getAffectedDatasets() {
    TreeMap<Long, Boolean> result = new TreeMap<Long, Boolean>();

    for (Map.Entry<DataSet, RecalculateStatus> entry : datasets.entrySet()) {
      if (entry.getValue().getRequired()) {
        result.put(entry.getKey().getId(),
          entry.getValue().getCanBeRecalculated());
      }
    }

    return result;
  }

  /**
   * Get the flag indicating whether or not the current calibration edit is
   * valid.
   *
   * <p>
   * The flag will be set by {@link #validateCalibration(int, Calibration)}.
   * </p>
   *
   * @return {@code true} if the current edit is valid; {@code false} if it is
   *         not.
   */
  public boolean editedCalibrationValid() {
    return editedCalibrationValid;
  }

  /**
   * Determine whether or not the changes made can be saved.
   *
   * <p>
   * Changes can be saved if:
   * </p>
   * <ul>
   * <li>One or more edits have been made.</li>
   * <li>There are no required {@link DataSet} recalculations that cannot be
   * performed due to the nature of the edits made.</li>
   * </ul>
   *
   * @return {@code true} if the changes can be saved; {@code false} otherwise.
   */
  public boolean canSave() {
    return edits.size() == 0 || datasets.values().stream()
      .anyMatch(rs -> rs.getRequired() && !rs.getCanBeRecalculated()) ? false
        : true;
  }

  /**
   * Write the edited {@link Calibration}s to the database and trigger
   * recalculation of the affected {@link DataSet}s.
   *
   * @return The navigation to the next page in the web app.
   */
  public String commitChanges() {
    try {
      // Commit edits to database
      // What about add then edit? Add then delete? Jut take the last entry for
      // each unique calibration ID?
      dbInstance.commitEdits(getDataSource(), edits.values());

      // Resubmit jobs for all affected datasets
      for (Map.Entry<DataSet, RecalculateStatus> entry : datasets.entrySet()) {

        if (entry.getValue().getRequired()) {

          Class<? extends Job> reprocessJobClass = getReprocessJobClass();

          Properties jobProperties = new Properties();

          // See GitHub Issue #1369
          jobProperties.setProperty(DataSetJob.ID_PARAM,
            String.valueOf(entry.getKey().getId()));
          JobManager.addJob(getDataSource(), getUser(),
            reprocessJobClass.getCanonicalName(), jobProperties);
        }
      }
    } catch (Exception e) {
      internalError(e);
    }

    return COMMIT_NAV;
  }

  /**
   * Holds details of whether a {@link DataSet} must be recalculated as a result
   * of the current set of edits in a {@link Calibration}, and whether
   * recalculation is possible.
   *
   * <p>
   * This class does not hold any details of the {@link DataSet} or
   * {@link Calibration}s; it holds only status information and is linked to the
   * relevant items by the {@link CalibrationBean}.
   * </p>
   *
   * <p>
   * Recalculation of a {@link DataSet} may not be possible for a variety of
   * reasons. The most common reasons are:
   * </p>
   * <ul>
   * <li>Recalculation requires a full set of {@link Calibration}s to be
   * available, but one or more have been deleted.</li>
   * <li>A {@link Calibration} has been created or edited such that it is within
   * the time period of the {@link DataSet}, and this is not permitted for the
   * type of the {@link Calibration}s being edited.</li>
   * </ul>
   */
  class RecalculateStatus {

    /**
     * Indicates whether or not the {@link DataSet} needs to be recalculated.
     */
    private boolean required;

    /**
     * Indicates whether or not the {@link DataSet} can be recalculated.
     */
    private boolean canBeRecalculated;

    /**
     * Basic constructor.
     *
     * <p>
     * The default status is that the {@link DataSet} does not need to be
     * recalculated.
     * </p>
     */
    protected RecalculateStatus() {
      this.required = false;
      this.canBeRecalculated = false;
    }

    /**
     * Indicates whether or not recalculation is required.
     *
     * @return {@code true} if the {@link DataSet} needs to be recalculated;
     *         {@code false} if it does not.
     */
    protected boolean getRequired() {
      return required;
    }

    /**
     * Indicates whether or not recalculation is possible.
     *
     * @return {@code true} if the {@link DataSet} can be recalculated;
     *         {@code false} if it does not.
     */
    protected boolean getCanBeRecalculated() {
      return canBeRecalculated;
    }

    /**
     * Set the values of the object.
     *
     * @param required
     *          Whether recalculation is required.
     * @param canBeRecalculated
     *          Whether recalculation is possible.
     */
    protected void set(boolean required, boolean canBeRecalculated) {
      this.required = required;
      this.canBeRecalculated = !required ? true : canBeRecalculated;
    }

    /**
     * Get the name of the CSS class to be used for displaying the dataset's
     * status to the user.
     *
     * @return The CSS display class.
     */
    protected String getDisplayClass() {
      String result;

      if (!required) {
        result = "recalculationNotRequired";
      } else if (canBeRecalculated) {
        result = "recalculationRequired";
      } else {
        result = "cannotRecalculate";
      }

      return result;
    }
  }
}
