package uk.ac.exeter.QuinCe.utils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.TreeSet;

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
   * <p>
   * If a {@code bufferSeconds} larger than zero is specified, the earliest end
   * time will be adjusted to be earlier by that amount. This is useful for
   * comparing file ranges where timestamps don't line up exactly, to ensure
   * that we keep closely related measurements.
   * </p>
   *
   * @param ranges
   *          The ranges to be examined.
   * @param bufferSeconds
   *          The buffer range.
   * @return The latest start time.
   */
  public static LocalDateTime getLatestStart(
    Collection<? extends TimeRange> ranges, long bufferSeconds) {

    TreeSet<TimeRange> sorted = new TreeSet<TimeRange>(
      new TimeRangeStartComparator());
    sorted.addAll(ranges);

    LocalDateTime latestStart = sorted.last().getStart();
    LocalDateTime bufferLimit = latestStart.minusSeconds(bufferSeconds);

    LocalDateTime result;

    if (bufferLimit.isBefore(sorted.first().getStart())) {
      result = sorted.first().getStart();
    } else {
      result = bufferLimit;
    }

    return result;

  }

  /**
   * Examine a {@link Collection} of {@link TimeRange} objects and return the
   * end time of the object with the earliest end time.
   *
   * <p>
   * If a {@code bufferSeconds} larger than zero is specified, the earliest end
   * time will be adjusted to be later by that amount. This is useful for
   * comparing file ranges where timestamps don't line up exactly, to ensure
   * that we keep closely related measurements.
   * </p>
   *
   * @param ranges
   *          The ranges to be examined.
   * @param bufferSeconds
   *          The buffer range.
   * @return The earliest end date.
   */
  public static LocalDateTime getEarliestEnd(
    Collection<? extends TimeRange> ranges, long bufferSeconds) {

    TreeSet<TimeRange> sorted = new TreeSet<TimeRange>(
      new TimeRangeEndComparator());
    sorted.addAll(ranges);

    LocalDateTime earliestEnd = sorted.first().getEnd();
    LocalDateTime bufferLimit = earliestEnd.plusSeconds(bufferSeconds);

    LocalDateTime result;

    if (bufferLimit.isAfter(sorted.last().getEnd())) {
      result = sorted.last().getEnd();
    } else {
      result = bufferLimit;
    }

    return result;

  }
}
