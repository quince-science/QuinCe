package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.DiagnosticQCConfig;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Data structure holding all the {@code SensorValue}s for a dataset, accessible
 * by different lookups.
 *
 * @author Steve Jones
 */
public class DatasetSensorValues {

  private Map<Long, SensorValue> valuesById;

  private Map<Long, SearchableSensorValuesList> valuesByColumn;

  private Map<SensorType, TreeSet<SensorValue>> valuesBySensorType;

  private TreeMap<LocalDateTime, Map<Long, SensorValue>> valuesByDateAndColumn;

  private final Instrument instrument;

  public static final long FLAG_TOTAL = -1L;

  /**
   * A cache of all the times in the dataset
   *
   * @see #getTimes()
   */
  private List<LocalDateTime> times = null;

  /**
   * Some datasets have columns that contain no data. You can ensure that they
   * are catered for by adding them as optional columns.
   */
  private TreeSet<Long> optionalColumns = new TreeSet<Long>();

  public DatasetSensorValues(Instrument instrument) {
    valuesById = new HashMap<Long, SensorValue>();
    valuesByColumn = new HashMap<Long, SearchableSensorValuesList>();
    valuesBySensorType = new HashMap<SensorType, TreeSet<SensorValue>>();
    valuesByDateAndColumn = new TreeMap<LocalDateTime, Map<Long, SensorValue>>();

    this.instrument = instrument;
  }

  public void add(SensorValue sensorValue) throws RecordNotFoundException {

    if (!contains(sensorValue)) {
      SensorType sensorType = instrument.getSensorAssignments()
        .getSensorTypeForDBColumn(sensorValue.getColumnId());

      addById(sensorValue);
      addByColumn(sensorValue);
      addBySensorType(sensorValue, sensorType);
      addByDateAndColumn(sensorValue);
    }
  }

  public void addOptionalColumn(long columnId) {
    if (!valuesByColumn.containsKey(columnId)) {
      optionalColumns.add(columnId);
    }
  }

  public boolean contains(SensorValue sensorValue) {
    return valuesById.containsKey(sensorValue.getId());
  }

  public boolean contains(LocalDateTime time) {
    return valuesByDateAndColumn.containsKey(time);
  }

  public void remove(SensorValue sensorValue) throws RecordNotFoundException {
    SensorType sensorType = instrument.getSensorAssignments()
      .getSensorTypeForDBColumn(sensorValue.getColumnId());

    removeById(sensorValue);
    removeByColumn(sensorValue);
    removeBySensorType(sensorValue, sensorType);
    removeByDateAndColumn(sensorValue);
  }

  public void removeAll(Collection<? extends SensorValue> values)
    throws RecordNotFoundException {

    for (SensorValue sensorValue : values) {
      remove(sensorValue);
    }
  }

  public Set<Long> getColumnIds() {
    return valuesByColumn.keySet();
  }

  public SearchableSensorValuesList getColumnValues(long columnId) {
    return valuesByColumn.get(columnId);
  }

  public SensorValue getById(long id) {
    return valuesById.get(id);
  }

  public List<SensorValue> getById(Collection<Long> ids) {
    return ids.stream().map(id -> getById(id)).toList();
  }

  public TreeSet<SensorValue> getBySensorType(SensorType sensorType) {
    return valuesBySensorType.get(sensorType);
  }

  public Collection<SensorValue> getAll() {
    return valuesById.values();
  }

  private void addById(SensorValue sensorValue) {
    valuesById.put(sensorValue.getId(), sensorValue);
  }

  private void removeById(SensorValue sensorValue) {
    valuesById.remove(sensorValue.getId());
  }

  private void addByColumn(SensorValue sensorValue) {
    long columnId = sensorValue.getColumnId();

    // Remove the column from the optional columns - it now contains data
    optionalColumns.remove(columnId);

    if (!valuesByColumn.containsKey(columnId)) {
      valuesByColumn.put(columnId, new SearchableSensorValuesList(columnId));
    }

    valuesByColumn.get(columnId).add(sensorValue);
  }

  private void removeByColumn(SensorValue sensorValue) {
    long columnId = sensorValue.getColumnId();
    if (valuesByColumn.containsKey(columnId)) {
      valuesByColumn.get(columnId).remove(sensorValue);
      if (valuesByColumn.get(columnId).isEmpty()) {
        valuesByColumn.remove(columnId);
      }
    }
  }

  private void addBySensorType(SensorValue sensorValue, SensorType sensorType) {
    if (!valuesBySensorType.containsKey(sensorType)) {
      valuesBySensorType.put(sensorType, new TreeSet<SensorValue>());
    }

    valuesBySensorType.get(sensorType).add(sensorValue);
  }

  private void removeBySensorType(SensorValue sensorValue,
    SensorType sensorType) {
    if (valuesBySensorType.containsKey(sensorType)) {
      valuesBySensorType.get(sensorType).remove(sensorValue);
      if (valuesBySensorType.get(sensorType).isEmpty()) {
        valuesBySensorType.remove(sensorType);
      }
    }
  }

