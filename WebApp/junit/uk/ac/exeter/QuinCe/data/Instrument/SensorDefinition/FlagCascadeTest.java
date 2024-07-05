package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for QC flag cascading. Mostly based on the setup for the Underway
 * Marine pCO2 variable.
 */
@TestInstance(Lifecycle.PER_CLASS)
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
  "resources/sql/data/Instrument/SensorDefinition/VariableTest/flagCascade" })
public class FlagCascadeTest extends BaseTest {

  SensorAssignments sensorAssignments;

  Variable variable;

  @BeforeAll
  public void setup() throws Exception {
    initResourceManager();
    Instrument instrument = InstrumentDB.getInstrument(getConnection(), 1);
    variable = instrument.getVariable("testVar");
    sensorAssignments = instrument.getSensorAssignments();
  }

  @AfterAll
  public void teardown() {
    sensorAssignments = null;
    variable = null;
    ResourceManager.destroy();
  }

  private SensorType getSensorType(String name)
    throws SensorTypeNotFoundException {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType(name);
  }

  private List<Flag> getNonGoodFlags() {
    return Arrays.asList(Flag.QUESTIONABLE, Flag.BAD);
  }

  private List<Flag> getGoodFlags() {
    return Arrays.asList(Flag.GOOD, Flag.ASSUMED_GOOD);
  }

  private List<SensorType> getFlagReturnSensorTypes()
    throws SensorTypeNotFoundException {
    return Arrays.asList(getSensorType("coreSensor"),
      SensorType.LONGITUDE_SENSOR_TYPE, SensorType.LATITUDE_SENSOR_TYPE);
  }

  private List<Arguments> getReturnFlagArguments() throws Exception {
    List<Arguments> result = new ArrayList<Arguments>();

    getFlagReturnSensorTypes().forEach(s -> {
      getNonGoodFlags().forEach(f -> {
        result.add(Arguments.of(s, f));
      });
    });

    return result;
  }

  @ParameterizedTest
  @MethodSource({ "getReturnFlagArguments" })
  public void flagReturnTest(SensorType sensorType, Flag flag)
    throws Exception {

    assertEquals(Flag.QUESTIONABLE,
      variable.getCascade(sensorType, Flag.QUESTIONABLE, sensorAssignments));
  }

  @ParameterizedTest
  @MethodSource({ "getGoodFlags" })
  public void goodFlagTest(Flag flag) throws Exception {
    SensorType sensorType = getSensorType("requiredSensor1");
    assertEquals(Flag.ASSUMED_GOOD,
      variable.getCascade(sensorType, flag, sensorAssignments));
  }

  private List<Arguments> getRequiredCascadeParams() throws Exception {
    List<Arguments> result = new ArrayList<Arguments>();

    result.add(Arguments.of(getSensorType("requiredSensor1"), Flag.QUESTIONABLE,
      Flag.QUESTIONABLE));
    result.add(Arguments.of(getSensorType("requiredSensor1"), Flag.BAD,
      Flag.QUESTIONABLE));

    result.add(Arguments.of(getSensorType("requiredSensor2"), Flag.QUESTIONABLE,
      Flag.QUESTIONABLE));
    result
      .add(Arguments.of(getSensorType("requiredSensor2"), Flag.BAD, Flag.BAD));

    result.add(Arguments.of(getSensorType("requiredSensor3"), Flag.QUESTIONABLE,
      Flag.BAD));
    result.add(Arguments.of(getSensorType("requiredSensor3"), Flag.BAD,
      Flag.QUESTIONABLE));

    result.add(Arguments.of(getSensorType("requiredSensor4"), Flag.QUESTIONABLE,
      Flag.BAD));
    result
      .add(Arguments.of(getSensorType("requiredSensor4"), Flag.BAD, Flag.BAD));

    return result;
  }

  @ParameterizedTest
  @MethodSource({ "getRequiredCascadeParams" })
  public void requiredCascadeTest(SensorType sensorType, Flag inFlag,
    Flag cascadeFlag) throws Exception {
    assertEquals(cascadeFlag,
      variable.getCascade(sensorType, inFlag, sensorAssignments));
  }

  @ParameterizedTest
  @MethodSource({ "getNonGoodFlags" })
  public void unusedSensorTypeTest(Flag flag) throws Exception {
    assertNull(
      variable.getCascade(getSensorType("Optional"), flag, sensorAssignments));
  }

  @Test
  public void otherFlagTest() throws Exception {
    assertEquals(Flag.NEEDED, variable.getCascade(
      getSensorType("requiredSensor4"), Flag.NEEDED, sensorAssignments));
  }

  @ParameterizedTest
  @MethodSource({ "getNonGoodFlags" })
  public void childSensorTypeTest(Flag flag) throws Exception {
    assertEquals(flag, variable.getCascade(getSensorType("childSensor1"), flag,
      sensorAssignments));
  }
}
