package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the {@link SensorAssignments} class.
 *
 * <p>
 * These tests assume that the sensor details for Marine Underway pCO₂ are in
 * the database. They are defined in the application's database migrations.
 * There is also the special Run Type {@link SensorType}, which is added in the
 * code and required if xH₂O or CO₂ are defined because they are registered as
 * being internally calibrated.
 * </p>
 *
 * <p>
 * Equilibrator Pressure (differential) depends on Ambient Pressure. CO₂ in gas
 * optionally depends on xH₂O in gas.
 * </p>
 *
 * <p>
 * Marine Underway pCO₂ requires the following sensors:
 * </p>
 *
 * <ul>
 * <li>Intake Temperature</li>
 * <li>Salinity</li>
 * <li>Equilibrator Temperature</li>
 * <li>Equilibrator Pressure</li>
 * <li>CO₂ in gas <i>(core)</i></li>
 * <li>Run Type</li>
 * </ul>
 *
 * <p>
 * There is also a second variable defined for these tests, which requires:
 * </p>
 * <ul>
 * <li>Intake Temperature</li>
 * <li>Salinity</li>
 * <li>testSensor <i>(core)</i>*</li>
 * </ul>
 *
 * <p>
 * * testSensor is a special temporary sensor that will only exist in the test
 * database. There is also an {@code Unused Sensor} defined for use in these
 * tests.
 * </p>
 *
 * <p>
 * The test variable and sensor types are defined in
 * {@code WebApp/resources/sql/testbase/variable}.
 * </p>
 *
 * <p>
 * Although these tests require an instrument to be registered in the database,
 * the sensor assignments will not be used from there, but built and tested
 * entirely within memory. Database tests will be done as part of
 * {@link InstrumentDB}. (<i>Broken link here - must fix when tests are
 * written</i>). The test instrument is defined in
 * {@code WebApp/resources/sql/testbase/instrument}.
 * </p>
 *
 * @author Steve Jones
 *
 */
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
  "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
public class SensorAssignmentsTest extends BaseTest {

  /**
   * An invalid sensor ID.
   */
  private static final long INVALID_SENSOR_ID = -1000L;

  /**
   * The filename for the first test file.
   */
  protected static final String DATA_FILE_NAME = "Data File";

  /**
   * The filename for the second test file.
   */
  private static final String DATA_FILE_2_NAME = "Second File";

  // Created by sensorConfigInit()
  /**
   * The application's sensor configuration.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private SensorsConfiguration config = null;

  /**
   * A list containing the IDs of the variables in the database.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private List<Long> varIds = null;

  // Created by assignmentsInit()
  /**
   * The application's {@link SensorAssignments} configuration.
   *
   * <p>
   * Loaded by {@link #assignmentsInit()}.
   * </p>
   */
  private SensorAssignments assignments = null;

  // Sensor type IDs. Populated by classInit()
  /**
   * The ID of the Intake Temperature {@link SensorType}.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private long intakeTemperatureId = -1L;

  /**
   * The ID of the Salinity {@link SensorType}.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private long salinityId = -1L;

  /**
   * The ID of the parent Equilibrator Pressure {@link SensorType}.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private long equilibratorPressureParentId = -1L;

  /**
   * The ID of the xH₂O {@link SensorType}.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private long xh2oId = -1L;

  /**
   * The ID of the CO₂ {@link SensorType}.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private long co2Id = -1L;

  /**
   * Get the total number of assignments made in the test.
   *
   * @return The assignment count
   */
  private int countAllAssignments() {
    int count = 0;

    for (List<SensorAssignment> assignmentSet : assignments.values()) {
      count += assignmentSet.size();
    }

    return count;
  }

  /**
   * Make a {@link SensorAssignment} for a given file, column and primary
   * status.
   *
   * @param file
   *          The file
   * @param column
   *          The column index
   * @param primary
   *          The primary/secondary status
   * @return The {@link SensorAssignment} object
   */
  protected static SensorAssignment makeAssignment(String file, int column,
    boolean primary) {
    return new SensorAssignment(file, column, "Assignment", primary, false,
      "NaN");
  }