  private void addByDateAndColumn(SensorValue sensorValue)
    throws RecordNotFoundException {

    LocalDateTime time = sensorValue.getTime();

    if (!valuesByDateAndColumn.containsKey(time)) {
      valuesByDateAndColumn.put(time, new HashMap<Long, SensorValue>());
    }

    valuesByDateAndColumn.get(time).put(sensorValue.getColumnId(), sensorValue);

    // Clear the cache of times, since it will need rebuilding.
    times = null;
  }

  private void removeByDateAndColumn(SensorValue sensorValue)
    throws RecordNotFoundException {

    LocalDateTime time = sensorValue.getTime();

    if (valuesByDateAndColumn.containsKey(time)) {
      Map<Long, SensorValue> values = valuesByDateAndColumn.get(time);
      if (values.containsKey(sensorValue.getColumnId())) {
        values.remove(sensorValue.getColumnId());
      }

      if (values.isEmpty()) {
        valuesByDateAndColumn.remove(time);
      }
    }

    // Clear the cache of times, since it will need rebuilding.
    times = null;
  }

  public List<LocalDateTime> getTimes() {
    if (null == times) {
      times = new ArrayList<LocalDateTime>(valuesByDateAndColumn.keySet());
    }

    return times;
  }

  public Map<Long, SensorValue> get(LocalDateTime time) {
    return valuesByDateAndColumn.get(time);
  }

  public SensorValue getSensorValue(LocalDateTime time, long columnID) {
    SensorValue result = null;

    if (valuesByDateAndColumn.containsKey(time)) {
      result = valuesByDateAndColumn.get(time).get(columnID);
    }

    return result;
  }

  /**
   * Get all sensor values within a specified time range.
   * <p>
   * The start time is inclusive, while the end time is exclusive.
   * </p>
   * <p>
   * <strong>Note:</strong> The start time must be present in the dataset for
   * this method to work.
   * </p>
   *
   * @param start
   *          The start time (inclusive).
   * @param end
   *          The end time (exclusive).
   * @return The sensor values that fall within the time range.
   */
  public List<SensorValue> getByTimeRange(LocalDateTime start,
    LocalDateTime end) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    List<LocalDateTime> times = getTimes();

    int timeIndex = Collections.binarySearch(times, start);
    if (timeIndex >= 0) {
      while (times.get(timeIndex).isBefore(end)) {

        Map<Long, SensorValue> timeSensorValues = valuesByDateAndColumn
          .get(times.get(timeIndex));

        result.addAll(timeSensorValues.values());
        timeIndex++;
      }
    }

