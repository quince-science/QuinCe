package uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
  public void latestStartExactTest() {
    Collection<TimeRange> ranges = makeStarts(12, 5, 15, 6);
    LocalDateTime latestStart = TimeRange.getLatestStart(ranges, 0);
    assertEquals(latestStart.getHour(), 15);
  }

  @Test
  public void earliestEndExactTest() {
    Collection<TimeRange> ranges = makeEnds(12, 5, 15, 6);
    LocalDateTime earliestEnd = TimeRange.getEarliestEnd(ranges, 0);
    assertEquals(earliestEnd.getHour(), 5);
  }

  /**
   * <ul>
   * <li>First file: 01:10:00</li>
   * <li>Second file: 01:00:00</li>
   * <li>Expected latest start time: 01:00:00</li>
   * </ul>
   */
  @Test
  public void latestStartShortFirstFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 1, 0, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 1, 10, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime latestStart = TimeRange.getLatestStart(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 1, 1, 0, 0), latestStart);
  }

  /**
   * <ul>
   * <li>First file: 01:00:00</li>
   * <li>Second file: 01:10:00</li>
   * <li>Expected latest start time: 01:00:00</li>
   * </ul>
   */
  @Test
  public void latestStartShortSecondFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 1, 10, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 1, 0, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime latestStart = TimeRange.getLatestStart(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 1, 1, 0, 0), latestStart);
  }

  /**
   * <ul>
   * <li>First file: 04:00:00</li>
   * <li>Second file: 01:00:00</li>
   * <li>Expected latest start time: 03:00:00</li>
   * </ul>
   */
  @Test
  public void latestStartVeryShortFirstFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 4, 0, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 1, 0, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime latestStart = TimeRange.getLatestStart(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 1, 3, 0, 0), latestStart);
  }

  /**
   * <ul>
   * <li>First file: 01:00:00</li>
   * <li>Second file: 04:00:00</li>
   * <li>Expected latest start time: 03:00:00</li>
   * </ul>
   */
  @Test
  public void latestStartVeryShortSecondFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 1, 0, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 4, 0, 0),
      LocalDateTime.of(2025, 1, 2, 1, 0, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime latestStart = TimeRange.getLatestStart(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 1, 3, 0, 0), latestStart);
  }

  /**
   * <ul>
   * <li>First file: 00:25:00</li>
   * <li>Second file: 00:30:00</li>
   * <li>Expected latest start time: 00:30:00</li>
   * </ul>
   */
  @Test
  public void earliestEndShortFirstFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 0, 25, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 0, 30, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime earliestEnd = TimeRange.getEarliestEnd(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 2, 0, 30, 0), earliestEnd);
  }

  /**
   * <ul>
   * <li>First file: 00:30:00</li>
   * <li>Second file: 00:25:00</li>
   * <li>Expected latest start time: 00:30:00</li>
   * </ul>
   */
  @Test
  public void earliestEndShortSecondFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 0, 30, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 0, 25, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime earliestEnd = TimeRange.getEarliestEnd(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 2, 0, 30, 0), earliestEnd);
  }

  /**
   * <ul>
   * <li>First file: 00:00:00</li>
   * <li>Second file: 04:00:00</li>
   * <li>Expected latest start time: 03:00:00</li>
   * </ul>
   */
  @Test
  public void earliestEndVeryShortFirstFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 0, 0, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 4, 0, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime earliestEnd = TimeRange.getEarliestEnd(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 2, 1, 0, 0), earliestEnd);
  }

  /**
   * <ul>
   * <li>First file: 04:00:00</li>
   * <li>Second file: 00:00:00</li>
   * <li>Expected latest start time: 03:00:00</li>
   * </ul>
   */
  @Test
  public void earliestEndVeryShortSecondFileBufferTest() {
    TimeRange range1 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 0, 0, 0));

    TimeRange range2 = new SimpleTimeRange(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0),
      LocalDateTime.of(2025, 1, 2, 4, 0, 0));

    List<TimeRange> ranges = new ArrayList<TimeRange>();
    ranges.add(range1);
    ranges.add(range2);

    LocalDateTime earliestEnd = TimeRange.getEarliestEnd(ranges, 3600);
    assertEquals(LocalDateTime.of(2025, 1, 2, 1, 0, 0), earliestEnd);
  }
}

class SimpleTimeRange implements TimeRange {

  private final LocalDateTime start;

  private final LocalDateTime end;

  protected SimpleTimeRange(LocalDateTime start, LocalDateTime end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public LocalDateTime getStart() {
    return start;
  }

  @Override
  public LocalDateTime getEnd() {
    return end;
  }

  @Override
  public String toString() {
    return start.toString() + " - " + end.toString();
  }

}
