package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;

@TestInstance(Lifecycle.PER_CLASS)
public class SensorValuePositionQCTest extends TestSetTest {

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int AUTO_QC_COL = 0;

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int USER_QC_COL = 1;

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int ALREADY_CONTAINS_POS_MESSAGE_COL = 2;

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int POSITION_QC_COL = 3;

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int RESULT_FLAG_COL = 4;

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int RESULT_CONTAINS_POS_MESSAGE_COL = 5;

  /**
   * A column in the Test Set file for {@link #positionQCTests(TestSetLine)}.
   */
  private static final int DIRTY_COL = 6;

  @ParameterizedTest
  @MethodSource("getLines")
  public void positionQCTests(TestSetLine line) throws Exception {

    SensorValue sensorValue = makeSensorValue(line);
    sensorValue.setPositionQC(new Flag(line.getIntField(POSITION_QC_COL)),
      "Position QC");

    Flag expectedFlag = new Flag(line.getIntField(RESULT_FLAG_COL));
    boolean resultContainsPositionMessage = line
      .getBooleanField(RESULT_CONTAINS_POS_MESSAGE_COL);

    assertEquals(expectedFlag, sensorValue.getUserQCFlag(), "Incorrect flag");

    if (resultContainsPositionMessage) {
      assertTrue(
        sensorValue.getUserQCMessage().contains(Measurement.POSITION_QC_PREFIX),
        "Missing Position message");
    } else {
      assertFalse(
        sensorValue.getUserQCMessage().contains(Measurement.POSITION_QC_PREFIX),
        "Position message present but shoudln't be");
    }

    assertEquals(line.getBooleanField(DIRTY_COL), sensorValue.isDirty());
  }

  private SensorValue makeSensorValue(TestSetLine line)
    throws InvalidFlagException {

    AutoQCResult autoQC = makeAutoQCResult(line.getIntField(AUTO_QC_COL));
    Flag userFlag = new Flag(line.getIntField(USER_QC_COL));
    String userMessage = "";
    if (userFlag.equals(Flag.QUESTIONABLE)) {
      userMessage = "User Questionable";
    } else if (userFlag.equals(Flag.BAD)) {
      userMessage = "User Bad";
    }

    if (line.getBooleanField(ALREADY_CONTAINS_POS_MESSAGE_COL)) {
      if (userMessage.length() > 0) {
        userMessage += ';';
      }

      userMessage += Measurement.POSITION_QC_PREFIX + "Position comment";
    }

    return new SensorValue(1L, 1L, 1L, LocalDateTime.now(), "0", autoQC,
      userFlag, userMessage);
  }

  private AutoQCResult makeAutoQCResult(int flag) throws InvalidFlagException {
    AutoQCResult mock = Mockito.mock(AutoQCResult.class);
    Mockito.when(mock.getOverallFlag()).thenReturn(new Flag(flag));
    return mock;
  }

  @Override
  protected String getTestSetName() {
    return "SensorValue_PositionQC";
  }
}
