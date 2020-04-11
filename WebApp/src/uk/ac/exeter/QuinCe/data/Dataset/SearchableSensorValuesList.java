package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * A list of SensorValue objects with stateful search capabilities.
 *
 * <p>
 * There are two stateful search types that can be performed:
 * <p>
 * <ul>
 * <li><b>dateSearch:</b> Search for the {@link SensorValue} that is measured on
 * or immediately before a specified date</li>
 * <li><b>rangeSearch:</b> Search for the set of values within a range of dates,
 * such that {@code date1 <= valueDate < date2}</li>
 * </ul>
 *
 * Each of these searches must be initialised by calling the appropriate
 * {@code init} method. Each subsequent call to the corresponding {@code search}
 * method will begin where the previous search call stopped.
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class SearchableSensorValuesList extends ArrayList<SensorValue> {

  /**
   * The date range search iterator
   */
  private int rangeSearchPos = Integer.MIN_VALUE;

  private Map<String, SensorValuesListDateSearch> dateSearches;

  /**
   * Constructor for an empty list
   */
  public SearchableSensorValuesList() {
    super();
    dateSearches = new HashMap<String, SensorValuesListDateSearch>();
  }

  /**
   * Convert an existing list of SensorValues to a navigable list
   *
   * @param list
   *          The existing list
   */
  public SearchableSensorValuesList(List<SensorValue> list) {
    super(list);
  }

  /**
   * Initialise the range search
   */
  public void initRangeSearch() {
    if (rangeSearchPos > Integer.MIN_VALUE) {
      throw new IllegalStateException("Range search is already initialised");
    }

    rangeSearchPos = -1;
  }

  /**
   * Finish the range search
   */
  public void finishRangeSearch() {
    if (rangeSearchPos == Integer.MIN_VALUE) {
      throw new IllegalStateException("Range search has not been initialised");
    }

    rangeSearchPos = Integer.MIN_VALUE;
  }

  /**
   * Find all the {@link SensorValue}s within the specified date range.
   *
   * <p>
   * The method searches for values where {@code date1 <= valueDate < date2}.
   * The search will always start from the {@link SensorValue} following the
   * last value returned by the previous call to this method.
   * </p>
   *
   * <p>
   * If the search does not find any values before the end date, the returned
   * list will be empty and the next call to this method will start from the
   * same position.
   * </p>
   *
   * @param start
   *          The first date in the range
   * @param end
   *          The last date in the range
   * @return The {@link SensorValue}s in the range
   */
  public List<SensorValue> rangeSearch(LocalDateTime start, LocalDateTime end) {

    List<SensorValue> result = new ArrayList<SensorValue>();

    // Search for the next value that is equal to or greater than the start date
    boolean foundStartDate = false;

    // Store the search start position
    int searchStartPos = rangeSearchPos;

    while (!foundStartDate) {
      rangeSearchPos++;

      if (rangeSearchPos >= size()) {
        // We fell off the list without finding a match. Reset the search
        // position and quit
        rangeSearchPos = searchStartPos;
        break;
      }

      LocalDateTime searchTime = get(rangeSearchPos).getTime();

      if (!searchTime.isBefore(end)) {
        // We haven't found any values in the range. Reset the search
        // position and quit
        rangeSearchPos = searchStartPos;
        break;
      } else if (!searchTime.isBefore(start)) {
        // We've found a date that's equal to or after the start date. We're in
        // the range.
        foundStartDate = true;
      }
    }

    if (foundStartDate) {
      // The current position is in the range
      result.add(get(rangeSearchPos));

      // Keep finding values until we either reach the end date or fall off the
      // list.
      boolean finished = false;
      while (!finished) {
        int nextPos = rangeSearchPos + 1;

        if (nextPos == size()) {
          // We fell off the end of the list
          finished = true;
        } else {
          LocalDateTime time = get(nextPos).getTime();
          if (time.isBefore(end)) {
            rangeSearchPos++;
            result.add(get(rangeSearchPos));
          } else {
            finished = true;
          }
        }
      }
    }

    return result;
  }

  public void initDateSearch(String name) {

    if (dateSearches.containsKey(name)) {
      throw new IllegalStateException(
        "Date search '" + name + "' already exists");
    }

    SensorValuesListDateSearch newSearch = new SensorValuesListDateSearch(this);
    dateSearches.put(name, newSearch);
  }

  public void destroyDateSearch(String name) {

    if (!dateSearches.containsKey(name)) {
      throw new IllegalStateException(
        "Date search '" + name + "' doesn not exist");
    }

    dateSearches.remove(name);
  }

  public SensorValue dateSearch(String name, LocalDateTime time) {
    if (!dateSearches.containsKey(name)) {
      throw new IllegalStateException(
        "Date search '" + name + "' doesn not exist");
    }

    return dateSearches.get(name).search(time);
  }

  public boolean hasDateSearch(String name) {
    return dateSearches.containsKey(name);
  }

  public MeasurementValue getMeasurementValue(Measurement measurement,
    SensorType sensorType, long columnId, String searchId)
    throws RoutineException {

    String searchName = searchId + "-" + columnId;

    if (!hasDateSearch(searchName)) {
      initDateSearch(searchName);
    }

    MeasurementValue result = new MeasurementValue(measurement, sensorType,
      columnId);

    SensorValue prior = dateSearch(searchName, measurement.getTime());
    SensorValue post = null;

    if (prior.getTime().isBefore(measurement.getTime())) {
      post = get(dateSearches.get(searchName).nextIndex());
    }

    result.setValues(prior, post);

    return result;
  }

  protected void destroySearchesWithPrefix(String prefix) {
    ArrayList<String> searchesToDestroy = new ArrayList<String>(
      dateSearches.size());

    for (String searchName : dateSearches.keySet()) {
      if (searchName.startsWith(prefix)) {
        searchesToDestroy.add(searchName);
      }
    }

    searchesToDestroy.forEach(x -> dateSearches.remove(x));
  }

  protected boolean hasSearchWithPrefix(String prefix) {
    boolean result = false;

    for (String searchName : dateSearches.keySet()) {
      if (searchName.startsWith(prefix)) {
        result = true;
        break;
      }
    }

    return result;
  }
}
