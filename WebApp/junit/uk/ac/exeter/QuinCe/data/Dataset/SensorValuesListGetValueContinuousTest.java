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

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Test the {@link SensorValuesList#getValue(LocalDateTime, boolean)} method for
 * continuous measurements.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SensorValuesListGetValueContinuousTest extends TestSetTest {

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

  protected LocalDateTime makeTime(int minute) {
    return LocalDateTime.of(2023, 1, 1, 12, minute, 0);
  }

  protected SensorValue makeSensorValue(int minute, char flagChar)
    throws InvalidFlagException {
    return new SensorValue((long) minute, 1L, 1L, makeTime(minute),
      String.valueOf(minute), new AutoQCResult(), new Flag(flagChar),
      String.valueOf(flagChar));
  }

  protected void makeSensorValues(DatasetSensorValues allSensorValues,
    TestSetLine line, int column, int firstMinute)
    throws RecordNotFoundException, InvalidFlagException {
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

    DatasetSensorValues allSensorValues = new DatasetSensorValues(
      InstrumentDB.getInstrument(getConnection(), 1L));

    buildSensorValues(allSensorValues, line);

    SensorValuesList list = allSensorValues.getColumnValues(1L);

    SensorValuesListOutput value = getValue(list, line);

    String expectedValueString = line.getStringField(getExpectedValueCol(),
      true);
    if (null == expectedValueString) {
      assertNull(value);
    } else {
      LocalDateTime expectedStartTime = makeTime(
        line.getIntField(getExpectedStartTimeCol()));

      assertEquals(expectedStartTime, value.getStartTime(),
        "Start time incorrect");

      LocalDateTime expectedEndTime = makeTime(
        line.getIntField(getExpectedEndTimeCol()));

      assertEquals(expectedEndTime, value.getEndTime(), "End time incorrect");

      LocalDateTime expectedNominalTime = makeTime(
        line.getIntField(getExpectedNominalTimeCol()));

      assertEquals(expectedNominalTime, value.getNominalTime(),
        "Nominal time incorrect");

      Double expectedValue = line.getDoubleField(getExpectedValueCol());
      assertEquals(expectedValue, value.getDoubleValue(), 0.004,
        "Value incorrect");

      Flag expectedFlag = new Flag(line.getCharField(getExpectedFlagCol()));
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

  protected SensorValuesListOutput getValue(SensorValuesList list,
    TestSetLine line) throws SensorValuesListException {
    return list.getValue(makeTime(line.getIntField(getRequestedMinuteCol())),
      true);
  }

  protected int getExpectedUsedValuesCol() {
    return 7;
  }

  protected int getExpectedFlagCol() {
    return 6;
  }

  protected int getExpectedNominalTimeCol() {
    return 4;
  }

  protected int getExpectedEndTimeCol() {
    return 3;
  }

  protected int getExpectedStartTimeCol() {
    return 2;
  }

  protected int getExpectedValueCol() {
    return 5;
  }

  protected int getRequestedMinuteCol() {
    return 1;
  }

  protected int getInterpolatesAroundFlagCol() {
    return 8;
  }

  protected void buildSensorValues(DatasetSensorValues allSensorValues,
    TestSetLine line) throws RecordNotFoundException, InvalidFlagException {

    makeSensorValues(allSensorValues, line, 0, 11);
  }
}
