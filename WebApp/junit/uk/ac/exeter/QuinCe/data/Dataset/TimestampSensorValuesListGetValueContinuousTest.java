package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Test the {@link SensorValuesList#getValue(LocalDateTime, boolean)} method for
 * continuous measurements.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class TimestampSensorValuesListGetValueContinuousTest
  extends TestSetTest {

  protected static final long DATASET_ID = 1L;

  @BeforeEach
  public void setUp() {
    initResourceManager();
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  @Override
  protected String getTestSetName() {
    return "SensorValuesListGetValueContinuous";
  }

  protected TimeCoordinate makeCoordinate(int minute)
    throws CoordinateException {
    return new TimeCoordinate(DATASET_ID,
      LocalDateTime.of(2023, 1, 1, 12, minute, 0));
  }

  protected SensorValue makeSensorValue(int minute, char flagChar)
    throws InvalidFlagException, CoordinateException {
    return new SensorValue((long) minute, DATASET_ID, flagScheme, 1L,
      makeCoordinate(minute), String.valueOf(minute),
      new AutoQCResult(flagScheme), flagScheme.getFlag(flagChar),
      String.valueOf(flagChar));
  }

  protected void makeSensorValues(DatasetSensorValues allSensorValues,
    TestSetLine line, int column, int firstMinute)
    throws RecordNotFoundException, InvalidFlagException, CoordinateException {
    char[] flags = line.getStringField(column, false).toCharArray();

    int minute = firstMinute;
    for (char flag : flags) {
      if (flag != 'N') {
        allSensorValues.add(makeSensorValue(minute, flag));
      }
      minute++;
    }
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getValueTest(TestSetLine line) throws Exception {

    Instrument instrument = InstrumentDB.getInstrument(getConnection(),
      DATASET_ID);
    DataSet dataSet = Mockito.mock(DataSet.class);
    Mockito.when(dataSet.getId()).thenReturn(DATASET_ID);
    DatasetSensorValues allSensorValues = new DatasetSensorValues(dataSet);
    Mockito.when(allSensorValues.getInstrument()).thenReturn(instrument);
    Mockito.when(allSensorValues.getFlagScheme()).thenReturn(flagScheme);

    buildSensorValues(allSensorValues, line);

    TimestampSensorValuesList list = (TimestampSensorValuesList) allSensorValues
      .getColumnValues(1L);

    TimestampSensorValuesListOutput value = getValue(list, line);

    String expectedValueString = line.getStringField(getExpectedValueCol(),
      true);
    if (null == expectedValueString) {
      assertNull(value);
    } else {
      Coordinate expectedStartTime = makeCoordinate(
        line.getIntField(getExpectedStartTimeCol()));

      assertEquals(expectedStartTime, value.getStartTime(),
        "Start time incorrect");

      Coordinate expectedEndTime = makeCoordinate(
        line.getIntField(getExpectedEndTimeCol()));

      assertEquals(expectedEndTime, value.getEndTime(), "End time incorrect");

      Coordinate expectedNominalTime = makeCoordinate(
        line.getIntField(getExpectedNominalTimeCol()));

      assertEquals(expectedNominalTime, value.getNominalTime(),
        "Nominal time incorrect");

      Double expectedValue = line.getDoubleField(getExpectedValueCol());
      assertEquals(expectedValue, value.getDoubleValue(), 0.004,
        "Value incorrect");

      Flag expectedFlag = flagScheme
        .getFlag((line.getCharField(getExpectedFlagCol())));
      assertEquals(expectedFlag, value.getQCFlag());

      String expectedUsedValueIds = line
        .getStringField(getExpectedUsedValuesCol(), false);

      String actualUsedValueIds = StringUtils.collectionToDelimited(value
        .getSourceSensorValues().stream().map(v -> v.getId()).sorted().toList(),
        ";");

      assertEquals(expectedUsedValueIds, actualUsedValueIds,
        "Used values incorrect");

      boolean expectedInterpolatesAroundFlags = line
        .getBooleanField(getInterpolatesAroundFlagCol());

      assertEquals(expectedInterpolatesAroundFlags,
        value.interpolatesAroundFlags(), "Interpolates Around Flags incorrect");
    }
  }

  protected TimestampSensorValuesListOutput getValue(
    TimestampSensorValuesList list, TestSetLine line)
    throws SensorValuesListException, CoordinateException {
    return (TimestampSensorValuesListOutput) list.getValue(
      makeCoordinate(line.getIntField(getRequestedMinuteCol())),
      getInterpolationAllowed(line));
  }

  protected boolean getInterpolationAllowed(TestSetLine line) {
    return line.getBooleanField(getAllowInterpolationCol());
  }

  protected int getExpectedUsedValuesCol() {
    return 8;
  }

  protected int getExpectedFlagCol() {
    return 7;
  }

  protected int getExpectedNominalTimeCol() {
    return 5;
  }

  protected int getExpectedEndTimeCol() {
    return 4;
  }

  protected int getExpectedStartTimeCol() {
    return 3;
  }

  protected int getExpectedValueCol() {
    return 6;
  }

  protected int getRequestedMinuteCol() {
    return 1;
  }

  protected int getInterpolatesAroundFlagCol() {
    return 9;
  }

  protected int getAllowInterpolationCol() {
    return 2;
  }

  protected void buildSensorValues(DatasetSensorValues allSensorValues,
    TestSetLine line)
    throws RecordNotFoundException, InvalidFlagException, CoordinateException {

    makeSensorValues(allSensorValues, line, 0, 11);
  }
}
