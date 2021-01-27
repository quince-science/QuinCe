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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
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
 * Equilibrator Pressure (differential) depends on Ambient Pressure. xCO₂ (with
 * standards) optionally depends on xH₂O (with standards).
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
 * <li>xCO₂ (with standards) <i>(core)</i></li>
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

  /**
   * The underway marine pCO2 variable
   */
  private Variable co2Var = null;

  // Created by assignmentsInit()
  /**
   * The application's {@link SensorAssignments} configuration.
   *
   * <p>
   * Loaded by {@link #assignmentsInit()}.
   * </p>
   */
  private SensorAssignments assignments = null;

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
   * @param sensorType
   *          The {@link SensorType} to assign.
   * @param file
   *          The file.
   * @param column
   *          The column index.
   * @param primary
   *          The primary/secondary status.
   * @return The {@link SensorAssignment} object.
   * @throws Exception
   *           If any internal errors are encountered.
   */
  protected static SensorAssignment makeAssignment(SensorType sensorType,
    String file, int column, boolean primary) throws Exception {

    return new SensorAssignment(file, column, sensorType, "Assignment", primary,
      false, "NaN");
  }

  protected static SensorAssignment makeAssignment(String file, int column,
    boolean primary) throws Exception {

    return makeAssignment(getTestSensorType(), file, column, primary);
  }

  private static SensorType getTestSensorType() throws Exception {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType("Intake Temperature");
  }

  /**
   * Get a {@link SensorType}'s database ID using its name.
   *
   * @param typeName
   *          The {@link SensorType}'s name
   * @return The {@link SensorType}'s ID
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private SensorType getSensorType(String typeName) throws Exception {

    SensorType result = null;

    for (SensorType type : config.getSensorTypes()) {
      if (type.getShortName().equals(typeName)) {
        result = type;
        break;
      }
    }

    if (null == result) {
      throw new SensorTypeNotFoundException(typeName);
    }

    return result;
  }

  /**
   * Read the variable and sensor configuration from the database.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @BeforeEach
  public void sensorConfigInit() throws Exception {
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
        co2Var = ResourceManager.getInstance().getSensorsConfiguration()
          .getInstrumentVariable(record.getLong(1));
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
   * Initialises a {@link SensorAssignments} object for the configured variables
   * ready to be populated by the tests.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @BeforeEach
  public void assignmentsInit() throws Exception {
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
  public void tearDown() {
    config = null;
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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void basicAssignmentTest() throws Exception {
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    Map<SensorType, List<SensorAssignment>> allAssignments = assignments;
    List<SensorAssignment> sensorAssignments = allAssignments
      .get(config.getSensorType(1));
    assertEquals(1, sensorAssignments.size());
    assertEquals(makeAssignment(DATA_FILE_NAME, 1, true),
      sensorAssignments.toArray()[0]);
  }

  /**
   * Test that attempting to assign a parent {@link SensorType} fails.
   */
  @Test
  public void assignParentTest() {
    // Parents cannot be assigned; only their children
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(makeAssignment(
        getSensorType("Equilibrator Pressure"), DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that attempting to assign the same column to the same
   * {@link SensorType} fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void duplicateColumnSameSensorTest() throws Exception {
    // The same column can't be assigned more than once
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that attempting to assign the same column to two different
   * {@link SensorType}s fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void duplicateColumnDifferentSensorTest() throws Exception {
    // The same column can't be assigned more than once
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  /**
   * Test that assigning the same column number from different files succeeds.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void duplicateColumnDifferentFileTest() throws Exception {
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    SensorAssignment assignment2 = new SensorAssignment(DATA_FILE_2_NAME, 1,
      getTestSensorType(), "Second file sensor", true, false, "NaN");
    assignments.addAssignment(assignment2);

    assertEquals(2, countAllAssignments());
  }

  /**
   * Test that an assignment can be removed from the system.
   *
   * <p>
   * Makes an assignment, and then removes it.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void removeAssignmentTest() throws Exception {

    SensorAssignment assignment = makeAssignment(DATA_FILE_NAME, 1, true);
    assignments.addAssignment(assignment);

    SensorAssignment removedAssignment = assignments
      .removeAssignment(getTestSensorType(), DATA_FILE_NAME, 1);

    assertEquals(0, countAllAssignments());
    assertEquals(assignment, removedAssignment);
  }

  /**
   * Test that all assignments for a single file can be removed.
   *
   * <p>
   * Adds assignments for two files, and removes the assignments for one of
   * them.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void removeFileAssignmentsTest() throws Exception {
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 2, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_2_NAME, 1, false));
    assignments.removeFileAssignments(DATA_FILE_NAME);
    assertEquals(1, countAllAssignments());
  }

  /**
   * Test that a large set of various assignments works correctly.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void addMultipleAssignments() throws Exception {
    // Add multiple assignments to sensor types
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 2, false));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 3, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 4, false));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 5, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 6, false));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 7, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 8, false));
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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void coreSensorAssignedPrimaryTest() throws Exception {
    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 1, true));
    assertTrue(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, true));
  }

  /**
   * Test that non-core sensor assignments are not detected as core assignments.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void coreSensorNotAssignedTest() throws Exception {
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 2, true));

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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void coreSensorSecondaryAssignedTest() throws Exception {
    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 1, false));
    assertTrue(assignments.coreSensorAssigned(DATA_FILE_NAME, false));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
  }

  /**
   * Test that a core {@link SensorType} cannot be assigned for a variable that
   * the instrument does not measure.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void assignCoreSensorForDisallowedVariableTest() throws Exception {
    // You're not allowed to assign a core sensor for a
    // variable that your instrument doesn't measure.

    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(
        makeAssignment(getSensorType("testSensor"), DATA_FILE_NAME, 1, true));
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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeNotRequiredNoInternalCalibTest() throws Exception {
    // Run type is not required if no sensor with internal calibration is
    // assigned
    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));

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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeRequiredOneInternalCalibTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("xH₂O (with standards)"), DATA_FILE_NAME, 1, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
    assertFalse(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  /**
   * Test the a Run Type {@link SensorType} is required for multiple files, each
   * of which is assigned a {@link SensorType} that has requires internal
   * calibrations.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeRequiredTwoInternalCalibTest() throws Exception {
    assignments.addAssignment(makeAssignment(
      getSensorType("xH₂O (with standards)"), DATA_FILE_NAME, 1, true));
    assignments.addAssignment(makeAssignment(
      getSensorType("xH₂O (with standards)"), DATA_FILE_2_NAME, 1, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
    assertTrue(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  /**
   * Test the a Run Type {@link SensorType} is required for a file where more
   * than one {@link SensorType} that has requires internal calibrations has
   * been assigned.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeRequiredBothInternalCalibTest() throws Exception {
    assignments.addAssignment(makeAssignment(
      getSensorType("xH₂O (with standards)"), DATA_FILE_NAME, 1, true));
    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 2, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
  }

  /**
   * Test that a variable with no sensors assigned registers as incomplete.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteNoAssignmentsTest() throws Exception {
    assertFalse(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a variable with one sensor type assigned is not complete.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteOneAssignmentTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));
    assertFalse(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a fully assigned variable with no dependents is complete.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteNoDependsTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Salinity"), DATA_FILE_NAME, 2, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Equilibrator Temperature"), DATA_FILE_NAME, 3, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (absolute)"),
        DATA_FILE_NAME, 4, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 5, true));

    assignments.addAssignment(
      makeAssignment(SensorType.RUN_TYPE_SENSOR_TYPE, DATA_FILE_NAME, 6, true));

    assertTrue(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a fully assigned variable with no dependents is complete.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteRunTypeNotSetTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Salinity"), DATA_FILE_NAME, 2, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Equilibrator Temperature"), DATA_FILE_NAME, 3, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (absolute)"),
        DATA_FILE_NAME, 4, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 5, true));

    assertFalse(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a fully assigned variable with an unassigned dependent is
   * incomplete.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteDependsNotSetTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Salinity"), DATA_FILE_NAME, 2, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Equilibrator Temperature"), DATA_FILE_NAME, 3, true));

    // Both Absolute and Differential are set, but Pressure At Instrument
    // (which differential depends on) is not set
    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (absolute)"),
        DATA_FILE_NAME, 4, true));
    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (differential)"),
        DATA_FILE_NAME, 5, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 6, true));

    assertFalse(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a fully assigned variable with an assigned dependent is complete.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteDependsSetTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Salinity"), DATA_FILE_NAME, 2, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Equilibrator Temperature"), DATA_FILE_NAME, 3, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (absolute)"),
        DATA_FILE_NAME, 4, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (differential)"),
        DATA_FILE_NAME, 5, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 6, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Pressure at instrument"), DATA_FILE_NAME, 7, true));

    assignments.addAssignment(
      makeAssignment(SensorType.RUN_TYPE_SENSOR_TYPE, DATA_FILE_NAME, 8, true));

    assertTrue(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a fully assigned variable with an answered depends question but
   * no dependent set is incomplete.
   *
   * <p>
   * {@link #variableCompleteNoDependsTest} already tests when the depends
   * question answer is no.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteDependsQuestionNotSetTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Salinity"), DATA_FILE_NAME, 2, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Equilibrator Temperature"), DATA_FILE_NAME, 3, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (differential)"),
        DATA_FILE_NAME, 4, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Pressure at instrument"), DATA_FILE_NAME, 6, true));

    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME, 5,
      getSensorType("xCO₂ (with standards)"), "Assignment", true, true, "NaN");
    assignments.addAssignment(co2Assignment);

    assertFalse(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test that a fully assigned variable with an answered depends question and
   * the dependent set is complete.
   *
   * <p>
   * {@link #variableCompleteNoDependsTest} already tests when the depends
   * question answer is no.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void variableCompleteDependsQuestionSetTest() throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("Intake Temperature"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Salinity"), DATA_FILE_NAME, 2, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Equilibrator Temperature"), DATA_FILE_NAME, 3, true));

    assignments.addAssignment(
      makeAssignment(getSensorType("Equilibrator Pressure (differential)"),
        DATA_FILE_NAME, 4, true));

    assignments.addAssignment(makeAssignment(
      getSensorType("Pressure at instrument"), DATA_FILE_NAME, 6, true));

    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME, 5,
      getSensorType("xCO₂ (with standards)"), "Assignment", true, true, "NaN");
    assignments.addAssignment(co2Assignment);

    assignments.addAssignment(makeAssignment(
      getSensorType("xH₂O (with standards)"), DATA_FILE_NAME, 7, true));

    assignments.addAssignment(
      makeAssignment(SensorType.RUN_TYPE_SENSOR_TYPE, DATA_FILE_NAME, 8, true));

    assertTrue(assignments.isVariableComplete(co2Var));
  }

  /**
   * Test the the 'Run Type' sensor type is not required if no
   * {@link SensorType} has been assigned.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeRequiredNoSensorTypesTest() throws Exception {
    assertFalse(
      assignments.isAssignmentRequired(SensorType.RUN_TYPE_SENSOR_TYPE));
  }

  /**
   * Test the the 'Run Type' sensor type is not required if no
   * {@link SensorType} with internal calibrations has been assigned.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeRequiredNoInternalCalibrationsAssignedTest()
    throws Exception {

    assignments.addAssignment(makeAssignment(DATA_FILE_NAME, 1, true));

    assertFalse(
      assignments.isAssignmentRequired(SensorType.RUN_TYPE_SENSOR_TYPE));
  }

  /**
   * Test the the 'Run Type' sensor type is not required if no
   * {@link SensorType} with internal calibrations has been assigned.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeRequiredInternalCalibrationsAssignedTest()
    throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 1, true));

    assertTrue(
      assignments.isAssignmentRequired(SensorType.RUN_TYPE_SENSOR_TYPE));
  }

  /**
   * Test the the 'Run Type' sensor type is not required if no
   * {@link SensorType} with internal calibrations has been assigned.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void runTypeAssignedInternalCalibrationsAssignedTest()
    throws Exception {

    assignments.addAssignment(makeAssignment(
      getSensorType("xCO₂ (with standards)"), DATA_FILE_NAME, 1, true));

    assignments.addAssignment(
      makeAssignment(SensorType.RUN_TYPE_SENSOR_TYPE, DATA_FILE_NAME, 2, true));

    assertFalse(
      assignments.isAssignmentRequired(SensorType.RUN_TYPE_SENSOR_TYPE));
  }
}
