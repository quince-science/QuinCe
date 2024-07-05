package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestLineException;
import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the {@link SensorAssignments#isAssignmentRequired(SensorType,
 * Map<Long, VariableAttributes>)} method.
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
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SENSOR_TYPE_COL = 0;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SENSOR_ASSIGN_PRIMARY_COL = 1;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SENSOR_ASSIGN_SECONDARY_COL = 2;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int RELATION_COL = 3;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SIBLING_ASSIGNED_PRIMARY_COL = 4;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int SIBLING_ASSIGNED_SECONDARY_COL = 5;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_COL = 6;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int HAS_DEPENDS_QUESTION_COL = 7;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_PRIMARY_COL = 8;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_PRIMARY_DEPENDS_QUESTION_ANSWER_COL = 9;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_SECONDARY_COL = 10;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_ASSIGNED_SECONDARY_DEPENDS_QUESTION_ANSWER_COL = 11;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_SIBLING_COL = 12;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_SIBLING_ASSIGNED_PRIMARY_COL = 13;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int DEPENDENT_SIBLING_ASSIGNED_SECONDARY_COL = 14;

  /**
   * A column index in the Test Set file for
   * {@link #isAssignmentRequiredTests(TestSetLine)}.
   */
  private static final int IS_ASSIGNMENT_REQUIRED_COL = 15;

  // Created by init()
  /**
   * The application's {@link SensorAssignments} configuration.
   *
   * <p>
   * Loaded by {@link #init()}.
   * </p>
   */
  private SensorAssignments assignments = null;

  // Created by init()
  /**
   * The application's sensor configuration.
   *
   * <p>
   * Loaded by {@link #init()}.
   * </p>
   */
  private SensorsConfiguration config = null;

  /**
   * An empty {@link VariableAttributes} map for basic tests. The functionality
   * of this map will be tested elsewhere.
   */
  private Map<Long, VariableAttributes> emptyVarAttributes = new HashMap<Long, VariableAttributes>();

  /**
   * Read the variable and sensor configuration from the database.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @BeforeEach
  public void init() throws Exception {
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
   * <caption>Columns required for the {@code isAssignmentRequired} test
   * set</caption>
   * <tr>
   * <th>Column Name</th>
   * <th>Purpose</th>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Sensor Type</td>
   * <td>The central {@link SensorType} of the test</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the main {@link SensorType}
   * should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Assigned Secondary</td>
   * <td>Indicates whether a Secondary assignment of the main {@link SensorType}
   * should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Relation</td>
   * <td>A related (i.e. sibling) {@link SensorType} to assign</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Sibling Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the sibling
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Sibling Assigned Secondary</td>
   * <td>Indicates whether a Secondary assignment of the sibling
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Dependent</td>
   * <td>A dependent {@link SensorType} (dependent on the main
   * {@link SensorType} to assign</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Has Depends Question</td>
   * <td>Indicates whether this has a Depends Question (a question where
   * answering {@code true} makes the sensor dependent, while {@code false} does
   * not).</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Dependent Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the dependent
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Depends Question Answer</td>
   * <td>The answer to the Depends Question for the Primary assignment</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Dependent Assigned Secondary</td>
   * <td>Indicates whether a Primary assignment of the dependent
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Depends Question Answer</td>
   * <td>The answer to the Depends Question for the Secondary assignment</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Dependent Has Relation?</td>
   * <td>Indicates the relation (sibling) to the dependent sensor to be
   * assigned, if any.</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Dependent Sibling Assigned Primary</td>
   * <td>Indicates whether a Primary assignment of the relation to the dependent
   * {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Dependent Sibling Assigned Secondary</td>
   * <td>Indicates whether a Secondary assignment of the relation to the
   * dependent {@link SensorType} should be made</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Sensor Type Required</td>
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
   * @throws Exception
   *           If any internal errors are encountered.
   * @see TestSetLine
   * @see #assignMainSensorType(TestSetLine)
   * @see #assignRelation(TestSetLine)
   * @see #assignDependent(TestSetLine)
   * @see #assignDependentSibling(TestSetLine)
   * @see #getExpectedAssignmentRequired(TestSetLine)
   */
  @ParameterizedTest
  @MethodSource("getLines")
  public void isAssignmentRequiredTests(TestSetLine line) throws Exception {

    try {
      assignMainSensorType(line);
      assignRelation(line);
      assignDependent(line);
      assignDependentSibling(line);

      assertEquals(
        assignments.isAssignmentRequired(getSensorType(line, SENSOR_TYPE_COL),
          emptyVarAttributes),
        getExpectedAssignmentRequired(line),
        "Test failed for test set line " + line.getLineNumber());
    } catch (Exception e) {
      throw new TestLineException(line, e);
    }

  }

  /**
   * Get the assigned {@link SensorType} for the specified test line and column.
   *
   * @param line
   *          The line number in the test file.
   * @param col
   *          The column index.
   * @return The assigned {@link SensorType}.
   * @throws Exception
   *           If the assignment is invalid.
   */
  private SensorType getSensorType(TestSetLine line, int col) throws Exception {
    return config.getSensorType(line.getStringField(col, true));
  }

  /**
   * Assign the main {@link SensorType} for an entry in the
   * {@link #isAssignmentRequiredTests(TestSetLine)} Test Set.
   *
   * @param line
   *          The Test Set line
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private void assignMainSensorType(TestSetLine line) throws Exception {
    if (line.getBooleanField(SENSOR_ASSIGN_PRIMARY_COL)) {
      assignments.addAssignment(SensorAssignmentsTest.makeAssignment(
        getSensorType(line, SENSOR_TYPE_COL),
        SensorAssignmentsTest.DATA_FILE_NAME, 1, "Sensor 1", true));
    }
    if (line.getBooleanField(SENSOR_ASSIGN_SECONDARY_COL)) {
      assignments.addAssignment(SensorAssignmentsTest.makeAssignment(
        SensorAssignmentsTest.DATA_FILE_NAME, 2, "Sensor 2", false));
    }
  }

  /**
   * Assign the related {@link SensorType} to the main {@link SensorType} for an
   * entry in the {@link #isAssignmentRequiredTests(TestSetLine)} Test Set.
   *
   * @param line
   *          The Test Set line
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private void assignRelation(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(RELATION_COL)) {
      if (line.getBooleanField(SIBLING_ASSIGNED_PRIMARY_COL)) {
        assignments.addAssignment(SensorAssignmentsTest.makeAssignment(
          getSensorType(line, RELATION_COL),
          SensorAssignmentsTest.DATA_FILE_NAME, 3, "Sensor 3", true));
      }
      if (line.getBooleanField(SIBLING_ASSIGNED_SECONDARY_COL)) {
        assignments.addAssignment(SensorAssignmentsTest.makeAssignment(
          getSensorType(line, RELATION_COL),
          SensorAssignmentsTest.DATA_FILE_NAME, 4, "Sensor 4", false));
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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private void assignDependent(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(DEPENDENT_COL)) {
      if (line.getBooleanField(DEPENDENT_ASSIGNED_PRIMARY_COL)) {

        boolean dependsQuestionAnswer = false;
        if (line.getBooleanField(HAS_DEPENDS_QUESTION_COL)) {
          dependsQuestionAnswer = line.getBooleanField(
            DEPENDENT_ASSIGNED_PRIMARY_DEPENDS_QUESTION_ANSWER_COL);
        }

        SensorAssignment assignment = new SensorAssignment(
          SensorAssignmentsTest.DATA_FILE_NAME, 5,
          getSensorType(line, DEPENDENT_COL), "Dependent 1", true,
          dependsQuestionAnswer, null);
        assignments.addAssignment(assignment);
      }
      if (line.getBooleanField(DEPENDENT_ASSIGNED_SECONDARY_COL)) {

        boolean dependsQuestionAnswer = false;
        if (line.getBooleanField(HAS_DEPENDS_QUESTION_COL)) {
          dependsQuestionAnswer = line.getBooleanField(
            DEPENDENT_ASSIGNED_SECONDARY_DEPENDS_QUESTION_ANSWER_COL);
        }

        SensorAssignment assignment = new SensorAssignment(
          SensorAssignmentsTest.DATA_FILE_NAME, 6,
          getSensorType(line, DEPENDENT_COL), "Dependent 2", true,
          dependsQuestionAnswer, null);
        assignments.addAssignment(assignment);
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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private void assignDependentSibling(TestSetLine line) throws Exception {
    if (!line.isFieldEmpty(DEPENDENT_SIBLING_COL)) {
      if (line.getBooleanField(DEPENDENT_SIBLING_ASSIGNED_PRIMARY_COL)) {
        assignments.addAssignment(SensorAssignmentsTest.makeAssignment(
          SensorAssignmentsTest.DATA_FILE_NAME, 7, "Sensor 7", true));
      }
      if (line.getBooleanField(DEPENDENT_SIBLING_ASSIGNED_SECONDARY_COL)) {
        assignments.addAssignment(SensorAssignmentsTest.makeAssignment(
          SensorAssignmentsTest.DATA_FILE_NAME, 8, "Sensor 8", false));
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

  @Override
  protected String getTestSetName() {
    return "isAssignmentRequired";
  }
}
