package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.DiagnosticQCConfig;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SensorValuePlotPageTableValue;

/**
 * Data structure holding all the {@code SensorValue}s for a dataset, accessible
 * by different lookups.
 *
 * <p>
 * Position values are kept separate from the others, and can be accessed by a
 * {@link LocalDateTime}. This will give either the position for that time, or
 * an interpolated value. It will strive to give only GOOD values.
 * </p>
 */
public class DatasetSensorValues {

  /**
   * The {@link SensorValue}s mapped by their database ID.
   */
  private Map<Long, SensorValue> valuesById;

  /**
   * The {@link SensorValues}s grouped by their source {@link FileColumn} ID.
   */
  private Map<Long, SensorValuesList> valuesByColumn;

  /**
   * The longitudes in the dataset.
   */
  private SensorValuesList longitudes = null;

  /**
   * The latitudes in the dataset.
   */
  private SensorValuesList latitudes = null;

  /**
   * The {@link Instrument} to which the {@link SensorValue}s belong.
   */
  private final Instrument instrument;

  /**
   * A special {@link Map} key used to indicate a summed total of flag values.
   *
   * <p>
   * Used by {@link #getNonPositionNeedsFlagCounts()} and
   * {@link #getPositionNeedsFlagCounts()}.
   * </p>
   */
  public static final long FLAG_TOTAL = -1L;

  /**
   * A set of optional columns in the dataset.
   *
   * <p>
   * Some datasets have columns that contain no data (e.g. some diagnostic
   * columns do not contain data in all datasets). They can be added as optional
   * columns to ensure that the data structures are consistent between datasets
   * from the same {@link Instrument}.
   * </p>
   */
  private TreeSet<Long> optionalColumns = new TreeSet<Long>();

  /**
   * Initialise an empty instance for a dataset attached to a given
   * {@link Instrument}.
   *
   * @param instrument
   *          The instrument.
   * @throws RecordNotFoundException
   */
  public DatasetSensorValues(Instrument instrument)
    throws RecordNotFoundException {
    valuesById = new HashMap<Long, SensorValue>();
    valuesByColumn = new HashMap<Long, SensorValuesList>();
    this.instrument = instrument;

    /*
     * The longitudes and latitudes cannot be instatiated in the constructor.
     * They will be initiated on demand.
     */
  }

  /**
   * Construct a {@code DatasetSensorValues} object from an existing
   * {@link Collection} of {@link SensorValue}s.
   *
   * <p>
   * This uses exactly the same logic as
   * {@link DataSetDataDB#getSensorValues(Connection, Instrument, long, boolean, boolean)},
   * but allows us to create the object without loading everything from the
   * database. It relies on having a <i>complete</i> set of all the
   * {@link SensorValue}s for a {@link DataSet}, since it performs filtering
   * based on that assumption. Using an already filtered set of
   * {@link SensorValues} may result in unexpected behaviour.
   * </p>
   *
   * @param conn
   *          A database connection
   * @param instrument
   *          The instrument to which the dataset belongs.
   * @param datasetId
   *          The database ID of the dataset whose values are to be retrieved
   * @param ignoreFlushing
   *          Indicates whether or not values in the instrument's flushing
   *          period should be left out of the result.
   * @param rawSensorValues
   *          The source {@link SensorValue} objects.
   * @return The values.
   * @throws DatabaseException
   * @throws RecordNotFoundException
   */
  public DatasetSensorValues(Connection conn, Instrument instrument,
    long datasetId, boolean ignoreFlushing, boolean ignoreInternalCalibrations,
    Collection<SensorValue> rawSensorValues)
    throws DatabaseException, RecordNotFoundException {

    valuesById = new HashMap<Long, SensorValue>();
    valuesByColumn = new HashMap<Long, SensorValuesList>();
    this.instrument = instrument;

    TreeSet<Long> ignoredSensorValues = new TreeSet<Long>();

    if (instrument.hasInternalCalibrations() && ignoreInternalCalibrations) {
      ignoredSensorValues = DataSetDataDB
        .getInternalCalibrationSensorValueIDs(conn, instrument, datasetId);
    }

    for (SensorValue sensorValue : rawSensorValues) {
      if (!ignoredSensorValues.contains(sensorValue.getId())) {
        if (!ignoreFlushing
          || !sensorValue.getUserQCFlag().equals(Flag.FLUSHING)) {
          add(sensorValue);
        }
      }
    }
  }

