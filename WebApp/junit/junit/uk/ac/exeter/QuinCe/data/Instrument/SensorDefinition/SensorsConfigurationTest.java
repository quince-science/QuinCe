package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests in this file assume that there are some valid sensor types already
 * configured in the database, but create their own types for specific testing
 *
 * @author Steve Jones
 *
 */
public class SensorsConfigurationTest extends DBTest {

  private static List<Long> var1List = null;
  private static List<Long> var2List = null;
  private static List<Long> bothVarsList = null;
  private static List<Long> invalidVarList = null;

  /**
   * Build a SensorsConfiguration
   * @return
   * @throws Exception
   */
  private SensorsConfiguration getConfig() throws Exception {
    return new SensorsConfiguration(getDataSource());
  }

  /**
   * Build a SensorsConfiguration in the application-wide ResourceManager
   * @return
   * @throws Exception
   */
  private SensorsConfiguration getResourceManagerConfig() throws Exception {
    initResourceManager();
    return ResourceManager.getInstance().getSensorsConfiguration();
  }

  @BeforeAll
  public static void initClass() {
    var1List = new ArrayList<Long>(1);
    var1List.add(1L);

    var2List = new ArrayList<Long>(1);
    var2List.add(2L);

    bothVarsList = new ArrayList<Long>(2);
    bothVarsList.add(1L);
    bothVarsList.add(2L);

    invalidVarList = new ArrayList<Long>(1);
    invalidVarList.add(-1000L);
  }

  @AfterEach
  public void destroyResourceManager() {
    ResourceManager.destroy();
  }

