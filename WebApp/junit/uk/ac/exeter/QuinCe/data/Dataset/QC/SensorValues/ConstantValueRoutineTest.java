package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@TestInstance(Lifecycle.PER_CLASS)
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user" })
public class ConstantValueRoutineTest extends BaseTest {

  @BeforeAll
  public void init() {
    initResourceManager();
  }

  @AfterAll
  public void tearDown() {
    ResourceManager.destroy();
  }

  @Test
  public void nullParametersTest() {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.validateParameters();
    });
  }

  @Test
  public void emptyParametersTest() throws Exception {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] {}));
    });
  }

  @Test
  public void tooManyParametersTest() throws Exception {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "5", "7" }));
    });
  }

  @Test
  public void nonNumericParameterTest() throws Exception {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "Not a number" }));
    });
  }

  @Test
  public void zeroParameterTest() throws Exception {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "0" }));
    });
  }

  @Test
  public void negativeParameterTest() throws Exception {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "-5" }));
    });
  }

  private ConstantValueRoutine getRoutine() throws RoutineException {
    ConstantValueRoutine routine = new ConstantValueRoutine();
    routine.setParameters(Arrays.asList(new String[] { "3" }));
    return routine;
  }

  @Test
  public void neverConstantTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5 }, new Double[] { 50D, 51D, 52D, 53D, 54D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void threeMinutesTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5 }, new Double[] { 50D, 50D, 50D, 50D, 54D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void fourMinutesTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5 }, new Double[] { 50D, 50D, 50D, 50D, 50D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 1L, 2L, 3L, 4L, 5L })));
  }

  @Test
  public void blipTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5 }, new Double[] { 50D, 50D, 51D, 50D, 50D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void twoConsecutivePeriodsTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
      new Double[] { 50D, 50D, 50D, 50D, 50D, 51D, 51D, 51D, 51D, 51D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L })));
  }

  @Test
  public void twoSplitPeriodsTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }, new Double[] { 50D,
        50D, 50D, 50D, 50D, 55D, 56D, 51D, 51D, 51D, 51D, 51D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 1L, 2L, 3L, 4L, 5L, 8L, 9L, 10L, 11L, 12L })));
  }

  @Test
  public void nanTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5 },
      new Double[] { 50D, 50D, 50D, Double.NaN, 50D });

    ConstantValueRoutine routine = getRoutine();
    routine.qcAction(sensorValues);

    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 1L, 2L, 3L, 5L })));
  }

}
