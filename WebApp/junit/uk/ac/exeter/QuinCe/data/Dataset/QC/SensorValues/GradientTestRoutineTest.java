package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
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

  private GradientTestRoutine getRoutine() throws RoutineException {
    Map<Flag, Range<Double>> limits = new HashMap<Flag, Range<Double>>();
    limits.put(flagScheme.getBadFlag(), Range.between(0D, 5D));
    return new GradientTestRoutine(flagScheme, Mockito.mock(SensorType.class),
      limits);
  }

  @Test
  public void constantTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 50D, 50D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void smallChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 52D, 52D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void limitChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 55D, 55D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void lowResChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4 }, new Double[] { 50D, 50D, 60D, 60D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void minimumChangeAboveLimitTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50D, 50D, 60D, 60D, 60D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] {})));
  }

  @Test
  public void permanentChangeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50.1D, 50D, 60D, 60D, 60D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] { 3L, 4L })));
  }

  @Test
  public void singleSpikeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50.1D, 50D, 60D, 50D, 50D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] { 4L })));
  }

  @Test
  public void twoValueSpikeTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6 },
      new Double[] { 50D, 50.1D, 50D, 60D, 60D, 50D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] { 3L, 4L, 5L, 6L })));
  }

  @Test
  public void twoGradientsTest() throws Exception {
    List<SensorValue> sensorValues = SVTestUtils.makeSensorValues(
      new int[] { 1, 2, 3, 4, 5, 6, 7 },
      new Double[] { 50D, 51D, 60D, 61D, 62D, 81D, 80D });

    GradientTestRoutine routine = getRoutine();
    routine.qcAction(sensorValues);
    assertTrue(SVTestUtils.checkAutoQC(sensorValues, flagScheme.getBadFlag(),
      Arrays.asList(new Long[] { 2L, 3L, 5L, 6L })));
  }
}
