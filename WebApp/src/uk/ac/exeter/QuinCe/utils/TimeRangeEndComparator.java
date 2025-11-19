package uk.ac.exeter.QuinCe.utils;

import java.util.Comparator;

public class TimeRangeEndComparator implements Comparator<TimeRange> {

  @Override
  public int compare(TimeRange arg0, TimeRange arg1) {
    return arg0.getEnd().compareTo(arg1.getEnd());
  }
}
