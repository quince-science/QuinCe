package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class DateTimeUtilsTest extends BaseTest {

  @Test
  public void isBetweenBetween() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2022, 01, 01, 12, 00, 00);
    assertTrue(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenBeforeStart() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2021, 12, 31, 12, 00, 00);
    assertFalse(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenAfterEnd() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2022, 01, 03, 00, 00, 00);
    assertFalse(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenEqualStart() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime testTime = start;
    assertTrue(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenEqualEnd() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime testTime = end;
    assertTrue(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenSwappedDates() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2022, 01, 01, 12, 00, 00);
    assertTrue(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenSameDates() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2022, 01, 01, 0, 00, 00);
    assertTrue(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenBeforeSameDates() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2021, 01, 01, 0, 00, 00);
    assertFalse(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void isBetweenAfterSameDates() {
    LocalDateTime start = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime testTime = LocalDateTime.of(2021, 01, 02, 0, 00, 00);
    assertFalse(DateTimeUtils.isBetween(testTime, start, end));
  }

  @Test
  public void overlapFullyInside() {
    LocalDateTime start1 = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2022, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyInsideSwappedStart() {
    LocalDateTime end1 = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime start1 = LocalDateTime.of(2022, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyInsideSwappedEnd() {
    LocalDateTime start1 = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2022, 01, 03, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyInsideBothSwapped() {
    LocalDateTime end1 = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime start1 = LocalDateTime.of(2022, 01, 03, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapStartOutside() {
    LocalDateTime start1 = LocalDateTime.of(2021, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2022, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapEndOutside() {
    LocalDateTime start1 = LocalDateTime.of(2022, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2023, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyEncompass() {
    LocalDateTime start1 = LocalDateTime.of(2021, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2023, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyOutsideStart() {
    LocalDateTime start1 = LocalDateTime.of(2021, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2021, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertFalse(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyOutsideEnd() {
    LocalDateTime start1 = LocalDateTime.of(2023, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2023, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertFalse(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyEndEqualsStart() {
    LocalDateTime start1 = LocalDateTime.of(2021, 01, 02, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }

  @Test
  public void overlapFullyStartEqualsEnd() {
    LocalDateTime start1 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);
    LocalDateTime end1 = LocalDateTime.of(2023, 01, 03, 00, 00, 00);
    LocalDateTime start2 = LocalDateTime.of(2022, 01, 01, 00, 00, 00);
    LocalDateTime end2 = LocalDateTime.of(2022, 01, 04, 00, 00, 00);

    assertTrue(DateTimeUtils.overlap(start1, end1, start2, end2));
  }
}
