package uk.ac.exeter.QuinCe.utils;

import java.util.Comparator;

public class TimeRangeStartComparator implements Comparator<TimeRange> {

  @Override
  public int compare(TimeRange arg0, TimeRange arg1) {
    return arg0.getStart().compareTo(arg1.getStart());
  }
}
