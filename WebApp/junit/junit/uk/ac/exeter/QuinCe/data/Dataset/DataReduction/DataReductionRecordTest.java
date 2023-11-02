package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Tests for the {@link DataReductionRecord} class.
 */
public class DataReductionRecordTest extends BaseTest {

  private static final long MEASUREMENT_ID = 1234L;

  private static final long VARIABLE_ID = 12L;

  private static final String PARAM_1 = "Param1";

  private static final String PARAM_2 = "Param2";

  private Measurement makeMeasurement() {
    Measurement measurement = Mockito.mock(Measurement.class);
    Mockito.when(measurement.getId()).thenReturn(MEASUREMENT_ID);
    return measurement;
  }

  private Variable makeVariable() {
    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(VARIABLE_ID);
    return variable;
  }

  private List<String> makeParameterNames() {
    return Arrays.asList(new String[] { PARAM_1, PARAM_2 });
  }

  private DataReductionRecord makeEmptyRecord() {
    return new DataReductionRecord(makeMeasurement(), makeVariable(),
      makeParameterNames());
  }

  private DataReductionRecord makeQuestionableRecord()
    throws DataReductionException {
    DataReductionRecord record = makeEmptyRecord();
    record.setQc(Flag.QUESTIONABLE, "Simple Message");
    return record;
  }

  /**
   * Test that the base constructor (with no calculation values) sets up the
   * object correctly.
   *
   * @throws DataReductionException
   */
  @Test
  public void noValuesConstructorTest() throws DataReductionException {
    DataReductionRecord record = new DataReductionRecord(makeMeasurement(),
      makeVariable(), makeParameterNames());

    assertEquals(MEASUREMENT_ID, record.getMeasurementId(),
      "Measurement ID does not match");
    assertEquals(VARIABLE_ID, record.getVariableId(),
      "Variable ID does not match");
    assertNull(record.getCalculationValue(PARAM_1),
      "PARAM_1 should not be set");
    assertNull(record.getCalculationValue(PARAM_2),
      "PARAM_2 should not be set");
    assertEquals(Flag.ASSUMED_GOOD, record.getQCFlag(), "QC Flag incorrect");
    assertEquals(0, record.getQCMessages().size(), "QC messages not empty");
  }

  /**
   * Test that a parameter can be set and retrieved.
   *
   * @throws DataReductionException
   */
  @Test
  public void setParameterTest() throws DataReductionException {
    DataReductionRecord record = makeEmptyRecord();

    Double value = 23.4D;
    record.put(PARAM_1, value);
    assertEquals(value, record.getCalculationValue(PARAM_1),
      "Param not set correctly");
  }

  /**
   * Try to set a non-existent calculation parameter.
   */
  @Test
  public void setBadParameterTest() {
    DataReductionRecord record = makeEmptyRecord();

    assertThrows(DataReductionException.class, () -> {
      record.put("BadParam", 43D);
    });
  }

  /**
   * Try to get a non-existent calculation parameter.
   */
  @Test
  public void getBadParameterTest() {
    DataReductionRecord record = makeEmptyRecord();

    assertNull(record.getCalculationValue("BadParam"));
  }

  /**
   * Test setting basic QC information.
   *
   * @throws DataReductionException
   */
  @Test
  public void basicQCTest() throws DataReductionException {
    DataReductionRecord record = makeEmptyRecord();

    record.setQc(Flag.BAD, "Bad");

    assertEquals(Flag.BAD, record.getQCFlag(), "QC Flag incorrect");
    assertEquals(1, record.getQCMessages().size());
    assertEquals("Bad", record.getQCMessages().iterator().next());
  }

  /**
   * Test setting multiple QC messages.
   *
   * @throws DataReductionException
   */
  @Test
  public void multipleMessagesQCTest() throws DataReductionException {
    DataReductionRecord record = makeEmptyRecord();

    List<String> messages = Arrays
      .asList(new String[] { "Message 1", "Message 2" });

    record.setQc(Flag.BAD, messages);

    assertEquals(2, record.getQCMessages().size(),
      "Mismatched message list size");
  }

  /**
   * Test that empty QC messages are not accepted.
   *
   * @param message
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void setInvalidEmptyQCTest(String message) {
    DataReductionRecord record = makeEmptyRecord();

    assertThrows(DataReductionException.class, () -> {
      record.setQc(Flag.BAD, message);
    });
  }

  /**
   * Test that empty QC messages are accepted for {@link Flag#NO_QC}.
   *
   * @param message
   * @throws DataReductionException
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void setNoQCEmptyQCTest(String message) throws DataReductionException {
    DataReductionRecord record = makeEmptyRecord();

    record.setQc(Flag.NO_QC, message);
    assertEquals(Flag.NO_QC, record.getQCFlag(), "Incorrect QC flag");
    assertEquals(0, record.getQCMessages().size());
  }

  /**
   * Test that setting less significant QC has no effect.
   *
   * @throws DataReductionException
   */
  @Test
  public void lessSignificantQCTest() throws DataReductionException {
    DataReductionRecord record = makeQuestionableRecord();
    record.setQc(Flag.GOOD, "Message");
    assertEquals(Flag.QUESTIONABLE, record.getQCFlag(), "Mismatched QC Flag");
  }

  /**
   * Test that setting a flag of equal significance adds the message.
   *
   * @throws DataReductionException
   */
  @Test
  public void equalQCTest() throws DataReductionException {
    DataReductionRecord record = makeQuestionableRecord();
    record.setQc(Flag.QUESTIONABLE, "New Message");

    assertEquals(Flag.QUESTIONABLE, record.getQCFlag(), "Mismatched QC Flag");
    assertEquals(2, record.getQCMessages().size(), "QC Messages not combined");
  }

  /**
   * Test that more significant QC overrides existing QC.
   *
   * @throws DataReductionException
   */
  @Test
  public void moreSignificantQCTest() throws DataReductionException {
    DataReductionRecord record = makeQuestionableRecord();
    record.setQc(Flag.BAD, "Bad Message");

    assertEquals(Flag.BAD, record.getQCFlag());
    assertEquals(1, record.getQCMessages().size());
    assertEquals("Bad Message", record.getQCMessages().iterator().next());
  }
}
