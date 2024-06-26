package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import org.primefaces.shaded.json.JSONArray;
import org.primefaces.shaded.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling calibrations
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
   * The datasets defined for the instrument.
   *
   * <p>
   * These are shown in the timeline and used to determine which datasets are
   * affected by an edit.
   * </p>
   */
  private TreeMap<DataSet, RecalculateStatus> datasets;

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
  private Calibration editedCalibration;

  /**
   * The calibration edit action
   */
  private int action = CalibrationEdit.EDIT;

  /**
   * The set of calibrations for the instrument. This will be changed as edits
   * are made.
   */
  private TreeMap<String, TreeSet<Calibration>> calibrations;

  private long instrumentId;

  private List<CalibrationEdit> edits;

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

    try {
      setCurrentInstrumentId(instrumentId);
      datasets = new TreeMap<DataSet, RecalculateStatus>();
      DataSetDB.getDataSets(getDataSource(), getCurrentInstrumentId(), true)
        .values().forEach(d -> datasets.put(d, new RecalculateStatus()));

      dbInstance = getDbInstance();
      calibrations = dbInstance.getCalibrations(getDataSource(),
        getCurrentInstrument());
      calibrationTargets = dbInstance.getTargets(getDataSource(),
        getCurrentInstrument());
      editedCalibration = initNewCalibration(generateNewId(), getLastDate());
      edits = new ArrayList<CalibrationEdit>();
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      nav = internalError(e);
    }

    return nav;
  }

  public long getInstrumentId() {
    return getCurrentInstrumentId();
  }

  /**
   * Set the database ID of the instrument
   *
   * @param instrumentId
   *          The instrument ID
   * @throws Exception
   *           If the instrument cannot be retrieved
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
   * Get the instrument name
   *
   * @return The instrument name
   */
  public String getInstrumentName() {
    return getCurrentInstrument().getDisplayName();
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
   * Get the job class for reprocessing a {@link DataSet}.
   *
   * @return The reprocessing job class.
   */
  protected Class<? extends Job> getReprocessJobClass() {
    return ExtractDataSetJob.class;
  }

  /**
   * Get the status to set on a {@link DataSet} that is being reprocessed.
   *
   * @return The new status.
   */
  protected abstract int getReprocessStatus();

  /**
   * Get an instance of the database interaction class for the calibrations
   *
   * @return The database interaction instance
   */
  protected abstract CalibrationDB getDbInstance();

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
  public String getTargetsJson()
    throws MissingParamException, DatabaseException {
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

    return StringUtils.javascriptString(groups.toString());
  }

  /**
   * Generate a new, empty {@link Calibration} object.
   *
   * @return The new object.
   */
  protected abstract Calibration initNewCalibration(long id, LocalDateTime date)
    throws Exception;

  /**
   * Get the JSON for the individual calibrations.
   *
   * @return The calibrations JSON.
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public String getTimelineJson()
    throws MissingParamException, DatabaseException {
    JSONArray items = new JSONArray();

    for (String key : calibrationTargets.keySet()) {

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
    for (Map.Entry<DataSet, RecalculateStatus> entry : datasets.entrySet()) {

      DataSet dataset = entry.getKey();

      JSONObject datasetJson = new JSONObject();
      datasetJson.put("id", getTimelineId(dataset));
      datasetJson.put("type", "range");
      datasetJson.put("group", "Datasets");
      datasetJson.put("start", DateTimeUtils.toIsoDate(dataset.getStart()));
      datasetJson.put("end", DateTimeUtils.toIsoDate(dataset.getEnd()));
      datasetJson.put("content", dataset.getName());
      datasetJson.put("title", dataset.getName());
      datasetJson.put("className", entry.getValue().getDisplayClass());
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

  public Calibration getEditedCalibration() {
    return editedCalibration;
  }

  public long getSelectedCalibrationId() {
    return selectedCalibrationId;
  }

  public void setSelectedCalibrationId(long selectedCalibrationId) {
    this.selectedCalibrationId = selectedCalibrationId;
  }

  public void loadSelectedCalibration() throws Exception {

    editedCalibration = null;

    if (action == CalibrationEdit.ADD) {
      editedCalibration = initNewCalibration(generateNewId(), getLastDate());
    } else {

      editedCalibration = getCalibration(selectedCalibrationId);

      // The calibration wasn't found
      if (null == editedCalibration) {
        throw new RecordNotFoundException(
          getCurrentInstrument().getDisplayName(), "calibration",
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
   * be thrown as exceptions.
   * </p>
   *
   * @throws InvalidCalibrationEditException
   *           If the edited calibration is invalid due to a coding error (as
   *           opposed to a user error).
   */
  private List<String> validateCalibration(int action, Calibration calibration)
    throws InvalidCalibrationEditException {

    List<String> result = new ArrayList<String>();

    // A zero calibration ID is invalid
    if (calibration.getId() == 0
      || calibration.getId() < 0 && action != CalibrationEdit.ADD
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

      // Check whether we land in the middle of a DataSet, and whether that
      // matters. (add and edit only)
      if (action != CalibrationEdit.DELETE && !allowCalibrationChangeInDataset()
        && isInDataset(calibration.getDeploymentDate())) {
        result.add("Calibration cannot change inside a dataset");
      }
    }

    return result;
  }

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
   * Find a calibration using its database ID. Returns {@code null} if the
   * calibration is not found.
   *
   * @param calibrationId
   *          The calbration's ID
   * @return The calibration
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

  public String getCalibrationName() {
    return "Calibration";
  }

  public int getAction() {
    return action;
  }

  public void setAction(int action) {
    this.action = action;
  }

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

  /**
   * Indicates whether a calibration can change within a datasets.
   *
   * @return {@code true} if a calibration values can change within the bounds
   *         of a dataset; {@code false} if they cannot.
   */
  protected boolean allowCalibrationChangeInDataset() {
    return true;
  }

  public String saveCalibration() {

    String nav = null;

    try {

      List<String> validationMessages = validateCalibration(action,
        editedCalibration);

      if (validationMessages.size() > 0) {
        validationMessages.forEach(m -> setMessage(null, m));
      } else {
        // The edit is valid.
        edits.add(new CalibrationEdit(action, editedCalibration));

        // Update calibrations in bean and calculate which DataSets have been
        // affected

        switch (action) {
        case CalibrationEdit.ADD: {
          // The list of calibrations for the same target as the one that is
          // being
          // edited (includes the edited calibration pre-edit).
          TreeSet<Calibration> targetCalibrations = calibrations
            .get(editedCalibration.getTarget());

          targetCalibrations.add(editedCalibration);

          calculatedAffectedDatasets(editedCalibration);

          break;
        }
        case CalibrationEdit.DELETE: {
          // The list of calibrations for the same target as the one that is
          // being
          // edited (includes the edited calibration pre-edit).
          TreeSet<Calibration> targetCalibrations = calibrations
            .get(editedCalibration.getTarget());

          // The editedCalibration is an independent copy of the "real" one held
          // in the data structures. Get the real one
          Calibration deletedCalibration = targetCalibrations.stream()
            .filter(c -> c.getId() == editedCalibration.getId()).findFirst()
            .get();

          targetCalibrations.remove(deletedCalibration);

          calculatedAffectedDatasets(deletedCalibration);

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

          calculatedAffectedDatasets(originalCalibration);
          calculatedAffectedDatasets(editedCalibration);

          break;
        }
        default: {
          throw new IllegalArgumentException("Unrecognised action");
        }
        }
      }

    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      nav = internalError(e);
    }

    return nav;
  }

  private void calculatedAffectedDatasets(Calibration calibration) {

    TreeSet<Calibration> targetCalibrations = calibrations
      .get(calibration.getTarget());

    // Datasets after the added Calibration can be recalculated if
    // there's a complete set of calibrations active at the point of the
    // added dataset.
    boolean calibrationsComplete = completeCalibrationSetAt(editedCalibration,
      true);

    Calibration after = targetCalibrations.higher(editedCalibration);

    Collection<DataSet> affectedDatasetsAfter = getDatasetsBetween(
      editedCalibration.getDeploymentDate(), after);

    affectedDatasetsAfter
      .forEach(d -> datasets.get(d).set(true, calibrationsComplete));

    if (!changeAffectsDatasetsAfterOnly()) {
      // Find the previous Calibration for the same target before the
      // added Calibration
      Calibration before = targetCalibrations.lower(editedCalibration);

      if (null == before) {
        // There is no previous Calibration, so no previous datasets
        // can be valid
        Collection<DataSet> invalidDatasets = getDatasetsBetween(null,
          editedCalibration.getDeploymentDate());

        invalidDatasets.forEach(d -> datasets.get(d).set(true, false));
      } else {
        Collection<DataSet> affectedDatasetsBefore = getDatasetsBetween(before,
          editedCalibration);
        affectedDatasetsBefore.forEach(d -> datasets.get(d).set(true, true));
      }
    }
  }

  private long generateNewId() {
    return DateTimeUtils.dateToLong(LocalDateTime.now()) * -1;
  }

  private boolean isInDataset(LocalDateTime time) {
    return datasets.keySet().stream()
      .anyMatch(d -> !d.getEnd().isBefore(time) && !d.getStart().isAfter(time));
  }

  private Collection<DataSet> getDatasetsBetween(Calibration start,
    Calibration end) {
    return getDatasetsBetween(null == start ? null : start.getDeploymentDate(),
      null == end ? null : end.getDeploymentDate());
  }

  private Collection<DataSet> getDatasetsBetween(LocalDateTime start,
    Calibration end) {
    return getDatasetsBetween(start,
      null == end ? null : end.getDeploymentDate());
  }

  private Collection<DataSet> getDatasetsBetween(LocalDateTime start,
    LocalDateTime end) {

    Collection<DataSet> result;

    if (null == start && null == end) {
      result = datasets.keySet();
    } else if (null == start) {
      result = datasets.keySet().stream()
        .filter(d -> d.getStart().isBefore(end)).toList();
    } else if (null == end) {
      result = datasets.keySet().stream()
        .filter((d -> d.getEnd().isAfter(start))).toList();
    } else {
      result = datasets.keySet().stream()
        .filter(d -> d.getStart().isBefore(end) && d.getEnd().isAfter(start))
        .toList();
    }

    return result;
  }

  /**
   * Determine whether or not all calibration targets have a valid
   * {@link Calibration} at the time of the supplied calibration.
   *
   * @param calibration
   *          The calibration whose timestamp is the basis of the check.
   * @param includeSupplied
   *          Indicates whether or not the supplied calibration should be
   *          considered part of the set.
   * @return {@code true} if all targets have a {@link Calibration};
   *         {@code false} otherwise.
   */
  private boolean completeCalibrationSetAt(Calibration calibration,
    boolean includeSupplied) {

    boolean allOK = true;

    for (String target : calibrations.keySet()) {

      if (!includeSupplied || !target.equals(calibration.getTarget())) {
        if (calibrations.get(target).stream().filter(
          c -> c.getDeploymentDate().isBefore(calibration.getDeploymentDate()))
          .findAny().isEmpty()) {

          allOK = false;
          break;
        }
      }
    }

    return allOK;
  }

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
}

class RecalculateStatus {
  private boolean required;

  private boolean canBeRecalculated;

  protected RecalculateStatus() {
    this.required = false;
    this.canBeRecalculated = false;
  }

  protected boolean getRequired() {
    return required;
  }

  protected boolean getCanBeRecalculated() {
    return canBeRecalculated;
  }

  protected void set(boolean required, boolean canBeRecalculated) {
    this.required = required;
    this.canBeRecalculated = canBeRecalculated;
  }

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
