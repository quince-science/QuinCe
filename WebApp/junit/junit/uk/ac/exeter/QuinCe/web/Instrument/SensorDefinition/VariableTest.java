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

@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
  "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
public class VariableTest extends BaseTest {

  private Variable var;

  private SensorType getSensorType(String name) throws Exception {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType(name);
  }

  @BeforeEach
  public void loadVariable() throws VariableNotFoundException {
    initResourceManager();
    var = ResourceManager.getInstance().getSensorsConfiguration()
      .getInstrumentVariable(1000000L);
  }

  @Test
  public void getIdTest() {
    assertEquals(1000000L, var.getId());
  }

  @Test
  public void getNameTest() {
    assertEquals("testVar", var.getName());
  }

  @Test
  public void getCoreSensorTypeTest() throws SensorTypeNotFoundException {
    SensorType expectedCoreSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(1000000L);
    assertEquals(expectedCoreSensorType, var.getCoreSensorType());
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void getRequiredSensorTypesTest(boolean includePosition)
    throws Exception {
    List<SensorType> sensorTypes = var.getAllSensorTypes(includePosition);

    assertTrue(sensorTypes.contains(getSensorType("testSensor")));
    assertTrue(sensorTypes.contains(getSensorType("Intake Temperature")));
    assertTrue(sensorTypes.contains(getSensorType("Salinity")));
    assertFalse(sensorTypes.contains(getSensorType("Unused sensor")));

    assertEquals(includePosition,
      sensorTypes.contains(SensorType.LONGITUDE_SENSOR_TYPE));
    assertEquals(includePosition,
      sensorTypes.contains(SensorType.LATITUDE_SENSOR_TYPE));
  }
}
