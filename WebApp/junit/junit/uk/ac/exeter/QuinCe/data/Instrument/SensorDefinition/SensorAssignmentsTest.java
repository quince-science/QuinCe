package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
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
  "resources/sql/data/Instrument/SensorDefinition/SensorAssignmentsTest/classMigrations"
})
@TestInstance(Lifecycle.PER_CLASS)
public class SensorAssignmentsTest extends DBTest {

  private static final long INVALID_SENSOR_ID = -1000L;

  private static final String DATA_FILE_NAME = "Data File";
  private static final String DATA_FILE_2_NAME = "Second File";

  // Created by classInit()
  private SensorsConfiguration config = null;

  // Created by createEmptySensorAssignments()
  private SensorAssignments assignments = null;

  // Sensor type IDs. Populated by classInit()
  private long intakeTemperatureId = -1L;
  private long salinityId = -1L;
  private long atmosphericPressureId = -1L;
  private long equilibratorPressureParentId = -1L;
  private long equilibratorPressureAbsoluteId = -1L;
  private long equilibratorPressureDifferentialId = -1L;
  private long ambientPressureId = -1L;
  private long xh2oId = -1L;
  private long co2Id = -1L;

  /**
   * Get the total number of assignments
   * @return The total number of assignments
   */
  private int countAllAssignments() {
    int count = 0;

    for (Set<SensorAssignment> assignmentSet : assignments.getAssignments().values()) {
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

  /**
   * Get the SensorType IDs of the two child Equilibrator Pressure
   * types
   * @return The IDs
   */
  @SuppressWarnings("unused")
  private Stream<Long> getEquilibratorPressureChildIds() {
    return Stream.of(
      equilibratorPressureAbsoluteId,
      equilibratorPressureDifferentialId);
  }

  @BeforeEach
  public void testInit() throws Exception {
    initResourceManager();
    config = ResourceManager.getInstance().getSensorsConfiguration();

    intakeTemperatureId = getSensorTypeId("Intake Temperature");
    salinityId = getSensorTypeId("Salinity");
    atmosphericPressureId = getSensorTypeId("Atmospheric Pressure");
    equilibratorPressureParentId = getSensorTypeId("Equilibrator Pressure");
    equilibratorPressureAbsoluteId = getSensorTypeId("Equilibrator Pressure (absolute)");
    equilibratorPressureDifferentialId = getSensorTypeId("Equilibrator Pressure (differential)");
    ambientPressureId = getSensorTypeId("Ambient Pressure");
    xh2oId = getSensorTypeId("xH₂O in gas");
    co2Id = getSensorTypeId("CO₂ in gas");

    assignments = config.getNewSensorAssigments(getDataSource(), 1);
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
    TreeMap<SensorType, Set<SensorAssignment>> allAssignments = assignments.getAssignments();
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
  public void simpleSensorRequiredTest() throws Exception {
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(intakeTemperatureId)));
  }

  @Test
  public void simpleSensorNotRequiredTest() throws Exception {
    // Atmospheric pressure is not required
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(atmosphericPressureId)));
  }

  @Test
  public void parentRequiredTest() throws Exception {
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureParentId)));
  }

  @ParameterizedTest
  @MethodSource("getEquilibratorPressureChildIds")
  public void childrenRequiredTest(long id) throws Exception {
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(id)));
  }

  @Test
  public void assignParentTest() {

    // Parents cannot be assigned; only their children
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(equilibratorPressureParentId, makeAssignment(DATA_FILE_NAME, 1, true));
    });
  }

  @Test
  public void assignChildTest() throws Exception {
    assignments.addAssignment(equilibratorPressureAbsoluteId, makeAssignment(DATA_FILE_NAME, 1, true));

    // Once one child is assigned, the parent and all siblings are not required
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureParentId)));
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureAbsoluteId)));
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureDifferentialId)));
  }

  @Test
  public void assignSecondaryChildTest() throws Exception {
    assignments.addAssignment(equilibratorPressureAbsoluteId, makeAssignment(DATA_FILE_NAME, 1, false));

    // A secondary assignment to a child
    // Means all are still required
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureParentId)));
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureAbsoluteId)));
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(equilibratorPressureDifferentialId)));
  }

  /* **********************
   * Basic dependency tests
   * **********************
   *
   *  If differential EqP is assigned, Ambient Pressure is required.
   * Otherwise it isn't.
   */

  @Test
  public void dependentNotRequiredTest() throws Exception {
    // Differential EqP is not assigned, therefore Ambient Pressure is not required
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(ambientPressureId)));
  }

  @Test
  public void dependentRequiredPrimaryTest() throws Exception {
    // Differential EqP is assigned, therefore Ambient Pressure is required
    assignments.addAssignment(equilibratorPressureDifferentialId, makeAssignment(DATA_FILE_NAME, 1, true));
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(ambientPressureId)));
  }

  @Test
  public void dependentRequiredSecondaryTest() throws Exception {
    // Secondary Differential EqP is defined, but Ambient Pressure is still required
    assignments.addAssignment(equilibratorPressureDifferentialId, makeAssignment(DATA_FILE_NAME, 1, false));
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(ambientPressureId)));
  }

  /* **********************
   * Depends Question tests
   * **********************
   *
   * CO2 depends on xh2oId only if the dependsQuestion answer is true in its assignment
   */
  @Test
  public void dependsQuestionDependentNoAssignmentTest() throws Exception {
    // Just testing that xh2oId isn't required by default
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(xh2oId)));
  }

  @Test
  public void dependsQuestionFalseTest() throws Exception {
    // CO2 is assigned, but the Depends Question answer is false.
    // Therefore xh2oId is not required
    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME,
      1, "Assignment", true, false, "NaN");

    assignments.addAssignment(co2Id, co2Assignment);
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(xh2oId)));
  }

  @Test
  public void dependsQuestionTrueTest() throws Exception {
    // CO2 is assigned, and the Depends Question answer is true.
    // Therefore xh2oId is required
    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME,
      1, "Assignment", true, true, "NaN");

    assignments.addAssignment(co2Id, co2Assignment);
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(xh2oId)));
  }

  @Test
  public void dependsQuestionAllAssignedTest() throws Exception {
    // CO2 is assigned, and the Depends Question answer is true.
    // Primary xh2oId is assigned. Therefore is isn't required
    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME,
      1, "Assignment", true, true, "NaN");

    assignments.addAssignment(co2Id, co2Assignment);
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 2, true));
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(xh2oId)));
}

  @Test
  public void dependsQuestionTrueSecondaryTest() throws Exception {
    // Secondary CO2 is assigned, and the Depends Question answer is true.
    // Therefore xh2oId is required
    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME,
      1, "Assignment", false, true, "NaN");

    assignments.addAssignment(co2Id, co2Assignment);
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(xh2oId)));
  }

  @Test
  public void dependsQuestionTrueSecondaryDependentTest() throws Exception {
    // CO2 is assigned, and the Depends Question answer is true.
    // Secondary xh2oId is assigned. Therefore xh2oId is still required
    SensorAssignment co2Assignment = new SensorAssignment(DATA_FILE_NAME,
      2, "Assignment", true, true, "NaN");

    assignments.addAssignment(co2Id, co2Assignment);
    assignments.addAssignment(xh2oId, makeAssignment(DATA_FILE_NAME, 1, false));
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(xh2oId)));
  }

  @Test
  public void primaryAssignedTest() throws Exception {
    // Assigning a primary sensor means it's no longer required
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, true));
    assertFalse(assignments.isAssignmentRequired(config.getSensorType(1)));
  }

  @Test
  public void secondaryAssignedTest() throws Exception {
    // Assigning just a secondary sensor means it's still required
    assignments.addAssignment(intakeTemperatureId, makeAssignment(DATA_FILE_NAME, 1, false));
    assertTrue(assignments.isAssignmentRequired(config.getSensorType(1)));
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
  public void assignmentRequiredNonExistentSensorTypeTest() {
    SensorType nonExistentSensor = Mockito.mock(SensorType.class);
    Mockito.when(nonExistentSensor.getId()).thenReturn(-1000L);

    assertThrows(SensorAssignmentException.class, () -> {
      assignments.isAssignmentRequired(nonExistentSensor);
    });
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
    long unobtaniumId = getSensorTypeId("Unobtanium");
    assertThrows(SensorAssignmentException.class, () -> {
      assignments.addAssignment(unobtaniumId, makeAssignment(DATA_FILE_NAME, 1, true));
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
}