  /**
   * Add a single {@link SensorValue}.
   *
   * @param sensorValue
   *          The SensorValue.
   * @throws RecordNotFoundException
   *           If the {@link Instrument} configuration is invalid.
   */
  public void add(SensorValue sensorValue) throws RecordNotFoundException {

    if (sensorValue.getColumnId() == SensorType.LONGITUDE_ID) {
      if (null == longitudes) {
        longitudes = new SensorValuesList(SensorType.LONGITUDE_ID, this, false);
      }
      longitudes.add(sensorValue);
      addById(sensorValue);
    } else if (sensorValue.getColumnId() == SensorType.LATITUDE_ID) {
      if (null == latitudes) {
        latitudes = new SensorValuesList(SensorType.LATITUDE_ID, this, false);
      }
      latitudes.add(sensorValue);
      addById(sensorValue);
    } else if (!contains(sensorValue)) {
      addById(sensorValue);
      addByColumn(sensorValue);
    }
  }

  /**
   * Add an optional {@link FileColumn}, which will be used in the data output
   * but which will not contain any {@link SensorValue}s.
   *
   * @param columnId
   *          The column's database ID.
   */
  public void addOptionalColumn(long columnId) {
    if (!valuesByColumn.containsKey(columnId)) {
      optionalColumns.add(columnId);
    }
  }

  /**
   * Determines whether or not the specified {@link SensorValue} is has already
   * been added.
   *
   * @param sensorValue
   *          The candidate {@link SensorValue}.
   * @return {@code true} if the {@link SensorValue} has been added;
   *         {@code false} otherwise.
   */
  public boolean contains(SensorValue sensorValue) {
    return valuesById.containsKey(sensorValue.getId());
  }

