package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

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
   */
  private SensorAssignment getNoIdFalsesAssignment() {
    return new SensorAssignment("Data File", 1, "Sensor", false, false, "NaN");
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
   */
  @Test
  public void noIdConstructorTest() {

    SensorAssignment assignment = new SensorAssignment("Data File", 1, "Sensor",
      false, false, "NaN");

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
   */
  @Test
  public void idConstructorTest() {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 4,
      "Sensor", false, false, "NaN");

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
   */
  @Test
  public void noIdTrueValuesConstructorTest() {
    SensorAssignment assignment = new SensorAssignment("Data File", 1, "Sensor",
      true, true, "NaN");

    assertTrue(assignment.isPrimary());
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  /**
   * Tests that a {@link SensorAssignment} with both Primary and Depends
   * Question Answer attributes set to {@code false} is correctly constructed.
   */
  @Test
  public void idTrueValuesConstructorTest() {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 4,
      "Sensor", true, true, "NaN");

    assertTrue(assignment.isPrimary());
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  /**
   * Tests that a {@link SensorAssignment} with a Sensor Name of {@code null} is
   * converted to an empty {@link String}.
   */
  @Test
  public void nullSensorNameTest() {
    SensorAssignment assignment = new SensorAssignment("Data File", 1, null,
      false, false, "Missing");

    assertEquals("", assignment.getSensorName());
  }

  /**
   * Test that the database ID for a {@link SensorAssignment} can be set.
   */
  @Test
  public void setDatabaseIdTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDatabaseId(77);
    assertEquals(77, assignment.getDatabaseId());
  }

  /**
   * Test that the Depends Question Answer for a {@link SensorAssignment} can be
   * set.
   */
  @Test
  public void setDependsQuestionAnswerTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDependsQuestionAnswer(true);
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  /**
   * Test that the Missing Value for a {@link SensorAssignment} can be set.
   */
  @Test
  public void setMissingValueTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setMissingValue("Missing");
    assertEquals("Missing", assignment.getMissingValue());
  }

  /**
   * Test that the human-readable target {@link String} for a
   * {@link SensorAssignment} can be retrieved and is correct.
   */
  @Test
  public void getTargetTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assertEquals("Data File: Sensor", assignment.getTarget());
  }

  /**
   * Test that the constructor for a {@link SensorAssignment} with no database
   * ID correctly converts a {@code null} Missing Value to an empty
   * {@link String}.
   */
  @Test
  public void constructor1MissingValueTest() {
    SensorAssignment assignment = new SensorAssignment("Data File", 1, "Sensor",
      false, false, null);
    assertEquals("", assignment.getMissingValue());
  }

  /**
   * Test that the constructor for a {@link SensorAssignment} with a database ID
   * correctly converts a {@code null} Missing Value to an empty {@link String}.
   */
  @Test
  public void constructor2MissingValueTest() {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 1,
      "Sensor", false, false, null);
    assertEquals("", assignment.getMissingValue());
  }

  /**
   * Test that setting the Missing Value to {@code null} correctly converts it
   * to an empty {@link String}.
   */
  @Test
  public void setNullMissingValueTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setMissingValue(null);
    assertEquals("", assignment.getMissingValue());
  }
}
