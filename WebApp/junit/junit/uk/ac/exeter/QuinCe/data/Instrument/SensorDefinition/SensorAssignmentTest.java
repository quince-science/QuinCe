package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

public class SensorAssignmentTest {

  private SensorAssignment getNoIdFalsesAssignment() {
    return new SensorAssignment("Data File", 1, "Sensor",
      false, false, false, "NaN");
  }

  @Test
  public void staticGetTargetTest() {
    assertEquals("Data File: Sensor",
      SensorAssignment.getTarget("Data File", "Sensor"));
  }

  @Test
  public void noIdConstructorTest() {

    SensorAssignment assignment = new SensorAssignment("Data File", 1, "Sensor",
      false, false, false, "NaN");

    assertEquals(DatabaseUtils.NO_DATABASE_RECORD, assignment.getDatabaseId());
    assertEquals("Data File", assignment.getDataFile());
    assertEquals(1, assignment.getColumn());
    assertEquals(-1, assignment.getDatabaseColumn());
    assertEquals("Sensor", assignment.getSensorName());
    assertFalse(assignment.getPostCalibrated());
    assertFalse(assignment.isPrimary());
    assertFalse(assignment.getDependsQuestionAnswer());
    assertEquals("NaN", assignment.getMissingValue());
  }

  @Test
  public void idConstructorTest() {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 4, 2,
      "Sensor", false, false, false, "NaN");

    assertEquals(1, assignment.getDatabaseId());
    assertEquals("Data File", assignment.getDataFile());
    assertEquals(4, assignment.getColumn());
    assertEquals(2, assignment.getDatabaseColumn());
    assertEquals("Sensor", assignment.getSensorName());
    assertFalse(assignment.getPostCalibrated());
    assertFalse(assignment.isPrimary());
    assertFalse(assignment.getDependsQuestionAnswer());
    assertEquals("NaN", assignment.getMissingValue());
  }

  @Test
  public void noIdTrueValuesConstructorTest() {
    SensorAssignment assignment = new SensorAssignment("Data File", 1, "Sensor",
      true, true, true, "NaN");

    assertTrue(assignment.getPostCalibrated());
    assertTrue(assignment.isPrimary());
    assertTrue(assignment.getDependsQuestionAnswer());
}

  @Test
  public void idTrueValuesConstructorTest() {
    SensorAssignment assignment = new SensorAssignment(1, "Data File", 4, 2,
      "Sensor", true, true, true, "NaN");

    assertTrue(assignment.getPostCalibrated());
    assertTrue(assignment.isPrimary());
    assertTrue(assignment.getDependsQuestionAnswer());
}

  @Test
  public void nullSensorNameTest() {
    SensorAssignment assignment = new SensorAssignment("Data File", 1, null,
      false, false, false, "Missing");

    assertEquals("", assignment.getSensorName());
  }

  @Test
  public void setDatabaseIdTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDatabaseId(77);
    assertEquals(77, assignment.getDatabaseId());
  }

  @Test
  public void setDatabaseColumnTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDatabaseColumn(77);
    assertEquals(77, assignment.getDatabaseColumn());
  }

  @Test
  public void setDependsQuestionAnswerTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setDependsQuestionAnswer(true);
    assertTrue(assignment.getDependsQuestionAnswer());
  }

  @Test
  public void setMissingValueTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assignment.setMissingValue("Missing");
    assertEquals("Missing", assignment.getMissingValue());
  }

  @Test
  public void getTargetTest() {
    SensorAssignment assignment = getNoIdFalsesAssignment();
    assertEquals("Data File: Sensor", assignment.getTarget());
  }
}
