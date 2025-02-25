package uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TimeRangeTest {

  /**
   * Generate a {@link LocalDateTime} object with a fixed date but the specified
   * hour.
   *
   * @param hour
   *          The required hour.
   * @return The {@link LocalDateTime} object.
   */
  private LocalDateTime makeTime(int hour) {
    return LocalDateTime.of(2025, 1, 1, hour, 0, 0);
  }

  /**
   * Generate a {@link Collection} of {@link TimeRange} objects, all for the
   * same date but with the specified starting hour.
   *
   * @param startHours
   *          The required starting hours.
   * @return The {@link TimeRange} objects.
   */
  private Collection<TimeRange> makeStarts(int... startHours) {
    Collection<TimeRange> results = new ArrayList<TimeRange>(startHours.length);

    for (int hour : startHours) {
      TimeRange range = Mockito.mock(TimeRange.class);
      Mockito.when(range.getStart()).thenReturn(makeTime(hour));
      results.add(range);
    }

    return results;
  }

  /**
   * Generate a {@link Collection} of {@link TimeRange} objects, all for the
   * same date but with the specified end hour.
   *
   * @param startHours
   *          The required end hours.
   * @return The {@link TimeRange} objects.
   */
  private Collection<TimeRange> makeEnds(int... endHours) {
    Collection<TimeRange> results = new ArrayList<TimeRange>(endHours.length);

    for (int hour : endHours) {
      TimeRange range = Mockito.mock(TimeRange.class);
      Mockito.when(range.getEnd()).thenReturn(makeTime(hour));
      results.add(range);
    }

    return results;
  }

  @Test
  public void latestStartTest() {
    Collection<TimeRange> ranges = makeStarts(12, 5, 15, 6);
    LocalDateTime latestStart = TimeRange.getLatestStart(ranges);
    assertEquals(latestStart.getHour(), 15);
  }

  @Test
  public void earliestEndTest() {
    Collection<TimeRange> ranges = makeEnds(12, 5, 15, 6);
    LocalDateTime earliestEnd = TimeRange.getEarliestEnd(ranges);
    assertEquals(earliestEnd.getHour(), 5);
  }
}
