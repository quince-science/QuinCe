package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Comparator;

/**
 * Comparator for {@link SensorValuesListValue}s, comparing them by time.
 */
public class SensorValuesListValueTimeComparator
  implements Comparator<SensorValuesListValue> {

  @Override
  public int compare(SensorValuesListValue o1, SensorValuesListValue o2) {

    int result;

    if (o1.getEndTime().isBefore(o2.getStartTime())) {
      result = -1;
    } else if (o1.getStartTime().isAfter(o2.getEndTime())) {
      result = 1;
    } else {
      result = 0;
    }

    return result;
  }
}
