package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.RangeCheckRoutine;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class SensorValueTest extends BaseTest {

  @BeforeEach
  public void init() {
    initResourceManager();
  }

  /**
   * Test that the Automatic QC cannot be updated if the value has not yet been
   * stored in the database.
   */
  @FlywayTest
  @Test
  public void cannotUpdateAutoQCOnNonStoredValueTest() {
    SensorValue sensorValue = new SensorValue(1L, 1L,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(RecordNotFoundException.class, () -> {
      sensorValue.addAutoQCFlag(
        new RoutineFlag(new RangeCheckRoutine(), Flag.BAD, "77", "88"));
    });
  }

}
