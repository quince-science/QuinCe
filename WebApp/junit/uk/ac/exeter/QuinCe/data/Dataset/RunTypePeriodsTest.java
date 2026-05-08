package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

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
}
