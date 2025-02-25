package uk.ac.exeter.QuinCe.utils;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Simple interface for objects that cover a time period.
 */
public interface TimeRange {

  /**
   * Get the start date of the period.
   *
   * @return The start date.
   */
  public LocalDateTime getStart();

  /**
   * Get the end date of the period.
   *
   * @return The end date.
   */
  public LocalDateTime getEnd();

  /**
   * Examine a {@link Collection} of {@link TimeRange} objects and return the
   * start time of the object with the latest start time.
   *
   * @param ranges
   *          The ranges to be examined.
   * @return The latest start time.
   */
  public static LocalDateTime getLatestStart(
    Collection<? extends TimeRange> ranges) {
    LocalDateTime result = LocalDateTime.MIN;

    for (TimeRange range : ranges) {
      if (null != range.getStart() && range.getStart().isAfter(result)) {
        result = range.getStart();
      }
    }

    return result;
  }

  /**
   * Examine a {@link Collection} of {@link TimeRange} objects and return the
   * end time of the object with the earliest end time.
   *
   * @param ranges
   *          The ranges to be examined.
   * @return The earliest end date.
   */
  public static LocalDateTime getEarliestEnd(
    Collection<? extends TimeRange> ranges) {
    LocalDateTime result = LocalDateTime.MAX;

    for (TimeRange range : ranges) {
      if (null != range.getEnd() && range.getEnd().isBefore(result)) {
        result = range.getEnd();
      }
    }

    return result;
  }
}
