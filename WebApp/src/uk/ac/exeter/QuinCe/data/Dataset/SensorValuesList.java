package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * A list of SensorValue objects with various search capabilities.
 *
 * <p>
 * <b>IMPORTANT: READ ALL OF THIS DESCRIPTION, PARTICULARLY THE NOTE IN THE LAST
 * PARAGRAPH.</b>
 * </p>
 *
 * <p>
 * The list is maintained in time order of its members. Attempting to add two
 * {@link SensorValue}s with the same timestamp will result in an
 * {@link UnsupportedOperationException}.
 * </p>
 *
 * <p>
 * When retrieving values, the list is aware of the different measurement
 * strategies (or MODEs as named here) used by different sensors. These are:
 * </p>
 * <ul>
 * <li><b>CONTINUOUS mode:</b> Regular measurements at short intervals (≤ 5
 * minutes).</li>
 * <li><b>PERIODIC mode:</b> Groups of measurements at long intervals (e.g. 5
 * measurements at one minute intervals, every 4 hours). This also encompasses
 * single measurements taken at extended intervals.</li>
 * </ul>
 *
 * <p>
 * {@code get} methods for the list allow access in two ways:
 * </p>
 *
 * <ul>
 * <li><b>Values:</b> Get the values as they should be used for performing data
 * reduction.</li>
 * <li><b>Raw:</b> Get the raw {@link SensorValue} objects.</li>
 * </ul>
 *
 * <p>
 * Methods are named accordingly, e.g. {@code valuesSize} or {@code rawSize}.
 * Typically, QC activities will access the {@code raw} methods and data
 * reduction will access the {@code values} methods.
 * </p>
 *
 * <p>
 * Values mode results are automatically stripped of {@link Flag#FLUSHING}
 * values.
 * </p>
 *
 * <p>
 * <b>Note:</b> For the PERIODIC mode, the auto-generated
 * {@link SensorValuesListValue}s may not be suitable for use. For example, a
 * sensor may wake up, take water and air measurements in sequence, and then
 * sleep. The automatic algorithm cannot detect this, and will group both sets
 * of measurements together. To work around this, you must determine the group
 * boundaries elsewhere (most likely in the {@link SensorValuesList} for the Run
 * Type), and use the
 * {@link #getValue(LocalDateTime, LocalDateTime, LocalDateTime, boolean)}
 * method to construct values covering the correct time periods.
 * </p>
 */
public class SensorValuesList {

  /**
   * Pre-defined exception thrown when an attempt is made to a
   * {@link SensorValue} with the same timestamp as an existing
   * {@link SensorValue}.
   *
   * <p>
   * Note that this is a {@link RuntimeException} and as such must be explicitly
   * caught if required.
   * </p>
   */
  private static final IllegalArgumentException SAME_TIMESTAMP_EXCEPTION = new IllegalArgumentException(
    "Cannot add two SensorValues with the same timestamp");

  /**
   * The maximum time between two measurements that can still be considered
   * continuous. This is also used as the limit for interpolating data in time.
   */
  private static final long CONTINUOUS_MEASUREMENT_LIMIT = 300;

  /**
   * The threshold group size between PERIODIC and CONTINUOUS measurements.
   *
   * @see #calculateMeasurementMode()
   */
  private static final int MAX_PERIODIC_GROUP_SIZE = 25;

  /**
   * The number of large groups (i.e. larger than
   * {@link #MAX_PERIODIC_GROUP_SIZE}) allowed in a PERIODIC list. Some PERIODIC
   * lists contain a couple of large groups at the start as the sensor
   * initialises.
   */
  private static final int LARGE_GROUP_LIMIT = 5;

  /**
   * Indicator for measurements in continuous mode.
   */
  public static final int MODE_CONTINUOUS = 0;

  /**
   * Indicator for measurement in periodic mode.
   */
  public static final int MODE_PERIODIC = 1;

  /**
   * An instance of the comparator used to compare two {@link SensorValue}s by
   * their timestamp.
   */
  private static final SensorValueTimeComparator TIME_COMPARATOR = new SensorValueTimeComparator();

  /**
   * The complete set of sensor values for the current dataset.
   */
  private final DatasetSensorValues allSensorValues;

  /**
   * The list of values.
   */
  private final ArrayList<SensorValue> list = new ArrayList<SensorValue>();

  /**
   * The list of {@link FileColumn} database IDs whose {@link SensorValue}s are
   * allowed to be members of this list.
   */
  private final TreeSet<Long> columnIds;

  /**
   * The {@link SensorType} of values in this list.
   */
  private final SensorType sensorType;

  /**
   * The measurement mode of these {@link SensorValue}s.
   */
  private int measurementMode = -1;

  /**
   * A cache of the timestamps of all the {@link SensorValue}s in the list.
   */
  private List<LocalDateTime> rawTimesCache = null;

  /**
   * The set of values to be returned to the rest of the application from this
   * list, based on its measurement mode.
   */
  private List<SensorValuesListValue> outputValues;

  /**
   * A cache of the timestamps of the entries in {@link #outputValues}.
   */
  private List<LocalDateTime> valueTimesCache = null;

  /**
   * Indicates whether or not String values can be used to determine groups when
   * calculating the measurement mode
   *
   * @see #calculateMeasurementMode()
   */
  private boolean allowStringPeriodicGroups = false;

  /**
   * Create a list for a single file column.
   *
   * @param sensorAssignments
   *          The instrument's sensor assignments, for checking
   *          {@link SensorType}s.
   * @param columnId
   *          The column's database ID.
   * @throws RecordNotFoundException
   *           If the {@link SensorType} for the column cannot be established.
   */
  public SensorValuesList(long columnId, DatasetSensorValues allSensorValues)
    throws RecordNotFoundException {
    columnIds = new TreeSet<Long>();
    columnIds.add(columnId);
    this.allSensorValues = allSensorValues;
    this.sensorType = allSensorValues.getInstrument().getSensorAssignments()
      .getSensorTypeForDBColumn(columnId);
  }

  /**
   * Create a list for a set of file columns.
   *
   * <p>
   * Note that all columns must be of the same {@link SensorType}, otherwise an
   * {@link IllegalArgumentException} will be thrown.
   * </p>
   *
   * @param sensorAssignments
   *          The instrument's sensor assignments, for checking
   *          {@link SensorType}s.
   * @param columnIds
   *          The columns' database IDs.
   * @throws RecordNotFoundException
   *           If the {@link SensorType} for any column cannot be established.
   */
  public SensorValuesList(Collection<Long> columnIds,
    DatasetSensorValues allSensorValues) throws RecordNotFoundException {

    SensorType testingSensorType = null;
    SensorAssignments sensorAssignments = allSensorValues.getInstrument()
      .getSensorAssignments();

    for (long columnId : columnIds) {
      if (null == testingSensorType) {
        testingSensorType = sensorAssignments
          .getSensorTypeForDBColumn(columnId);
      } else {
        if (!sensorAssignments.getSensorTypeForDBColumn(columnId)
          .equals(testingSensorType)) {
          throw new IllegalArgumentException(
            "All column IDs must be for the same SensorType");
        }
      }
    }

    this.columnIds = new TreeSet<Long>(columnIds);
    this.sensorType = testingSensorType;
    this.allSensorValues = allSensorValues;
  }

  /**
   * Factory method to build a list directly from a collection of
   * {@link SensorValue}s.
   *
   * <p>
   * All the values in the passed in {@link Collection} must be of the same
   * {@link SensorType}, and have unique timestamps. Otherwise an
   * {@link IllegalArgumentException} will be thrown.
   * </p>
   *
   * @param values
   *          The sensor values.
   * @return The constructed list.
   * @throws RecordNotFoundException
   *           If the {@link SensorType}s for any of the values cannot be
   *           established.
   */
  public static SensorValuesList newFromSensorValueCollection(
    Collection<SensorValue> values, DatasetSensorValues allSensorValues)
    throws RecordNotFoundException {

    TreeSet<Long> columnIds = values.stream().map(SensorValue::getColumnId)
      .collect(Collectors.toCollection(TreeSet::new));

    SensorValuesList list = new SensorValuesList(columnIds, allSensorValues);
    list.addAll(values);

    return list;
  }

  /**
   * Add a {@link SensorValue} to the list.
   *
   * <p>
   * Attempting to add a value from a column other than those listed in
   * {@link #columnIds} will result in an {@link IllegalArgumentException}.
   * Attempting to add a value with a timestamp identical to a value already in
   * the list will also cause an {@link IllegalArgumentException}.
   * </p>
   *
   * @param value
   *          The value to add.
   */
  public void add(SensorValue value) {

    // Null values are not allowed
    if (null == value) {
      throw new IllegalArgumentException("null values are not permitted");
    }

    // The value must have a columnId that matches one of the specified
    // columnIDs
    if (!columnIds.contains(value.getColumnId())) {
      throw new IllegalArgumentException("Invalid column ID");
    }

    if (list.size() == 0) {
      list.add(value);
    } else {
      int lastComparison = TIME_COMPARATOR.compare(value, last());

      if (lastComparison == 0) {
        // Values with identical timestamps are not allowed
        throw SAME_TIMESTAMP_EXCEPTION;
      } else if (lastComparison > 0) {
        // The value being added is after the last value, so we just add it to
        // the end.
        list.add(value);
      } else {

        int binarySearchResult = Collections.binarySearch(list, value,
          TIME_COMPARATOR);

        // Values with the identical timestamps are not allowed
        if (binarySearchResult >= 0) {
          throw SAME_TIMESTAMP_EXCEPTION;
        } else {
          list.add((binarySearchResult * -1) - 1, value);
        }
      }
    }

    // Reset list properties for recalculation
    measurementMode = -1;
    rawTimesCache = null;
    outputValues = null;
    valueTimesCache = null;
  }

  /**
   * Add all the supplied {@link SensorValue}s to the list.
   *
   * <p>
   * All the restrictions for adding values enforced in the
   * {@link #add(SensorValue)} method apply.
   * </p>
   *
   * @param values
   *          The values to add.
   */
  private void addAll(Collection<? extends SensorValue> values) {
    values.forEach(this::add);
  }

  /**
   * Add the contents of another {@link SensorValuesList} to this list.
   *
   * <p>
   * All the restrictions for adding values enforced in the
   * {@link #add(SensorValue)} method apply.
   * </p>
   *
   * @param values
   *          The list of values to add.
   */
  public void addAll(SensorValuesList values) {
    if (null != values) {
      values.list.forEach(this::add);
    }
  }

  /**
   * Remove a value from the list.
   *
   * @param sensorValue
   *          The value to remove.
   * @return If the list was changed.
   */
  public boolean remove(SensorValue sensorValue) {
    return list.remove(sensorValue);
  }

  /**
   * Determine whether or not the list is empty.
   *
   * @return {@code true} if the list is empty; {@code false} if it contains any
   *         values.
   */
  public boolean isEmpty() {
    return list.isEmpty();
  }

  /**
   * Construct the output values available via this list.
   *
   * <p>
   * If the measurement mode is CONTINUOUS, the values will match the timestamps
   * of the underlying {@link SensorValue}s. If the mode is PERIODIC, they will
   * be constructed by averaging the values of groups of measurements with the
   * timestamp as the midpoint of each group. (For text-only values, groups will
   * be constructed using consecutive measurements with the same value.)
   * </p>
   *
   * @throws SensorValuesListException
   */
  private void buildOutputValues() throws SensorValuesListException {

    outputValues = new ArrayList<SensorValuesListValue>();

    switch (getMeasurementMode()) {
    case MODE_CONTINUOUS: {
      buildContinuousOutputValues();
      break;
    }
    case MODE_PERIODIC: {
      buildPeriodicOutputValues();
      break;
    }
    default: {
      throw new IllegalStateException("Invalid measurement mode");
    }
    }

    valueTimesCache = outputValues.stream().map(SensorValuesListValue::getTime)
      .toList();
  }

  /**
   * Build the output values for a list with PERIODIC measurement mode.
   *
   * <p>
   * The output values are built differently according to whether the list
   * contains string or numeric values.
   * </p>
   *
   * @throws SensorValuesListException
   */
  private void buildPeriodicOutputValues() throws SensorValuesListException {
    if (containsStringValue()) {
      buildPeriodicStringOutputValues();
    } else {
      buildPeriodicNumericOutputValues();
    }
  }

  /**
   * Build the output values for a list with PERIODIC measurement mode
   * containing string values.
   *
   * <p>
   * Groups of measurements are constructed such that:
   * </p>
   *
   * <ul>
   * <li>Values are within the {@link #CONTINUOUS_MEASUREMENT_LIMIT}.</li>
   * <li>All entries in the group have the same value.</li>
   * </ul>
   *
   * <p>
   * The timestamp for each group will be the midpoint between the first and
   * last entries in the group.
   * </p>
   */
  private void buildPeriodicStringOutputValues()
    throws SensorValuesListException {

    try {
      outputValues = new ArrayList<SensorValuesListValue>();

      LocalDateTime groupStartTime = null;
      LocalDateTime groupEndTime = null;
      String groupValue = null;
      List<SensorValue> groupMembers = new ArrayList<SensorValue>();

      for (SensorValue sensorValue : list) {

        // Ignore empty and flushing values
        if (null != sensorValue.getValue() && !sensorValue.getValue().equals("")
          && !sensorValue.getUserQCFlag().equals(Flag.FLUSHING)) {

          // If this is the first value...
          if (null == groupStartTime) {
            groupStartTime = sensorValue.getTime();
            groupEndTime = sensorValue.getTime();
            groupValue = sensorValue.getValue();
            groupMembers.add(sensorValue);
          } else {

            boolean endGroup = false;

            if (!sensorValue.getValue().equals(groupValue)) {
              endGroup = true;
            } else if (DateTimeUtils.secondsBetween(groupEndTime,
              sensorValue.getTime()) > CONTINUOUS_MEASUREMENT_LIMIT) {

              endGroup = true;
            }

            if (endGroup) {
              // All values have the same value, so we grab the first one to get
              // details
              SensorValue firstValue = groupMembers.stream().findFirst().get();

              SensorValuesListValue outputValue = new SensorValuesListValue(
                groupStartTime, groupEndTime,
                DateTimeUtils.midPoint(groupStartTime, groupEndTime),
                groupMembers, sensorType, firstValue.getValue(),
                firstValue.getDisplayFlag(allSensorValues),
                firstValue.getDisplayQCMessage(allSensorValues));
              outputValues.add(outputValue);

              // End time and group members updated outside this block below
              groupStartTime = sensorValue.getTime();
              groupValue = sensorValue.getValue();
              groupMembers = new ArrayList<SensorValue>();
            }

            groupEndTime = sensorValue.getTime();
            groupMembers.add(sensorValue);

          }
        }
      }

      // Finish the last group
      if (null != groupStartTime) {
        // All values have the same value, so we grab the first one to get
        // details
        SensorValue firstValue = groupMembers.stream().findFirst().get();

        SensorValuesListValue outputValue = new SensorValuesListValue(
          groupStartTime, groupEndTime,
          DateTimeUtils.midPoint(groupStartTime, groupEndTime), groupMembers,
          sensorType, firstValue.getValue(),
          firstValue.getDisplayFlag(allSensorValues),
          firstValue.getDisplayQCMessage(allSensorValues));
        outputValues.add(outputValue);
      }
    } catch (Exception e) {
      throw new SensorValuesListException(e);
    }
  }

  /**
   * Build the output values for a list with PERIODIC measurement mode
   * containing string values.
   *
   * <p>
   * Groups of measurements are constructed such that:
   * </p>
   *
   * <ul>
   * <li>Values are within the {@link #CONTINUOUS_MEASUREMENT_LIMIT}.</li>
   * <li>The group value is the mean of the member values with the best QC
   * flag.</li>
   * </ul>
   *
   * <p>
   * The timestamp for each group will be the midpoint between the first and
   * last entries in the group. Every member of the group will be considered for
   * these regardless of its QC flag.
   * </p>
   *
   * @throws SensorValuesListException
   */
  private void buildPeriodicNumericOutputValues()
    throws SensorValuesListException {

    // Collect the members of a group together
    List<SensorValue> groupMembers = new ArrayList<SensorValue>();

    for (SensorValue sensorValue : list) {
      if (!sensorValue.isNaN()
        && !sensorValue.getUserQCFlag().equals(Flag.FLUSHING)) {

        if (groupMembers.size() == 0) {
          groupMembers.add(sensorValue);
        } else {

          LocalDateTime groupEndTime = groupMembers.get(groupMembers.size() - 1)
            .getTime();
          if (DateTimeUtils.secondsBetween(groupEndTime,
            sensorValue.getTime()) > CONTINUOUS_MEASUREMENT_LIMIT) {

            LocalDateTime groupStartTime = groupMembers.get(0).getTime();

            outputValues.add(makeNumericValue(groupMembers,
              DateTimeUtils.midPoint(groupStartTime, groupEndTime), false));
            groupMembers = new ArrayList<SensorValue>();
          }

          groupMembers.add(sensorValue);
        }
      }
    }

    if (groupMembers.size() > 0) {
      LocalDateTime groupStartTime = groupMembers.get(0).getTime();
      LocalDateTime groupEndTime = groupMembers.get(groupMembers.size() - 1)
        .getTime();
      outputValues.add(makeNumericValue(groupMembers,
        DateTimeUtils.midPoint(groupStartTime, groupEndTime), false));
    }
  }

  /**
   * Create a {@link NumericValue} using the mean of the best quality entries in
   * the supplied set of {@link SensorValue}s.
   *
   * <p>
   * The {@link SensorValue}s are assumed to be in ascending time order for the
   * purposes of calculating the result's timestamp. The timestamp is calculated
   * from the values that are not missing or flushing: even if the first value
   * is {@link Flag#BAD}, its timestamp will count.
   * </p>
   *
   * @param sensorValues
   *          The input {@link SensorValue}s.
   * @return The generated value.
   * @throws SensorValuesListException
   */
  private SensorValuesListOutput makeNumericValue(
    List<SensorValue> sensorValues, LocalDateTime nominalTime,
    boolean allowInterpolatesOverFlags) throws SensorValuesListException {

    // Get the timestamps for the value
    List<LocalDateTime> timestamps = sensorValues.stream()
      .filter(v -> !v.isNaN()
        && !v.getDisplayFlag(allSensorValues).equals(Flag.FLUSHING))
      .map(v -> v.getTime()).toList();

    LocalDateTime startTime = timestamps.get(0);
    LocalDateTime endTime = timestamps.get(timestamps.size() - 1);

    // Work out which flags are represented in the sensor values
    List<Flag> presentFlags = sensorValues.stream()
      .map(sv -> sv.getDisplayFlag(allSensorValues)).distinct().toList();

    Flag chosenFlag;

    if (presentFlags.contains(Flag.GOOD)
      || presentFlags.contains(Flag.ASSUMED_GOOD)) {
      chosenFlag = Flag.GOOD;
    } else if (presentFlags.contains(Flag.QUESTIONABLE)) {
      chosenFlag = Flag.QUESTIONABLE;
    } else if (presentFlags.contains(Flag.BAD)) {
      chosenFlag = Flag.BAD;
    } else {
      throw new IllegalStateException("No valid flags in sensor values");
    }

    try {

      boolean interpolatesOverFlags = false;

      List<SensorValue> usedValues;

      if (chosenFlag.isGood()) {
        usedValues = sensorValues.stream()
          .filter(v -> v.getDisplayFlag(allSensorValues).isGood()).toList();
        if (Flag.containsWorseFlag(presentFlags, Flag.GOOD)) {
          if (allowInterpolatesOverFlags) {
            interpolatesOverFlags = true;
          }
        }
      } else {
        usedValues = sensorValues.stream()
          .filter(v -> v.getDisplayFlag(allSensorValues).equals(chosenFlag))
          .toList();
        if (Flag.containsWorseFlag(presentFlags, chosenFlag)) {
          if (allowInterpolatesOverFlags) {
            interpolatesOverFlags = true;
          }
        }
      }

      // Use a Set so we don't get duplicate QC messages in the output value
      TreeSet<String> qcMessages = new TreeSet<String>();

      for (SensorValue v : usedValues) {
        String message = v.getDisplayQCMessage(allSensorValues);

        if (null != message) {
          qcMessages.add(message);
        }
      }

      MeanCalculator mean = new MeanCalculator(
        usedValues.stream().map(SensorValue::getDoubleValue).toList());

      return new SensorValuesListOutput(startTime, endTime, nominalTime,
        usedValues, sensorType, mean.mean(), chosenFlag,
        StringUtils.collectionToDelimited(qcMessages, ";"),
        interpolatesOverFlags);
    } catch (Exception e) {
      throw new SensorValuesListException(e);
    }

  }

  /**
   * Determine whether or not the list contains any non-numeric ({@link String})
   * values.
   *
   * @return {@code true} if at least one non-numeric value exists;
   *         {@code false} if there are none.
   */
  private boolean containsStringValue() {
    Optional<SensorValue> foundString = list.stream()
      .filter(v -> null != v.getValue() && !v.isNumeric()).findAny();
    return foundString.isPresent();
  }

  /**
   * Build the output values for a list with CONTINUOUS measurement mode.
   *
   * <p>
   * The resulting output values consist of all {@link SensorValue}s except
   * those with {@code null} values and those with a {@link Flag#FLUSHING} QC
   * flag.
   * </p>
   *
   * @throws SensorValuesListException
   */
  private void buildContinuousOutputValues() throws SensorValuesListException {
    for (SensorValue sensorValue : list) {
      // We skip null values
      if (null != sensorValue.getValue()
        && !sensorValue.getUserQCFlag().equals(Flag.FLUSHING)) {
        outputValues.add(
          new SensorValuesListValue(sensorValue, sensorType, allSensorValues));
      }
    }
  }

  /**
   * Get the list of times for values in the list.
   *
   * <p>
   * For CONTINUOUS mode, this is the list of every valid {@link SensorValue}.
   * For PERIODIC mode, this is the midpoint of every group of values.
   * </p>
   *
   * @return The list of times.
   * @throws SensorValuesListException
   *           If the times cannot be retrieved.
   */
  public List<LocalDateTime> getValueTimes() throws SensorValuesListException {
    if (null == outputValues) {
      buildOutputValues();
    }

    return Collections.unmodifiableList(valueTimesCache);
  }

  /**
   * Get a list of the timestamps of all the {@link SensorValue}s in the list.
   *
   * return The value times.
   */
  public List<LocalDateTime> getRawTimes() {
    if (null == rawTimesCache) {
      rawTimesCache = list.stream().map(SensorValue::getTime).toList();
    }

    return rawTimesCache;
  }

  /**
   * Construct a {@link SensorValuesListValue} for the specified time using the
   * values in this list.
   *
   * <p>
   * The value will be constructed from the {@link #outputValues}. If there is a
   * value exactly corresponding to the specified time it will be used as the
   * result. Otherwise the {@link SensorValuesListValue} will be constructed
   * from an interpolation as follows:
   * </p>
   *
   * <p>
   * If the list has a CONTINUOUS measurement mode, it will interpolate between
   * the two closest values either side of the specified time, as long as those
   * values are within the {@link #CONTINUOUS_MEASUREMENT_LIMIT}. The
   * interpolation will attempt to use only {@link Flag#GOOD} values in the
   * first instance, but fall back to {@link Flag#QUESTIONABLE} or
   * {@link Flag#BAD} values if required. If only one value is available (either
   * before or after the specified time), that value will be used. It will still
   * be marked as {@link PlotPageTableValue#INTERPOLATED_TYPE}.
   * </p>
   *
   * <p>
   * If the list has a PERIODIC measurement mode, it will interpolate between
   * the groups either side of the time. The two groups must have the same QC
   * flag. If not, or if only one value is available (before or after the
   * specified time), then only the group with the best QC flag will be used. It
   * will still be marked as {@link PlotPageTableValue#INTERPOLATED_TYPE}.
   * </p>
   *
   * <p>
   * Each interpolation is a linear interpolation in time between the two chosen
   * values, calculating the numeric value to the point of the specified time
   * between the two chosen times.
   * </p>
   *
   * @param time
   *          The desired time
   *
   * @return The constructed {@link MeasurementValue}.
   * @throws SensorValuesListException
   *           If the {@link MeasurementValue} cannot be constructed.
   */
  public SensorValuesListOutput getValue(LocalDateTime time,
    boolean allowInterpolation) throws SensorValuesListException {

    if (null == outputValues) {
      buildOutputValues();
    }

    SensorValuesListOutput result;

    if (outputValues.size() == 0) {
      result = null;
    } else {
      switch (getMeasurementMode()) {
      case MODE_CONTINUOUS: {
        result = getValueContinuous(time, allowInterpolation);
        break;
      }
      case MODE_PERIODIC: {
        result = getValuePeriodic(time, allowInterpolation);
        break;
      }
      default: {
        throw new IllegalStateException("Invalid measurement mode");
      }
      }
    }

    return result;
  }

  /**
   * Construct a {@link SensorValuesListValue} for the list in CONTINUOUS mode.
   *
   * @param time
   *          The required time.
   * @return The constructed {@link SensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   * @see #getValue(LocalDateTime)
   */
  private SensorValuesListOutput getValueContinuous(LocalDateTime time,
    boolean allowInterpolation) throws SensorValuesListException {

    SensorValuesListOutput result;

    int searchIndex = Collections.binarySearch(valueTimesCache, time);
    SensorValuesListValue exactMatch = null;

    if (searchIndex >= 0) {
      exactMatch = outputValues.get(searchIndex);
    }

    if (!allowInterpolation
      || (null != exactMatch && exactMatch.getQCFlag().isGood())) {
      // If the exact match is GOOD, use it
      result = new SensorValuesListOutput(exactMatch, false);
    } else {
      // Get the best possible interpolated value
      int priorIndex = searchIndex >= 0 ? searchIndex - 1
        : Math.abs(searchIndex) - 2;
      int postIndex = searchIndex >= 0 ? searchIndex + 1
        : Math.abs(searchIndex) - 1;

      SensorValuesListOutput prior = findInterpContinuousValue(priorIndex, time,
        -1, (x) -> x >= 0);
      SensorValuesListOutput post = findInterpContinuousValue(postIndex, time,
        1, (x) -> x < outputValues.size());

      SensorValuesListOutput interpolated = buildInterpolatedValue(prior, post,
        time, SensorValuesListOutput.interpolatesAroundFlags(prior, post));

      // If the interpolated value has a better flag than the exact match,
      // use that.
      if (null == interpolated) {
        result = null == exactMatch ? null
          : new SensorValuesListOutput(exactMatch, false);
      } else if (null == exactMatch) {
        result = interpolated;
      } else if (exactMatch.getQCFlag()
        .moreSignificantThan(interpolated.getQCFlag())) {
        result = interpolated;
        interpolated.setInterpolatesAroundFlags();
      } else {
        result = new SensorValuesListOutput(exactMatch, false);
        ;
      }
    }

    return result;
  }

  /**
   * Attempt to find the best value from the list to use in constructing an
   * interpolated value.
   *
   * <p>
   * The search will start at {@code startIndex}, and proceed in the direction
   * specified by {@link stepDirection}. The value must have a timestamp within
   * the {@link #CONTINUOUS_MEASUREMENT_LIMIT} of the {@code referenceTime}, and
   * also pass the test predicate provided by {@code limitTest}.
   * </p>
   *
   * <p>
   * The method will return the value with the best {@link Flag} that meets the
   * above criteria.
   * </p>
   *
   * @param startIndex
   *          The start point for the search.
   * @param referenceTime
   *          The time used to determine the temporal limit of the search.
   * @param stepDirection
   *          The search direction.
   * @param limitTest
   *          An additional test that the value must meet.
   * @return The found value.
   */
  private SensorValuesListOutput findInterpContinuousValue(int startIndex,
    LocalDateTime referenceTime, int stepDirection, IntPredicate limitTest) {

    SensorValuesListOutput result = null;
    boolean stopSearch = false;

    int currentIndex = startIndex;

    // If the start point is already outside the bounds, abort.
    if (!limitTest.test(currentIndex)) {
      stopSearch = true;
    }

    while (!stopSearch) {
      SensorValuesListValue testValue = outputValues.get(currentIndex);

      // Check that we're still within the interpolation limit
      if (Math.abs(DateTimeUtils.secondsBetween(referenceTime,
        testValue.getTime())) > CONTINUOUS_MEASUREMENT_LIMIT) {
        stopSearch = true;
      } else {
        if (testValue.getQCFlag().equals(Flag.GOOD)) {
          // We want the first GOOD value we find
          boolean interpolatesAroundFlags = false;
          if (null != result && !result.getQCFlag().isGood()) {
            interpolatesAroundFlags = true;
          }
          result = new SensorValuesListOutput(testValue,
            interpolatesAroundFlags);
          stopSearch = true;
        } else {
          if (null == result) {
            // If this is the first value in the search, store it
            result = new SensorValuesListOutput(testValue, false);
          } else {
            /*
             * Store this value if it's got a better flag than what we already
             * have. Note that we do this search 'backwards' so we can use the
             * flag comparison methods.
             */
            if (result.getQCFlag().moreSignificantThan(testValue.getQCFlag())) {
              result = new SensorValuesListOutput(testValue, true);
            }
          }

          // Prepare for the next iteration
          currentIndex = currentIndex + stepDirection;
          if (!limitTest.test(currentIndex)) {
            // We failed the predicate test,
            // which is the limit to the search
            stopSearch = true;
          }
        }
      }
    }

    return result;
  }

  /**
   * Construct a {@link SensorValuesListValue} for the list in PERIODIC mode.
   *
   * @param time
   *          The required time.
   * @return The constructed {@link SensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   * @see #getValue(LocalDateTime)
   */
  private SensorValuesListOutput getValuePeriodic(LocalDateTime time,
    boolean allowInterpolation) throws SensorValuesListException {

    SensorValuesListOutput result;

    /*
     * Search for a group of the specified time. This works as a binary search,
     * but: 1. If the time is inside a group, we have an exact match and get the
     * index of that group. 2. If we fall between groups, we get the negative
     * result per a standard binary search.
     */
    int searchIndex = Collections.binarySearch(outputValues,
      makeDummyValue(time), new SensorValuesListValueTimeComparator());

    SensorValuesListValue exactMatch = searchIndex >= 0
      ? outputValues.get(searchIndex)
      : null;

    if (null != exactMatch) {
      result = new SensorValuesListOutput(exactMatch, time, false);
    } else if (!allowInterpolation) {
      result = null;
    } else {
      // Get the previous and next groups
      SensorValuesListValue prior = null;
      SensorValuesListValue post = null;

      // Get the best possible interpolated value
      int priorIndex = searchIndex >= 0 ? searchIndex - 1
        : Math.abs(searchIndex) - 2;

      if (priorIndex >= 0 && priorIndex < outputValues.size()) {
        prior = outputValues.get(priorIndex);
      }

      int postIndex = searchIndex >= 0 ? searchIndex + 1
        : Math.abs(searchIndex) - 1;

      if (postIndex >= 0 && postIndex < outputValues.size()) {
        post = outputValues.get(postIndex);
      }

      if (null == prior) {
        result = new SensorValuesListOutput(post, time, false);
      } else if (null == post) {
        result = new SensorValuesListOutput(prior, time, false);
      } else {
        result = getBestOrInterpolate(prior, post, time);
      }
    }

    return result;
  }

  /**
   * Takes two values and returns either the value with the best quality
   * {@link Flag}, or an interpolation of the two values if they both have the
   * same quality {@link Flag}.
   *
   * @param first
   *          The first value.
   * @param second
   *          The second value.
   * @param targetTime
   *          The timestamp to use for the interpolated value, if required.
   * @return The selected or interpolated value.
   * @throws SensorValuesListException
   *           If the interpolation cannot be performed.
   */
  private SensorValuesListOutput getBestOrInterpolate(
    SensorValuesListValue first, SensorValuesListValue second,
    LocalDateTime targetTime) throws SensorValuesListException {

    SensorValuesListOutput result;

    if (first.getQCFlag().moreSignificantThan(second.getQCFlag())) {
      result = new SensorValuesListOutput(second, false);
    } else if (second.getQCFlag().moreSignificantThan(first.getQCFlag())) {
      result = new SensorValuesListOutput(first, false);
    } else {
      // Equal flags -> interpolate
      result = buildInterpolatedValue(first, second, targetTime, false);
    }

    return result;
  }

  /**
   * Create a dummy value with the specified timestamp.
   *
   * <p>
   * Used to search for values based on a timestamp for which a value may or may
   * not exist. If such a value does not exist, it will trigger the
   * interpolation mechanism.
   * </p>
   *
   * @param time
   *          The required timestamp.
   * @return The dummy value.
   */
  private SensorValuesListValue makeDummyValue(LocalDateTime time) {
    return new SensorValuesListValue(time, time, time,
      new ArrayList<SensorValue>(), sensorType, "DUMMY", Flag.BAD, "DUMMY");
  }

  /**
   * Construct an interpolated {@link SensorValuesListValue} from two
   * {@link SensorValuesListValue}s.
   *
   * <p>
   * The value of the result will be the linear interpolation of the supplied
   * values to the specified target time. If there is only a prior or post
   * value, no interpolation will be performed and a {@code null} value will be
   * returned. If the prior and post values have different flags, the
   * interpolated value will be given the worst of those flags.
   * </p>
   *
   * @param first
   *          The first value.
   * @param second
   *          The second value.
   * @param targetTime
   *          The target time.
   * @return The constructed {@link SensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  private SensorValuesListOutput buildInterpolatedValue(
    SensorValuesListValue first, SensorValuesListValue second,
    LocalDateTime targetTime, boolean interpolatingOverFlag)
    throws SensorValuesListException {

    SensorValuesListOutput result;

    if (null == first || null == second) {
      // Only 'interpolate' if we have values both before and after
      result = null;
    } else {
      if (!first.getQCFlag().isGood() || !second.getQCFlag().isGood()) {
        // We only interpolate good values
        /*
         * I'm not sure why this restriction is here, but it's built into the
         * unit tests so I assume there was a good reason that I failed to
         * document.
         */
        result = null;
      } else {
        Double interpValue = Calculators.interpolate(first.getTime(),
          first.getDoubleValue(), second.getTime(), second.getDoubleValue(),
          targetTime);

        TreeSet<SensorValue> combinedSourceValues = new TreeSet<SensorValue>(
          TIME_COMPARATOR);
        combinedSourceValues.addAll(first.getSourceSensorValues());
        combinedSourceValues.addAll(second.getSourceSensorValues());

        result = new SensorValuesListOutput(first.getStartTime(),
          second.getEndTime(), targetTime, combinedSourceValues,
          first.getSensorType(), interpValue,
          Flag.getMostSignificantFlag(first.getQCFlag(), second.getQCFlag()),
          StringUtils.combine(first.getQCMessage(), second.getQCMessage(), ";",
            true),
          interpolatingOverFlag);
      }
    }
    return result;
  }

  /**
   * Get the set of output values for the list.
   *
   * <p>
   * The output values are constructed according to the measurement mode of the
   * list and the QC flags of the member {@link SensorValue}s.
   * </p>
   *
   * @return The output values.
   * @throws SensorValuesListException
   *
   * @see #buildOutputValues()
   */
  public List<SensorValuesListValue> getValues()
    throws SensorValuesListException {
    if (null == outputValues) {
      buildOutputValues();
    }

    return Collections
      .unmodifiableList(new ArrayList<SensorValuesListValue>(outputValues));
  }

  /**
   * Returns the number of values in the list taking into account any averaging
   * performed for PERIODIC mode.
   *
   * @return The number of values in the list.
   * @throws SensorValuesListException
   *           If the output values cannot be constructed.
   */
  public int valuesSize() throws SensorValuesListException {
    if (null == outputValues) {
      buildOutputValues();
    }

    return outputValues.size();
  }

  /**
   * Get the last value in the list.
   *
   * @return The last value.
   */
  private SensorValue last() {
    return list.get(list.size() - 1);
  }

  /**
   * Get the measurement mode for the {@link SensorValue}s.
   *
   * <p>
   * This is either CONTINUOUS mode if most measurements are taken within the
   * {@link #CONTINUOUS_MEASUREMENT_LIMIT}, or PERIODIC mode if measurements are
   * taken in small groups with a longer 'sleep' periods between.
   * </p>
   *
   * @return The measurement mode.
   */
  public int getMeasurementMode() {
    if (measurementMode == -1) {
      calculateMeasurementMode();
    }

    return measurementMode;
  }

  /**
   * Calculate the measurement mode for these {@link SensorValue}s.
   *
   * <p>
   * We create groups of {@link SensorValue}s whose timestamps are within the
   * {@link #CONTINUOUS_MEASUREMENT_LIMIT}. If either the mean group size or the
   * largest group size are within the {@link #MAX_PERIODIC_GROUP_SIZE} then the
   * measurement mode is {@link #MODE_PERIODIC}; otherwise it is
   * {@link #MODE_CONTINUOUS}.
   * </p>
   *
   * <p>
   * If the list contains String values, we also create new groups whenever the
   * value changes (in addition to the time difference threshold).
   * </p>
   */
  private void calculateMeasurementMode() {

    // If the list contains string values, we operate slightly differently.
    boolean stringMode = allowStringPeriodicGroups && containsStringValue();

    // The largest group size
    int largeGroupCount = 0;

    // Calculate the mean group size as we go along
    int groupsByTime = 0;
    int totalGroupCount = 0;
    float meanGroupSize = 0f;

    int groupSize = 0;
    for (int i = 1; i < list.size(); i++) {
      long timeDiff = DateTimeUtils.secondsBetween(list.get(i - 1).getTime(),
        list.get(i).getTime());

      boolean newGroupFromTime = timeDiff > CONTINUOUS_MEASUREMENT_LIMIT;

      boolean newGroupFromValue = stringMode
        ? SensorValue.valuesEqual(list.get(i - 1), list.get(i))
        : false;

      if (newGroupFromTime || newGroupFromValue) {
        if (newGroupFromTime) {
          groupsByTime++;
        }

        if (groupSize > 0) {

          // Update the max group size
          if (groupSize > MAX_PERIODIC_GROUP_SIZE) {
            largeGroupCount++;
          }

          // Update the running mean group size
          totalGroupCount++;
          meanGroupSize = meanGroupSize
            + (groupSize - meanGroupSize) / totalGroupCount;

          // Reset the group
          groupSize = 0;
        }
      }

      groupSize++;
    }

    // Tidy up from the last value
    if (groupSize > 0) {
      // Update the max group size
      if (groupSize > MAX_PERIODIC_GROUP_SIZE) {
        largeGroupCount++;
      }

      // Update the running mean group size
      totalGroupCount++;
      meanGroupSize = meanGroupSize
        + (groupSize - meanGroupSize) / (float) totalGroupCount;
    }

    if (groupsByTime > 1 && (meanGroupSize <= MAX_PERIODIC_GROUP_SIZE
      && largeGroupCount <= LARGE_GROUP_LIMIT)) {
      measurementMode = MODE_PERIODIC;
    } else {
      measurementMode = MODE_CONTINUOUS;
    }
  }

  /**
   * Get all the raw {@link SensorValue} objects in the list.
   *
   * <p>
   * This allows access to the individual {@link SensorValue} objects without
   * being able to modify the overall list.
   * </p>
   *
   * @return The {@link SensorValue}s in the list.
   */
  public List<SensorValue> getRawValues() {
    return Collections.unmodifiableList(list);
  }

  /**
   * Get the raw {@link SensorValue}s between two times. Both times are
   * inclusive.
   *
   * @param start
   *          The start time.
   * @param end
   *          The end time.
   * @return The {@link SensorValue}s between the given times.
   */
  public List<SensorValue> getRawValues(LocalDateTime start,
    LocalDateTime end) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    int currentIndex = Collections.binarySearch(getRawTimes(), start);

    // If we didn't get an exact match, move to the index after the returned
    // insertion point.
    if (currentIndex < 0) {
      currentIndex = Math.abs(currentIndex) - 1;
      if (currentIndex < 0) {
        currentIndex = 0;
      }
    }

    while (currentIndex < list.size() - 1
      && !list.get(currentIndex).getTime().isAfter(end)) {
      result.add(list.get(currentIndex));
      currentIndex++;
    }

    return result;
  }

  /**
   * Get a single {@link SensorValue} from the list referenced by timestamp.
   *
   * <p>
   * This method should be used for QC purposes. To get values for use in data
   * reduction, use {@link #getMeasurementValue(LocalDateTime)}.
   * </p>
   *
   * @param time
   *          The desired timestamp.
   * @return The value with the specified timestamp, or {@code null} if there is
   *         not one.
   */
  public SensorValue getRawSensorValue(LocalDateTime time) {
    int searchIndex = Collections.binarySearch(getRawTimes(), time);
    return searchIndex < 0 ? null : list.get(searchIndex);
  }

  /**
   * Get the closest raw {@link SensorValue}(s) to the specified time.
   *
   * <p>
   * If there is a value at exactly the specified time, that value is returned.
   * If there is no such value, the values immediately before and after the time
   * are returned, if they exist.
   * </p>
   *
   * @param assignment
   *          The column to search.
   * @param time
   *          The time.
   * @return The closest values.
   */
  public List<SensorValue> getClosestSensorValues(LocalDateTime time) {
    List<SensorValue> result = new ArrayList<SensorValue>(2);

    int searchIndex = Collections.binarySearch(getRawTimes(), time);
    if (searchIndex >= 0) {
      result.add(list.get(searchIndex));
    } else {
      int priorIndex = Math.abs(searchIndex) - 2;
      int postIndex = Math.abs(searchIndex) - 1;

      if (priorIndex >= 0) {
        result.add(list.get(priorIndex));
      }

      if (postIndex < list.size()) {
        result.add(list.get(postIndex));
      }
    }

    return result;
  }

  /**
   * Get the number of individual {@link SensorValue} objects in the list.
   *
   * <p>
   * Note that this may differ from the number of values returned by
   * {@link #size()} method, depending on the measurement mode.
   * </p>
   *
   * @return The total number of {@link SensorValue}s in the list.
   */
  public int rawSize() {
    return list.size();
  }

  /**
   * Get a value from the list that has a timestamp matching the specified time,
   * or the value immediately before it. The usual rules regarding limiting
   * searches to the {@link #CONTINUOUS_MEASUREMENT_LIMIT} are not applied.
   *
   * <p>
   * If the measurement mode is CONTINUOUS, the method will return the value on
   * or before the specified time.
   * </p>
   *
   * <p>
   * If the mode is PERIODIC, the values are collected into groups of
   * consecutive {@link SensorValue}s (where each group boundary is either a
   * change in value or a period between measurements larger than
   * {@link #CONTINUOUS_MEASUREMENT_LIMIT}) where the timestamp for each group
   * is the midpoint of each group. The returned value will be that found in the
   * group encompassing the passed in time, or the group immediately preceding
   * that time.
   * </p>
   *
   * @param time
   *          The desired time.
   * @return The matched value with its timestamp.
   * @throws SensorValuesListException
   */
  public SensorValuesListOutput getValueOnOrBefore(LocalDateTime time)
    throws SensorValuesListException {

    if (null == outputValues) {
      buildOutputValues();
    }

    SensorValuesListValue result = null;

    List<LocalDateTime> times = getValueTimes();
    int searchIndex = Collections.binarySearch(times, time);

    // A >= 0 = an exact match
    if (searchIndex >= 0) {
      result = outputValues.get(searchIndex);
    } else {

      /*
       * If we get a -1 result, there is no value before the requested time. So
       * we don't return anything. Otherwise we return the value before the
       * returned insertion point.
       */
      if (searchIndex < -1) {
        int getIndex = Math.abs(searchIndex) - 2;
        if (getIndex >= 0) {
          result = outputValues.get(getIndex);
        }
      }

    }

    return new SensorValuesListOutput(result, false);
  }

  /**
   * Construct a {@link SensorValuesListValue} using {@link SensorValue}s in the
   * specified time range.
   *
   * <p>
   * The method finds values to use in two stages.
   * </p>
   *
   * <p>
   * First, it ignores the measurement mode and looks directly at the raw
   * values. If values are found within the range of the start and end time
   * (both inclusive), it collects the values with the best available QC flag
   * and provides an averaged value as the result.
   * </p>
   *
   * <p>
   * If no usable raw values are found and {@code allowInterpolation} is
   * {@code true}, the method falls back to using the
   * {@link #getValue(LocalDateTime)} method, using the nominal time as the
   * parameter.
   * </p>
   *
   * <p>
   * This method exists to handle cases of PERIODIC measurements where the
   * instrument wakes up, measures some measurements in one mode (aka Run Type)
   * and some in another mode (e.g. water and air) back to back. The standard
   * methods provided here cannot distinguish these, so an external method (e.g.
   * {@link DataReductionJob}) will determine the periods for each Run Type and
   * request the values from those periods only. The fallback described above is
   * for if a given sensor is measuring to a different schedule, which indicates
   * that it's running independently of the run types and therefore using
   * different values doesn't matter.
   * </p>
   *
   * @param start
   *          The start time.
   * @param end
   *          The end time.
   * @param nominalTime
   *          The nominal time for the constructed value.
   * @param allowInterpolation
   *          Indicates whether interpolation can be used to try to find a value
   *          if nothing is available in the specified range.
   * @return The constructed {@link SensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  public SensorValuesListOutput getValue(LocalDateTime start, LocalDateTime end,
    LocalDateTime nominalTime, boolean allowInterpolation)
    throws SensorValuesListException {

    List<SensorValue> usedValues = new ArrayList<SensorValue>();

    // Search for the start time
    int startSearchIndex = Collections.binarySearch(getRawTimes(), start);

    int currentIndex;
    if (startSearchIndex >= 0) {
      currentIndex = startSearchIndex;
    } else {
      // Get the index immediately after the requested time
      currentIndex = Math.abs(startSearchIndex) - 1;
      if (currentIndex < 0) {
        currentIndex = 0;
      }
    }

    // Get all the values between the start and end times
    while (currentIndex < getRawTimes().size()
      && !getRawTimes().get(currentIndex).isAfter(end)) {
      usedValues.add(list.get(currentIndex));
      currentIndex++;
    }

    SensorValuesListOutput result = null;

    if (usedValues.size() > 0) {
      result = makeNumericValue(usedValues, nominalTime, true);
    } else if (allowInterpolation) {
      result = getValue(nominalTime, true);
    }

    return result;
  }

  /**
   * Construct a {@link SensorValuesListValue} using {@link SensorValue}s in the
   * time range specified in the provided {@link SensorValuesListValue}.
   *
   * <p>
   * This method simply extracts the times and passes them to
   * {@link #getValue(LocalDateTime, LocalDateTime, LocalDateTime)}.
   * </p>
   *
   *
   * @param timeReference
   *          A {@link SensorValuesListValue} object containing the times
   *          required.
   * @param allowInterpolation
   *          Indicates whether interpolation can be used to try to find a value
   *          if nothing is available in the time range of the passed in value.
   * @return The constructed {@link SensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  public SensorValuesListOutput getValue(SensorValuesListValue timeReference,
    boolean allowInterpolation) throws SensorValuesListException {

    return getValue(timeReference.getStartTime(), timeReference.getEndTime(),
      timeReference.getNominalTime(), allowInterpolation);
  }

  /**
   * Determine whether or not the list contains a {@link SensorValue} with the
   * specified timestamp.
   *
   * @param time
   *          The timestamp.
   * @return {@code true} if the list contains a {@link SensorValue} with the
   *         specified timestamp; {@code false} otherwise.
   */
  public boolean containsTime(LocalDateTime time) {
    return Collections.binarySearch(getRawTimes(), time) >= 0;
  }

  /**
   * Clear any already calculated output values, forcing them to be recalculated
   * at the next call to {@code getValue} methods.
   */
  public void resetOutput() {
    outputValues = null;
  }

  /**
   * Set a flag indicating whether or not changes in String values can be used
   * to identify measurement groups.
   *
   * @param allow
   *          Indicates whether string groups are allowed
   * @throws SensorValuesListException
   * @see #calculateMeasurementMode()
   */
  public void allowStringValuesToDefineGroups(boolean allow)
    throws SensorValuesListException {

    if (allow && !containsStringValue()) {
      throw new SensorValuesListException("No string values in list");
    } else if (allow != allowStringPeriodicGroups) {
      // Only reset things if we're actually changing the flag
      allowStringPeriodicGroups = allow;
      resetOutput();
    }
  }
}

/**
 * Comparator class that compares two {@link SensorValue} objects by their
 * timestamp.
 */
class SensorValueTimeComparator implements Comparator<SensorValue> {
  @Override
  public int compare(SensorValue o1, SensorValue o2) {
    return o1.getTime().compareTo(o2.getTime());
  }
}
