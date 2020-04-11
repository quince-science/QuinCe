package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

/**
 * A stateful date-based search for a {@link SearchableSensorValuesList}.
 *
 * @author Steve Jones
 *
 */
class SensorValuesListDateSearch {

  private final SearchableSensorValuesList list;

  private int currentIndex = -1;

  protected SensorValuesListDateSearch(SearchableSensorValuesList list) {
    this.list = list;
  }

  /**
   * Perform an incremental search for the specified time from the current
   * search state. Returns the latest SensorValue that is before or equal to the
   * specified time. Returns {@code null} if there is no element before that
   * time.
   *
   * @param time
   *          The time to search for
   * @return The latest SensorValue that is before or equal to the specified
   *         time
   */
  protected SensorValue search(LocalDateTime time) {

    SensorValue result = null;

    while (currentIndex < list.size() - 1
      && !list.get(currentIndex + 1).getTime().isAfter(time)) {
      currentIndex++;
    }

    if (currentIndex >= 0) {
      result = list.get(currentIndex);
    }

    return result;
  }

  protected int nextIndex() {
    return currentIndex + 1;
  }
}
