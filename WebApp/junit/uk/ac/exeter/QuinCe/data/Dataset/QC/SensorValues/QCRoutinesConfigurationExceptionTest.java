package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

public class QCRoutinesConfigurationExceptionTest extends BaseTest {

  @Test
  public void fileMessageConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", "message");

    assertEquals("FILE file: message", e.getMessage());
    assertEquals("file", e.getFile());
    assertNull(e.getItemName());
    assertEquals(-1L, e.getLine());
    assertNull(e.getCause());
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileLineMessageConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", 100L, "message");

    assertEquals("FILE file, LINE 100: message", e.getMessage());
    assertEquals("file", e.getFile());
    assertNull(e.getItemName());
    assertEquals(100L, e.getLine());
    assertNull(e.getCause());
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileNegativeLineMessageConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", -1L, "message");

    assertEquals("FILE file: message", e.getMessage());
    assertEquals("file", e.getFile());
    assertNull(e.getItemName());
    assertEquals(-1L, e.getLine());
    assertNull(e.getCause());
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileNormaliseNegativeLineMessageConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", -100L, "message");

    assertEquals("FILE file: message", e.getMessage());
    assertEquals("file", e.getFile());
    assertNull(e.getItemName());
    assertEquals(-1L, e.getLine());
    assertNull(e.getCause());
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileLineMessageCauseConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", 100L, "message", new Exception("cause"));

    assertEquals("FILE file, LINE 100: message ->\ncause", e.getMessage());
    assertEquals("file", e.getFile());
    assertNull(e.getItemName());
    assertEquals(100L, e.getLine());
    assertTrue(throwableWithMessage(e.getCause(), "cause"));
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileItemLineMessageConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", "item", 100L, "message");

    assertEquals("FILE file, ITEM item, LINE 100: message", e.getMessage());
    assertEquals("file", e.getFile());
    assertEquals("item", e.getItemName());
    assertEquals(100L, e.getLine());
    assertNull(e.getCause());
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileItemLineMessageCauseConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", "item", 100L, "message", new Exception("cause"));

    assertEquals("FILE file, ITEM item, LINE 100: message ->\ncause",
      e.getMessage());
    assertEquals("file", e.getFile());
    assertEquals("item", e.getItemName());
    assertEquals(100L, e.getLine());
    assertTrue(throwableWithMessage(e.getCause(), "cause"));
    assertEquals("message", e.getMessageOnly());
  }

  @Test
  public void fileMessageCauseConstructorTest() {
    QCRoutinesConfigurationException e = new QCRoutinesConfigurationException(
      "file", "message", new Exception("cause"));

    assertEquals("FILE file: message ->\ncause", e.getMessage());
    assertEquals("file", e.getFile());
    assertNull(e.getItemName());
    assertEquals(-1L, e.getLine());
    assertTrue(throwableWithMessage(e.getCause(), "cause"));
    assertEquals("message", e.getMessageOnly());
  }
}
