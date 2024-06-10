package uk.ac.exeter.QuinCe.utils;

import java.time.LocalDateTime;

/**
 * Receives a number of {@link LocalDateTime} or {@link TimeRange} objects and
 * calculates the overall time range.
 */
public class TimeRangeBuilder implements TimeRange {

  private LocalDateTime start;

  private LocalDateTime end;

  public TimeRangeBuilder() {
    start = LocalDateTime.MAX;
    end = LocalDateTime.MIN;
  }

  public void add(LocalDateTime time) {
    if (null != time) {
      if (time.isBefore(start)) {
        start = time;
      }

      if (end.isAfter(end)) {
        end = time;
      }
    }
  }

  public void add(TimeRange range) {
    if (null != range) {
      if (range.getStart().isBefore(start)) {
        start = range.getStart();
      }

      if (range.getEnd().isAfter(end)) {
        end = range.getEnd();
      }
    }
  }

  public LocalDateTime getStart() {
    return start;
  }

  public LocalDateTime getEnd() {
    return end;
  }
}
