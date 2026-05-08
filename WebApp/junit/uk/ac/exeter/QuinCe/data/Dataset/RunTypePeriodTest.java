package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

/**
 * Tests for the {@link RunTypePeriod} class. Note that these can't be create
 * directly, we use {@link RunTypePeriods} to create them.
 */
public class RunTypePeriodTest extends BaseTest {

  /**
   * Create a single-entry {@link RunTypePeriod} and check that it contains
   * everything it should.
   */
  @Test
  public void creationTest() throws Exception {

    LocalDateTime time = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", time);

    RunTypePeriod period = periods.get(0);

    assertEquals("RunType", period.getRunType());
    assertEquals(time, period.getStart());
    assertEquals(time, period.getEnd());
  }

  /**
   * Test that the end of a period can be set correctly.
   */
  @Test
  public void endSetTest() throws Exception {

    LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    LocalDateTime end = LocalDateTime.of(2000, 1, 1, 1, 0, 0);

    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", start);
    periods.add("RunType", end);

    assertEquals(end, periods.get(0).getEnd());
  }

  /**
   * Test the {@link RunTypePeriod#encompasses(LocalDateTime)} method.
   */
  @Test
  public void encompassesTest() throws Exception {
    LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    LocalDateTime end = LocalDateTime.of(2000, 1, 1, 1, 0, 0);

    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", start);
    periods.add("RunType", end);

    RunTypePeriod period = periods.get(0);

    assertTrue(period.encompasses(start));
    assertTrue(period.encompasses(end));
    assertTrue(period.encompasses(LocalDateTime.of(2000, 1, 1, 0, 30, 0)));

    assertFalse(period.encompasses(LocalDateTime.of(1999, 12, 31, 23, 59, 59)));
    assertFalse(period.encompasses(LocalDateTime.of(2000, 2, 1, 0, 0, 0)));
  }

  @Test
  public void singleTimeEncompassesTest() throws Exception {
    LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    RunTypePeriods periods = new RunTypePeriods();
    periods.add("RunType", start);

    RunTypePeriod period = periods.get(0);

    assertTrue(period.encompasses(start));
    assertFalse(period.encompasses(LocalDateTime.of(2000, 1, 1, 0, 30, 0)));
    assertFalse(period.encompasses(LocalDateTime.of(1999, 12, 31, 23, 59, 59)));
    assertFalse(period.encompasses(LocalDateTime.of(2000, 2, 1, 0, 0, 0)));
  }
}