    return result;
  }

  /**
   * Get the {@link SensorValue} for a column that occurs either on or before
   * the specified time.
   *
   * @param columnId
   *          The required column
   * @param time
   *          The time
   * @return The {@link SensorValue}.
   */
  public SensorValue getSensorValueOnOrBefore(long columnId,
    LocalDateTime time) {

    SensorValue result = valuesByDateAndColumn.get(time).get(columnId);

    if (null == result) {
      int timeIndex = getTimes().indexOf(time);
      while (null == result && timeIndex > 0) {
        timeIndex--;
        result = valuesByDateAndColumn.get(time).get(columnId);
      }
    }

    return result;
  }

  /**
   * Determines whether or not this data contains the specified column,
   * identified by its ID.
   *
   * @param columnId
   *          The column ID.
   * @return {@code true} if the column exists; {@code false} if it does not.
   */
  public boolean containsColumn(long columnId) {
    return valuesByColumn.containsKey(columnId)
      || optionalColumns.contains(columnId);
  }

  /**
   * Get the number of NEEDED flags in the dataset.
   * <p>
   * The flags are grouped by column ID, with an additional {@link #FLAG_TOTAL}
   * entry giving the total number of NEEDED flags.
   * </p>
   *
   * @return The number of NEEDED flags
   */
  public Map<Long, Integer> getNeedsFlagCounts() {

    Map<Long, Integer> result = new HashMap<Long, Integer>();
    int total = 0;

    for (Map.Entry<Long, SearchableSensorValuesList> entry : valuesByColumn
      .entrySet()) {

      int columnFlags = 0;

      for (SensorValue value : entry.getValue()) {
        if (value.getUserQCFlag().equals(Flag.NEEDED)) {
          columnFlags++;
          total++;
        }
      }

      result.put(entry.getKey(), columnFlags);
    }

    result.put(FLAG_TOTAL, total);

    return result;
  }

  public Instrument getInstrument() {
    return instrument;
  }

  public int size() {
    return valuesById.size();
  }

  public boolean isOfSensorType(SensorValue sensorValue,
    SensorType sensorType) {
    boolean result;

    if (!valuesBySensorType.containsKey(sensorType)) {
      result = false;
    } else {
      result = valuesBySensorType.get(sensorType).contains(sensorValue);
    }

    return result;
  }

  /**
   * Create a subset of this object containing the specified items.
   *
   * <p>
   * All values with the specified time are kept, plus those with the specified
   * ids and all position values. If any specified times or ids are not in this
   * object they will be ignored.
   * </p>
   *
   * @param times
   *          The times of values to keep
   * @param ids
   *          The additional {@lonk SensorValue} ids to keep
   * @return The subsetted values
   * @throws RecordNotFoundException
   */
  public DatasetSensorValues subset(TreeSet<LocalDateTime> times,
    TreeSet<Long> ids) throws RecordNotFoundException {

    DatasetSensorValues result = new DatasetSensorValues(instrument);

    /*
     * Build a TreeSet of values to be added to the result to ensure that
     * they're added in the correct order.
     */
    TreeSet<SensorValue> valuesToAdd = new TreeSet<SensorValue>();

    for (SensorValue value : this.valuesById.values()) {
      /*
       * Copy the value if either:
       *
       * (a) its time is in the times list, (b) its ID is in the ids list, (c)
       * it's a position
       */

      if (value.getColumnId() == SensorType.LONGITUDE_ID
        || value.getColumnId() == SensorType.LATITUDE_ID
        || ids.contains(value.getId()) || times.contains(value.getTime())) {
        valuesToAdd.add(value);
      }
    }

    for (SensorValue sensorValue : valuesToAdd) {
      result.add(sensorValue);
    }

    return result;
  }

  /**
   * Cascade QC values from the specified {@link SensorValue} to other sensors.
   *
   * <p>
   * When QC flags are applied to certain {@link SensorValue}s, they also apply
   * to other sensors. For example, a bad Position value means that all sensor
   * values will be bad.
   * </p>
   *
   * <p>
   * For each affected sensor type, if there is a SensorValue for the same time
   * as the source value, it will be flagged. If there is no value at that time,
   * the values before and after the source value will be flagged because we
   * don't know when the issue started. Flags will not be applied if the
   * value(s) are not within a specified affected Run Type.
   * </p>
   *
   * @param source
   *          The source QCed SensorValue
   * @param changedValues
   *          The SensorValues that have been changed as part of this cascade.
   *          Will be updated by this method.
   * @throws RecordNotFoundException
   */
  public Set<SensorValue> applyQCCascade(SensorValue source,
    RunTypePeriods runTypePeriods) throws RecordNotFoundException {

    Set<SensorValue> changedValues = new HashSet<SensorValue>();

    Map<SensorAssignment, Collection<String>> affectedSensorAssignments = getCascadeAffectedSensorAssignments(
      source);

    for (SensorAssignment assignment : affectedSensorAssignments.keySet()) {
      List<SensorValue> affectedSensorValues = valuesByColumn
        .get(assignment.getDatabaseId()).getClosest(source.getTime());

      for (SensorValue value : affectedSensorValues) {
        String valueRunType = runTypePeriods.getRunType(value.getTime());
        if (affectedSensorAssignments.get(assignment).contains(valueRunType)) {

          if (!source.getDisplayFlag().equals(Flag.GOOD)) {
            value.setCascadingQC(source);
          } else {
            value.removeCascadingQC(source);
          }

          changedValues.add(value);
        }
      }
    }

    return changedValues;
  }

  /**
   * Get the set of sensor assignments that will be affected by the cascading QC
   * from the specified SensorValue, along with the run types that the cascade
   * will apply to.
   *
   * <p>
   * For sensors without calibrations, all run types are affected. For those
   * with calibrations, either only measurement run types are affected, or those
   * affected have been specified by the user elsewhere.
   * </p>
   *
   * <p>
   * Position values always affect all sensors, and calibrated sensors during
   * measurements. Diagnostic sensors affect only those sensors specified in the
   * instrument's configuration.
   * </p>
   *
   * @param source
   *          The source SensorValue
   * @return The affected sensors and run types.
   * @throws RecordNotFoundException
   */
  private Map<SensorAssignment, Collection<String>> getCascadeAffectedSensorAssignments(
    SensorValue source) throws RecordNotFoundException {
    SensorType sourceType = instrument.getSensorAssignments()
      .getSensorTypeForDBColumn(source.getColumnId());

    Map<SensorAssignment, Collection<String>> result = new HashMap<SensorAssignment, Collection<String>>();

    if (sourceType.isPosition()) {
      // Positions affect all sensors.
      Collection<SensorAssignment> assignments = instrument
        .getSensorAssignments().getNonDiagnosticSensors(false);

      assignments.forEach(a -> {
        if (a.getSensorType().hasInternalCalibration()) {
          result.put(a, instrument.getMeasurementRunTypes());
        } else {
          result.put(a, instrument.getAllRunTypeNames());
        }
      });
    } else if (sourceType.isDiagnostic()) {
      // Diagnostics affect the sensors configured by the user
      SensorAssignment sourceAssignment = instrument.getSensorAssignments()
        .getById(source.getColumnId());

      Collection<SensorAssignment> measurementSensors = instrument
        .getSensorAssignments().getNonDiagnosticSensors(false);

      DiagnosticQCConfig diagnosticQCConfig = instrument
        .getDiagnosticQCConfig();

      measurementSensors.forEach(m -> {
        Collection<String> runTypes = diagnosticQCConfig
          .getAssignedRunTypes(sourceAssignment, m);
        if (!runTypes.isEmpty()) {
          result.put(m, runTypes);
        }
      });

    }

    return result;
  }
}
