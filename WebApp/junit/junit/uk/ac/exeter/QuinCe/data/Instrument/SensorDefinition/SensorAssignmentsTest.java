package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import junit.uk.ac.exeter.QuinCe.TestBase.TestLineException;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the SensorAssignments class.
 *
 * Note that these tests assume that the sensor details for Marine Underway
 * pCO₂ are in the database. These consist of the following sensors:
 *
 * <ul>
 *   <li>Intake Temperature <i>(ID 1)</i></li>
 *   <li>salinityId <i>(2)</i></li>
 *   <li>Equilibrator Temperature <i>(3)</i></li>
 *   <li>Equilibrator Pressure <i>(4)</i> <i>parent of</i>
 *     <ul>
 *       <li>Equilibrator Pressure (absolute) <i>(5)</i></li>
 *       <li>Equilibrator Pressure (differential) <i>(6)</i></li>
 *     </ul>
 *   </li>
 *   <li>Ambient Pressure <i>(7)</i></li>
 *   <li>xH₂O in gas <i>(8)</i></li>
 *   <li>CO₂ in gas <i>(9)</i></li>
 *   <li>Atmospheric Pressure <i>(10)</i></li>
 *   <li>Diagnostic Temperature <i>(11)</i></li>
 *   <li>Diagnostic Pressure <i>(12)</i></li>
 *   <li>Diagnostic Gas Flow <i>(13)</i></li>
 *   <li>Diagnostic Water Flow <i>(14)</i></li>
 *   <li>Diagnostic Voltage <i>(15)</i></li>
 *   <li>Diagnostic Misc <i>(16)</i></li>
 * </ul>
 *
 * There is also the special Run Type SensorType, which is added in the code
 * and required if xH2O or CO2 are defined because they are registered as
 * being internall calibrated
 *
 * Equilibrator Pressure (differential) depends on Ambient Pressure.
 * CO₂ in gas optionally depends on xH₂O in gas.
 *
 * Marine Underway pCO₂ requires the following sensors:
 *
 * <ul>
 *   <li>Intake Temperature</li>
 *   <li>salinityId</li>
 *   <li>Equilibrator Temperature</li>
 *   <li>Equilibrator Pressure</li>
 *   <li>CO₂ in gas <i>(core)</i></li>
 *   <li>Run Type</li>
 * <ul>
 *
 *
 * Although these tests require an instrument to be registered in the database,
 * the sensor assignments will not be used from there, but built and tested
 * entirely within memory. Database tests will be done as part of InstrumentDB.
 *
 * @author Steve Jones
 *
 */
@FlywayTest(locationsForMigrate = {
  "resources/sql/testbase/user",
  "resources/sql/testbase/instrument",
  "resources/sql/testbase/variable",
  "resources/sql/data/Instrument/SensorDefinition/SensorAssignmentsTest/isAssignmentRequired"
})
@TestInstance(Lifecycle.PER_CLASS)
public class SensorAssignmentsTest extends BaseTest {

  private static final long INVALID_SENSOR_ID = -1000L;

  private static final String DATA_FILE_NAME = "Data File";
  private static final String DATA_FILE_2_NAME = "Second File";

  // Created by sensorConfigInit()
  private SensorsConfiguration config = null;
  private List<Long> varIds = null;

  // Created by assignmentsInit()
  private SensorAssignments assignments = null;

  // Sensor type IDs. Populated by classInit()
  private long intakeTemperatureId = -1L;
  private long salinityId = -1L;
  private long equilibratorPressureParentId = -1L;
  private long xh2oId = -1L;
  private long co2Id = -1L;

  /**
   * Get the total number of assignments
   * @return The total number of assignments
   */
  private int countAllAssignments() {
    int count = 0;

    for (Set<SensorAssignment> assignmentSet : assignments.values()) {
      count += assignmentSet.size();
    }

    return count;
  }

  /**
   * Make an assignment for a given file, column and primary status
   * @param file The file
   * @param column The column index
   * @param primary The primary/secondary status
   * @return The assignment object
   */
  private SensorAssignment makeAssignment(String file, int column, boolean primary) {
    return new SensorAssignment(file, column, "Assignment", primary, false, "NaN");
  }