  /**
   * Determines whether any {@link SensorValue}s have been added with the
   * specified timestamp.
   *
   * @param time
   *          The timestamp.
   * @return {@code true} if a {@link SensorValue} with the timestamp exists;
   *         {@code false} otherwise.
   */
  public boolean contains(LocalDateTime time) {
    boolean result = false;

    result = null != longitudes && longitudes.containsTime(time);
    if (!result) {
      result = null != latitudes && latitudes.containsTime(time);
    }
    if (!result) {
      for (SensorValuesList sensorValues : valuesByColumn.values()) {
        result = sensorValues.containsTime(time);
        if (result) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Remove the specified {@link SensorValue} from the data structure.
   *
   * @param sensorValue
   *          The {@link SensorValue} to be removed.
   * @throws RecordNotFoundException
   *           If the {@link Instrument} configuration is invalid.
   */
  public void remove(SensorValue sensorValue) throws RecordNotFoundException {

    if (sensorValue.getColumnId() == SensorType.LONGITUDE_ID) {
      longitudes.remove(sensorValue);
    }
    if (sensorValue.getColumnId() == SensorType.LATITUDE_ID) {
      latitudes.remove(sensorValue);
    } else {
      removeById(sensorValue);
      removeByColumn(sensorValue);
    }
  }

  /**
   * Remove all the specified {@link SensorValue}s from the data structure.
   *
   * @param values
   *          The {@link SensorValue}s to be removed.
   * @throws RecordNotFoundException
   *           If the {@link Instrument} configuration is invalid.
   */
  public void removeAll(Collection<? extends SensorValue> values)
    throws RecordNotFoundException {

    for (SensorValue sensorValue : values) {
      remove(sensorValue);
    }
  }

  /**
   * Get the database IDs of all the {@link FileColumn}s represented in the data
   * structure, excluding the optional columns.
   *
   * @return The column IDs.
   */
  public Set<Long> getColumnIds() {
    return valuesByColumn.keySet();
  }

  /**
   * Get the {@link SensorValue}s for a specified {@link FileColumn} using its
   * database ID.
   *
   * @param columnId
   *          The {@link FileColumn} ID.
   * @return The {@link SensorValue}s in the column.
   * @throws RecordNotFoundException
   */
  public SensorValuesList getColumnValues(long columnId)
    throws RecordNotFoundException {

    SensorValuesList values;

    if (columnId == SensorType.LONGITUDE_ID) {
      values = longitudes;
    } else if (columnId == SensorType.LATITUDE_ID) {
      values = latitudes;
    } else {
      values = valuesByColumn.get(columnId);
      if (null == values) {
        values = new SensorValuesList(columnId, this, false);
      }
    }

    return values;
  }

  /**
   * Retrieve a {@link SensorValue} using its database ID.
   *
   * @param id
   *          The {@link SensorValue}'s ID.
   * @return The {@link SensorValue}, or {@code null} if it is not in the data
   *         structure.
   */
  public SensorValue getById(long id) {
    return valuesById.get(id);
  }

  /**
   * Retrieve a set of {@link SensorValue}s using their database IDs.
   *
   * <p>
   * The returned {@link SensorValue}s will be in a {@link List} with the same
   * iteration order as the passed in {@link Collection} of IDs. Any IDs that
   * are not in the data structure will result in a {@code null} value in the
   * corresponding list position.
   * </p>
   *
   * @param ids
   *          The {@link SensorValue} IDs.
   * @return The {@link SensorValue} objects.
   */
  public List<SensorValue> getById(Collection<Long> ids) {
    return ids.stream().map(id -> getById(id)).toList();
  }

  /**
   * Retrieve all the {@link SensorValue}s from the data structure.
   *
   * <p>
   * The order of the {@link SensorValue}s is not defined.
   * </p>
   *
   * <p>
   * Position values are not included in the result. Use
   * {@link #getAllPositionSensorValues()}.
   * </p>
   *
   * @return The {@link SensorValue}s in the data structure.
   */
  public Collection<SensorValue> getAll() {
    return valuesById.values();
  }

  /**
   * Add the given {@link SensorValue} to the {@link #valuesById} lookup.
   *
   * <p>
   * Used by {@link #add(SensorValue)}.
   * </p>
   *
   * @param sensorValue
   *          The {@link SensorValue}.
   */
  private void addById(SensorValue sensorValue) {
    valuesById.put(sensorValue.getId(), sensorValue);
  }

  /**
   * Remove the given {@link SensorValue} from the {@link #valuesById} lookup.
   *
   * <p>
   * Used by {@link #remove(SensorValue)}.
   * </p>
   */
  private void removeById(SensorValue sensorValue) {
    valuesById.remove(sensorValue.getId());
  }

  /**
   * Add the given {@link SensorValue} to the {@link #valuesByColumn} lookup.
   *
   * <p>
   * If the {@link SensorValue}'s column is in the {@link #optionalColumns}
   * list, it is removed from there.
   * </p>
   *
   * <p>
   * Used by {@link #add(SensorValue)}.
   * </p>
   *
   * @param sensorValue
   *          The {@link SensorValue}.
   * @throws RecordNotFoundException
   */
  private void addByColumn(SensorValue sensorValue)
    throws RecordNotFoundException {

    long columnId = sensorValue.getColumnId();

    // Remove the column from the optional columns - it now contains data
    optionalColumns.remove(columnId);

    if (!valuesByColumn.containsKey(columnId)) {
      valuesByColumn.put(columnId, new SensorValuesList(columnId, this, false));
    }

    valuesByColumn.get(columnId).add(sensorValue);
  }

  /**
   * Remove the given {@link SensorValue} from the {@link #valuesByColumn}
   * lookup.
   *
   * <p>
   * If this is the only {@link SensorValue} for the column, that column is
   * moved to the {@link #optionalColumns} list.
   * </p>
   *
   * <p>
   * Used by {@link #remove(SensorValue)}.
   * </p>
   */
  private void removeByColumn(SensorValue sensorValue) {
    long columnId = sensorValue.getColumnId();
    if (valuesByColumn.containsKey(columnId)) {
      valuesByColumn.get(columnId).remove(sensorValue);
      if (valuesByColumn.get(columnId).isEmpty()) {
        valuesByColumn.remove(columnId);
        optionalColumns.add(columnId);
      }
    }
  }

  /**
   * Get all the timestamps for which non-position {@link SensorValue}s have
   * been added.
   *
   * <p>
   * The returned {@link List} is in ascending time order.
   * </p>
   *
   * @return The timestamps.
   */
  public List<LocalDateTime> getTimes() {
    TreeSet<LocalDateTime> times = new TreeSet<LocalDateTime>();

    for (SensorValuesList sensorValues : valuesByColumn.values()) {
      times.addAll(sensorValues.getRawTimes());
    }

    return new ArrayList<LocalDateTime>(times);
  }

  public List<LocalDateTime> getPositionValueTimes()
    throws SensorValuesListException {
    TreeSet<LocalDateTime> result = new TreeSet<LocalDateTime>();

    if (null != longitudes) {
      result.addAll(longitudes.getValueTimes());
    }

    if (null != latitudes) {
      result.addAll(latitudes.getValueTimes());
    }

    return new ArrayList<LocalDateTime>(result);
  }

  public List<LocalDateTime> getRawPositionTimes() {
    TreeSet<LocalDateTime> result = new TreeSet<LocalDateTime>();

    if (null != longitudes) {
      result.addAll(longitudes.getRawTimes());
    }

    if (null != latitudes) {
      result.addAll(latitudes.getRawTimes());
    }

    return new ArrayList<LocalDateTime>(result);
  }

  /**
   * Get all the {@link SensorValue}s with the specified timestamp.
   *
   * <p>
   * The returned values are grouped by their source (@link FileColumn}.
   *
   * @param time
   *          The required timestamp.
   * @return The {@link SensorValue}s with the timestamp.
   */
  public Map<Long, SensorValue> get(LocalDateTime time) {

    Map<Long, SensorValue> result = new HashMap<Long, SensorValue>();

    for (Map.Entry<Long, SensorValuesList> entry : valuesByColumn.entrySet()) {
      SensorValue value = entry.getValue().getRawSensorValue(time);
      if (null != value) {
        result.put(entry.getKey(), value);
      }
    }

    return result;
  }

  /**
   * Get the {@link SensorValue} from the specified {@link FileColumn} with the
   * specified timestamp.
   *
   * @param columnID
   *          The {@link FileColumn}'s database ID.
   * @param time
   *          The timestamp.
   * @return The matching {@link SensorValue}, or {@code null} if there is no
   *         such value.
   */
  public SensorValue getRawSensorValue(long columnID, LocalDateTime time) {
    SensorValue result = null;

    if (columnID == SensorType.LONGITUDE_ID) {
      result = null == longitudes ? null : longitudes.getRawSensorValue(time);
    } else if (columnID == SensorType.LATITUDE_ID) {
      result = null == latitudes ? null : latitudes.getRawSensorValue(time);
    } else if (valuesByColumn.containsKey(columnID)) {
      result = valuesByColumn.get(columnID).getRawSensorValue(time);
    }

    return result;
  }

  /**
   * Determines whether or not this data contains the specified
   * {@link FileColumn}, identified by its database ID.
   *
   * <p>
   * The method will search both the {@link #valuesByColumn} and
   * {@link #optionalColumns} lookups.
   * </p>
   *
   * @param columnId
   *          The {@link FileColumn}'s database ID.
   * @return {@code true} if the {@link FileColumn} is registered in the
   *         dataset; {@code false} if it does not.
   */
  public boolean containsColumn(long columnId) {
    return valuesByColumn.containsKey(columnId)
      || optionalColumns.contains(columnId);
  }

  /**
   * Get the number of {@link Flag#NEEDED} flags set on non-position
   * {@link SensorValue}s in the dataset.
   *
   * <p>
   * The flags are grouped by column ID, with an additional {@link #FLAG_TOTAL}
   * entry giving the total number of flags across all columns.
   * </p>
   *
   * @return The number of non-position values with a {@link Flag#NEEDED} flag.
   */
  public Map<Long, Integer> getNonPositionNeedsFlagCounts() {

    Map<Long, Integer> result = new HashMap<Long, Integer>();
    int total = 0;

    for (Map.Entry<Long, SensorValuesList> entry : valuesByColumn.entrySet()) {

      int columnFlags = 0;

      for (SensorValue value : entry.getValue().getRawValues()) {
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

  /**
   * Get the number of {@link Flag#NEEDED} flags set on position
   * {@link SensorValue}s in the dataset.
   *
   * <p>
   * The flags are grouped by column ID, with an additional {@link #FLAG_TOTAL}
   * entry giving the total number of flags across all columns.
   * </p>
   *
   * @return The number of position values with a {@link Flag#NEEDED} flag.
   */
  public Map<Long, Integer> getPositionNeedsFlagCounts() {

    Set<LocalDateTime> needsFlagTimes = new HashSet<LocalDateTime>();

    int lonCount = 0;
    for (SensorValue longitude : longitudes.getRawValues()) {
      if (longitude.getUserQCFlag().equals(Flag.NEEDED)) {
        needsFlagTimes.add(longitude.getTime());
        lonCount++;
      }
    }

    int latCount = 0;
    for (SensorValue latitude : latitudes.getRawValues()) {
      if (latitude.getUserQCFlag().equals(Flag.NEEDED)) {
        needsFlagTimes.add(latitude.getTime());
        latCount++;
      }
    }

    Map<Long, Integer> result = new HashMap<Long, Integer>();
    result.put(SensorType.LONGITUDE_ID, lonCount);
    result.put(SensorType.LATITUDE_ID, latCount);
    result.put(FLAG_TOTAL, needsFlagTimes.size());

    return result;
  }

  /**
   * Get the {@link Instrument} for which these {@link SensorValue}s were
   * recorded.
   *
   * @return The {@link Instrument}.
   */
  public Instrument getInstrument() {
    return instrument;
  }

  /**
   * Get the total number of {@link SensorValue}s added to the data structure.
   *
   * <p>
   * Includes both non-position and position values.
   * </p>
   *
   * @return The number of {@link SensorValue}s.
   */
  public int size() {
    int result = valuesById.size();
    if (null != latitudes) {
      result += latitudes.rawSize();
    }
    if (null != longitudes) {
      result += longitudes.rawSize();
    }

    return result;
  }

  /**
   * Create a subset of this object containing the specified
   * {@link SensorValue}s, identified by their database IDs.
   *
   * <p>
   * All {@link SensorValue}s with the specified timestamp are kept, plus those
   * with the specified database IDs and all position {@link SensorValue}s. If
   * any of the specified timestamps or {@link SensorValue} IDs are not in the
   * data structure they will be ignored.
   * </p>
   *
   * @param times
   *          The timestamps of {@link SensorValues} to keep.
   * @param ids
   *          The additional {@link SensorValue}s to keep.
   * @return A new {@link DatasetSensorValues} object containing only the
   *         selected {@link SensorValue}s.
   * @throws RecordNotFoundException
   *           If the {@link Instrument}'s configuration is invalid.
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
   * For each affected sensor type, if there is a {@link SensorValue} for the
   * same time as the source value, it will be flagged. If there is no value at
   * that time, the values before and after the source value will be flagged
   * because we don't know when the issue started. Flags will not be applied if
   * the value(s) are not within a specified set of Run Type periods.
   * </p>
   *
   * @param source
   *          The source QCed {@link SensorValue}.
   * @param runTypePeriods
   *          The run type periods to which the cascade should be applied.
   * @return The {@link SensorValues} that have been changed as part of this
   *         cascade.
   * @throws RecordNotFoundException
   * @throws InvalidFlagException
   */
  public Set<SensorValue> applyQCCascade(SensorValue source,
    RunTypePeriods runTypePeriods)
    throws RecordNotFoundException, InvalidFlagException {

    Set<SensorValue> changedValues = new HashSet<SensorValue>();

    // For position values, we just copy from the source to the counterpart.
    // Other sensors are sorted out as part of the Data Reduction QC.
    if (SensorType.isPosition(source.getColumnId())) {

      SensorValue other;

      if (source.getColumnId() == SensorType.LONGITUDE_ID) {
        other = getRawSensorValue(SensorType.LATITUDE_ID, source.getTime());
      } else {
        other = getRawSensorValue(SensorType.LONGITUDE_ID, source.getTime());
      }

      if (null != other) {
        other.setUserQC(source);
        changedValues.add(other);
      }
    } else {
      Map<SensorAssignment, Collection<String>> affectedSensorAssignments = getCascadeAffectedSensorAssignments(
        source);

      for (SensorAssignment assignment : affectedSensorAssignments.keySet()) {
        List<SensorValue> affectedSensorValues = valuesByColumn
          .get(assignment.getDatabaseId())
          .getClosestSensorValues(source.getTime());

        for (SensorValue value : affectedSensorValues) {
          String valueRunType = runTypePeriods.getRunType(value.getTime());
          if (null == valueRunType || affectedSensorAssignments.get(assignment)
            .contains(valueRunType)) {

            if (!source.getDisplayFlag(this).equals(Flag.GOOD)) {
              value.setCascadingQC(source);
            } else {
              value.removeCascadingQC(source.getId());
            }

            // Update the value in the By ID lookup map
            valuesById.put(value.getId(), value);

            changedValues.add(value);
          }
        }

        // Reset the output cache for the SensorType
        valuesByColumn.get(assignment.getDatabaseId()).resetOutput();
      }
    }

    return changedValues;
  }

  /**
   * Get the set of {@link SensorAssignment}s that will be affected by the
   * cascading QC from the specified {@link SensorValue}, along with the run
   * types that the cascade will apply to.
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
   *          The source {@link SensorValue}.
   * @return The affected sensors and run types.
   * @throws RecordNotFoundException
   *           If the {@link Instrument}'s configuration is invalid.
   */
  private Map<SensorAssignment, Collection<String>> getCascadeAffectedSensorAssignments(
    SensorValue source) throws RecordNotFoundException {
    SensorType sourceType = instrument.getSensorAssignments()
      .getSensorTypeForDBColumn(source.getColumnId());

    Map<SensorAssignment, Collection<String>> result = new HashMap<SensorAssignment, Collection<String>>();

    if (sourceType.isDiagnostic()) {
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

  /**
   * Get the position value at the specified timestamp without performing any
   * interpolation.
   *
   * <p>
   * The {@code columnId} must be either
   * {@link FileDefinition#LONGITUDE_COLUMN_ID} or
   * {@link FileDefinition#LATITUDE_COLUMN_ID}.
   * </p>
   *
   * @param columnId
   *          The position column ID.
   * @param time
   *          The timestamp.
   * @return The position value.
   * @throws PositionException
   *           If the supplied {@code columnId} is invalid.
   */
  public PlotPageTableValue getRawPositionTableValue(long columnId,
    LocalDateTime time) throws PositionException {

    SensorValue value = getPositionValuesList(columnId).getRawSensorValue(time);
    return null == value ? null : new SensorValuePlotPageTableValue(value);
  }

  /**
   * Get the position value at the specified timestamp, interpolating values if
   * necessary.
   *
   * <p>
   * The {@code columnId} must be either
   * {@link FileDefinition#LONGITUDE_COLUMN_ID} or
   * {@link FileDefinition#LATITUDE_COLUMN_ID}.
   * </p>
   *
   * @param columnId
   *          The position column ID.
   * @param time
   *          The timestamp.
   * @param preferGoodValues
   *          Indicate whether or not the method should try to find values with
   *          {@link Flag#GOOD} flags over closer values with non-GOOD flags.
   * @return The position value.
   * @throws SensorValuesListException
   * @throws PositionException
   *           If the supplied {@code columnId} is invalid.
   */
  public PlotPageTableValue getPositionTableValue(long columnId,
    LocalDateTime time) throws SensorValuesListException, PositionException {

    SensorType sensorType = columnId == FileDefinition.LONGITUDE_COLUMN_ID
      ? SensorType.LONGITUDE_SENSOR_TYPE
      : SensorType.LATITUDE_SENSOR_TYPE;

    SensorValuesList sensorValues = getPositionValuesList(columnId);
    SensorValuesListOutput positionValue = null == sensorValues ? null
      : sensorValues.getValue(time, true);

    return null == positionValue ? null
      : new MeasurementValue(sensorType, positionValue);
  }

  /**
   * Get all the position {@link SensorValue}s added to the data structure.
   *
   * <p>
   * The iteration order of the returned values is undefined.
   * </p>
   *
   * @return The position values.
   */
  public Collection<SensorValue> getAllPositionSensorValues() {
    Collection<SensorValue> result = new HashSet<SensorValue>();
    if (null != longitudes) {
      result.addAll(longitudes.getRawValues());
    }

    if (null != latitudes) {
      result.addAll(latitudes.getRawValues());
    }

    return result;
  }

  /**
   * Get all the position {@link SensorValue}s for the specified position
   * column.
   *
   * <p>
   * The {@code columnId} must be either
   * {@link FileDefinition#LONGITUDE_COLUMN_ID} or
   * {@link FileDefinition#LATITUDE_COLUMN_ID}.
   * </p>
   *
   * @param columnId
   *          The position column ID.
   * @return The position {@link SensorValue}s.
   * @throws PositionException
   *           If the supplied {@code columnId} is invalid.
   */
  public SensorValuesList getPositionValuesList(long columnId)
    throws PositionException {

    SensorValuesList values;

    if (columnId == SensorType.LONGITUDE_ID) {
      values = longitudes;
    } else if (columnId == SensorType.LATITUDE_ID) {
      values = latitudes;
    } else {
      throw new PositionException("Invalid position column ID");
    }

    return values;
  }

  /**
   * Get the position values (both latitude and longitude) measured at or before
   * the specified timestamp.
   *
   * <p>
   * If there is no position available for the exact timestamp, the latest
   * position before the timestamp is returned. There is no limit on how far
   * before the requested timestamp this may be.
   * </p>
   *
   * @param time
   *          The time.
   * @return The position measurements.
   * @throws SensorValuesListException
   */
  public LatLng getClosestPosition(LocalDateTime time)
    throws SensorValuesListException {

    SensorValuesListValue lat = latitudes.getValueOnOrBefore(time);
    SensorValuesListValue lon = longitudes.getValueOnOrBefore(time);

    LatLng result = null;

    if (null != lat && null != lon && lat.getTime().equals(lon.getTime())) {
      result = new LatLng(lat.getDoubleValue(), lon.getDoubleValue());
    }

    return result;
  }

  public SensorValuesList getSensorValues(Collection<Long> columnIds,
    boolean forceString) throws RecordNotFoundException {

    SensorValuesList result = new SensorValuesList(columnIds, this,
      forceString);

    for (long id : columnIds) {
      result.addAll(valuesByColumn.get(id));
    }

    return result;
  }

  public SensorValuesList getColumnValues(SensorAssignment assignment)
    throws RecordNotFoundException {
    return getColumnValues(assignment.getDatabaseId());
  }

  public SensorValuesList getRunTypes() throws RecordNotFoundException {
    return getSensorValues(
      getInstrument().getSensorAssignments().getRunTypeColumnIDs(), true);
  }
}
