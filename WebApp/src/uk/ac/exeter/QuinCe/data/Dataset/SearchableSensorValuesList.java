package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.utils.CollectionUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.ModeCalculator;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageDataException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SensorValuePlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageTableValue;

/**
 * A list of SensorValue objects with various search capabilities.
 * <p>
 * There are two search types that can be performed:
 * </p>
 * <ul>
 * <li>{@code rangeSearch}: Search for the set of values within a range of
 * dates, such that {@code date1 <= valueDate < date2}</li>
 * <li>{@code timeSearch}: Find the value on or immediately before a specified
 * time.</li>
 * </ul>
 *
 * <p>
 * The list is maintained in time order of its members. Attempting to add two
 * {@link SensorValue}s with the same timestamp will result in an
 * {@link UnsupportedOperationException}.
 * </p>
 */
@SuppressWarnings("serial")
public class SearchableSensorValuesList extends ArrayList<SensorValue> {

  private static final IllegalArgumentException SAME_TIMESTAMP_EXCEPTION = new IllegalArgumentException(
    "Cannot add two SensorValues with the same timestamp");

  private static final UnsupportedOperationException ADD_INDEX_EXCEPTION = new UnsupportedOperationException(
    "Cannot add a value in a custom location");

  /**
   * The default maximum interpolation limit in seconds.
   */
  private static final long DEFAULT_INTERPOLATION_LIMIT = 300;

  /**
   * An instance of the comparator used to compare two {@link SensorValue}s by
   * their timestamp.
   */
  private static final SensorValueTimeComparator TIME_COMPARATOR = new SensorValueTimeComparator();

  /**
   * The list of {@link FileColumn} database IDs whose {@link SensorValue}s are
   * allowed to be members of this list.
   */
  private final TreeSet<Long> columnIds;

  /**
   * The most common time interval between consecutive {@link SensorValue}s in
   * the list.
   */
  private int modeTimeStep = 0;

  /**
   * A cache of the timestamps of all the {@link SensorValue}s in the list.
   */
  private List<LocalDateTime> times = null;

  /**
   * Constructor for an empty list with one supported column ID
   */
  public SearchableSensorValuesList(long columnId) {
    super();
    columnIds = new TreeSet<Long>();
    columnIds.add(columnId);
  }

  /**
   * Constructor for an empty list with multiple supported column IDs
   */
  public SearchableSensorValuesList(Collection<Long> columnIds) {
    super();
    this.columnIds = new TreeSet<Long>(columnIds);
  }

  /**
   * Factory method to build a list directly from a collection of SensorsValues
   *
   * @param values
   * @return
   */
  public static SearchableSensorValuesList newFromSensorValueCollection(
    Collection<SensorValue> values) {

    TreeSet<Long> columnIds = values.stream().map(x -> x.getColumnId())
      .collect(Collectors.toCollection(TreeSet::new));

    SearchableSensorValuesList list = new SearchableSensorValuesList(columnIds);
    list.addAll(values);

    return list;
  }

  /**
   * We ensure that the added values are inserted into the list in the correct
   * position according to time order.
   */
  @Override
  public boolean add(SensorValue value) {

    // Null values are not allowed
    if (null == value) {
      throw new IllegalArgumentException("null values are not permitted");
    }

    // The value must have a columnId that matches one of the specified
    // columnIDs
    if (!columnIds.contains(value.getColumnId())) {
      throw new IllegalArgumentException("Invalid column ID");
    }

    if (size() == 0) {
      super.add(value);
    } else {
      int lastComparison = TIME_COMPARATOR.compare(value, last());

      if (lastComparison == 0) {
        // Values with identical timestamps are not allowed
        throw SAME_TIMESTAMP_EXCEPTION;
      } else if (lastComparison > 0) {
        // The value being added is after the last value, so we just add it to
        // the end.
        super.add(value);
      } else {

        int binarySearchResult = Collections.binarySearch(this, value,
          TIME_COMPARATOR);

        // Values with the identical timestamps are not allowed
        if (binarySearchResult >= 0) {
          throw SAME_TIMESTAMP_EXCEPTION;
        } else {
          super.add((binarySearchResult * -1) - 1, value);
        }
      }
    }

    // Reset the times cache
    times = null;

    return true;
  }

