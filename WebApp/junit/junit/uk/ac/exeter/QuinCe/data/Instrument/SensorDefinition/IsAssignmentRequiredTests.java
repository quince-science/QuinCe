package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestLineException;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
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
 * Tests for the {@link SensorAssignments#isAssignmentRequired(SensorType)}
 * method.
 *
 * <p>
 * These are complex and many, so live in their own class to make the main
 * {@link SensorAssignmentsTest} class less complicated.
 * </p>
 *
 * @author Steve Jones
 *
 */
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
  "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
  "resources/sql/data/Instrument/SensorDefinition/SensorAssignmentsTest/isAssignmentRequired" })
@TestInstance(Lifecycle.PER_CLASS)
public class IsAssignmentRequiredTests extends TestSetTest {

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SENSOR_TYPE_COL = 0;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SENSOR_ASSIGN_PRIMARY_COL = 1;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SENSOR_ASSIGN_SECONDARY_COL = 2;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int RELATION_COL = 3;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SIBLING_ASSIGNED_PRIMARY_COL = 4;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SIBLING_ASSIGNED_SECONDARY_COL = 5;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_COL = 6;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int HAS_DEPENDS_QUESTION_COL = 7;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_PRIMARY_COL = 8;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_PRIMARY_DEPENDS_QUESTION_ANSWER_COL = 9;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_SECONDARY_COL = 10;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_SECONDARY_DEPENDS_QUESTION_ANSWER_COL = 11;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_SIBLING_COL = 12;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_SIBLING_ASSIGNED_PRIMARY_COL = 13;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_SIBLING_ASSIGNED_SECONDARY_COL = 14;

  /**
   * A column in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int IS_ASSIGNMENT_REQUIRED_COL = 15;

  // Created by init()
  /**
   * The application's {@link SensorAssignments} configuration.
   *
   * <p>
   * Loaded by {@link #assignmentsInit()}.
   * </p>
   */
  private SensorAssignments assignments = null;

  // Created by init()
  /**
   * The application's sensor configuration.
   *
   * <p>
   * Loaded by {@link #sensorConfigInit()}.
   * </p>
   */
  private SensorsConfiguration config = null;

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
   * @throws SensorConfigurationException
   *           If the sensor configuration is invalid
   */
  @BeforeEach
  public void init() throws DatabaseException, SensorTypeNotFoundException,
    SQLException, SensorConfigurationException {
    initResourceManager();
    config = ResourceManager.getInstance().getSensorsConfiguration();
    ArrayList<Long> varIds = new ArrayList<Long>(1);

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

    assignments = new SensorAssignments(getDataSource().getConnection(),
      varIds);
  }

  /**
   * Ensure that the configuration and assignments are reset after each test.
   */
  @AfterEach
  public void tearDown() {
    assignments = null;
    config = null;
    ResourceManager.destroy();
  }

