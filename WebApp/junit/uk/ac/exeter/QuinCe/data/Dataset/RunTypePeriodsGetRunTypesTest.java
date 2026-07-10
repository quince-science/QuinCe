package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

public class RunTypePeriodsGetRunTypesTest extends BaseTest {

  private static RunTypePeriods periods = null;

  private static LocalDateTime getTime(int hour, int minute) {
    return LocalDateTime.of(2026, 7, 9, hour, minute, 0);
  }

  @BeforeAll
  public static void setup() {
    periods = new RunTypePeriods();
    for (int i = 10; i <= 13; i++) {
      LocalDateTime start = getTime(i, 0);
      LocalDateTime end = getTime(i, 30);

      RunTypePeriod period = new RunTypePeriod(i % 2 == 0 ? "A" : "B", start);
      period.setEnd(end);
      periods.add(period);
    }
  }

  @Test
  public void outOfOrderTest() {
    List<LocalDateTime> times = Arrays.asList(getTime(12, 0), getTime(11, 0));
    assertThrows(IllegalArgumentException.class,
      () -> periods.getRunTypes(times, false));
  }

  private static Stream<Arguments> getSingleTimeTestParams() {
    return Stream.of(Arguments.of(9, 45, true, null),
      Arguments.of(9, 45, false, null), Arguments.of(10, 0, true, "A"),
      Arguments.of(10, 15, true, "A"), Arguments.of(10, 30, true, "A"),
      Arguments.of(10, 45, true, "A"), Arguments.of(10, 45, false, null),
      Arguments.of(11, 10, true, "B"), Arguments.of(14, 0, true, "B"),
      Arguments.of(14, 0, false, null));
  }

  @ParameterizedTest
  @MethodSource("getSingleTimeTestParams")
  public void singleTimeTest(int hour, int minute, boolean fallback,
    String runType) {

    List<LocalDateTime> time = Arrays.asList(getTime(hour, minute));

    Map<LocalDateTime, String> runTypes = periods.getRunTypes(time, fallback);
    assertEquals(runType, runTypes.get(time.get(0)));
  }

  private static Stream<Arguments> getMultipleTimeTestParams() {
    return Stream.of(Arguments.of(9, 10, 9, 20, true, null, null),
      Arguments.of(9, 10, 9, 20, false, null, null),
      Arguments.of(9, 10, 10, 10, true, null, "A"),
      Arguments.of(9, 10, 10, 10, true, null, "A"),
      Arguments.of(10, 10, 10, 20, true, "A", "A"),
      Arguments.of(10, 10, 10, 20, true, "A", "A"),
      Arguments.of(10, 10, 10, 40, true, "A", "A"),
      Arguments.of(10, 10, 10, 40, false, "A", null),
      Arguments.of(10, 40, 10, 45, true, "A", "A"),
      Arguments.of(10, 40, 10, 45, false, null, null),
      Arguments.of(12, 10, 13, 45, true, "A", "B"),
      Arguments.of(12, 10, 13, 45, false, "A", null),
      Arguments.of(12, 45, 13, 45, true, "A", "B"),
      Arguments.of(12, 45, 13, 45, false, null, null),
      Arguments.of(9, 10, 14, 10, true, null, "B"),
      Arguments.of(9, 10, 14, 10, false, null, null),
      Arguments.of(11, 10, 12, 10, true, "B", "A"),
      Arguments.of(11, 10, 12, 10, false, "B", "A"),
      Arguments.of(13, 30, 15, 0, true, "B", "B"),
      Arguments.of(13, 30, 15, 0, false, "B", null),
      Arguments.of(14, 30, 15, 0, true, "B", "B"),
      Arguments.of(14, 30, 15, 0, false, null, null));
  }

  @ParameterizedTest
  @MethodSource("getMultipleTimeTestParams")
  public void multipleTimeTest(int hour1, int minute1, int hour2, int minute2,
    boolean fallback, String runType1, String runType2) {

    LocalDateTime time1 = getTime(hour1, minute1);
    LocalDateTime time2 = getTime(hour2, minute2);

    List<LocalDateTime> times = Arrays.asList(time1, time2);

    Map<LocalDateTime, String> runTypes = periods.getRunTypes(times, fallback);

    assertEquals(runType1, runTypes.get(time1));
    assertEquals(runType2, runTypes.get(time2));
  }

  @ParameterizedTest
  @CsvSource({ "true", "false" })
  public void emptyPeriodsTest(boolean fallback) {
    RunTypePeriods emptyPeriods = new RunTypePeriods();
    assertNull(emptyPeriods.getRunType(getTime(12, 0), fallback));
  }
}
