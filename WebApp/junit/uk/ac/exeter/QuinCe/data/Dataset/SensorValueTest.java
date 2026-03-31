package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.ConstantValueRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.RangeCheckRoutine;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class SensorValueTest extends BaseTest {

  @BeforeEach
  public void init() {
    initResourceManager();
  }

  private SensorValue makeDBSensorValue() {
    SensorValue sensorValue = new SensorValue(1L, 1L, flagScheme, 1L,
      new TimeCoordinate(LocalDateTime.of(2021, 1, 1, 0, 0, 0)), "20", null,
      flagScheme.getAssumedGoodFlag(), null);
    return sensorValue;
  }

  /**
   * Test that the Automatic QC cannot be updated if the value has not yet been
   * stored in the database.
   */
  @FlywayTest
  @Test
  public void cannotUpdateAutoQCOnNonStoredValueTest() {
    SensorValue sensorValue = new SensorValue(1L, flagScheme, 1L,
      new TimeCoordinate(LocalDateTime.of(2021, 1, 1, 0, 0, 0)), "20");

    assertThrows(RecordNotFoundException.class, () -> {
      sensorValue.addAutoQCFlag(
        new RoutineFlag(flagScheme, Mockito.mock(RangeCheckRoutine.class),
          flagScheme.getBadFlag(), "77", "88"));
    });
  }

  @FlywayTest
  @Test
  public void singleAutoQCFlagTest()
    throws RecordNotFoundException, RoutineException {

    AutoQCRoutine routine = Mockito.mock(ConstantValueRoutine.class);
    RoutineFlag flag = new RoutineFlag(flagScheme, routine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(flag);

    assertEquals(flagScheme.getBadFlag(), sensorValue.getAutoQcFlag(),
      "Incorrect Auto QC Flag");
    assertEquals(routine.getShortMessage(),
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class)),
      "Incorrect QC message");
    assertEquals(FlagScheme.NEEDED_FLAG, sensorValue.getUserQCFlag(),
      "Incorrect User QC flag");
  }

  @FlywayTest
  @Test
  public void twoBadAutoQCFlagTest()
    throws RecordNotFoundException, RoutineException {
    ConstantValueRoutine constantRoutine = Mockito
      .mock(ConstantValueRoutine.class);
    RoutineFlag constantFlag = new RoutineFlag(flagScheme, constantRoutine,
      flagScheme.getBadFlag(), "1", "2");

    RangeCheckRoutine rangeRoutine = Mockito.mock(RangeCheckRoutine.class);
    RoutineFlag rangeFlag = new RoutineFlag(flagScheme, rangeRoutine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(constantFlag);
    sensorValue.addAutoQCFlag(rangeFlag);

    assertEquals(flagScheme.getBadFlag(), sensorValue.getAutoQcFlag(),
      "Incorrect Auto QC Flag");
    assertTrue(
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class))
        .contains(constantRoutine.getShortMessage()),
      "Missing constant message");
    assertTrue(
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class))
        .contains(rangeRoutine.getShortMessage()),
      "Missing range message");
  }

  @FlywayTest
  @Test
  public void oneBadOneQuestionableAutoQCFlagTest()
    throws RecordNotFoundException, RoutineException {
    ConstantValueRoutine constantRoutine = Mockito
      .mock(ConstantValueRoutine.class);
    RoutineFlag constantFlag = new RoutineFlag(flagScheme, constantRoutine,
      IcosFlagScheme.QUESTIONABLE_FLAG, "1", "2");

    RangeCheckRoutine rangeRoutine = Mockito.mock(RangeCheckRoutine.class);
    RoutineFlag rangeFlag = new RoutineFlag(flagScheme, rangeRoutine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(constantFlag);
    sensorValue.addAutoQCFlag(rangeFlag);

    assertEquals(flagScheme.getBadFlag(), sensorValue.getAutoQcFlag(),
      "Incorrect Auto QC Flag");
    assertTrue(
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class))
        .contains(constantRoutine.getShortMessage()),
      "Missing constant message");
    assertTrue(
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class))
        .contains(rangeRoutine.getShortMessage()),
      "Missing range message");
  }

  @FlywayTest
  @Test
  public void userQCOverridesAutoQCTest()
    throws RecordNotFoundException, RoutineException, InvalidFlagException {
    ConstantValueRoutine constantRoutine = Mockito
      .mock(ConstantValueRoutine.class);
    RoutineFlag constantFlag = new RoutineFlag(flagScheme, constantRoutine,
      IcosFlagScheme.QUESTIONABLE_FLAG, "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(constantFlag);

    sensorValue.setUserQC(flagScheme.getBadFlag(), "User Message");

    assertEquals(flagScheme.getBadFlag(),
      sensorValue.getDisplayFlag(Mockito.mock(DatasetSensorValues.class)),
      "Incorrect Flag");
    assertEquals("User Message",
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class)),
      "Incorrect message");
  }

  @FlywayTest
  @Test
  public void removeOneAutoQCFlagTest()
    throws RecordNotFoundException, RoutineException {
    ConstantValueRoutine constantRoutine = Mockito
      .mock(ConstantValueRoutine.class);
    RoutineFlag constantFlag = new RoutineFlag(flagScheme, constantRoutine,
      flagScheme.getBadFlag(), "1", "2");

    RangeCheckRoutine rangeRoutine = Mockito.mock(RangeCheckRoutine.class);
    RoutineFlag rangeFlag = new RoutineFlag(flagScheme, rangeRoutine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(constantFlag);
    sensorValue.addAutoQCFlag(rangeFlag);

    assertTrue(sensorValue.removeAutoQCFlag(constantRoutine));

    assertEquals(flagScheme.getBadFlag(), sensorValue.getAutoQcFlag(),
      "Incorrect Auto QC Flag");
    assertFalse(
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class))
        .contains(constantRoutine.getShortMessage()),
      "Missing constant message");
    assertTrue(
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class))
        .contains(rangeRoutine.getShortMessage()),
      "Missing range message");
  }

  @FlywayTest
  @Test
  public void removeUnusedAutoQCFlagTest()
    throws RecordNotFoundException, RoutineException {

    ConstantValueRoutine constantRoutine = Mockito
      .mock(ConstantValueRoutine.class);
    RoutineFlag constantFlag = new RoutineFlag(flagScheme, constantRoutine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(constantFlag);

    assertFalse(
      sensorValue.removeAutoQCFlag(Mockito.mock(RangeCheckRoutine.class)));
  }

  @FlywayTest
  @Test
  public void removeSingleAutoQCFlagTest()
    throws RecordNotFoundException, RoutineException {

    AutoQCRoutine routine = Mockito.mock(ConstantValueRoutine.class);
    RoutineFlag flag = new RoutineFlag(flagScheme, routine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(flag);

    assertTrue(sensorValue.removeAutoQCFlag(routine));

    assertEquals(flagScheme.getGoodFlag(), sensorValue.getAutoQcFlag(),
      "Incorrect Auto QC Flag");
    assertEquals("",
      sensorValue.getDisplayQCMessage(Mockito.mock(DatasetSensorValues.class)),
      "Incorrect QC message");
    assertEquals(flagScheme.getAssumedGoodFlag(), sensorValue.getUserQCFlag(),
      "Incorrect User QC flag");
  }

  @FlywayTest
  @Test
  public void voidremoveSingleAutoQCWithUserQCTest()
    throws RecordNotFoundException, RoutineException, InvalidFlagException {
    AutoQCRoutine routine = Mockito.mock(ConstantValueRoutine.class);
    RoutineFlag flag = new RoutineFlag(flagScheme, routine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.addAutoQCFlag(flag);

    sensorValue.setUserQC(IcosFlagScheme.QUESTIONABLE_FLAG, "User Message");

    sensorValue.removeAutoQCFlag(routine);

    assertEquals(IcosFlagScheme.QUESTIONABLE_FLAG, sensorValue.getUserQCFlag());
    assertEquals("User Message", sensorValue.getUserQCMessage());
  }

  @FlywayTest
  @Test
  public void autoQCDoesNotOverrideUserQCTest()
    throws RecordNotFoundException, RoutineException, InvalidFlagException {
    AutoQCRoutine routine = Mockito.mock(ConstantValueRoutine.class);
    RoutineFlag flag = new RoutineFlag(flagScheme, routine,
      flagScheme.getBadFlag(), "1", "2");

    SensorValue sensorValue = makeDBSensorValue();
    sensorValue.setUserQC(IcosFlagScheme.QUESTIONABLE_FLAG, "User Message");
    sensorValue.addAutoQCFlag(flag);

    assertEquals(IcosFlagScheme.QUESTIONABLE_FLAG, sensorValue.getUserQCFlag());
    assertEquals("User Message", sensorValue.getUserQCMessage());
  }

}
