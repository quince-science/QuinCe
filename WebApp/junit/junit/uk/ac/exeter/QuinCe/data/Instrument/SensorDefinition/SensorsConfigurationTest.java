package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the {@list SensorsConfiguration} class.
 *
 * <p>
 * These tests assume that the Underway Marine pCO₂ variable is configured in
 * the test database, and also a test variable required for some tests. The test
 * variable is defined in
 * {@code WebApp/resources/sql/testbase/variable/V1000002_test_variable.sql}
 * </p>
 *
 * <p>
 * Many of these tests use additional configurations in the database. The
 * relevant {@code .sql} files are in
 * {@code resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/}.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SensorsConfigurationTest extends BaseTest {

  /**
   * A reference list containing the ID of the Underway Marine pCO₂ variable.
   *
   * @see #initVarList()
   */
  private List<Long> var1List = null;

  /**
   * A reference list containing the ID of the temporary test variable
   *
   * @see #initTestVarList()
   */
  private List<Long> var2List = null;

  /**
   * A reference list containing the IDs of both the Underway Marine pCO₂ and
   * test variables.
   *
   * @see #initVarList()
   * @see #initTestVarList()
   */
  private List<Long> bothVarsList = null;

  /**
   * A reference list containing an invalid variable ID.
   *
   * @see #initVarList()
   */
  private List<Long> invalidVarList = null;

  /**
   * Build a standalone {@link SensorsConfiguration} from the test database.
   * This will include both the Underway Marine pCO₂ and test variables, plus
   * any additional variables and sensor types defined in Flyway migration files
   * specified for the current test.
   *
   * @return The {@link SensorsConfiguration}.
   * @throws Exception
   *           If the object cannot be created.
   */
  private SensorsConfiguration getConfig() throws Exception {
    return new SensorsConfiguration(getDataSource());
  }

  /**
   * Initialise the application's {@link ResourceManager}, which will in turn
   * initialise a {@link SensorsConfiguration} object. This will be returned
   * from the method.
   *
   * @return The {@link SenssorsConfiguration} object.
   * @throws Exception
   *           If the {@link ResourceManager} cannot be created.
   */
  private SensorsConfiguration getResourceManagerConfig() throws Exception {
    initResourceManager();
    return ResourceManager.getInstance().getSensorsConfiguration();
  }

  /**
   * Initialise {@link #var1List} with the ID of the Underway Marine pCO₂
   * variable, and the {@link #invalidVarList}.
   *
   * @throws Exception
   *           If the Underway Marine pCO₂ variable cannot be retrieved from the
   *           database.
   */
  private void initVarList() throws Exception {

    initResourceManager();

    var1List = new ArrayList<Long>(1);

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      conn = ResourceManager.getInstance().getDBDataSource().getConnection();
      stmt = conn.prepareStatement(
        "SELECT id FROM variables " + "WHERE name = 'Underway Marine pCO₂'");

      record = stmt.executeQuery();
      if (!record.next()) {
        throw new DatabaseException(
          "'Underway Marine pCO₂' variable does not exist");
      } else {
        var1List.add(record.getLong(1));
      }
    } catch (SQLException e) {
      throw e;
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    invalidVarList = new ArrayList<Long>(1);
    invalidVarList.add(-1000L);
  }

  /**
   * Initialise {@link #var2List} with the ID of the test variable. Also builds
   * {@link #bothVarsList}.
   *
   * @throws Exception
   *           If the test variable cannot be retrieved from the database.
   */
  private void initTestVarList() throws Exception {

    var2List = new ArrayList<Long>(1);
    bothVarsList = new ArrayList<Long>(2);

    bothVarsList.addAll(var1List);

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      conn = ResourceManager.getInstance().getDBDataSource().getConnection();
      stmt = conn.prepareStatement(
        "SELECT id FROM variables " + "WHERE name = 'testVar'");

      record = stmt.executeQuery();
      if (!record.next()) {
        throw new DatabaseException("'testVar' variable does not exist");
      } else {
        var2List.add(record.getLong(1));
        bothVarsList.add(record.getLong(1));
      }
    } catch (SQLException e) {
      throw e;
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Destroy the {@link ResourceManager} if it was created for the test.
   */
  @AfterEach
  public void destroyResourceManager() {
    ResourceManager.destroy();
  }

  /**
   * Ensure that the base configuration in the database loads without errors,
   * and the required special {@link SensorType}s are present.
   *
   * @throws Exception
   *           If the configuration cannot be loaded.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void loadConfigurationTest() throws Exception {
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
   * Tests that a {@link SensorType} defined in the database with a non-existent
   * parent causes a {@link SensorConfigurationException} to be thrown when the
   * configuration is loaded.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/nonExistentParent" })
  @Test
  public void nonExistentParentTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Tests that a {@link SensorType} defined in the database that depends on a
   * non-existent sensor type causes a {@link SensorConfigurationException} to
   * be thrown when the configuration is loaded.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/nonExistentDependsOn" })
  @Test
  public void nonExistentDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Test that the list of {@link SensorType}s is returned in the correct
   * display order.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/displayOrder" })
  @Test
  public void displayOrderTest() throws Exception {

    // Get the last 4 sensor types in the list - these should be our test types
    List<SensorType> types = getConfig().getSensorTypes();
    List<SensorType> testTypes = types.subList(types.size() - 4, types.size());

    for (int i = 0; i < testTypes.size(); i++) {
      assertEquals("DisplayOrder " + i, testTypes.get(i).getName());
    }
  }

  /**
   * Test that {@link SensorsConfiguration#validateSensorNames(List)} throws a
   * {@link SensorConfigurationException} if a non-existent {@link SensorType}
   * name is included in the input list.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
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
   * Test that {@link SensorsConfiguration#validateSensorNames(List)} does not
   * throw an exception when only valid {@link SensorType} names are included in
   * the input list.
   *
   * <p>
   * This test includes the special {@code Run Type} {@link SensorType}, which
   * is not defined in the database but is injected by the application.
   * </p>
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
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
   * Test that a {@link SensorType} with no parent in the database returns
   * {@code null} when {@link SensorType#getParent()} is called.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getParentWithoutParent" })
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
   * Test that a {@link SensorType} with a parent in the database returns that
   * parent when {@link SensorType#getParent()} is called.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getParentWithParent" })
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
    assertEquals("Test Parent", config.getParent(child).getName());
  }

  /**
   * Test that the {@link SensorsConfiguration#getSiblings(SensorType)} method
   * returns an empty list for a {@link SensorType} with no parent.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithNoParent" })
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
   * Test that configuring a parent {@link SensorType} with only one child
   * throws a {@link SensorConfigurationException}.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/parentWithOneChild" })
  @Test
  public void parentWithOneChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Test that configuring a {@link SensorType} that is both a parent and a
   * child throws a {@link SensorConfigurationException}.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/parentAndChild" })
  @Test
  public void parentAndChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Test that a child {@link SensorType}'s siblings can be correctly retrieved
   * from the configuration.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithParentAndSiblings" })
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

    List<SensorType> siblings = config.getSiblings(child);
    assertEquals(2, config.getSiblings(child).size());
    siblings.stream()
      .forEach(c -> assertTrue(c.getName().startsWith("Sibling")));
  }

  /**
   * Test that a parent {@link SensorType} is correctly identified.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void isParentForParentTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType equilibratorPressure = config
      .getSensorType("Equilibrator Pressure");
    assertTrue(config.isParent(equilibratorPressure));
  }

  /**
   * Test that a non-parent {@link SensorType} is correctly identified.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void isParentForNonParentTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType equilibratorPressure = config.getSensorType("Salinity");
    assertFalse(config.isParent(equilibratorPressure));
  }

  /**
   * Check that a child {@link SensorType} is correctly identified as not being
   * a parent.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void isParentForChildTest() throws Exception {
    SensorsConfiguration config = getConfig();
    SensorType equilibratorPressure = config
      .getSensorType("Equilibrator Pressure (absolute)");
    assertFalse(config.isParent(equilibratorPressure));
  }

  /**
   * Test that a {@link SensorType} can be retrieved using its ID.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
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
   * Test that retrieving a {@link SensorType} with an invalid ID throws a
   * {@link SensorTypeNotFoundException}.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getInvalidSensorTypeByIdTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      getConfig().getSensorType(-1000);
    });
  }

  /**
   * Test that a {@link SensorType} can be retrieved using its name.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
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
   * Test that retrieving a {@link SensorType} with an invalid name throws a
   * {@link SensorTypeNotFoundException}.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getInvalidSensorTypeByNameTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      getConfig().getSensorType("Flurble");
    });
  }

  /**
   * Test that the {@link SensorType}s defined for the test variable are
   * correctly retrieved.
   *
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void multipleVariableConfigurationTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getConfig();
    assertNotNull(config);

    // Make sure the correct sensors are in the set
    Set<SensorType> testSensorTypes = config.getSensorTypes(var2List, false);
    assertTrue(
      testSensorTypes.contains(config.getSensorType("Intake Temperature")));
    assertTrue(testSensorTypes.contains(config.getSensorType("Salinity")));
    assertTrue(testSensorTypes.contains(config.getSensorType("testSensor")));
    assertFalse(testSensorTypes
      .contains(config.getSensorType("Equilibrator Temperature")));
  }

  /**
   * Test that {@link SensorsConfiguration#getSensorTypes(List, boolean)} with
   * {@code replaceParentsWithChildren == true} returns parent types without
   * their children.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getSensorTypesWithParentTest() throws Exception {
    initVarList();
    SensorsConfiguration config = getConfig();

    Set<SensorType> sensorTypes = config.getSensorTypes(var1List, false);
    assertTrue(
      sensorTypes.contains(config.getSensorType("Equilibrator Pressure")));
    assertFalse(sensorTypes
      .contains(config.getSensorType("Equilibrator Pressure (absolute)")));
    assertFalse(sensorTypes
      .contains(config.getSensorType("Equilibrator Pressure (differential)")));
  }

  /**
   * Test that {@link SensorsConfiguration#getSensorTypes(List, boolean)} with
   * {@code replaceParentsWithChildren == false} returns child types without
   * their parents.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getSensorTypesWithChildrenTest() throws Exception {
    initVarList();
    SensorsConfiguration config = getConfig();

    Set<SensorType> sensorTypes = config.getSensorTypes(var1List, true);
    assertFalse(
      sensorTypes.contains(config.getSensorType("Equilibrator Pressure")));
    assertTrue(sensorTypes
      .contains(config.getSensorType("Equilibrator Pressure (absolute)")));
    assertTrue(sensorTypes
      .contains(config.getSensorType("Equilibrator Pressure (differential)")));
  }

  /**
   * Test that a single core {@link SensorType} for a variable is correctly
   * retrieved.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getCoreSensorTest() throws Exception {
    initVarList();
    SensorsConfiguration config = getConfig();
    List<SensorType> coreSensors = config.getCoreSensors(var1List);
    assertEquals(1, coreSensors.size());
    assertTrue(coreSensors.get(0).getName().equals("CO₂ in gas"));
  }

  /**
   * Test that multiple core {@link SensorType}s for a variable is correctly
   * retrieved.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void getMultipleCoreSensorsTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getConfig();
    List<SensorType> coreSensors = config.getCoreSensors(bothVarsList);

    assertEquals(2, coreSensors.size());
    assertTrue(coreSensors.contains(config.getSensorType("CO₂ in gas")));
    assertTrue(coreSensors.contains(config.getSensorType("testSensor")));
  }

  /**
   * Test that requesting the core {@link SensorType} for an invalid variable
   * throws a {@link SensorConfigurationException}.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getCoreSensorInvalidVariableTst() throws Exception {
    initVarList();
    SensorsConfiguration config = getConfig();

    assertThrows(SensorConfigurationException.class, () -> {
      config.getCoreSensors(invalidVarList);
    });
  }

  /**
   * Test that the non-core {@link SensorType}s for a variable are correctly
   * retrieved without the core {@link SensorType}s.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void getNonCoreSensorsTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getConfig();

    Connection conn = null;
    try {
      Set<SensorType> nonCoreSensors = config
        .getNonCoreSensors(getDataSource().getConnection());

      // Should not contain any of the core sensors
      assertFalse(nonCoreSensors.contains(config.getSensorType("CO₂ in gas")));
      assertFalse(nonCoreSensors.contains(config.getSensorType("testSensor")));

      // Should include the used sensors, whether used multiple times or just
      // once
      assertTrue(nonCoreSensors.contains(config.getSensorType("Salinity")));
      assertTrue(nonCoreSensors
        .contains(config.getSensorType("Equilibrator Temperature")));

      // Should include the Unused Sensor type even though it's not defined as
      // part of either variable
      assertTrue(
        nonCoreSensors.contains(config.getSensorType("Unused sensor")));

      // Should include children but not parents
      assertFalse(
        nonCoreSensors.contains(config.getSensorType("Equilibrator Pressure")));
      assertTrue(nonCoreSensors
        .contains(config.getSensorType("Equilibrator Pressure (absolute)")));
      assertTrue(nonCoreSensors.contains(
        config.getSensorType("Equilibrator Pressure (differential)")));
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Test that a core sensor is reported as such
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void isCoreSensorCoreTest() throws Exception {
    SensorsConfiguration config = getConfig();
    assertTrue(config.isCoreSensor(config.getSensorType("CO₂ in gas")));
  }

  /**
   * Test that a non-core sensor is reported as such
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void isCoreSensorNonCoreTest() throws Exception {
    SensorsConfiguration config = getConfig();
    assertFalse(config.isCoreSensor(config.getSensorType("Salinity")));
  }

  /**
   * Test that a non-core {@link SensorType} required for one of two variables
   * is correctly identified for different variables.
   *
   * <p>
   * Specifically, the {@link SensorType} must be:
   * </p>
   *
   * <ul>
   * <li>Required for a variable it is required for</li>
   * <li>Not required for a variable it is not required for</li>
   * <li>Required for a list containing both variables</li>
   * </ul>
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void requiredForVarNonCoreOneVarTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("Equilibrator Temperature");

    assertTrue(config.requiredForVariables(sensorType, var1List));
    assertFalse(config.requiredForVariables(sensorType, var2List));
    assertTrue(config.requiredForVariables(sensorType, bothVarsList));
  }

  /**
   * Test that a core {@link SensorType} required for one of two variables is
   * correctly identified for different variables.
   *
   * <p>
   * Specifically, the {@link SensorType} must be:
   * </p>
   *
   * <ul>
   * <li>Required for a variable it is required for</li>
   * <li>Not required for a variable it is not required for</li>
   * <li>Required for a list containing both variables</li>
   * </ul>
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void requiredForVarCoreOneVarTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("CO₂ in gas");

    assertTrue(config.requiredForVariables(sensorType, var1List));
    assertFalse(config.requiredForVariables(sensorType, var2List));
    assertTrue(config.requiredForVariables(sensorType, bothVarsList));
  }

  /**
   * Test that a {@link SensorType} required for two variables is correctly
   * identified for both.
   *
   * <p>
   * Specifically, the {@link SensorType} must be:
   * </p>
   *
   * <ul>
   * <li>Required for the first variable it is required for</li>
   * <li>Required for the second variable it is required for</li>
   * <li>Required for a list containing both variables</li>
   * </ul>
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void requiredForVarTwoVarsTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("Salinity");

    assertTrue(config.requiredForVariables(sensorType, var1List));
    assertTrue(config.requiredForVariables(sensorType, var2List));
    assertTrue(config.requiredForVariables(sensorType, bothVarsList));
  }

  /**
   * Test that a {@link SensorType} that is not required for any variables is
   * not identified as required for any variables.
   *
   * <p>
   * Specifically, the {@link SensorType} must be:
   * </p>
   *
   * <ul>
   * <li>Not required a single variable</li>
   * <li>Not required for for a list containing multiple variables</li>
   * </ul>
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void requiredForVarNoVarsTest() throws Exception {
    initVarList();
    initTestVarList();
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType sensorType = config.getSensorType("Atmospheric Pressure");

    assertFalse(config.requiredForVariables(sensorType, var1List));
    assertFalse(config.requiredForVariables(sensorType, var2List));
    assertFalse(config.requiredForVariables(sensorType, bothVarsList));
  }

  /**
   * Test that both parent and child {@link SensorType}s are identified as
   * required for a variable if the parent is defined as required for that
   * variable.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void requiredForVarParentsAndChildrenTest() throws Exception {
    initVarList();
    SensorsConfiguration config = getResourceManagerConfig();
    SensorType parentType = config.getSensorType("Equilibrator Pressure");
    SensorType childType = config
      .getSensorType("Equilibrator Pressure (absolute)");

    assertTrue(config.requiredForVariables(parentType, var1List));
    assertTrue(config.requiredForVariables(childType, var1List));
  }

  /**
   * Test that checking whether a {@link SensorType} is required for an invalid
   * variable throws a {@link SensorConfigurationException}.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void requiredForVarInvalidVarTest() throws Exception {
    initVarList();
    SensorsConfiguration config = getConfig();
    SensorType sensorType = config.getSensorType("Salinity");

    assertThrows(SensorConfigurationException.class, () -> {
      config.requiredForVariables(sensorType, invalidVarList);
    });
  }

  /**
   * Test that attempting to retrieve the {@link SensorType}s for an invalid
   * variable throws a {@link SensorConfigurationException}.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getSensorTypesInvalidVarTest() throws Exception {
    initVarList();
    SensorsConfiguration config = getConfig();
    assertThrows(SensorConfigurationException.class, () -> {
      config.getSensorTypes(invalidVarList, false);
    });
  }

  /**
   * Test that configuring a parent {@link SensorType} to depend on another
   * {@link SensorType} is invalid and throws a
   * {@link SensorConfigurationException}.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/dependentParent" })
  @Test
  public void dependentParentTest() throws Exception {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Test that configuring a child {@link SensorType} as a core type instead of
   * its parent throws a {@link SensorConfigurationException}.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/variableWithCoreChild" })
  @Test
  public void variableWithCoreChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Test that configuring a child {@link SensorType} as a non-core type instead
   * of its parent throws a {@link SensorConfigurationException}.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/variableWithNonCoreChild" })
  @Test
  public void variableWithNonCoreChildTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  /**
   * Test that an {@link InstrumentVariable} can be retrieved using its ID.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getInstrumentVariableTest() throws Exception {
    InstrumentVariable variable = getConfig().getInstrumentVariable(1L);
    assertEquals(1L, variable.getId());
  }

  /**
   * Test that attempting to retrieve a non-existent {@link InstrumentVariable}
   * throws a {@link VariableNotFoundException}.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest
  @Test
  public void getNonExistentInstrumentVariableTest() throws Exception {
    assertThrows(VariableNotFoundException.class, () -> {
      getConfig().getInstrumentVariable(-1000L);
    });
  }

  /**
   * Test that retrieving multiple {@link InstrumentVariable}s works.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/variable" })
  @Test
  public void getInstrumentVariablesTest() throws Exception {
    initVarList();
    initTestVarList();
    List<InstrumentVariable> variables = getConfig()
      .getInstrumentVariables(bothVarsList);
    for (InstrumentVariable variable : variables) {
      assertTrue(variable.getName().equals("Underway Marine pCO₂")
        || variable.getName().equals("testVar"));
    }
  }

  /**
   * Test that retrieving multiple {@link InstrumentVariable}s where one or more
   * variables doesn't exist throws a {@link VariableNotFoundException}.
   *
   * @throws Exception
   *           If the {@link SensorsConfiguration} cannot be accessed.
   *
   * @see #getConfig()
   */
  @Test
  public void getNonExistentInstrumentVariablesTest() throws Exception {
    initVarList();
    assertThrows(VariableNotFoundException.class, () -> {
      getConfig().getInstrumentVariables(invalidVarList);
    });
  }
}
