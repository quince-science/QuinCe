package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * A linked list of SensorValue objects with a stateful search capability
 * @author Steve Jones
 *
 */
public class NavigableSensorValuesList extends LinkedList<SensorValue> {

  /**
   * The search iterator
   */
  private ListIterator<SensorValue> incrementalSearchIterator = null;

  /**
   * Constructor for an empty list
   */
  public NavigableSensorValuesList() {
    super();
  }

  /**
   * Convert an existing list of SensorValues to a navigable list
   * @param list The existing list
   */
  public NavigableSensorValuesList(List<SensorValue> list) {
    super(list);
  }

  /**
   * Initialise the stateful search
   */
  public void initIncrementalSearch() {
    if (null != incrementalSearchIterator) {
      throw new IllegalStateException("Incremental search is already initialised");
    }

    incrementalSearchIterator = listIterator();
  }

  /**
   * Finish the stateful search
   */
  public void finishIncrementalSearch() {
    if (null == incrementalSearchIterator) {
      throw new IllegalStateException("Incremental search has not been initialised");
    }

    incrementalSearchIterator = null;
  }

  /**
   * Perform an incremental search for the specified time from the
   * current search state. Returns the latest SensorValue that is before
   * or equal to the specified time. Returns {@code null} if there
   * is no element before that time.
   * @param time The time to search for
   * @return The latest SensorValue that is before or equal to the specified time
   */
  public SensorValue incrementalSearch(LocalDateTime time) {
    SensorValue result = null;

    if (null == incrementalSearchIterator) {
      throw new IllegalStateException("Incremental search has not been initialised");
    }

    while (incrementalSearchIterator.hasNext() &&
      !get(incrementalSearchIterator.nextIndex()).getTime().isAfter(time)) {

      incrementalSearchIterator.next();
    }

    if (incrementalSearchIterator.hasPrevious()) {
      result = get(incrementalSearchIterator.previousIndex());
    }

    return result;
  }
}
