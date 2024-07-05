package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Tests for the {@link SensorAssignment} class.
 *
 * @author Steve Jones
 *
 */
public class SensorAssignmentTest {

  /**
   * Construct a {@link SensorAssignment} object outside the database.
   *
   * <p>
   * This will have no database ID. It is a Secondary assignment with the
   * Depends Question Answer set to {@code false}.
   * </p>
   *
   * @return The constructed {@link SensorAssignment}
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private SensorAssignment getNoIdFalsesAssignment() throws Exception {
    return new SensorAssignment("Data File", 1, getTestSensorType(), "Sensor",
      false, false, "NaN");
  }

  private SensorType getTestSensorType() throws SensorTypeNotFoundException,
    MissingParamException, SensorConfigurationException {

    SensorType result = Mockito.mock(SensorType.class);
    Mockito.when(result.getId()).thenReturn(1L);
    Mockito.when(result.getShortName()).thenReturn("Test Sensor");
    Mockito.when(result.getGroup()).thenReturn("Test Group");
    Mockito.when(result.getUnits()).thenReturn("ewe knit");
    Mockito.when(result.getLongName()).thenReturn("Test Sensor (long name)");
    Mockito.when(result.getCodeName()).thenReturn("Code");

    return result;
  }

  /**
   * Tests that the {@code static} method to get a human-readable target
   * {@link String} for data file and sensor name works correctly.
   */
  @Test
  public void staticGetTargetTest() {
    assertEquals("Data File: Sensor",
      SensorAssignment.getTarget("Data File", "Sensor"));
  }

  /**
   * Tests that a {@link SensorAssignment} with no database ID is correctly
   * constructed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void noIdConstructorTest() throws Exception {

    SensorAssignment assignment = new SensorAssignment("Data File", 1,
      getTestSensorType(), "Sensor", false, false, "NaN");

    assertEquals(DatabaseUtils.NO_DATABASE_RECORD, assignment.getDatabaseId());
    assertEquals("Data File", assignment.getDataFile());
    assertEquals(1, assignment.getColumn());
    assertEquals("Sensor", assignment.getSensorName());
    assertFalse(assignment.isPrimary());
    assertFalse(assignment.getDependsQuestionAnswer());
    assertEquals("NaN", assignment.getMissingValue());
  }

  /**
   * Tests that a {@link SensorAssignment} with a database ID is correctly
   * constructed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void idConstructorTest() throws Exception {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 4,
      getTestSensorType(), "Sensor", false, false, "NaN");

    assertEquals(1, assignment.getDatabaseId());
    assertEquals("Data File", assignment.getDataFile());
    assertEquals(4, assignment.getColumn());
    assertEquals("Sensor", assignment.getSensorName());
    assertFalse(assignment.isPrimary());
    assertFalse(assignment.getDependsQuestionAnswer());
    assertEquals("NaN", assignment.getMissingValue());
  }

  /**
   * Tests that a {@link SensorAssignment} with both Primary and Depends
   * Question Answer attributes set to {@code true} is correctly constructed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void noIdTrueValuesConstructorTest() throws Exception {
    SensorAssignment assignment = new SensorAssignment("Data File", 1,
      getTestSensorType(), "Sensor", true, true, "NaN");

    assertTrue(assignment.isPrimary());
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  /**
   * Tests that a {@link SensorAssignment} with both Primary and Depends
   * Question Answer attributes set to {@code false} is correctly constructed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void idTrueValuesConstructorTest() throws Exception {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 4,
      getTestSensorType(), "Sensor", true, true, "NaN");

    assertTrue(assignment.isPrimary());
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  /**
   * Tests that a {@link SensorAssignment} with a Sensor Name of {@code null} is
   * converted to an empty {@link String}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void nullSensorNameTest() throws Exception {
    SensorAssignment assignment = new SensorAssignment("Data File", 1,
      getTestSensorType(), null, false, false, "Missing");

    assertEquals("", assignment.getSensorName());
  }

  /**
   * Tests that a {@link SensorAssignment} with a Sensor Name of {@code null} is
   * converted to an empty {@link String}.
   */
  @Test
  public void nullSensorTypeTest() {

    assertThrows(SensorAssignmentException.class, () -> {
      new SensorAssignment("Data File", 1, null, null, false, false, "Missing");
    });
  }

  /**
   * Test that the database ID for a {@link SensorAssignment} can be set.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void setDatabaseIdTest() throws Exception {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDatabaseId(77);
    assertEquals(77, assignment.getDatabaseId());
  }

  /**
   * Test that the Depends Question Answer for a {@link SensorAssignment} can be
   * set.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void setDependsQuestionAnswerTest() throws Exception {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDependsQuestionAnswer(true);
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  /**
   * Test that the Missing Value for a {@link SensorAssignment} can be set.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void setMissingValueTest() throws Exception {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setMissingValue("Missing");
    assertEquals("Missing", assignment.getMissingValue());
  }

  /**
   * Test that the human-readable target {@link String} for a
   * {@link SensorAssignment} can be retrieved and is correct.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void getTargetTest() throws Exception {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assertEquals("Data File: Sensor", assignment.getTarget());
  }

  /**
   * Test that the constructor for a {@link SensorAssignment} with no database
   * ID correctly converts a {@code null} Missing Value to an empty
   * {@link String}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void constructor1MissingValueTest() throws Exception {
    SensorAssignment assignment = new SensorAssignment("Data File", 1,
      getTestSensorType(), "Sensor", false, false, null);
    assertEquals("", assignment.getMissingValue());
  }

  /**
   * Test that the constructor for a {@link SensorAssignment} with a database ID
   * correctly converts a {@code null} Missing Value to an empty {@link String}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void constructor2MissingValueTest() throws Exception {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 1,
      getTestSensorType(), "Sensor", false, false, null);
    assertEquals("", assignment.getMissingValue());
  }

  /**
   * Test that setting the Missing Value to {@code null} correctly converts it
   * to an empty {@link String}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void setNullMissingValueTest() throws Exception {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setMissingValue(null);
    assertEquals("", assignment.getMissingValue());
  }
}
