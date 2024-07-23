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
public class GradientTestRoutineTest extends BaseTest {

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
    GradientTestRoutine routine = new GradientTestRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.validateParameters();
    });
  }

  @Test
  public void emptyParametersTest() throws Exception {
    GradientTestRoutine routine = new GradientTestRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] {}));
    });
  }

  @Test
  public void tooManyParametersTest() throws Exception {
    GradientTestRoutine routine = new GradientTestRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "5", "7" }));
    });
  }

  @Test
  public void nonNumericParameterTest() throws Exception {
    GradientTestRoutine routine = new GradientTestRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "Not a number" }));
    });
  }

  @Test
  public void zeroParameterTest() throws Exception {
    GradientTestRoutine routine = new GradientTestRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "0" }));
    });
  }

  @Test
  public void negativeParameterTest() throws Exception {
    GradientTestRoutine routine = new GradientTestRoutine();
    assertThrows(RoutineException.class, () -> {
      routine.setParameters(Arrays.asList(new String[] { "-5" }));
    });
  }

  private GradientTestRoutine getRoutine() throws RoutineException {
    GradientTestRoutine routine = new GradientTestRoutine();
    routine.setParameters(Arrays.asList(new String[] { "5" }));
    return routine;
  }

  @Test
  public void constantTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 50D, 50D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void smallChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 52D, 52D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void limitChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 55D, 55D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void lowResChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 60D, 60D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void minimumChangeAboveLimitTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50D, 50D, 60D, 60D, 60D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void permanentChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50.1D, 50D, 60D, 60D, 60D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 3L, 4L })));
  }

  @Test
  public void singleSpikeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50.1D, 50D, 60D, 50D, 50D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 4L })));
  }

  @Test
  public void twoValueSpikeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50.1D, 50D, 60D, 60D, 50D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 3L, 4L, 5L, 6L })));
  }

  @Test
  public void twoGradientsTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6, 7 },
      new Double[] { 50D, 51D, 60D, 61D, 62D, 81D, 80D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, Flag.BAD,
      Arrays.asList(new Long[] { 2L, 3L, 5L, 6L })));
  }
}