  /**
   * We do not allow adding values in specified locations; the list order is
   * maintained internally.
   */
  @Override
  public void add(int index, SensorValue value) {
    throw ADD_INDEX_EXCEPTION;
  }

  @Override
  public boolean addAll(Collection<? extends SensorValue> values) {
    values.forEach(this::add);
    return true;
  }

  /**
   * We do not allow adding values in specified locations; the list order is
   * maintained internally.
   */
  @Override
  public boolean addAll(int index, Collection<? extends SensorValue> values) {
    throw ADD_INDEX_EXCEPTION;
  }

  /**
   * Find the {@link SensorValue} on or closest before the specified time.
   *
   * @param time
   *          The target time
   * @return The matching SensorValue
   * @throws MissingParamException
   */
  public SensorValue timeSearch(LocalDateTime time)
    throws MissingParamException {

    MissingParam.checkMissing(time, "time");

    SensorValue result = null;

    int searchIndex = Collections.binarySearch(getTimes(), time);

    if (searchIndex > -1) {
      result = get(searchIndex);
    } else {
      int priorIndex = Math.abs(searchIndex) - 1;
      if (priorIndex != -1) {
        result = get(priorIndex);
      }
    }

    return result;
  }

  public List<LocalDateTime> getTimes() {
    if (null == times) {
      times = stream().map(v -> v.getTime()).toList();
    }

    return times;
  }

  /**
   * Find all the {@link SensorValue}s within the specified date range.
   * <p>
   * The method searches for values where {@code date1 <= valueDate < date2}. If
   * the search does not find any values before the end date, the returned list
   * will be empty.
   * </p>
   *
   * @param start
   *          The first date in the range
   * @param end
   *          The last date in the range
   * @return The {@link SensorValue}s in the range
   * @throws MissingParamException
   */
  public List<SensorValue> rangeSearch(LocalDateTime start, LocalDateTime end)
    throws MissingParamException {

    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    if (start.equals(end)) {
      throw new IllegalArgumentException("Start and end cannot be equal");
    }

    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start must be before end");
    }

    List<SensorValue> result = new ArrayList<SensorValue>();

    int startPoint = Collections.binarySearch(this, dummySensorValue(start),
      TIME_COMPARATOR);

    // If the search result is -(list size), all the values are before the start
    // so we don't do anything and return an empty list. The easiest way to do
    // this is set the start point off the end of the list.
    if (startPoint == (size() + 1) * -1) {
      startPoint = size();
    }

    // If the result is negative, then we haven't found an exact time match. The
    // start point will therefore be the absolute result - 1
    if (startPoint < 0 && startPoint >= (size() * -1)) {
      startPoint = Math.abs(startPoint) - 1;
    }

    // Add values until we hit the end time, or fall off the list.
    int currentIndex = startPoint;
    while (currentIndex < size() && get(currentIndex).getTime().isBefore(end)) {
      result.add(get(currentIndex));
      currentIndex++;
    }

