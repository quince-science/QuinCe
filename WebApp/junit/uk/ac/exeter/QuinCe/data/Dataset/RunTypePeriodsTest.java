package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

/**
 * Tests for the {@link RunTypePeriods} class.
 */
public class RunTypePeriodsTest extends BaseTest {

  @Test
  public void singleEntryTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    // Tests of the contents of the period are in RunTypePeriodTest
    assertEquals(1, periods.size());
  }

  @Test
  public void twoRunTypesTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    periods.add("RunType2", LocalDateTime.of(2000, 1, 2, 0, 0, 0));

    assertEquals(2, periods.size());
  }

  @Test
  public void secondRunTypeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    periods.add("RunType2", LocalDateTime.of(2000, 1, 2, 0, 0, 0));
    periods.add("RunType", LocalDateTime.of(2000, 1, 3, 0, 0, 0));

    assertEquals(3, periods.size());
  }

  @Test
  public void twoPartsThenSecondRunTypeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    periods.add("RunType", LocalDateTime.of(2000, 1, 2, 0, 0, 0));
    periods.add("RunType2", LocalDateTime.of(2000, 1, 3, 0, 0, 0));

    assertEquals(2, periods.size());
  }

  @Test
  public void earlierTimeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    assertThrows(DataSetException.class, () -> {
      periods.add("RunType", LocalDateTime.of(1999, 12, 31, 0, 0, 0));
    });
  }

  @Test
  public void newPeriodEarlierTimeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    assertThrows(DataSetException.class, () -> {
      periods.add("RunType2", LocalDateTime.of(1999, 12, 31, 0, 0, 0));
    });
  }

  @Test
  public void sameTimeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    assertThrows(DataSetException.class, () -> {
      periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    });
  }

  @Test
  public void newPeriodSameTimeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    assertThrows(DataSetException.class, () -> {
      periods.add("RunType2", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    });
  }

  @Test
  public void finishedEmptyTest() {
    RunTypePeriods periods = new RunTypePeriods();
    periods.finish();
    assertThrows(DataSetException.class, () -> {
      periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    });
  }

  @Test
  public void finishedContinueRunTypeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    periods.finish();
    assertThrows(DataSetException.class, () -> {
      periods.add("RunType", LocalDateTime.of(2000, 1, 2, 0, 0, 0));
    });
  }

  @Test
  public void finishedNewRunTypeTest() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    periods.finish();
    assertThrows(DataSetException.class, () -> {
      periods.add("RunType2", LocalDateTime.of(2000, 1, 2, 0, 0, 0));
    });
  }

  @Test
  public void finishedLastTimeTest() throws DataSetException {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    periods.finish();

    RunTypePeriod period = periods.get(0);
    assertEquals(LocalDateTime.of(2000, 1, 1, 0, 0, 0), period.getStart());
    assertEquals(LocalDateTime.MAX, period.getEnd());
  }

  @Test
  public void finishedUpdatedLastTimeTest() throws DataSetException {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    periods.add("RunType", LocalDateTime.of(2000, 1, 2, 0, 0, 0));
    periods.finish();

    RunTypePeriod period = periods.get(0);
    assertEquals(LocalDateTime.of(2000, 1, 1, 0, 0, 0), period.getStart());
    assertEquals(LocalDateTime.MAX, period.getEnd());
  }

  @Test
  public void containsEmptyTest() {
    RunTypePeriods periods = new RunTypePeriods();
    assertFalse(periods.contains(LocalDateTime.of(2000, 1, 1, 0, 0, 0)));
  }

  private RunTypePeriods makeContainsPeriods() throws Exception {
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", LocalDateTime.of(2000, 1, 1, 0, 0, 0));
    periods.add("RunType", LocalDateTime.of(2000, 1, 2, 0, 0, 0));
    periods.add("RunType2", LocalDateTime.of(2000, 2, 1, 0, 0, 0));
    periods.add("RunType2", LocalDateTime.of(2000, 2, 2, 0, 0, 0));
    // Deliberately left unfinished
    return periods;
  }

  @Test
  public void containsBeforeFirst() throws Exception {
    assertFalse(makeContainsPeriods()
      .contains(LocalDateTime.of(1999, 12, 31, 23, 59, 59)));
  }

  @Test
  public void containsAfterLast() throws Exception {
    assertFalse(
      makeContainsPeriods().contains(LocalDateTime.of(2000, 2, 3, 0, 0, 0)));
  }

  @Test
  public void containsBetween() throws Exception {
    assertFalse(
      makeContainsPeriods().contains(LocalDateTime.of(2000, 1, 12, 0, 0, 0)));
  }

  @Test
  public void containsInFirst() throws Exception {
    assertTrue(
      makeContainsPeriods().contains(LocalDateTime.of(2000, 1, 1, 12, 0, 0)));
  }

  @Test
  public void containsInSecond() throws Exception {
    assertTrue(
      makeContainsPeriods().contains(LocalDateTime.of(2000, 2, 1, 12, 0, 0)));
  }

  private static Stream<Arguments> getSingleTimeTestParams() {
    return Stream.of(Arguments.of(9, 45, true, null),
      Arguments.of(9, 45, false, null), Arguments.of(10, 0, true, "A"),
      Arguments.of(10, 15, true, "A"), Arguments.of(10, 30, true, "A"),
      Arguments.of(10, 45, true, "A"), Arguments.of(10, 45, false, null),
      Arguments.of(11, 10, true, "B"), Arguments.of(14, 0, true, "B"),
      Arguments.of(14, 0, false, null));
  }

  private LocalDateTime getTime(int hour, int minute) {
    return LocalDateTime.of(2026, 7, 9, hour, minute, 0);
  }

  @ParameterizedTest
  @MethodSource("getSingleTimeTestParams")
  public void getRunTypePeriodTest(int hour, int minute, boolean fallback,
    String runType) {

    RunTypePeriods periods = new RunTypePeriods();
    for (int i = 10; i <= 13; i++) {
      LocalDateTime start = getTime(i, 0);
      LocalDateTime end = getTime(i, 30);

      RunTypePeriod period = new RunTypePeriod(i % 2 == 0 ? "A" : "B", start);
      period.setEnd(end);
      periods.add(period);
    }

    assertEquals(runType, periods.getRunType(getTime(hour, minute), fallback));
  }
}