  /**
   * Ensure that the base configuration in the database loads without errors
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void loadConfigurationTest() throws Exception  {
    SensorsConfiguration config = getConfig();
    assertNotNull(config);

    boolean runTypeFound = false;
    for (SensorType type : config.getSensorTypes()) {
      if (type.getId() == SensorType.RUN_TYPE_ID) {
        if (type.getName().equals("Run Type")) {
          runTypeFound = true;
          break;
        }
      }
    }

    assertTrue(runTypeFound, "Run Type not added to sensors configuration");
  }

  /**
   * Specify a SensorType with a parent that doesn't exist
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/nonExistentParent"
  })
  @Test
  public void nonExistentParentTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Specify Depends On for a SensorType that doesn't exist
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/nonExistentDependsOn"
  })
  @Test
  public void nonExistentDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Ensure that returned SensorTypes are in alphabetical order
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void sensorTypesListTest() throws Exception {
    List<SensorType> types = getConfig().getSensorTypes();

    // Extract the names in list order
    List<String> names = new ArrayList<String>(types.size());
    for (SensorType type : types) {
      names.add(type.getName());
    }

    // Sort the names into alphabetical order
    List<String> sortedNames = new ArrayList<String>(names);
    Collections.sort(sortedNames);

    // Check that the lists are equal
    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), sortedNames.get(i));
    }
  }

  /**
   * Handle unknown SensorType names
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void missingSensorNameTest() throws Exception {
    List<String> names = new ArrayList<String>(2);
    names.add("Intake Temperature");
    names.add("Flurble");

    assertThrows(SensorConfigurationException.class, () -> {
      getConfig().validateSensorNames(names);
    });
  }

  /**
   * Handle correct sensor names, including the auto-added Run Type
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void validSensorNameTest() throws Exception {
    List<String> names = new ArrayList<String>(2);
    names.add("Intake Temperature");
    names.add("Salinity");
    names.add("Run Type"); // Include the special Run Type for giggles
    getConfig().validateSensorNames(names);
  }

  /**
   * Get the parent of a SensorType that has no parent
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getParentWithoutParent"
  })
  @Test
  public void getParentWithoutParent() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType orphan = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Orphan")) {
        orphan = type;
        break;
      }
    }

    assertNull(config.getParent(orphan));
  }

  /**
   * Successfully get a SensorType's parent that exists
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getParentWithParent"
  })
  @Test
  public void getParentWithParent() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType child = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Child")) {
        child = type;
        break;
      }
    }

    assertNotNull(config.getParent(child));
  }

  /**
   * Try to get siblings for a SensorType with no parent
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithNoParent"
  })
  @Test
  public void getSiblingsWithNoParent() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType orphan = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Orphan")) {
        orphan = type;
        break;
      }
    }

    assertEquals(0, config.getSiblings(orphan).size());
  }

  /**
   * Try to configure a parent with only one sibling
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/parentWithOneChild"
  })
  @Test
  public void parentWithOneChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Try to create a SensorType that is both a parent and a child
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/parentAndChild"
  })
  @Test
  public void parentAndChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Get siblings of a given SensorType
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithParentAndSiblings"
  })
  @Test
  public void getSiblingsWithParentAndSiblings() throws Exception {
   SensorsConfiguration config = getConfig();
   SensorType child = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Child")) {
        child = type;
        break;
      }
    }

    assertEquals(2, config.getSiblings(child).size());
  }

  /**
   * Check that a parent SensorType is correctly identified
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void isParentForParentTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType equilibratorPressure = config.getSensorType("Equilibrator Pressure");
    assertTrue(config.isParent(equilibratorPressure));
  }

  /**
   * Check that a non-parent SensorType is correctly identified
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void isParentForNonParentTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType equilibratorPressure = config.getSensorType("Salinity");
    assertFalse(config.isParent(equilibratorPressure));
  }

  /**
   * Check that a non-parent SensorType is correctly identified
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void isParentForChildTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType equilibratorPressure = config.getSensorType("Equilibrator Pressure (absolute)");
    assertFalse(config.isParent(equilibratorPressure));
  }

  /**
   * Get a SensorType by its ID
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void getSensorTypeByIdTest() throws Exception {
    try {
      getConfig().getSensorType(1L);
    } catch (SensorTypeNotFoundException e) {
      // This exception should not be thrown
      fail("SensorTypeNotFoundException thrown when it shouldn't have been");
    }
  }

  /**
   * Get a SensorType with in invalid ID
   */
  @FlywayTest
  @Test
  public void getInvalidSensorTypeByIdTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      getConfig().getSensorType(-1000);
    });
  }

  /**
   * Get a SensorType by its ID
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void getSensorTypeByNameTest() throws Exception {
    try {
      getConfig().getSensorType("Salinity");
    } catch (SensorTypeNotFoundException e) {
      // This exception should not be thrown
      fail("SensorTypeNotFoundException thrown when it shouldn't have been");
    }
  }

  /**
   * Get a SensorType with in invalid ID
   */
  @FlywayTest
  @Test
  public void getInvalidSensorTypeByNameTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      getConfig().getSensorType("Flurble");
    });
  }

  /**
   * Check that the SensorTypes for the test variable are correct
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void multipleVariableConfigurationTest() throws Exception {
    SensorsConfiguration config = getConfig();
    assertNotNull(config);

    // Make sure the correct sensors are in the set
    Set<SensorType> testSensorTypes = config.getSensorTypes(var2List, false);
    assertTrue(testSensorTypes.contains(config.getSensorType("Intake Temperature")));
    assertTrue(testSensorTypes.contains(config.getSensorType("Salinity")));
    assertTrue(testSensorTypes.contains(config.getSensorType("testSensor")));
    assertFalse(testSensorTypes.contains(config.getSensorType("Equilibrator Temperature")));
  }

  /**
   * Get the SensorTypes for a variable with the parent types, no children
   */
  @FlywayTest
  @Test
  public void getSensorTypesWithParentTest() throws Exception {
    SensorsConfiguration config = getConfig();

    Set<SensorType> sensorTypes = config.getSensorTypes(var1List, false);
    assertTrue(sensorTypes.contains(config.getSensorType("Equilibrator Pressure")));
    assertFalse(sensorTypes.contains(config.getSensorType("Equilibrator Pressure (absolute)")));
    assertFalse(sensorTypes.contains(config.getSensorType("Equilibrator Pressure (differential)")));
  }

  /**
   * Get the SensorTypes for a variable with the child types, no parents
   */
  @FlywayTest
  @Test
  public void getSensorTypesWithChildrenTest() throws Exception {
    SensorsConfiguration config = getConfig();

    Set<SensorType> sensorTypes = config.getSensorTypes(var1List, true);
    assertFalse(sensorTypes.contains(config.getSensorType("Equilibrator Pressure")));
    assertTrue(sensorTypes.contains(config.getSensorType("Equilibrator Pressure (absolute)")));
    assertTrue(sensorTypes.contains(config.getSensorType("Equilibrator Pressure (differential)")));
  }

  /**
   * Get the Core SensorType
   */
  @FlywayTest
  @Test
  public void getCoreSensorTest() throws Exception {
    SensorsConfiguration config = getConfig();
    List<SensorType> coreSensors = config.getCoreSensors(var1List);
    assertEquals(1, coreSensors.size());
    assertTrue(coreSensors.get(0).getName().equals("CO₂ in gas"));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void getMultipleCoreSensorsTest() throws Exception {
    SensorsConfiguration config = getConfig();
    List<SensorType> coreSensors = config.getCoreSensors(bothVarsList);

    assertEquals(2, coreSensors.size());
    assertTrue(coreSensors.contains(config.getSensorType("CO₂ in gas")));
    assertTrue(coreSensors.contains(config.getSensorType("testSensor")));
  }

  /**
   * Try to get the core sensor for an invalid variable
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void getCoreSensorInvalidVariableTst() throws Exception {
    SensorsConfiguration config = getConfig();

    assertThrows(SensorConfigurationException.class, () -> {
      config.getCoreSensors(invalidVarList);
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void getNonCoreSensorsTest() throws Exception {
    SensorsConfiguration config = getConfig();

    Connection conn = null;
    try {
      Set<SensorType> nonCoreSensors =
        config.getNonCoreSensors(getDataSource().getConnection());

      // Should not contain any of the core sensors
      assertFalse(nonCoreSensors.contains(config.getSensorType("CO₂ in gas")));
      assertFalse(nonCoreSensors.contains(config.getSensorType("testSensor")));

      // Should include the used sensors, whether used multiple times or just once
      assertTrue(nonCoreSensors.contains(config.getSensorType("Salinity")));
      assertTrue(nonCoreSensors.contains(config.getSensorType("Equilibrator Temperature")));

      // Should include the Unused Sensor type even though it's not defined as
      // part of either variable
      assertTrue(nonCoreSensors.contains(config.getSensorType("Unused sensor")));

      // Should include children but not parents
      assertFalse(nonCoreSensors.contains(config.getSensorType("Equilibrator Pressure")));
      assertTrue(nonCoreSensors.contains(config.getSensorType("Equilibrator Pressure (absolute)")));
      assertTrue(nonCoreSensors.contains(config.getSensorType("Equilibrator Pressure (differential)")));
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Test that a core sensor is reported as such
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void isCoreSensorCoreTest() throws Exception {
    SensorsConfiguration config = getConfig();
    assertTrue(config.isCoreSensor(config.getSensorType("CO₂ in gas")));
  }

  /**
   * Test that a non-core sensor is reported as such
   * @throws Exception
   */
  @FlywayTest
  @Test
  public void isCoreSensorNonCoreTest() throws Exception {
    SensorsConfiguration config = getConfig();
    assertFalse(config.isCoreSensor(config.getSensorType("Salinity")));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void requiredForVarNonCoreOneVarTest() throws Exception {
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("Equilibrator Temperature");

    assertTrue(config.requiredForVariables(sensorType, var1List));
    assertFalse(config.requiredForVariables(sensorType, var2List));
    assertTrue(config.requiredForVariables(sensorType, bothVarsList));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void requiredForVarCoreOneVarTest() throws Exception {
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("CO₂ in gas");

    assertTrue(config.requiredForVariables(sensorType, var1List));
    assertFalse(config.requiredForVariables(sensorType, var2List));
    assertTrue(config.requiredForVariables(sensorType, bothVarsList));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void requiredForVarTwoVarsTest() throws Exception {
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("Salinity");

    assertTrue(config.requiredForVariables(sensorType, var1List));
    assertTrue(config.requiredForVariables(sensorType, var2List));
    assertTrue(config.requiredForVariables(sensorType, bothVarsList));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/variable"
  })
  @Test
  public void requiredForVarNoVarsTest() throws Exception {
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("Atmospheric Pressure");

    assertFalse(config.requiredForVariables(sensorType, var1List));
    assertFalse(config.requiredForVariables(sensorType, var2List));
    assertFalse(config.requiredForVariables(sensorType, bothVarsList));
  }

  @FlywayTest
  @Test
  public void requiredForVarParentsAndChildrenTest() throws Exception {
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType parentType = config.getSensorType("Equilibrator Pressure");
    SensorType childType = config.getSensorType("Equilibrator Pressure (absolute)");

    assertTrue(config.requiredForVariables(parentType, var1List));
    assertTrue(config.requiredForVariables(childType, var1List));
  }

  @FlywayTest
  @Test
  public void requiredForVarInvalidVarTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType sensorType = config.getSensorType("Salinity");

    assertThrows(SensorConfigurationException.class, () -> {
      config.requiredForVariables(sensorType, invalidVarList);
    });
  }

  @FlywayTest
  @Test
  public void getSensorTypesInvalidVarTest() throws Exception {
    SensorsConfiguration config = getConfig();
    assertThrows(SensorConfigurationException.class, () -> {
      config.getSensorTypes(invalidVarList, false);
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/dependentParent"
  })
  @Test
  public void dependentParentTest() throws Exception {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/variableWithCoreChild"
  })
  @Test
  public void variableWithCoreChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/variableWithNonCoreChild"
  })
  @Test
  public void variableWithNonCoreChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }
}
