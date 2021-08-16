package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * A list of SensorValue objects with various search capabilities.
 *
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
 * <b>NOTE: It is the user's responsibility to ensure that entries are added in
 * the correct order.</b>
 * </p>
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class SearchableSensorValuesList extends ArrayList<SensorValue> {

  private static final SensorValueTimeComparator TIME_COMPARATOR = new SensorValueTimeComparator();

  private final TreeSet<Long> columnIds;

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

  @Override
  public boolean add(SensorValue value) {
    checkColumnId(value);
    return super.add(value);
  }

  @Override
  public void add(int index, SensorValue value) {
    checkColumnId(value);
    super.add(index, value);
  }

  @Override
  public boolean addAll(Collection<? extends SensorValue> values) {
    values.forEach(this::add);
    return true;
  }

  @Override
  public boolean addAll(int index, Collection<? extends SensorValue> values) {
    values.forEach(this::checkColumnId);
    super.addAll(index, values);
    return true;
  }

  private void checkColumnId(SensorValue value) {
    if (!columnIds.contains(value.getColumnId())) {
      throw new IllegalArgumentException("Invalid column ID");
    }
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

    int searchIndex = Collections.binarySearch(this, dummySensorValue(time),
      TIME_COMPARATOR);

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

  /**
   * Find all the {@link SensorValue}s within the specified date range.
   *
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
    if (startPoint < 0 && startPoint > (size() * -1)) {
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
   * Get the {@link SensorValue}(s) most relevant for the specified time.
   *
   * <p>
   * If there is a {@link SensorValue} at the specified time, that value is
   * returned. Otherwise an array of the closes prior and post values is
   * returned. If there is an exact match and its QC flag is
   * {@link Flag#FLUSHING}, an empty array is returned to indicate that the
   * value should not be used.
   * </p>
   *
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
   * @param preferGoodFlags
   *          Only return values with {@link Flag#GOOD} QC flags if possible.
   * @return An array of either one {@link SensorValue} (if it exactly matches
   *         the specified time) or two (for the closest matches either side of
   *         the time).
   */
  public List<SensorValue> getWithInterpolation(LocalDateTime time,
    boolean preferGoodFlags) {

    List<SensorValue> result;

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
        || (!exactTimeFlag.isGood() && preferGoodFlags)) {

        priorPostValues = getPriorPost(startPoint, true);
      }
    } else {
      priorPostValues = getPriorPost(startPoint, false);
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

    return result;
  }

  private List<SensorValue> getPriorPost(int startPoint, boolean goodOnly) {
    // First set the start point to the list in the right place

    // If the search result is -(list size), the search point is off the end
    // of the list.
    if (startPoint == (size() + 1) * -1) {
      startPoint = size();
    }

    // If the result is negative, then we haven't found an exact time match.
    // The start point will therefore be the absolute result - 1, which is the
    // point immediately after the search time
    if (startPoint < 0) {
      startPoint = Math.abs(startPoint) - 1;
    }

    int priorIndex = priorSearch(startPoint - 1, goodOnly);
    int postIndex = postSearch(startPoint, goodOnly);

    SensorValue prior = priorIndex == -1 ? null : get(priorIndex);
    SensorValue post = postIndex == -1 ? null : get(postIndex);

    List<SensorValue> result = new ArrayList<SensorValue>(2);
    result.add(prior);
    result.add(post);
    return result;
  }

  /*
   * public void populateMeasurementValue(MeasurementValue value, boolean
   * goodFlagsOnly) throws RoutineException, MissingParamException {
   *
   * MissingParam.checkMissing(value, "value");
   *
   * if (!columnIds.contains(value.getColumnId())) { throw new
   * IllegalArgumentException(
   * "Column ID for measurement value does not match list column ID"); }
   *
   * // Search for the closest point to the measurement time int startPoint =
   * Collections.binarySearch(this,
   * dummySensorValue(value.getMeasurement().getTime()), TIME_COMPARATOR);
   *
   * boolean searchForSurrounds = true;
   *
   * // If we get a positive result, we hit the measurement exactly. if
   * (startPoint >= 0) {
   *
   * if (getQCFlag(startPoint).equals(Flag.FLUSHING)) {
   *
   * // If we hit a FLUSHING value, we treat this as our match but return an //
   * empty value. value.setValues(null, null); searchForSurrounds = false; }
   * else if (!goodFlagsOnly || getQCFlag(startPoint).equals(Flag.GOOD) ||
   * getQCFlag(startPoint).equals(Flag.ASSUMED_GOOD)) {
   *
   * // If we have a GOOD flag, or GOOD flags are not required, just use the //
   * value we found value.setValues(get(startPoint), null); searchForSurrounds =
   * false; } }
   *
   * // We need to search for the prior and post if (searchForSurrounds) {
   *
   * // First set the start point to the list in the right place
   *
   * // If the search result is -(list size), the search point is off the end //
   * of the list. if (startPoint == (size() + 1) * -1) { startPoint = size(); }
   *
   * // If the result is negative, then we haven't found an exact time match. //
   * The start point will therefore be the absolute result - 1, which is the //
   * point immediately after the search time if (startPoint < 0) { startPoint =
   * Math.abs(startPoint) - 1; }
   *
   * int priorIndex = priorSearch(startPoint - 1, goodFlagsOnly); int postIndex
   * = postSearch(startPoint, goodFlagsOnly);
   *
   * SensorValue prior = priorIndex == -1 ? null : get(priorIndex); SensorValue
   * post = postIndex == -1 ? null : get(postIndex);
   *
   * value.setValues(prior, post); } }
   */

  private int priorSearch(int startPoint, boolean goodFlagsOnly) {
    return search(startPoint, goodFlagsOnly, -1, (x) -> x > -1);
  }

  private int postSearch(int startPoint, boolean goodFlagsOnly) {
    return search(startPoint, goodFlagsOnly, 1, (x) -> x < size());
  }

  private int search(int startPoint, boolean goodFlagsOnly, int searchStep,
    IntPredicate limitTest) {

    int result = -1;

    int closestGood = -1;
    int closestQuestionable = -1;
    int closestBad = -1;

    int currentIndex = startPoint;
    mainLoop: while (limitTest.test(currentIndex)) {

      Flag qcFlag = getQCFlag(currentIndex);

      switch (qcFlag.getFlagValue()) {
      case Flag.VALUE_GOOD:
      case Flag.VALUE_ASSUMED_GOOD: {
        closestGood = currentIndex;
        break mainLoop;
      }
      case Flag.VALUE_BAD: {
        // Only do something if we aren't looking for GOOD flags only
        if (!goodFlagsOnly && closestBad == -1) {
          closestBad = currentIndex;
          break mainLoop; // Remove for #1558
        }
        break;
      }
      case Flag.VALUE_QUESTIONABLE: {
        // Only do something if we aren't looking for GOOD flags only
        if (!goodFlagsOnly && closestQuestionable == -1) {
          closestQuestionable = currentIndex;
          break mainLoop; // Remove for #1558
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
}

class SensorValueTimeComparator implements Comparator<SensorValue> {
  @Override
  public int compare(SensorValue o1, SensorValue o2) {
    return o1.getTime().compareTo(o2.getTime());
  }
}