  /**
   * Get a {@link SensorType}'s database ID using its name.
   *
   * @param typeName
   *          The {@link SensorType}'s name
   * @return The {@link SensorType}'s ID
   * @throws SensorTypeNotFoundException
   *           If the name is not found
   */
  private long getSensorTypeId(String typeName)
    throws SensorTypeNotFoundException {

    long result = -1;

    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals(typeName)) {
        result = type.getId();
      }
    }

    if (result == -1) {
      throw new SensorTypeNotFoundException(typeName);
    }

    return result;
  }

  /**
   * Read the variable and sensor configuration from the database.
   *
   * @throws DatabaseException
   *           If the data retrieval methods fail
   * @throws SensorTypeNotFoundException
   *           If any of the expected {@link SensorType}s are not in the
   *           database
   * @throws SQLException
   *           If a database error occurs
   */
  @BeforeEach
  public void sensorConfigInit()
    throws DatabaseException, SensorTypeNotFoundException, SQLException {
    initResourceManager();
    config = ResourceManager.getInstance().getSensorsConfiguration();
    varIds = new ArrayList<Long>(1);

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
        varIds.add(record.getLong(1));
      }
    } catch (SQLException e) {
      throw e;
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    intakeTemperatureId = getSensorTypeId("Intake Temperature");
    salinityId = getSensorTypeId("Salinity");
    equilibratorPressureParentId = getSensorTypeId("Equilibrator Pressure");
    xh2oId = getSensorTypeId("xH₂O in gas");
    co2Id = getSensorTypeId("CO₂ in gas");
  }

  /**
   * Initialises a {@link SensorAssignments} object for the configured variables
   * ready to be populated by the tests.
   *
   * @throws SQLException
   *           If a database error occurs
   * @throws DatabaseException
   *           If any of the data retrieval methods fail
   * @throws SensorTypeNotFoundException
   *           If any expected {@link SensorType}s are not in the database
   * @throws SensorConfigurationException
   *           If the variable and sensor configuration is internally
   *           inconsistent in the database
   */
  @BeforeEach
  public void assignmentsInit() throws SensorConfigurationException,
    SensorTypeNotFoundException, DatabaseException, SQLException {
    assignments = new SensorAssignments(getDataSource().getConnection(),
      varIds);
  }

  /**
   * Destroy the currently loaded {@link SensorAssignments}.
   *
   * <p>
   * Also destroys the {@link ResourceManager}. This is usually done as an
   * {@link AfterAll} method with {@link BaseTest#globalTeardown()}, but a fresh
   * {@link ResourceManager} that loads the variable and sensor configurations
   * is required here since they can change between tests.
   * </p>
   */
  @AfterEach
  public void destroySensorAssignments() {
    assignments = null;
    ResourceManager.destroy();
  }

  /**
   * Test that a {@link SensorAssignments} object can be successfully
   * constructed based on the application configuration and database.
   */
  @Test
  public void getNewSensorAssignmentsTest() {
    // Just check that we can get a new assignments object from the system
    assertNotNull(assignments);
  }

  /**
   * Test that a basic sensor can be assigned using the {@link SensorType} ID.
   *
   * <p>
   * Adds an assignment for an Intake Temperature sensor.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the {@link SensorType} is not found in the database
   * @throws SensorAssignmentException
   *           If the assignment action fails
   */
  @Test
  public void basicAssignmentTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    Map<SensorType, List<SensorAssignment>> allAssignments = assignments;
    List<SensorAssignment> sensorAssignments = allAssignments
      .get(config.getSensorType(1));
    assertEquals(1, sensorAssignments.size());
    assertEquals(makeAssignment(DATA_FILE_NAME, 1, true),
      sensorAssignments.toArray()[0]);
  }

  /**
   * Test that attempting to assign a {@link SensorType} with an ID that does
   * not exist fails.
   */
  @Test
  public void assignToInvalidSensorTypeTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      assignments.addAssignment(INVALID_SENSOR_ID,
        makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that a basic sensor can be assigned using the {@link SensorType} name.
   *
   * <p>
   * Adds an assignment for an Intake Temperature sensor.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the {@link SensorType} is not found in the database
   * @throws SensorAssignmentException
   *           If the assignment action fails
   */
  @Test
  public void assignByNameTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment("Intake Temperature",
      makeAssignment(DATA_FILE_NAME, 1, true));
    Map<SensorType, List<SensorAssignment>> allAssignments = assignments;
    List<SensorAssignment> sensorAssignments = allAssignments
      .get(config.getSensorType(1));
    assertEquals(1, sensorAssignments.size());
    assertEquals(makeAssignment(DATA_FILE_NAME, 1, true),
      sensorAssignments.toArray()[0]);
  }

  /**
   * Test that attempting to assign a {@link SensorType} with a name that does
   * not exist fails.
   */
  @Test
  public void assignByNonExistentNameTest() throws Exception {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      assignments.addAssignment("Flurble",
        makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that attempting to assign a parent {@link SensorType} fails.
   */
  @Test
  public void assignParentTest() {

    // Parents cannot be assigned; only their children
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(equilibratorPressureParentId,
        makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that attempting to assign the same column to the same
   * {@link SensorType} fails.
   *
   * @throws SensorTypeNotFoundException
   *           If the {@link SensorType} is not found in the database
   * @throws SensorAssignmentException
   *           If the initial assignment fails for any reason
   */
  @Test
  public void duplicateColumnSameSensorTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    // The same column can't be assigned more than once
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(intakeTemperatureId,
        makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that attempting to assign the same column to two different
   * {@link SensorType}s fails.
   *
   * @throws SensorTypeNotFoundException
   *           If the {@link SensorType} is not found in the database
   * @throws SensorAssignmentException
   *           If the initial assignment fails for any reason
   */
  @Test
  public void duplicateColumnDifferentSensorTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    // The same column can't be assigned more than once
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(salinityId,
        makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that assigning the same column number from different files succeeds.
   *
   * @throws SensorTypeNotFoundException
   *           If a {@link SensorType} is not found in the database
   * @throws SensorAssignmentException
   *           If the assignments fail for any reason
   */
  @Test
  public void duplicateColumnDifferentFileTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    SensorAssignment assignment2 = new SensorAssignment(DATA_FILE_2_NAME, 1,
      "Second file sensor", true, false, "NaN");
    assignments.addAssignment(salinityId, assignment2);

    assertEquals(2, countAllAssignments());
  }

  /**
   * Test that an assignment can be removed from the system.
   *
   * <p>
   * Makes an assignment, and then removes it.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the assignment fails because the {@link SensorType} is not
   *           found in the database
   * @throws SensorAssignmentException
   *           If the assignment fails for any reason
   */
  @Test
  public void removeAssignmentTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.removeAssignment(DATA_FILE_NAME, 1);
    assertEquals(0, countAllAssignments());
  }

  /**
   * Test that all assignments for a single file can be removed.
   *
   * <p>
   * Adds assignments for two files, and removes the assignments for one of
   * them.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the assignment fails because the {@link SensorType} is not
   *           found in the database
   * @throws SensorAssignmentException
   *           If the assignment fails for any reason
   */
  @Test
  public void removeFileAssignmentsTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(salinityId,
      makeAssignment(DATA_FILE_NAME, 2, true));
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_2_NAME, 1, false));
    assignments.removeFileAssignments(DATA_FILE_NAME);
    assertEquals(1, countAllAssignments());
  }

  /**
   * Test that a large set of various assignments works correctly.
   *
   * @throws SensorTypeNotFoundException
   *           If any of the assigned {@link SensorType}s are not found
   * @throws SensorAssignmentException
   *           If any of the assignments fail
   */
  @Test
  public void addMultipleAssignments()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    // Add multiple assignments to sensor types
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 2, false));
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 3, true));
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 4, false));
    assignments.addAssignment(salinityId,
      makeAssignment(DATA_FILE_NAME, 5, true));
    assignments.addAssignment(salinityId,
      makeAssignment(DATA_FILE_NAME, 6, false));
    assignments.addAssignment(salinityId,
      makeAssignment(DATA_FILE_NAME, 7, true));
    assignments.addAssignment(salinityId,
      makeAssignment(DATA_FILE_NAME, 8, false));
    assertEquals(8, countAllAssignments());

  }

  /**
   * Test that a core {@link SensorType} assigned with {@code primary} status is
   * subsequently detected correctly.
   *
   * <p>
   * The assignment must result in
   * {@link SensorAssignments#coreSensorAssigned(String, boolean)} returning
   * {@code true} for the file in which it was assigned, and {@code false} for
   * any other file.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the core {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If the assignment fails
   * @throws SensorConfigurationException
   *           If the configuration is invalid
   */
  @Test
  public void coreSensorAssignedPrimaryTest()
    throws SensorConfigurationException, SensorTypeNotFoundException,
    SensorAssignmentException {
    assignments.addAssignment(co2Id, makeAssignment(DATA_FILE_NAME, 1, true));
    assertTrue(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, true));
  }

  /**
   * Test that non-core sensor assignments are not detected as core assignments.
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   * @throws SensorConfigurationException
   *           If the configuration is invalid
   *
   * @throws Exception
   */
  @Test
  public void coreSensorNotAssignedTest() throws SensorTypeNotFoundException,
    SensorAssignmentException, SensorConfigurationException {
    assignments.addAssignment(salinityId,
      makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 2, true));

    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, false));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, false));
  }

  /**
   * Test that a core {@link SensorType} assigned with {@code secondary} status
   * is subsequently detected correctly.
   *
   * <p>
   * The assignment must result in
   * {@link SensorAssignments#coreSensorAssigned(String, boolean)} returning
   * {@code true} if the {@code primary} flag is set to {@code false}, and vice
   * versa.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the core {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If the assignment fails
   * @throws SensorConfigurationException
   *           If the configuration is invalid
   */
  @Test
  public void coreSensorSecondaryAssignedTest() throws Exception {
    assignments.addAssignment(co2Id, makeAssignment(DATA_FILE_NAME, 1, false));
    assertTrue(assignments.coreSensorAssigned(DATA_FILE_NAME, false));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
  }

  /**
   * Test that a core {@link SensorType} cannot be assigned for a variable that
   * the instrument does not measure.
   *
   * @throws SensorTypeNotFoundException
   *           If the core {@link SensorType} is not found
   */
  @Test
  public void assignCoreSensorForDisallowedVariableTest()
    throws SensorTypeNotFoundException {
    // You're not allowed to assign a core sensor for a
    // variable that your instrument doesn't measure.
    long testSensorId = getSensorTypeId("testSensor");
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(testSensorId,
        makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that a Run Type assignment is not required for a file that has no
   * {@link SensorType}s with internal calibrations required.
   *
   * <p>
   * Tests both a file with a {@link SensorType} that does not require internal
   * calibrations, and a file with no assignments at all.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  @Test
  public void runTypeNotRequiredNoInternalCalibTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    // Run type is not required if no sensor with internal calibration is
    // assigned
    assignments.addAssignment(intakeTemperatureId,
      makeAssignment(DATA_FILE_NAME, 1, true));

    assertFalse(assignments.runTypeRequired(DATA_FILE_NAME));
    assertFalse(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  /**
   * Tests that a Run Type {@link SensorType} is required only for a file with a
   * {@link SensorType} assigned that requires internal calibrations.
   *
   * <p>
   * Assigns a {@link SensorType} to a file that requires internal calibrations,
   * and not to a second file. {@link SensorAssignments#runTypeRequired(String)}
   * must return {@code true} for the first file and {@code false} for the
   * second.
   * </p>
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  @Test
  public void runTypeRequiredOneInternalCalibTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
    assertFalse(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  /**
   * Test the a Run Type {@link SensorType} is required for multiple files, each
   * of which is assigned a {@link SensorType} that has requires internal
   * calibrations.
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  @Test
  public void runTypeRequiredTwoInternalCalibTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(xh2oId,
      makeAssignment(DATA_FILE_2_NAME, 1, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
    assertTrue(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  /**
   * Test the a Run Type {@link SensorType} is required for a file where more
   * than one {@link SensorType} that has requires internal calibrations has
   * been assigned.
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  @Test
  public void runTypeRequiredBothInternalCalibTest()
    throws SensorTypeNotFoundException, SensorAssignmentException {
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(co2Id, makeAssignment(DATA_FILE_NAME, 2, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
  }
}