  /**
   * Get a sensor type's database ID using its name
   * @param typeName The type's name
   * @return The ID
   * @throws SensorTypeNotFoundException If the name is not found
   */
  private long getSensorTypeId(String typeName) throws SensorTypeNotFoundException {

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

  @BeforeAll
  public void sensorConfigInit() throws Exception {
    initResourceManager();
    config = ResourceManager.getInstance().getSensorsConfiguration();
    varIds = new ArrayList<Long>(1);
    varIds.add(1L);

    intakeTemperatureId = getSensorTypeId("Intake Temperature");
    salinityId = getSensorTypeId("Salinity");
    equilibratorPressureParentId = getSensorTypeId("Equilibrator Pressure");
    xh2oId = getSensorTypeId("xH₂O in gas");
    co2Id = getSensorTypeId("CO₂ in gas");
  }

  @BeforeEach
  public void assignmentsInit() throws Exception {
    assignments = new SensorAssignments(getDataSource().getConnection(), varIds);
  }

  @AfterEach
  public void destroySensorAssignments() {
    assignments = null;
  }

  @Test
  public void getNewSensorAssignmentsTest() throws Exception {
    // Just check that we can get a new assignments object from the system
    assertNotNull(assignments);
  }

  @Test
  public void basicAssignmentTest() throws Exception {
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    Map<SensorType, Set<SensorAssignment>> allAssignments = assignments;
    Set<SensorAssignment> sensorAssignments = allAssignments.get(config.getSensorType(1));
    assertEquals(1, sensorAssignments.size());
    assertEquals(makeAssignment(DATA_FILE_NAME, 1, true), sensorAssignments.toArray()[0]);
  }

  @Test
  public void assignToInvalidSensorTypeTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      assignments.addAssignment(INVALID_SENSOR_ID, makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void assignByNameTest() throws Exception {
    assignments.addAssignment("Intake Temperature", makeAssignment(DATA_FILE_NAME, 1, true));
    Map<SensorType, Set<SensorAssignment>> allAssignments = assignments;
    Set<SensorAssignment> sensorAssignments = allAssignments.get(config.getSensorType(1));
    assertEquals(1, sensorAssignments.size());
    assertEquals(makeAssignment(DATA_FILE_NAME, 1, true), sensorAssignments.toArray()[0]);
  }

  @Test
  public void assignByNonExistentNameTest() throws Exception {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      assignments.addAssignment("Flurble", makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void assignParentTest() {

    // Parents cannot be assigned; only their children
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(equilibratorPressureParentId, makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void duplicateColumnSameSensorTest() throws Exception {
    // The same column can't be assigned more than once
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void duplicateColumnDifferentSensorTest() throws Exception {
    // The same column can't be assigned more than once
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void duplicateColumnDifferentFileTest() throws Exception {
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    SensorAssignment assignment2 = new SensorAssignment(DATA_FILE_2_NAME,
      1, "Second file sensor", true, false, "NaN");
    assignments.addAssignment(salinityId, assignment2);

    assertEquals(2, countAllAssignments());
  }

  @Test
  public void removeAssignmentTest() throws Exception {
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.removeAssignment(DATA_FILE_NAME, 1);
    assertEquals(0, countAllAssignments());
  }

  @Test
  public void removeFileAssignmentsTest() throws Exception {
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 2, true));
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_2_NAME, 1, false));
    assignments.removeFileAssignments(DATA_FILE_NAME);
    assertEquals(1, countAllAssignments());
  }

  @Test
  public void addMultipleAssignments() throws Exception {
    // Add multiple assignments to sensor types
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 2, false));
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 3, true));
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 4, false));
    assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 5, true));
    assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 6, false));
    assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 7, true));
    assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 8, false));
    assertEquals(8, countAllAssignments());

  }

  @Test
  public void coreSensorAssignedPrimaryTest() throws Exception {
    assignments.addAssignment(co2Id, makeAssignment(DATA_FILE_NAME, 1, true));
    assertTrue(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, true));
  }

  @Test
  public void coreSensorNotAssignedTest() throws Exception {
    assignments.addAssignment(salinityId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 2, true));

    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, true));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, false));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_2_NAME, false));
  }

  @Test
  public void coreSensorSecondaryAssignedTest() throws Exception {
    assignments.addAssignment(co2Id, makeAssignment(DATA_FILE_NAME, 1, false));
    assertTrue(assignments.coreSensorAssigned(DATA_FILE_NAME, false));
    assertFalse(assignments.coreSensorAssigned(DATA_FILE_NAME, true));
  }

  @Test
  public void assignCoreSensorForDisallowedVariableTest() throws Exception {
    // You're not allowed to assign a core sensor for a
    // variable that your instrument doesn't measure.
    long testSensorId = getSensorTypeId("testSensor");
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(testSensorId, makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void runTypeNotRequiredNoInternalCalibTest() throws Exception {
    // Run type is not required if no sensor with internal calibration is
    // assigned
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));

    assertFalse(assignments.runTypeRequired(DATA_FILE_NAME));
    assertFalse(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  @Test
  public void runTypeRequiredOneInternalCalibTest() throws Exception {
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
    assertFalse(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  @Test
  public void runTypeRequiredTwoInternalCalibTest() throws Exception {
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_2_NAME, 1, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
    assertTrue(assignments.runTypeRequired(DATA_FILE_2_NAME));
  }

  @Test
  public void runTypeRequiredBothInternalCalibTest() throws Exception {
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, true));
    assignments.addAssignment(co2Id, makeAssignment(DATA_FILE_NAME, 2, true));

    assertTrue(assignments.runTypeRequired(DATA_FILE_NAME));
  }

  /* *************************************************************************
   *
   * isAssignmentRequired tests. This uses a TestSet CSV file.
   *
   * The test is below. Utility methods used by the test are under that.
   *
   * Note that not all possible combinations are tested at this time - just
   * the ones that we know to be in use (or are most likely to be used in
   * the near future)
   *
   */
  @ParameterizedTest
  @MethodSource("getAssignmentRequiredTestSet")
  public void isAssignmentRequiredTests(TestSetLine line) throws TestLineException {

    try {
      assignMainSensorType(line);
      assignRelation(line);
      assignDependent(line);
      assignDependentSibling(line);

      assertEquals(assignments.isAssignmentRequired(getMainSensorType(line)),
        getExpectedAssignmentRequired(line),
        "Test failed for test set line " + line.getLineNumber());
    } catch (Exception e) {
      throw new TestLineException(line, e);
    }

  }

  private static final int SENSOR_TYPE_COL = 0;
  private static final int SENSOR_ASSIGN_PRIMARY_COL = 1;
  private static final int SENSOR_ASSIGN_SECONDARY_COL = 2;
  private static final int RELATION_COL = 3;
  private static final int SIBLING_ASSIGNED_PRIMARY_COL = 4;
  private static final int SIBLING_ASSIGNED_SECONDARY_COL = 5;
  private static final int DEPENDENT_COL = 6;
  private static final int HAS_DEPENDS_QUESTION_COL = 7;
  private static final int DEPENDENT_ASSIGNED_PRIMARY_COL = 8;
  private static final int DEPENDENT_ASSIGNED_PRIMARY_DEPENDS_QUESTION_ANSWER_COL = 9;
  private static final int DEPENDENT_ASSIGNED_SECONDARY_COL = 10;
  private static final int DEPENDENT_ASSIGNED_SECONDARY_DEPENDS_QUESTION_ANSWER_COL = 11;
  private static final int DEPENDENT_SIBLING_COL = 12;
  private static final int DEPENDENT_SIBLING_ASSIGNED_PRIMARY_COL = 13;
  private static final int DEPENDENT_SIBLING_ASSIGNED_SECONDARY_COL = 14;
  private static final int IS_ASSIGNMENT_REQUIRED_COL = 15;


  @SuppressWarnings("unused")
  private Stream<TestSetLine> getAssignmentRequiredTestSet() throws Exception {
    return getTestSet("isAssignmentRequired");
  }

  private SensorType getMainSensorType(TestSetLine line) throws Exception {
    return config.getSensorType(line.getStringField(SENSOR_TYPE_COL));
  }

  /**
   * Assign the main sensor type for a line
   * @param line The line
   * @throws Exception If the assignment(s) fail
   */
  private void assignMainSensorType(TestSetLine line) throws Exception {
    if (line.getBooleanField(SENSOR_ASSIGN_PRIMARY_COL)) {
      assignments.addAssignment(getMainSensorType(line).getId(), makeAssignment(DATA_FILE_NAME, 1, true));
    }
    if (line.getBooleanField(SENSOR_ASSIGN_SECONDARY_COL)) {
      assignments.addAssignment(getMainSensorType(line).getId(), makeAssignment(DATA_FILE_NAME, 2, false));
    }
  }

  /**
   * Assign the relation to the main sensor type
   * @param line The line
   * @throws Exception If the assignment(s) fail
   */
  private void assignRelation(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(RELATION_COL)) {
      String relationTypeName = line.getStringField(RELATION_COL);
      if (line.getBooleanField(SIBLING_ASSIGNED_PRIMARY_COL)) {
        assignments.addAssignment(getSensorTypeId(relationTypeName), makeAssignment(DATA_FILE_NAME, 3, true));
      }
      if (line.getBooleanField(SIBLING_ASSIGNED_SECONDARY_COL)) {
        assignments.addAssignment(getSensorTypeId(relationTypeName), makeAssignment(DATA_FILE_NAME, 4, false));
      }
    }
  }

  /**
   * Assign the relation to the main sensor type
   * @param line The line
   * @throws Exception If the assignment(s) fail
   */
  private void assignDependent(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(DEPENDENT_COL)) {
      String dependentTypeName = line.getStringField(DEPENDENT_COL);
      if (line.getBooleanField(DEPENDENT_ASSIGNED_PRIMARY_COL)) {

        boolean dependsQuestionAnswer = false;
        if (line.getBooleanField(HAS_DEPENDS_QUESTION_COL)) {
          dependsQuestionAnswer = line.getBooleanField(DEPENDENT_ASSIGNED_PRIMARY_DEPENDS_QUESTION_ANSWER_COL);
        }

        SensorAssignment assignment = new SensorAssignment(DATA_FILE_NAME, 5, "Assignment", true, dependsQuestionAnswer, null);
        assignments.addAssignment(getSensorTypeId(dependentTypeName), assignment);
      }
      if (line.getBooleanField(DEPENDENT_ASSIGNED_SECONDARY_COL)) {

        boolean dependsQuestionAnswer = false;
        if (line.getBooleanField(HAS_DEPENDS_QUESTION_COL)) {
          dependsQuestionAnswer = line.getBooleanField(DEPENDENT_ASSIGNED_SECONDARY_DEPENDS_QUESTION_ANSWER_COL);
        }

        SensorAssignment assignment = new SensorAssignment(DATA_FILE_NAME, 6, "Assignment", true, dependsQuestionAnswer, null);
        assignments.addAssignment(getSensorTypeId(dependentTypeName), assignment);
      }
    }
  }

  /**
   * Assign the relation to the main sensor type
   * @param line The line
   * @throws Exception If the assignment(s) fail
   */
  private void assignDependentSibling(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(DEPENDENT_SIBLING_COL)) {
      String dependentSiblingTypeName = line.getStringField(DEPENDENT_SIBLING_COL);
      if (line.getBooleanField(DEPENDENT_SIBLING_ASSIGNED_PRIMARY_COL)) {
        assignments.addAssignment(getSensorTypeId(dependentSiblingTypeName), makeAssignment(DATA_FILE_NAME, 7, true));
      }
      if (line.getBooleanField(DEPENDENT_SIBLING_ASSIGNED_SECONDARY_COL)) {
        assignments.addAssignment(getSensorTypeId(dependentSiblingTypeName), makeAssignment(DATA_FILE_NAME, 8, false));
      }
    }
  }

  /**
   * Get the expected isAssignmentRequired result for a line
   * @param line The line
   * @return The expected result
   */
  private boolean getExpectedAssignmentRequired(TestSetLine line) {
    return line.getBooleanField(IS_ASSIGNMENT_REQUIRED_COL);
  }
}