  /**
   * Test the {@link SensorAssignments#isAssignmentRequired(SensorType)} method.
   *
   * <p>
   * This requires a large number of tests with different combinations. These
   * are defined in the file
   * {@code WebApp/junit/resources/testsets/isAssignmentRequired.csv}. The file
   * defines different combinations of assignments to apply, and the expected
   * result of calling
   * {@link SensorAssignments#isAssignmentRequired(SensorType)}. The column
   * headers in the file are as follows:
   * </p>
   *
   * <table>
   * <tr>
   * <td><b>Column Name</b></td>
   * <td><b>Purpose</b></td>
   * </tr>
   * <tr>
   * <td>Sensor Type</td>
   * <td>The central {@link SensorType} of the test</td>
   * </tr>
   * <tr>
   * <td>Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the main {@link SensorType}
   * should be made</td>
   * </tr>
   * <tr>
   * <td>Assigned Secondary</td>
   * <td>Indicates whether a Secondary assignment of the main {@link SensorType}
   * should be made</td>
   * </tr>
   * <tr>
   * <td>Relation</td>
   * <td>A related (i.e. sibling) {@link SensorType} to assign</td>
   * </tr>
   * <tr>
   * <td>Sibling Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the sibling
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td>Sibling Assigned Secondary</td>
   * <td>Indicates whether a Secondary assignment of the sibling
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td>Dependent</td>
   * <td>A dependent {@link SensorType} (dependent on the main
   * {@link SensorType} to assign</td>
   * </tr>
   * <tr>
   * <td>Has Depends Question</td>
   * <td>Indicates whether this has a Depends Question (a question where
   * answering {@code true} makes the sensor dependent, while {@code false} does
   * not).</td>
   * </tr>
   * <tr>
   * <td>Dependent Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the dependent
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td>Depends Question Answer</td>
   * <td>The answer to the Depends Question for the Primary assignment</td>
   * </tr>
   * <tr>
   * <td>Dependent Assigned Secondary</td>
   * <td>Indicates whether a Primary assignment of the dependent
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td>Depends Question Answer</td>
   * <td>The answer to the Depends Question for the Secondary assignment</td>
   * </tr>
   * <tr>
   * <td>Dependent Has Relation?</td>
   * <td>Indicates the relation (sibling) to the dependent sensor to be
   * assigned, if any.</td>
   * </tr>
   * <tr>
   * <td>Dependent Sibling Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the relation to the dependent
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td>Dependent Sibling Assigned Secondary</td>
   * <td>Indicates whether a Secondary assignment of the relation to the
   * dependent {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td>Sensor Type Required</td>
   * <td>The expected result of the
   * {@link SensorAssignments#isAssignmentRequired(SensorType)} function</td>
   * </tr>
   * </table>
   *
   * <p>
   * Note that not all possible combinations are tested at this time - just the
   * ones that we know to be in use (or are most likely to be used in the near
   * future).
   * </p>
   *
   * @param line
   *          A line from the Test Set file
   * @throws TestLineException
   *           If one of the test lines cannot be processed
   * @see TestSetLine
   * @see #getAssignmentRequiredTestSet()
   * @see #assignMainSensorType(TestSetLine)
   * @see #assignRelation(TestSetLine)
   * @see #assignDependent(TestSetLine)
   * @see #assignDependentSibling(TestSetLine)
   * @see #getExpectedAssignmentRequired(TestSetLine)
   */
  @ParameterizedTest
  @MethodSource("getLines")
  public void isAssignmentRequiredTests(TestSetLine line)
    throws TestLineException {

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

  private SensorType getMainSensorType(TestSetLine line) throws Exception {
    return config.getSensorType(line.getStringField(SENSOR_TYPE_COL, true));
  }

  /**
   * Assign the main {@link SensorType} for an entry in the
   * {@link #isAssignmentRequiredTests(TestSetLine)} Test Set.
   *
   * @param line
   *          The Test Set line
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  private void assignMainSensorType(TestSetLine line)
    throws SensorTypeNotFoundException, SensorAssignmentException, Exception {
    if (line.getBooleanField(SENSOR_ASSIGN_PRIMARY_COL)) {
      assignments.addAssignment(getMainSensorType(line).getId(),
        SensorAssignmentsTest
          .makeAssignment(SensorAssignmentsTest.DATA_FILE_NAME, 1, true));
    }
    if (line.getBooleanField(SENSOR_ASSIGN_SECONDARY_COL)) {
      assignments.addAssignment(getMainSensorType(line).getId(),
        SensorAssignmentsTest
          .makeAssignment(SensorAssignmentsTest.DATA_FILE_NAME, 2, false));
    }
  }

  /**
   * Assign the related {@link SensorType} to the main {@link SensorType} for an
   * entry in the {@link #isAssignmentRequiredTests(TestSetLine)} Test Set.
   *
   * @param line
   *          The Test Set line
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  private void assignRelation(TestSetLine line)
    throws SensorTypeNotFoundException, SensorAssignmentException {
    if (!line.isFieldEmpty(RELATION_COL)) {
      String relationTypeName = line.getStringField(RELATION_COL, true);
      if (line.getBooleanField(SIBLING_ASSIGNED_PRIMARY_COL)) {
        assignments.addAssignment(getSensorTypeId(relationTypeName),
          SensorAssignmentsTest
            .makeAssignment(SensorAssignmentsTest.DATA_FILE_NAME, 3, true));
      }
      if (line.getBooleanField(SIBLING_ASSIGNED_SECONDARY_COL)) {
        assignments.addAssignment(getSensorTypeId(relationTypeName),
          SensorAssignmentsTest
            .makeAssignment(SensorAssignmentsTest.DATA_FILE_NAME, 4, false));
      }
    }
  }

  /**
   * Assign the dependent {@link SensorType} to the main {@link SensorType} for
   * an entry in the {@link #isAssignmentRequiredTests(TestSetLine)} Test Set.
   *
   * @param line
   *          The Test Set line
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  private void assignDependent(TestSetLine line)
    throws SensorTypeNotFoundException, SensorAssignmentException {
    if (!line.isFieldEmpty(DEPENDENT_COL)) {
      String dependentTypeName = line.getStringField(DEPENDENT_COL, true);
      if (line.getBooleanField(DEPENDENT_ASSIGNED_PRIMARY_COL)) {

        boolean dependsQuestionAnswer = false;
        if (line.getBooleanField(HAS_DEPENDS_QUESTION_COL)) {
          dependsQuestionAnswer = line.getBooleanField(
            DEPENDENT_ASSIGNED_PRIMARY_DEPENDS_QUESTION_ANSWER_COL);
        }

        SensorAssignment assignment = new SensorAssignment(
          SensorAssignmentsTest.DATA_FILE_NAME, 5, "Assignment", true,
          dependsQuestionAnswer, null);
        assignments.addAssignment(getSensorTypeId(dependentTypeName),
          assignment);
      }
      if (line.getBooleanField(DEPENDENT_ASSIGNED_SECONDARY_COL)) {

        boolean dependsQuestionAnswer = false;
        if (line.getBooleanField(HAS_DEPENDS_QUESTION_COL)) {
          dependsQuestionAnswer = line.getBooleanField(
            DEPENDENT_ASSIGNED_SECONDARY_DEPENDS_QUESTION_ANSWER_COL);
        }

        SensorAssignment assignment = new SensorAssignment(
          SensorAssignmentsTest.DATA_FILE_NAME, 6, "Assignment", true,
          dependsQuestionAnswer, null);
        assignments.addAssignment(getSensorTypeId(dependentTypeName),
          assignment);
      }
    }
  }

  /**
   * Assign the dependent sibling {@link SensorType} to the main
   * {@link SensorType} for an entry in the
   * {@link #isAssignmentRequiredTests(TestSetLine)} Test Set.
   *
   * @param line
   *          The Test Set line
   *
   * @throws SensorTypeNotFoundException
   *           If the an assigned {@link SensorType} is not found
   * @throws SensorAssignmentException
   *           If an assignment fails
   */
  private void assignDependentSibling(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(DEPENDENT_SIBLING_COL)) {
      String dependentSiblingTypeName = line
        .getStringField(DEPENDENT_SIBLING_COL, true);
      if (line.getBooleanField(DEPENDENT_SIBLING_ASSIGNED_PRIMARY_COL)) {
        assignments.addAssignment(getSensorTypeId(dependentSiblingTypeName),
          SensorAssignmentsTest
            .makeAssignment(SensorAssignmentsTest.DATA_FILE_NAME, 7, true));
      }
      if (line.getBooleanField(DEPENDENT_SIBLING_ASSIGNED_SECONDARY_COL)) {
        assignments.addAssignment(getSensorTypeId(dependentSiblingTypeName),
          SensorAssignmentsTest
            .makeAssignment(SensorAssignmentsTest.DATA_FILE_NAME, 8, false));
      }
    }
  }

  /**
   * Get the expected {@link SensorAssignments#isAssignmentRequired(SensorType)}
   * result for an entry in the {@link #isAssignmentRequiredTests(TestSetLine)}
   * Test Set.
   *
   * @param line
   *          The Test Set line
   * @return The expected result
   */
  private boolean getExpectedAssignmentRequired(TestSetLine line) {
    return line.getBooleanField(IS_ASSIGNMENT_REQUIRED_COL);
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

  @Override
  protected String getTestSetName() {
    return "isAssignmentRequired";
  }
}
