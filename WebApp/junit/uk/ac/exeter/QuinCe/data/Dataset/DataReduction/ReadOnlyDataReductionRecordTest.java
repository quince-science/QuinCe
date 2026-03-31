package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;

/**
 * Tests for the {@link ReadOnlyDataReductionRecord} class.
 *
 * @author Steve Jones
 *
 */
public class ReadOnlyDataReductionRecordTest extends BaseTest {

  private static final long MEASUREMENT_ID = 1234L;

  private static final long VARIABLE_ID = 12L;

  private static final String PARAM = "Param1";

  private static final Double PARAM_VALUE = 21D;

  private static final String BASE_MESSAGE = "Base Message";

  private Map<String, Double> makeCalculationValues() {
    Map<String, Double> values = new HashMap<String, Double>();
    values.put(PARAM, PARAM_VALUE);
    return values;
  }

  private ReadOnlyDataReductionRecord makeRecord(Flag qcFlag) {
    return ReadOnlyDataReductionRecord.makeRecord(MEASUREMENT_ID, VARIABLE_ID,
      flagScheme, makeCalculationValues(), qcFlag, BASE_MESSAGE);
  }

  /**
   * Test building a record.
   *
   * @throws DataReductionException
   */
  @Test
  public void constructorSingleMessage() throws DataReductionException {
    ReadOnlyDataReductionRecord record = ReadOnlyDataReductionRecord.makeRecord(
      MEASUREMENT_ID, VARIABLE_ID, flagScheme, makeCalculationValues(),
      flagScheme.getBadFlag(), "Single Message");

    assertEquals(MEASUREMENT_ID, record.getMeasurementId(),
      "Mismatched measurement ID");
    assertEquals(VARIABLE_ID, record.getVariableId(), "Mismatched variable ID");
    assertEquals(PARAM_VALUE, record.getCalculationValue(PARAM),
      "Mismatched parameter value");
    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(1, record.getQCMessages().size());
    assertEquals("Single Message", record.getQCMessages().iterator().next());
    assertFalse(record.isDirty(), "Dirty flag set");
  }

  /**
   * Test building a record with multiple QC messages.
   */
  @Test
  public void constructorMultipleMessages() {
    ReadOnlyDataReductionRecord record = ReadOnlyDataReductionRecord.makeRecord(
      MEASUREMENT_ID, VARIABLE_ID, flagScheme, makeCalculationValues(),
      flagScheme.getBadFlag(), "First Message;Second Message");

    assertEquals(2, record.getQCMessages().size());
  }

  /**
   * Test that values can't be set.
   */
  @Test
  public void setValueTest() {
    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getGoodFlag());

