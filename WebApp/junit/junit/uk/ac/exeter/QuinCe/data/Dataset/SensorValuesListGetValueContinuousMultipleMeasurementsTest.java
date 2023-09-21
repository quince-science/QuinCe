package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Test the {@link SensorValuesList#getValue(java.time.LocalDateTime, boolean)
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SensorValuesListGetValueContinuousMultipleMeasurementsTest
  extends TestSetTest {

  private static final int VALUE_FLAGS_COL = 0;

  private static final int REQUESTED_MINUTE_COL = 1;

  private static final int EXPECTED_VALUE_COL = 2;

  private static final int EXPECTED_FLAG_COL = 3;

  private static final int USED_VALUES_COL = 4;

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
    return "SensorValuesListGetValueContinuousMultipleMeasurements";
  }

  private LocalDateTime makeTime(int minute) {
    return LocalDateTime.of(2023, 1, 1, 12, minute, 0);
  }

  private SensorValue makeSensorValue(int minute, char flagChar)
    throws InvalidFlagException {
    return new SensorValue((long) minute, 1L, 1L, makeTime(minute),
      String.valueOf(minute), new AutoQCResult(), new Flag(flagChar),
      String.valueOf(flagChar));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getValueTest(TestSetLine line) throws Exception {

    // Build the SensorValues
    DatasetSensorValues allSensorValues = new DatasetSensorValues(
      InstrumentDB.getInstrument(getConnection(), 1L));

    char[] elevenToTwentyThreeFlags = line
      .getStringField(VALUE_FLAGS_COL, false).toCharArray();

    int minute = 11;
    for (char flag : elevenToTwentyThreeFlags) {
      if (flag != 'N') {
        allSensorValues.add(makeSensorValue(minute, flag));
      }
      minute++;
    }

    SensorValuesList list = allSensorValues.getColumnValues(1L);

    SensorValuesListValue value = list
      .getValue(makeTime(line.getIntField(REQUESTED_MINUTE_COL)));

    String expectedValue = line.getStringField(EXPECTED_VALUE_COL, true);
    if (null == expectedValue) {
      assertNull(value);
    } else {
      assertEquals(line.getDoubleField(EXPECTED_VALUE_COL),
        value.getDoubleValue());

      Flag expectedFlag = new Flag(line.getCharField(EXPECTED_FLAG_COL));
      assertEquals(expectedFlag, value.getQCFlag());

      List<Long> expectedUsedValueIds = StringUtils
        .delimitedToLongList(line.getStringField(USED_VALUES_COL, false), ';');

      List<Long> actualUsedValueIds = value.getSourceSensorValues().stream()
        .map(v -> v.getId()).sorted().toList();

      assertTrue(listsEqual(expectedUsedValueIds, actualUsedValueIds),
        "Used SensorValues incorrect");
    }
  }
}