    return result;
  }

  public SensorValue get(LocalDateTime time) {
    int valueIndex = Collections.binarySearch(this, dummySensorValue(time),
      TIME_COMPARATOR);

    SensorValue result = valueIndex >= 0 ? get(valueIndex) : null;
    if (null != result && result.getUserQCFlag().equals(Flag.FLUSHING)) {
      result = null;
    }

    return result;
  }

  /**
   * Get the closest {@link SensorValue}(s) to the specified time.
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
  public List<SensorValue> getClosest(LocalDateTime time) {

    List<SensorValue> result = new ArrayList<SensorValue>(2);

    if (!isEmpty()) {
      int valueIndex = Collections.binarySearch(this, dummySensorValue(time),
        TIME_COMPARATOR);

      if (valueIndex >= 0) {
        result.add(get(valueIndex));
      } else if (valueIndex == -1) {
        result.add(get(0));
      } else if (Math.abs(valueIndex) > size()) {
        result.add(get(size() - 1));
      } else {
        result.add(get(Math.abs(valueIndex) - 2));
        result.add(get(Math.abs(valueIndex) - 1));
      }
    }
    return result;
  }

  /**
   * Get the {@link SensorValue}(s) most relevant for the specified time.
   * <p>
   * If there is a {@link SensorValue} at the specified time, that value is
   * returned. Otherwise an array of the closes prior and post values is
   * returned. If there is an exact match and its QC flag is
   * {@link Flag#FLUSHING}, an empty array is returned to indicate that the
   * value should not be used.
   * </p>
   * <p>
   * If {@code allowOutsideTimeRange} is {@code true}, allow the user to specify
   * values outside the time range of these values. This will then return the
   * first/last value as appropriate. If set to {@false}, an empty list is
   * returned.
   * </p>
   * <p>
   * The above logic is overridden by {@code preferGoodFlags}. If this is set
   * and any selected value is {@link Flag#QUESTIONABLE} or {@link Flag#BAD},
   * the interpolation continues until a {@link Flag#GOOD} value is found. If no
   * {@link Flag#GOOD} values are found, the closest {@link Flag#QUESTIONABLE}
   * value is used, finally falling back to a {@link Flag#BAD} value.
   * </p>
   *
   * @param time
   *          The time to search for
   * @param allowOutsideTimeRange
   *          Allow the user to specify values outside the time range.
   * @param preferGoodFlags
   *          Only return values with {@link Flag#GOOD} QC flags if possible.
   * @return An array of either one {@link SensorValue} (if it exactly matches
   *         the specified time) or two (for the closest matches either side of
   *         the time).
   */
  public List<SensorValue> getWithInterpolation(LocalDateTime time,
    boolean allowOutsideTimeRange, boolean preferGoodFlags) {

    List<SensorValue> result;

    if (size() == 0) {
      result = new ArrayList<SensorValue>(0);
    } else if (!allowOutsideTimeRange && (time.isBefore(get(0).getTime())
      || time.isAfter(get(size() - 1).getTime()))) {
      result = new ArrayList<SensorValue>(0);
    } else {

      int startPoint = Collections.binarySearch(this, dummySensorValue(time),
        TIME_COMPARATOR);

      SensorValue exactTimeValue = null;
      List<SensorValue> priorPostValues = null;
      boolean useExactValue = true;

      // If we get a positive result, we hit the measurement exactly.
      if (startPoint >= 0) {

        exactTimeValue = get(startPoint);
        Flag exactTimeFlag = getQCFlag(startPoint);

        // We can use the exact time value if:
        // (a) The flag is FLUSHING (it won't actually be used but we work that
        // out later)
        // (b) The flag is GOOD
        // (c) The flag is BAD or QUESTIONABLE but we don't mind
        if (!exactTimeFlag.equals(Flag.FLUSHING)
          && (!exactTimeFlag.isGood() && preferGoodFlags)) {

          priorPostValues = getPriorPost(time, startPoint);

          // If the prior and post are both empty, then there are no
          // interpolatable values available. Therefore we use the exact value.
          if (CollectionUtils.getNonNullCount(priorPostValues) == 1L
            && CollectionUtils.getFirstNonNull(priorPostValues).getTime()
              .equals(time)) {
            useExactValue = true;
          } else {
            useExactValue = false;
          }
        }
      } else {
        priorPostValues = getPriorPost(time, startPoint);
        useExactValue = false;
      }

      // Now we decide what to put in our returned list
      if (useExactValue) {
        result = new ArrayList<SensorValue>();

        // A FLUSHING value means an empty list. Otherwise put in the exact time
        // value.
        if (!exactTimeValue.getUserQCFlag().equals(Flag.FLUSHING)) {
          result.add(exactTimeValue);
        }
      } else {
        result = priorPostValues;
      }
    }

    return result;
  }

  /**
   * Get the value to be displayed in a table.
   *
   * <p>
   * Uses {@link #getWithInterpolation} to find suitable value(s), and
   * interpolates as needed.
   * </p>
   *
   * @param time
   *          The required time.
   * @param allowOutsideTimeRange
   *          Allow/disallow the caller to specify a time outside the range of
   *          these values.
   * @param preferGoodFlags
   *          Only use values with {@link Flag#GOOD} QC flags if possible.
   * @return The table value.
   * @throws PlotPageDataException
   */
  public PlotPageTableValue getTableValue(LocalDateTime time,
    boolean allowOutsideTimeRange, boolean preferGoodFlags,
    DatasetSensorValues allSensorValues) throws PlotPageDataException {

    PlotPageTableValue result = null;

    List<SensorValue> valuesToUse = getWithInterpolation(time,
      allowOutsideTimeRange, preferGoodFlags);

    switch (valuesToUse.size()) {
    case 0: {
      // Flushing value - do nothing
      break;
    }
    case 1: {
      // Value from exact time - use it directly
      result = new SensorValuePlotPageTableValue(valuesToUse.get(0));
      break;
    }
    case 2: {
      Double value = SensorValue.interpolate(valuesToUse.get(0),
        valuesToUse.get(1), time);

      List<Long> ids = valuesToUse.stream().filter(Objects::nonNull)
        .map(v -> v.getId()).toList();

      if (null != value) {
        try {
          result = new SimplePlotPageTableValue(String.valueOf(value),
            SensorValue.getCombinedDisplayFlag(valuesToUse),
            SensorValue.getCombinedQcComment(valuesToUse, allSensorValues),
            false, PlotPageTableValue.INTERPOLATED_TYPE, ids);
        } catch (RoutineException e) {
          throw new PlotPageDataException(
            "Unable to get SensorValue QC Comments", e);
        }
      }

      break;
    }
    default: {
      throw new PlotPageDataException(
        "Invalid number of values in sensor value search");
    }
    }

    return result;
  }

  private List<SensorValue> getPriorPost(LocalDateTime targetTime,
    int startPoint) {
    // First set the start point to the list in the right place

    // If the start point is >= 0, our starting point is a value in the list.
    // Assume this as the default, override below.
    int priorSearchStartPoint = startPoint;
    int postSearchStartPoint = startPoint;

    // If the search result is -(list size), the search point is off the end
    // of the list. Start at the end.
    if (startPoint == (size() + 1) * -1) {
      priorSearchStartPoint = size() - 1;
      postSearchStartPoint = size();
    }

    // If the result is negative, then we haven't found an exact time match -
    // startPoint is offset from the true insertion point where the value would
    // be. We start searching after either side of that point.
    if (startPoint < 0) {
      priorSearchStartPoint = Math.abs(startPoint) - 2;
      postSearchStartPoint = Math.abs(startPoint) - 1;
    }

    int priorIndex = priorSearch(priorSearchStartPoint);
    int postIndex = postSearch(postSearchStartPoint);

    /*
     * If the found values aren't within the interpolation limit, discard them.
     */
    if (priorIndex >= 0
      && !withinTimeInterpolationLimit(targetTime, priorIndex)) {
      priorIndex = -1;
    }

    if (postIndex >= 0
      && !withinTimeInterpolationLimit(targetTime, postIndex)) {
      postIndex = -1;
    }

    SensorValue prior = priorIndex == -1 ? null : get(priorIndex);
    SensorValue post = postIndex == -1 ? null : get(postIndex);

    // The final values we return depend on what was found and their QC flags
    List<SensorValue> result = new ArrayList<SensorValue>(2);

    // If either value isn't found, just add what we have.
    if (null == prior || null == post) {
      result.add(prior);
      result.add(post);
    } else {
      // If the QC flags are different, use the least significant value only
      if (prior.getDisplayFlag().moreSignificantThan(post.getDisplayFlag())) {
        result.add(null);
        result.add(post);
      } else if (post.getDisplayFlag()
        .moreSignificantThan(prior.getDisplayFlag())) {
        result.add(prior);
        result.add(null);
      } else if (post.equals(prior)) {
        // The search found the same value twice. Only add it once.
        result.add(prior);
        result.add(null);
      } else {
        // Two different viable values
        result.add(prior);
        result.add(post);
      }
    }

    return result;
  }

  private int priorSearch(int startPoint) {
    return search(startPoint, -1, (x) -> x > -1);
  }

  private int postSearch(int startPoint) {
    return search(startPoint, 1, (x) -> x < size());
  }

  private int search(int startPoint, int searchStep, IntPredicate limitTest) {

    int result = -1;

    int closestGood = -1;
    int closestQuestionable = -1;
    int closestBad = -1;

    int currentIndex = startPoint;
    mainLoop: while (limitTest.test(currentIndex)
      && withinTimeInterpolationLimit(startPoint, currentIndex)) {

      Flag qcFlag = getQCFlag(currentIndex);

      switch (qcFlag.getFlagValue()) {
      case Flag.VALUE_FLUSHING: {
        // We stop immediately if we see a Flushing flag
        break mainLoop;
      }
      case Flag.VALUE_GOOD:
      case Flag.VALUE_ASSUMED_GOOD: {
        closestGood = currentIndex;
        break mainLoop;
      }
      case Flag.VALUE_BAD:
      case Flag.VALUE_LOOKUP: {
        // Only do something if we aren't looking for GOOD flags only
        if (closestBad == -1) {
          closestBad = currentIndex;
        }
        break;
      }
      case Flag.VALUE_QUESTIONABLE: {
        // Only do something if we aren't looking for GOOD flags only
        if (closestQuestionable == -1) {
          closestQuestionable = currentIndex;
        }
        break;
      }
      default: {
        // Ignore all other flags
      }
      }

      currentIndex = currentIndex + searchStep;
    }

    if (closestGood > -1) {
      result = closestGood;
    } else if (closestQuestionable > -1) {
      result = closestQuestionable;
    } else if (closestBad > -1) {
      result = closestBad;
    }

    return result;
  }

  /**
   * Generate a dummy {@link SensorValue} for a specified time, for use with
   * {@link #TIME_COMPARATOR}.
   *
   * @param time
   *          The time to use.
   * @return The dummy {@link SensorValue}.
   */
  private SensorValue dummySensorValue(LocalDateTime time) {
    return new SensorValue(-1, -1, time, null);
  }

  private Flag getQCFlag(int index) {
    return get(index).getUserQCFlag().equals(Flag.NEEDED)
      ? get(index).getAutoQcFlag()
      : get(index).getUserQCFlag();
  }

  private boolean withinTimeInterpolationLimit(int startPoint, int testPoint) {
    return Math.abs(DateTimeUtils.secondsBetween(get(startPoint).getTime(),
      get(testPoint).getTime())) <= getInterpolationLimit();
  }

  private boolean withinTimeInterpolationLimit(LocalDateTime targetTime,
    int testPoint) {
    return Math.abs(DateTimeUtils.secondsBetween(targetTime,
      get(testPoint).getTime())) <= getInterpolationLimit();
  }

  private double getInterpolationLimit() {
    return getModeTimeStep() / 2 < DEFAULT_INTERPOLATION_LIMIT
      ? DEFAULT_INTERPOLATION_LIMIT
      : modeTimeStep / 2;
  }

  private Integer getModeTimeStep() {
    if (modeTimeStep == 0) {
      calculateModeTimeStep();
    }

    return modeTimeStep;
  }

  private void calculateModeTimeStep() {
    ModeCalculator mode = new ModeCalculator();

    for (int i = 1; i < size(); i++) {
      long prev = DateTimeUtils.dateToLong(get(i - 1).getTime());
      long current = DateTimeUtils.dateToLong(get(i).getTime());
      mode.add(current - prev);
    }

    // ms to s
    modeTimeStep = mode.getMode().intValue() / 1000;
  }

  /**
   * Get the last value in the list.
   *
   * @return The last value.
   */
  private SensorValue last() {
    return get(size() - 1);
  }
}

class SensorValueTimeComparator implements Comparator<SensorValue> {
  @Override
  public int compare(SensorValue o1, SensorValue o2) {
    return o1.getTime().compareTo(o2.getTime());
  }
}