    assertThrows(NotImplementedException.class, () -> {
      record.put(PARAM, 22D);
    });
  }

  /**
   * Test overriding QC with lesser QC flag and a single message.
   *
   * @throws DataReductionException
   */
  @Test
  public void setLesserQCSingleMessage() throws DataReductionException {
    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getBadFlag());

    record.setQc(IcosFlagScheme.QUESTIONABLE_FLAG, "New Message");

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(1, record.getQCMessages().size(), "Mismatched message count");
    assertEquals(BASE_MESSAGE, record.getQCMessages().iterator().next(),
      "Mismatched message");
    assertFalse(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding QC with lesser QC flag and multiple messages.
   *
   * @throws DataReductionException
   */
  @Test
  public void setLesserQCMultipleMessages() throws DataReductionException {
    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getBadFlag());

    record.setQc(IcosFlagScheme.QUESTIONABLE_FLAG,
      Arrays.asList(new String[] { "New Message 1", "New Message 2" }));

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(1, record.getQCMessages().size(), "Mismatched message count");
    assertEquals(BASE_MESSAGE, record.getQCMessages().iterator().next(),
      "Mismatched message");
    assertFalse(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding QC with equal QC flag and a single message.
   *
   * @throws DataReductionException
   */
  @Test
  public void setEqualQCSingleMessage() throws DataReductionException {

    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getBadFlag());
    record.setQc(flagScheme.getBadFlag(), "New Message");

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(2, record.getQCMessages().size(), "Mismatched message count");
    assertTrue(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding QC with equal QC flag and multiple messages.
   *
   * @throws DataReductionException
   */
  @Test
  public void setEqualQCMultipleMessages() throws DataReductionException {

    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getBadFlag());

    record.setQc(flagScheme.getBadFlag(),
      Arrays.asList(new String[] { "New Message 1", "New Message 2" }));

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(3, record.getQCMessages().size(), "Mismatched message count");
    assertTrue(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding QC with more significant QC flag and a single message.
   *
   * @throws DataReductionException
   */
  @Test
  public void setMoreSignificantQCSingleMessage()
    throws DataReductionException {

    ReadOnlyDataReductionRecord record = makeRecord(
      IcosFlagScheme.QUESTIONABLE_FLAG);
    record.setQc(flagScheme.getBadFlag(), "New Message");

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(1, record.getQCMessages().size(), "Mismatched message count");
    assertEquals("New Message", record.getQCMessages().iterator().next(),
      "Mismatched message");
    assertTrue(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding QC with more significant QC flag and multiple messages.
   *
   * @throws DataReductionException
   */
  @Test
  public void setMoreSignificantQCMultipleMessages()
    throws DataReductionException {

    ReadOnlyDataReductionRecord record = makeRecord(
      IcosFlagScheme.QUESTIONABLE_FLAG);

    record.setQc(flagScheme.getBadFlag(),
      Arrays.asList(new String[] { "New Message 1", "New Message 2" }));

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(2, record.getQCMessages().size(), "Mismatched message count");
    assertTrue(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding QC with an empty message.
   */
  @Test
  public void overrideQCEmptyMessageTest() {
    ReadOnlyDataReductionRecord record = makeRecord(
      IcosFlagScheme.QUESTIONABLE_FLAG);

    assertThrows(DataReductionException.class, () -> {
      record.setQc(flagScheme.getBadFlag(), Arrays.asList(new String[] { "" }));
    });
  }

  /**
   * Test overriding an existing overridden QC with a lesser QC
   *
   * @throws DataReductionException
   */
  @Test
  public void overrideOverriddenQCWithLesserTest()
    throws DataReductionException {
    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getGoodFlag());

    record.setQc(flagScheme.getBadFlag(), "Bad Message");
    record.setQc(IcosFlagScheme.QUESTIONABLE_FLAG, "Questionable Message");

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(1, record.getQCMessages().size());
    assertEquals("Bad Message", record.getQCMessages().iterator().next());
    assertTrue(record.isDirty(), "Mismatched dirty flag");

  }

  /**
   * Test overriding an existing overridden QC with an equal QC
   *
   * @throws DataReductionException
   */
  @Test
  public void overrideOverriddenQCWithEqualTest()
    throws DataReductionException {
    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getGoodFlag());

    record.setQc(flagScheme.getBadFlag(), "Bad Message");
    record.setQc(flagScheme.getBadFlag(), "Bad Message 2");

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(2, record.getQCMessages().size());
    assertTrue(record.isDirty(), "Mismatched dirty flag");
  }

  /**
   * Test overriding an existing overridden QC with a more significant QC
   *
   * @throws DataReductionException
   */
  @Test
  public void overrideOverriddenQCWithMoreSignificantTest()
    throws DataReductionException {
    ReadOnlyDataReductionRecord record = makeRecord(flagScheme.getGoodFlag());

    record.setQc(IcosFlagScheme.QUESTIONABLE_FLAG, "Questionable Message");
    record.setQc(flagScheme.getBadFlag(), "Bad Message");

    assertEquals(flagScheme.getBadFlag(), record.getQCFlag(),
      "Mismatched QC Flag");
    assertEquals(1, record.getQCMessages().size());
    assertEquals("Bad Message", record.getQCMessages().iterator().next());
    assertTrue(record.isDirty(), "Mismatched dirty flag");
  }
}
