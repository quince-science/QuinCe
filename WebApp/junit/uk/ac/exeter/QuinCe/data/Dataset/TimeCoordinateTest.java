package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TimeCoordinateTest {

  @ParameterizedTest
  @CsvSource({ "10,1", "20,0", "30,-1" })
  public void compareToTest(int minute, int compareResult) {

    TimeCoordinate referenceCoordinate = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 12, 20, 0));

    TimeCoordinate testCoordinate = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 12, minute, 0));

    assertEquals(compareResult, referenceCoordinate.compareTo(testCoordinate));
  }

  @ParameterizedTest
  @CsvSource({ "10,false", "20,false", "30,true" })
  public void isBeforeTest(int minute, boolean expectedResult) {

    TimeCoordinate referenceCoordinate = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 12, 20, 0));

    LocalDateTime testTime = LocalDateTime.of(2025, 1, 1, 12, minute, 0);

    assertEquals(expectedResult, referenceCoordinate.isBefore(testTime));
  }

  @ParameterizedTest
  @CsvSource({ "10,true", "20,false", "30,false" })
  public void isAfterTest(int minute, boolean expectedResult) {

    TimeCoordinate referenceCoordinate = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 12, 20, 0));

    LocalDateTime testTime = LocalDateTime.of(2025, 1, 1, 12, minute, 0);

    assertEquals(expectedResult, referenceCoordinate.isAfter(testTime));
  }

  @Test
  public void equalsDifferentDatasetTest() throws CoordinateException {
    TimeCoordinate a = new TimeCoordinate(1L,
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    TimeCoordinate b = new TimeCoordinate(2L,
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));

    assertFalse(a.equals(b));
  }

  @Test
  public void equalsDifferentTimeTest() throws CoordinateException {
    TimeCoordinate a = new TimeCoordinate(1L,
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    TimeCoordinate b = new TimeCoordinate(1L,
      LocalDateTime.of(2025, 1, 1, 0, 0, 1));

    assertFalse(a.equals(b));
  }

  @Test
  public void equalsTest() throws CoordinateException {
    TimeCoordinate a = new TimeCoordinate(1L,
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    TimeCoordinate b = new TimeCoordinate(1L,
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));

    assertTrue(a.equals(b));
  }
}
