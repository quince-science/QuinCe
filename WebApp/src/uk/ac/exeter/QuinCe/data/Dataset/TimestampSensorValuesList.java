package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.IntPredicate;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * Extended version of {@link SensorValuesList} that operates on time-based
 * values.
 *
 * <p>
 * <b>IMPORTANT: READ ALL OF THIS DESCRIPTION, PARTICULARLY THE NOTE IN THE LAST
 * PARAGRAPH.</b>
 * </p>
 *
 * <p>
 * This class provides several methods for special time-based value handling
 * functions, including different measurement modes and interpolation.
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
 *
 * <p>
 * Values mode results are automatically stripped of {@link Flag#FLUSHING}
 * values.
 * </p>
 *
 * <p>
 * <b>Note:</b> For the PERIODIC mode, the auto-generated
 * {@link TimestampSensorValuesListValue}s may not be suitable for use. For
 * example, a sensor may wake up, take water and air measurements in sequence,
 * and then sleep. The automatic algorithm cannot detect this, and will group
 * both sets of measurements together. To work around this, you must determine
 * the group boundaries elsewhere (most likely in the {@link SensorValuesList}
 * for the Run Type), and use the
 * {@link #getValue(LocalDateTime, LocalDateTime, LocalDateTime, boolean)}
 * method to construct values covering the correct time periods.
 * </p>
 */
public class TimestampSensorValuesList extends SensorValuesList {

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
   * The measurement mode of these {@link SensorValue}s.
   */
  private int measurementMode = -1;

  /**
   * For periodic mode, the interval between groups of measurements (in
   * seconds).
   */
  private long periodicGroupTimeInterval = -1L;

  /**
   * The set of values to be returned to the rest of the application from this
   * list, based on its measurement mode.
   */
  private List<TimestampSensorValuesListValue> outputValues;

  /**
   * A cache of the timestamps of the entries in {@link #outputValues}.
   */
  private List<TimeCoordinate> valueCoordinatesCache = null;

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
  protected TimestampSensorValuesList(long columnId,
    DatasetSensorValues allSensorValues, boolean forceString)
    throws RecordNotFoundException {

    super(columnId, allSensorValues, forceString);
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
  protected TimestampSensorValuesList(Collection<Long> columnIds,
    DatasetSensorValues allSensorValues, boolean forceString)
    throws RecordNotFoundException {

    super(columnIds, allSensorValues, forceString);
  }

  /**
   * Add a {@link SensorValue} to the list.
   *
   * <p>
   * Only values whose coordinates have a {@link Instrument#BASIS_TIME}
   * {@link Coordinate} type can be added. Any other {@link Coordinate} type
   * will result in an {@link IllegalArgumentExcpetion}.
   * </p>
   */
  @Override
  public void add(SensorValue value) {
    if (null == value) {
      throw new IllegalArgumentException("Value cannot be null");
    }

    if (value.getCoordinate().getType() != Instrument.BASIS_TIME) {
      throw new IllegalArgumentException(
        "Only time-based SensorValues are allowed");
    }

    super.add(value);

    // Reset list properties for recalculation
    resetOutput();
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
   * @throws CoordinateException
   */
  private void buildOutputValues() throws SensorValuesListException {

    outputValues = new ArrayList<TimestampSensorValuesListValue>();

    switch (getMeasurementMode()) {
    case MODE_CONTINUOUS: {
      buildContinuousOutputValues();
      break;
    }
    case MODE_PERIODIC: {
      try {
        buildPeriodicOutputValues();
      } catch (CoordinateException e) {
        throw new SensorValuesListException("Error building list output", e);
      }
      break;
    }
    default: {
      throw new IllegalStateException("Invalid measurement mode");
    }
    }

    valueCoordinatesCache = outputValues.stream()
      .map(v -> (TimeCoordinate) v.getCoordinate()).toList();
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
        && !sensorValue.getUserQCFlag().equals(FlagScheme.FLUSHING_FLAG)) {
        try {
          if (sensorValue.isNumeric()) {
            outputValues.add(new TimestampSensorValuesListValue(sensorValue,
              sensorType, allSensorValues));
          } else {
            outputValues.add(new TimestampSensorValuesListValue(sensorValue,
              sensorType, allSensorValues));
          }
        } catch (RoutineException e) {
          throw new SensorValuesListException(e);
        }
      }
    }
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
   * @throws CoordinateException
   */
  private void buildPeriodicOutputValues()
    throws SensorValuesListException, CoordinateException {
    if (containsStringValue()) {
      buildPeriodicStringOutputValues();
    } else {
      buildPeriodicNumericOutputValues();
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
   * @throws CoordinateException
   */
  private void buildPeriodicNumericOutputValues()
    throws SensorValuesListException, CoordinateException {

    // Collect the members of a group together
    List<SensorValue> groupMembers = new ArrayList<SensorValue>();

    for (SensorValue sensorValue : list) {
      if (!sensorValue.isNaN()
        && !sensorValue.getUserQCFlag().equals(FlagScheme.FLUSHING_FLAG)) {

        if (groupMembers.size() == 0) {
          groupMembers.add(sensorValue);
        } else {

          LocalDateTime groupEndTime = groupMembers.get(groupMembers.size() - 1)
            .getCoordinate().getTime();
          if (DateTimeUtils.secondsBetween(groupEndTime, sensorValue
            .getCoordinate().getTime()) > CONTINUOUS_MEASUREMENT_LIMIT) {

            LocalDateTime groupStartTime = groupMembers.get(0).getCoordinate()
              .getTime();

            outputValues
              .add(makeNumericValue(groupMembers,
                getCoordinate(
                  DateTimeUtils.midPoint(groupStartTime, groupEndTime)),
                false));
            groupMembers = new ArrayList<SensorValue>();
          }

          groupMembers.add(sensorValue);
        }
      }
    }

    if (groupMembers.size() > 0) {
      LocalDateTime groupStartTime = groupMembers.get(0).getCoordinate()
        .getTime();
      LocalDateTime groupEndTime = groupMembers.get(groupMembers.size() - 1)
        .getCoordinate().getTime();
      outputValues.add(makeNumericValue(groupMembers,
        getCoordinate(DateTimeUtils.midPoint(groupStartTime, groupEndTime)),
        false));
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
      outputValues = new ArrayList<TimestampSensorValuesListValue>();

      TimeCoordinate groupStartTime = null;
      TimeCoordinate groupEndTime = null;
      String groupValue = null;
      List<SensorValue> groupMembers = new ArrayList<SensorValue>();

      for (SensorValue sensorValue : list) {

        // Ignore empty and flushing values
        if (null != sensorValue.getValue() && !sensorValue.getValue().equals("")
          && !sensorValue.getUserQCFlag().equals(FlagScheme.FLUSHING_FLAG)) {

          // If this is the first value...
          if (null == groupStartTime) {
            groupStartTime = (TimeCoordinate) sensorValue.getCoordinate();
            groupEndTime = (TimeCoordinate) sensorValue.getCoordinate();
            groupValue = sensorValue.getValue();
            groupMembers.add(sensorValue);
          } else {

            boolean endGroup = false;

            if (!sensorValue.getValue().equals(groupValue)) {
              endGroup = true;
            } else if (DateTimeUtils.secondsBetween(groupEndTime.getTime(),
              sensorValue.getCoordinate()
                .getTime()) > CONTINUOUS_MEASUREMENT_LIMIT) {

              endGroup = true;
            }

            if (endGroup) {
              // All values have the same value, so we grab the first one to get
              // details
              SensorValue firstValue = groupMembers.stream().findFirst().get();

              TimestampSensorValuesListOutput outputValue = new TimestampSensorValuesListOutput(
                groupStartTime, groupEndTime,
                getCoordinate(DateTimeUtils.midPoint(groupStartTime.getTime(),
                  groupEndTime.getTime())),
                groupMembers, sensorType, firstValue.getValue(),
                firstValue.getDisplayFlag(allSensorValues),
                firstValue.getDisplayQCMessage(allSensorValues), false);
              outputValues.add(outputValue);

              // End time and group members updated outside this block below
              groupStartTime = (TimeCoordinate) sensorValue.getCoordinate();
              groupValue = sensorValue.getValue();
              groupMembers = new ArrayList<SensorValue>();
            }

            groupEndTime = (TimeCoordinate) sensorValue.getCoordinate();
            groupMembers.add(sensorValue);

          }
        }
      }

      // Finish the last group
      if (null != groupStartTime) {
        // All values have the same value, so we grab the first one to get
        // details
        SensorValue firstValue = groupMembers.stream().findFirst().get();

        TimestampSensorValuesListOutput outputValue = new TimestampSensorValuesListOutput(
          groupStartTime, groupEndTime,
          getCoordinate(DateTimeUtils.midPoint(groupStartTime.getTime(),
            groupEndTime.getTime())),
          groupMembers, sensorType, firstValue.getValue(),
          firstValue.getDisplayFlag(allSensorValues),
          firstValue.getDisplayQCMessage(allSensorValues), false);
        outputValues.add(outputValue);
      }
    } catch (Exception e) {
      throw new SensorValuesListException(e);
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
  @Override
  public List<Coordinate> getValueCoordinates()
    throws SensorValuesListException {
    if (null == outputValues) {
      buildOutputValues();
    }

    return Collections.unmodifiableList(valueCoordinatesCache);
  }

  /**
   * Construct a {@link TimestampSensorValuesListValue} for the specified time
   * using the values in this list.
   *
   * <p>
   * The value will be constructed from the {@link #outputValues}. If there is a
   * value exactly corresponding to the specified time it will be used as the
   * result. Otherwise the {@link TimestampSensorValuesListValue} will be
   * constructed from an interpolation as follows:
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
  @Override
  public SensorValuesListOutput getValue(Coordinate time,
    boolean allowInterpolation) throws SensorValuesListException {

    if (null == outputValues) {
      buildOutputValues();
    }

    TimestampSensorValuesListOutput result;

    if (outputValues.size() == 0) {
      result = null;
    } else {
      switch (getMeasurementMode()) {
      case MODE_CONTINUOUS: {
        result = getValueContinuous((TimeCoordinate) time, allowInterpolation);
        break;
      }
      case MODE_PERIODIC: {
        result = getValuePeriodic((TimeCoordinate) time, allowInterpolation);
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
   * Construct a {@link TimestampSensorValuesListValue} for the list in
   * CONTINUOUS mode.
   *
   * @param time
   *          The required time.
   * @return The constructed {@link TimestampSensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   * @see #getValue(LocalDateTime)
   */
  private TimestampSensorValuesListOutput getValueContinuous(
    TimeCoordinate time, boolean allowInterpolation)
    throws SensorValuesListException {

    TimestampSensorValuesListOutput result;

    int searchIndex = Collections.binarySearch(getValueCoordinates(), time);
    TimestampSensorValuesListValue exactMatch = null;

    if (searchIndex >= 0) {
      exactMatch = outputValues.get(searchIndex);
    }

    /*
     * If we get an exact match and it's good, then we use it.
     *
     * If we get an exact match but it's not good, and interpolation isn't
     * allowed, then we use it.
     *
     * If don't get an exact match, and interpolation isn't allowed, then return
     * null.
     *
     * Otherwise we interpolate to find a value.
     *
     */
    if (null != exactMatch
      && allSensorValues.getFlagScheme().isGood(exactMatch.getQCFlag(), true)) {
      result = new TimestampSensorValuesListOutput(exactMatch, false);
    } else if (!allowInterpolation) {
      if (null != exactMatch) {
        result = new TimestampSensorValuesListOutput(exactMatch, false);
      } else {
        result = null;
      }
    } else {
      // Get the best possible interpolated value
      int priorIndex = searchIndex >= 0 ? searchIndex - 1
        : Math.abs(searchIndex) - 2;
      int postIndex = searchIndex >= 0 ? searchIndex + 1
        : Math.abs(searchIndex) - 1;

      TimestampSensorValuesListOutput prior = findInterpContinuousValue(
        priorIndex, time.getTime(), -1, (x) -> x >= 0);
      TimestampSensorValuesListOutput post = findInterpContinuousValue(
        postIndex, time.getTime(), 1, (x) -> x < outputValues.size());

      TimestampSensorValuesListOutput interpolated = buildInterpolatedValue(
        prior, post, time,
        SensorValuesListOutput.interpolatesAroundFlags(prior, post));

      // If the interpolated value has a better flag than the exact match,
      // use that.
      if (null == interpolated) {
        result = null == exactMatch ? null
          : new TimestampSensorValuesListOutput(exactMatch, false);
      } else if (null == exactMatch) {
        result = interpolated;
      } else if (exactMatch.getQCFlag()
        .moreSignificantThan(interpolated.getQCFlag())) {
        result = interpolated;
        interpolated.setInterpolatesAroundFlags();
      } else {
        result = new TimestampSensorValuesListOutput(exactMatch, false);
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
  private TimestampSensorValuesListOutput findInterpContinuousValue(
    int startIndex, LocalDateTime referenceTime, int stepDirection,
    IntPredicate limitTest) {

    TimestampSensorValuesListOutput result = null;
    boolean stopSearch = false;

    int currentIndex = startIndex;

    // If the start point is already outside the bounds, abort.
    if (!limitTest.test(currentIndex)) {
      stopSearch = true;
    }

    while (!stopSearch) {
      TimestampSensorValuesListValue testValue = outputValues.get(currentIndex);

      // Check that we're still within the interpolation limit
      if (Math.abs(DateTimeUtils.secondsBetween(referenceTime,
        testValue.getCoordinate().getTime())) > CONTINUOUS_MEASUREMENT_LIMIT) {
        stopSearch = true;
      } else {
        if (allSensorValues.getFlagScheme().isGood(testValue.getQCFlag(),
          true)) {
          // We want the first GOOD value we find
          boolean interpolatesAroundFlags = false;
          if (null != result && !allSensorValues.getFlagScheme()
            .isGood(result.getQCFlag(), true)) {
            interpolatesAroundFlags = true;
          }
          result = new TimestampSensorValuesListOutput(testValue,
            interpolatesAroundFlags);
          stopSearch = true;
        } else {
          if (null == result) {
            // If this is the first value in the search, store it
            result = new TimestampSensorValuesListOutput(testValue, false);
          } else {
            /*
             * Store this value if it's got a better flag than what we already
             * have. Note that we do this search 'backwards' so we can use the
             * flag comparison methods.
             */
            if (result.getQCFlag().moreSignificantThan(testValue.getQCFlag())) {
              result = new TimestampSensorValuesListOutput(testValue, true);
            }
          }

          // Prepare for the next iteration
          currentIndex = currentIndex + stepDirection;
          if (!limitTest.test(currentIndex)) {
            // We fell off the end of the list
            stopSearch = true;
          }
        }
      }
    }

    return result;
  }

  /**
   * Construct a {@link TimestampSensorValuesListValue} for the list in PERIODIC
   * mode.
   *
   * @param time
   *          The required time.
   * @return The constructed {@link TimestampSensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  private TimestampSensorValuesListOutput getValuePeriodic(TimeCoordinate time,
    boolean allowInterpolation) throws SensorValuesListException {

    TimestampSensorValuesListOutput result;

    /*
     * Search for a group of the specified time. This works as a binary search,
     * but: 1. If the time is inside a group, we have an exact match and get the
     * index of that group. 2. If we fall between groups, we get the negative
     * result per a standard binary search.
     */
    int searchIndex = Collections.binarySearch(getValueCoordinates(), time);

    TimestampSensorValuesListValue exactMatch = searchIndex >= 0
      ? outputValues.get(searchIndex)
      : null;

    if (null != exactMatch) {
      result = new TimestampSensorValuesListOutput(exactMatch, time, false);
    } else if (!allowInterpolation) {
      result = null;
    } else {
      // Get the previous and next groups
      TimestampSensorValuesListValue prior = null;
      TimestampSensorValuesListValue post = null;

      // Get the best possible interpolated value
      int priorIndex = searchIndex >= 0 ? searchIndex - 1
        : Math.abs(searchIndex) - 2;

      if (priorIndex >= 0 && priorIndex < outputValues.size()) {
        /*
         * We don't interpolate missing values, so we look for the closest group
         * only. To account for missing values we ensure that this group is
         * within 1.5 times the mean period between measurement groups.
         *
         * The 1.5 factor is to allow some wiggle room for inconsistencies in
         * timing.
         */
        TimestampSensorValuesListValue priorCandidate = outputValues
          .get(priorIndex);

        if (DateTimeUtils.secondsBetween(priorCandidate.getNominalTime(),
          time) <= periodicGroupTimeInterval * 1.5) {

          prior = priorCandidate;
        }
      }

      int postIndex = searchIndex >= 0 ? searchIndex + 1
        : Math.abs(searchIndex) - 1;

      if (postIndex >= 0 && postIndex < outputValues.size()) {
        TimestampSensorValuesListValue postCandidate = outputValues
          .get(postIndex);

        if (DateTimeUtils.secondsBetween(time,
          postCandidate.getNominalTime()) <= periodicGroupTimeInterval * 1.5) {
          post = postCandidate;
        }
      }

      if (null == prior && null == post) {
        result = null;
      } else if (null != prior && prior.encompasses(time)) {
        result = new TimestampSensorValuesListOutput(prior, time, false);
      } else if (null != post && post.encompasses(time)) {
        result = new TimestampSensorValuesListOutput(post, time, false);
      } else if (null == prior) {
        result = new TimestampSensorValuesListOutput(post, time, false);
      } else if (null == post) {
        result = new TimestampSensorValuesListOutput(prior, time, false);
      } else {
        result = getBestOrInterpolate(prior, post, time);
      }
    }

    return result;
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

    // The start times of each group
    List<LocalDateTime> groupStartTimes = new ArrayList<LocalDateTime>();

    // Calculate the mean group size as we go along
    int groupsByTime = 0;
    int totalGroupCount = 0;
    float meanGroupSize = 0f;

    int groupSize = 0;
    for (int i = 1; i < list.size(); i++) {
      long timeDiff = DateTimeUtils.secondsBetween(
        list.get(i - 1).getCoordinate().getTime(),
        list.get(i).getCoordinate().getTime());

      boolean newGroupFromTime = timeDiff > CONTINUOUS_MEASUREMENT_LIMIT;

      boolean newGroupFromValue = stringMode
        ? SensorValue.valuesEqual(list.get(i - 1), list.get(i))
        : false;

      if (newGroupFromTime || newGroupFromValue) {
        if (newGroupFromTime) {
          groupsByTime++;
          groupStartTimes.add(list.get(i).getCoordinate().getTime());
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

      // Calculate the mean time between groups
      MeanCalculator mean = new MeanCalculator();
      for (int i = 1; i < groupStartTimes.size(); i++) {
        mean.add(DateTimeUtils.secondsBetween(groupStartTimes.get(i - 1),
          groupStartTimes.get(i)));
      }
      periodicGroupTimeInterval = Math.round(mean.mean());
    } else {
      measurementMode = MODE_CONTINUOUS;
    }
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
   * <p>
   * <b>Developer Note:</b> Although this algorithm is the same as that in
   * {@link SensorValuesList#getValueOnOrBefore(Coordinate)}, it uses the local
   * specialised sets of coordinates to take account of the possible measurement
   * modes.
   * </p>
   *
   * @param time
   *          The desired time.
   * @return The matched value with its timestamp.
   * @throws SensorValuesListException
   */
  @Override
  public TimestampSensorValuesListValue getValueOnOrBefore(Coordinate time)
    throws SensorValuesListException {

    if (null == outputValues) {
      buildOutputValues();
    }

    TimestampSensorValuesListValue result = null;

    List<Coordinate> times = getValueCoordinates();
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

    return new TimestampSensorValuesListOutput(result, false);
  }

  /**
   * Construct a {@link TimestampSensorValuesListValue} using
   * {@link SensorValue}s in the specified time range.
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
   * @return The constructed {@link TimestampSensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  public TimestampSensorValuesListOutput getValue(TimeCoordinate start,
    TimeCoordinate end, TimeCoordinate nominalTime, boolean allowInterpolation)
    throws SensorValuesListException {

    List<SensorValue> usedValues = new ArrayList<SensorValue>();

    // Search for the start time
    int startSearchIndex = Collections.binarySearch(getRawCoordinates(), start);

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
    while (currentIndex < getRawCoordinates().size() && !getRawCoordinates()
      .get(currentIndex).getTime().isAfter(end.getTime())) {
      usedValues.add(list.get(currentIndex));
      currentIndex++;
    }

    TimestampSensorValuesListOutput result = null;

    if (usedValues.size() > 0) {
      result = makeNumericValue(usedValues, nominalTime, true);
    } else if (allowInterpolation) {
      TimestampSensorValuesListOutput foundValue = (TimestampSensorValuesListOutput) getValue(
        nominalTime, true);
      result = null == foundValue ? null
        : new TimestampSensorValuesListOutput(foundValue,
          foundValue.interpolatesAroundFlags());
    }

    return result;
  }

  /**
   * Construct a {@link TimestampSensorValuesListValue} using
   * {@link SensorValue}s in the time range specified in the provided
   * {@link TimestampSensorValuesListValue}.
   *
   * <p>
   * This method simply extracts the times and passes them to
   * {@link #getValue(LocalDateTime, LocalDateTime, LocalDateTime)}.
   * </p>
   *
   *
   * @param timeReference
   *          A {@link TimestampSensorValuesListValue} object containing the
   *          times required.
   * @param allowInterpolation
   *          Indicates whether interpolation can be used to try to find a value
   *          if nothing is available in the time range of the passed in value.
   * @return The constructed {@link TimestampSensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  public TimestampSensorValuesListValue getValue(
    TimestampSensorValuesListValue timeReference, boolean allowInterpolation)
    throws SensorValuesListException {

    return getValue(timeReference.getStartTime(), timeReference.getEndTime(),
      timeReference.getNominalTime(), allowInterpolation);
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
   * <p>
   * Only those values with the least significant (aka best quality) flags when
   * calculating the mean. For example, if there are 2 GOOD values amongst 3 BAD
   * values, we will use only the GOOD values in the calculated mean.
   * </p>
   *
   * @param sensorValues
   *          The input {@link SensorValue}s.
   * @return The generated value.
   * @throws SensorValuesListException
   */
  private TimestampSensorValuesListOutput makeNumericValue(
    List<SensorValue> sensorValues, TimeCoordinate nominalTime,
    boolean allowInterpolatesAroundFlags) throws SensorValuesListException {

    // Get the timestamps for the value
    List<TimeCoordinate> timestamps = sensorValues.stream()
      .filter(v -> !v.isNaN()
        && !v.getDisplayFlag(allSensorValues).equals(FlagScheme.FLUSHING_FLAG))
      .map(v -> (TimeCoordinate) v.getCoordinate()).toList();

    TimeCoordinate startTime = timestamps.get(0);
    TimeCoordinate endTime = timestamps.get(timestamps.size() - 1);

    List<Flag> presentFlags = sensorValues.stream()
      .map(sv -> sv.getDisplayFlag(allSensorValues)).distinct().toList();

    FlagScheme flagScheme = sensorValues.get(0).getFlagScheme();
    Flag chosenFlag;

    Collection<Flag> userAssignedFlags = presentFlags.stream()
      .filter(f -> f.isUserAssignable()).toList();

    if (userAssignedFlags.size() > 0) {
      chosenFlag = Flag.leastSignificant(userAssignedFlags);
    } else if (presentFlags.stream()
      .allMatch(f -> f.equals(FlagScheme.NEEDED_FLAG))) {
      /*
       * If all we have are NEEDED flags, get the flag from the underlying auto
       * QC
       */
      chosenFlag = Flag.leastSignificant(
        sensorValues.stream().map(sv -> sv.getAutoQcFlag()).toList());
    } else {
      throw new IllegalStateException("No valid flags in sensor values");
    }

    try {

      boolean interpolatesAroundFlags = false;

      List<SensorValue> usedValues;

      if (flagScheme.isGood(chosenFlag, true)) {
        usedValues = sensorValues.stream()
          .filter(
            v -> flagScheme.isGood(v.getDisplayFlag(allSensorValues), true))
          .toList();
        if (FlagScheme.containsMoreSignificantFlag(presentFlags,
          flagScheme.getGoodFlag())) {
          if (allowInterpolatesAroundFlags) {
            interpolatesAroundFlags = true;
          }
        }
      } else {
        usedValues = sensorValues.stream()
          .filter(v -> v.getDisplayFlag(allSensorValues).equals(chosenFlag))
          .toList();
        if (FlagScheme.containsMoreSignificantFlag(presentFlags, chosenFlag)) {
          if (allowInterpolatesAroundFlags) {
            interpolatesAroundFlags = true;
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

      return new TimestampSensorValuesListOutput(startTime, endTime,
        nominalTime, usedValues, sensorType, mean.mean(), chosenFlag,
        StringUtils.collectionToDelimited(qcMessages, ";"),
        interpolatesAroundFlags);

    } catch (Exception e) {
      throw new SensorValuesListException(e);
    }
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
  private TimestampSensorValuesListOutput getBestOrInterpolate(
    TimestampSensorValuesListValue first, TimestampSensorValuesListValue second,
    TimeCoordinate targetTime) throws SensorValuesListException {

    TimestampSensorValuesListOutput result;

    if (first.getQCFlag().moreSignificantThan(second.getQCFlag())) {
      result = new TimestampSensorValuesListOutput(second, false);
    } else if (second.getQCFlag().moreSignificantThan(first.getQCFlag())) {
      result = new TimestampSensorValuesListOutput(first, false);
    } else {
      // Equal flags -> interpolate
      result = buildInterpolatedValue(first, second, targetTime, false);
    }

    return result;
  }

  /**
   * Construct an interpolated {@link TimestampSensorValuesListValue} from two
   * {@link TimestampSensorValuesListValue}s.
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
   * @return The constructed {@link TimestampSensorValuesListValue}.
   * @throws SensorValuesListException
   *           If the value cannot be constructed.
   */
  private TimestampSensorValuesListOutput buildInterpolatedValue(
    TimestampSensorValuesListValue first, TimestampSensorValuesListValue second,
    TimeCoordinate targetTime, boolean interpolatingAroundFlags)
    throws SensorValuesListException {

    TimestampSensorValuesListOutput result;

    if (null == first || null == second) {
      // Only 'interpolate' if we have values both before and after
      result = null;
    } else {
      if (!allSensorValues.getFlagScheme().isGood(first.getQCFlag(), true)
        || !allSensorValues.getFlagScheme().isGood(second.getQCFlag(), true)) {
        // We only interpolate good values
        result = null;
      } else {
        Double interpValue = Calculators.interpolate(first.getTime(),
          first.getDoubleValue(), second.getTime(), second.getDoubleValue(),
          targetTime.getTime());

        TreeSet<SensorValue> combinedSourceValues = new TreeSet<SensorValue>(
          COORDINATE_COMPARATOR);
        combinedSourceValues.addAll(first.getSourceSensorValues());
        combinedSourceValues.addAll(second.getSourceSensorValues());

        result = new TimestampSensorValuesListOutput(first.getStartTime(),
          second.getEndTime(), targetTime, combinedSourceValues,
          first.getSensorType(), interpValue,
          Flag.mostSignificant(first.getQCFlag(), second.getQCFlag()),
          StringUtils.combine(first.getQCMessage(), second.getQCMessage(), ";",
            true),
          interpolatingAroundFlags);
      }
    }
    return result;
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
   * Get the raw {@link SensorValue}s between two {@link Coordinate}s. Both
   * {@link Coordinate}s are inclusive.
   *
   * @param start
   *          The start coordinate.
   * @param end
   *          The end coordinate.
   * @return The {@link SensorValue}s between the given coordinates.
   */
  public List<SensorValue> getRawValues(TimeCoordinate start,
    TimeCoordinate end) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    int currentIndex = Collections.binarySearch(getRawCoordinates(), start);

    // If we didn't get an exact match, move to the index after the returned
    // insertion point.
    if (currentIndex < 0) {
      currentIndex = Math.abs(currentIndex) - 1;
      if (currentIndex < 0) {
        currentIndex = 0;
      }
    }

    while (currentIndex < list.size() - 1 && !list.get(currentIndex)
      .getCoordinate().getTime().isAfter(end.getTime())) {
      result.add(list.get(currentIndex));
      currentIndex++;
    }

    return result;
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
   * @throws CoordinateException
   */
  public List<SensorValue> getRawValues(LocalDateTime start, LocalDateTime end)
    throws CoordinateException {

    return getRawValues(TimeCoordinate.dummyCoordinate(start),
      TimeCoordinate.dummyCoordinate(end));
  }

  /**
   * Get a {@link TimeCoordinate} for the specified timestamp.
   *
   * <p>
   * If a {@link TimeCoordinate} already exists in the data, it is returned.
   * Otherwise a dummy {@link TimeCoordinate} is returned that has no database
   * ID.
   *
   * @param time
   * @return
   * @throws CoordinateException
   */
  public TimeCoordinate getCoordinate(LocalDateTime time)
    throws CoordinateException {
    TimeCoordinate result;

    TimeCoordinate dummyCoordinate = new TimeCoordinate(
      DatabaseUtils.NO_DATABASE_RECORD, allSensorValues.getDatasetId(), time);

    int foundCoordinate = Collections.binarySearch(getRawCoordinates(),
      dummyCoordinate);
    if (foundCoordinate >= 0) {
      result = (TimeCoordinate) getRawCoordinates().get(foundCoordinate);
    } else {
      result = dummyCoordinate;
    }

    return result;
  }

  @Override
  public SensorValue getRawSensorValue(Coordinate coordinate, long columnId) {
    int searchIndex = Collections.binarySearch(getRawCoordinates(), coordinate);
    return searchIndex < 0 ? null : list.get(searchIndex);
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

  @Override
  protected List<? extends SensorValuesListValue> getOutputValues()
    throws SensorValuesListException {
    if (null == outputValues) {
      buildOutputValues();
    }

    return outputValues;
  }

  @Override
  protected void listContentsUpdated() {
    outputValues = null;
  }
}
