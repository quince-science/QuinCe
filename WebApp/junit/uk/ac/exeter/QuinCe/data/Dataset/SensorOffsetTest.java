package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

public class SensorOffsetTest extends BaseTest {

  @Test
  public void constructor() {

    LocalDateTime time = LocalDateTime.of(2021, 01, 01, 12, 31, 00);
    long offsetSize = 100L;

    SensorOffset offset = new SensorOffset(time, offsetSize);

    assertAll(() -> assertEquals(time, offset.getTime()),
      () -> assertEquals(offsetSize, offset.getOffset()));
  }

  @Test
  public void timeMilliseconds() {
    LocalDateTime time = LocalDateTime.of(2021, 01, 01, 12, 31, 00);
    long timeMillis = time.toInstant(ZoneOffset.UTC).toEpochMilli();

    SensorOffset offset = new SensorOffset(time, 100L);
    assertEquals(timeMillis, offset.getTimeMilliseconds());
  }
}
