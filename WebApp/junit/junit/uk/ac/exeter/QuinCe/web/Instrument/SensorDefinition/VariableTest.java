package junit.uk.ac.exeter.QuinCe.web.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class VariableTest extends BaseTest {

  private SensorType getSensorType(String name) throws Exception {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType(name);
  }

  @BeforeEach
  public void loadVariable() throws VariableNotFoundException {
    initResourceManager();
  }

  private Variable getVariable(long id) throws VariableNotFoundException {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getInstrumentVariable(id);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
  @Test
  public void getIdTest() throws VariableNotFoundException {
    assertEquals(1000000L, getVariable(1000000L).getId());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
  @Test
  public void getNameTest() throws VariableNotFoundException {
    assertEquals("testVar", getVariable(1000000L).getName());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
  @Test
  public void getCoreSensorTypeTest()
    throws SensorTypeNotFoundException, VariableNotFoundException {
    SensorType expectedCoreSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(1000000L);
    assertEquals(expectedCoreSensorType,
      getVariable(1000000L).getCoreSensorType());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void getRequiredSensorTypesTest(boolean includePosition)
    throws Exception {
    List<SensorType> sensorTypes = getVariable(1000000L)
      .getAllSensorTypes(includePosition);

    assertTrue(sensorTypes.contains(getSensorType("testSensor")));
    assertTrue(sensorTypes.contains(getSensorType("Intake Temperature")));
    assertTrue(sensorTypes.contains(getSensorType("Salinity")));
    assertFalse(sensorTypes.contains(getSensorType("Unused sensor")));

    assertEquals(includePosition,
      sensorTypes.contains(SensorType.LONGITUDE_SENSOR_TYPE));
    assertEquals(includePosition,
      sensorTypes.contains(SensorType.LATITUDE_SENSOR_TYPE));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
  @Test
  public void noInternalCalibrationsTest() throws VariableNotFoundException {
    assertFalse(getVariable(1000000L).hasInternalCalibrations());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/data/Instrument/SensorDefinition/VariableTest/hasInternalCalibrations" })
  @Test
  public void hasInternalCalibrationsTest() throws VariableNotFoundException {
    assertTrue(getVariable(2000000L).hasInternalCalibrations());
  }
}
