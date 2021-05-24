package junit.uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Tests that check the ability of flags set on Position values to be propagated
 * to other {@link SensorValue}s.
 *
 * <p>
 * When a flag is placed on a position value, it must be copied to all other
 * {@link SensorValue}s from the time of the position value (inclusive) up to
 * the next position value (exclusive). Candidate {@link SensorValue}s are
 * identified from the current {@link DatasetSensorValues} by their column IDs,
 * all of which are passed into the {@link PositionQCRoutine}.
 * </p>
 *
 * <p>
 * These tests use a simple dataset with a number of position values and
 * {@link SensorValue}s at different times:
 * </p>
 *
 * <table>
 * <th>
 * <td>Time</td>
 * <td>Position</td>
 * <td>Sensor Value</td>
 * <th>
 * <tr>
 * <td>T1</td>
 * <td>P1</td>
 * <td>&nbsp;</td>
 * <tr>
 * <tr>
 * <td>T2</td>
 * <td>&nbsp;</td>
 * <td>S1</td>
 * <tr>
 * <tr>
 * <td>T3</td>
 * <td>P2</td>
 * <td>S2</td>
 * <tr>
 * <tr>
 * <td>T4</td>
 * <td>P3</td>
 * <td>S3</td>
 * <tr>
 * </table>
 *
 * <p>
 * P1 will always be an invalid position, and P2/P3 will be valid. The tests
 * will selectively remove position values P2 and/or P3 and observe how flags
 * are propagated to the {@link SensorValue}s.
 * </p>
 *
 * @author Steve Jones
 *
 */
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
  "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
@TestInstance(Lifecycle.PER_CLASS)
public class PositionQCRoutinePropagationTest extends BaseTest {

  private LocalDateTime makeTime(int second) {
    return LocalDateTime.of(2000, 1, 1, 0, 0, second);
  }

  private DatasetSensorValues makeSensorValues() throws Exception {
    Instrument instrument = InstrumentDB.getInstrument(getDataSource(), 1);

    DatasetSensorValues result = new DatasetSensorValues(instrument);

    SensorValue s1 = new SensorValue(1L, 1L, makeTime(2), "1");
    result.add(s1);

    SensorValue s2 = new SensorValue(1L, 1L, makeTime(3), "2");
    result.add(s2);

    SensorValue s3 = new SensorValue(1L, 1L, makeTime(4), "3");
    result.add(s3);

    return result;
  }

  private List<SensorValue> makeLons(boolean include2, boolean include3) {

    List<SensorValue> result = new ArrayList<SensorValue>(3);

    result
      .add(new SensorValue(1L, SensorType.LONGITUDE_ID, makeTime(1), "999"));

    if (include2) {
      result
        .add(new SensorValue(1L, SensorType.LONGITUDE_ID, makeTime(3), "0"));
    }

    if (include3) {
      result
        .add(new SensorValue(1L, SensorType.LONGITUDE_ID, makeTime(4), "0"));
    }

    return result;
  }

  private List<SensorValue> makeLats(boolean include2, boolean include3) {

    List<SensorValue> result = new ArrayList<SensorValue>(3);

    result.add(new SensorValue(1L, SensorType.LATITUDE_ID, makeTime(1), "999"));

    if (include2) {
      result.add(new SensorValue(1L, SensorType.LATITUDE_ID, makeTime(3), "0"));
    }

    if (include3) {
      result.add(new SensorValue(1L, SensorType.LATITUDE_ID, makeTime(4), "0"));
    }

    return result;
  }

  private List<Long> getDataColumnIds() {
    return Arrays.asList(new Long[] { 1L });
  }

  private Flag getSensorValueFlag(DatasetSensorValues sensorValues,
    int second) {
    return sensorValues.get(makeTime(second)).get(1L).getUserQCFlag();
  }

  @BeforeEach
  public void init() {
    initResourceManager();
  }

  @Test
  public void allPositionsTest() throws Exception {

    DatasetSensorValues sensorValues = makeSensorValues();

    PositionQCRoutine routine = new PositionQCRoutine(makeLons(true, true),
      makeLats(true, true), getDataColumnIds(), sensorValues);

    routine.qc(null);

    // Only the first SensorValue should have been flagged
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 2));
    assertEquals(Flag.ASSUMED_GOOD, getSensorValueFlag(sensorValues, 3));
    assertEquals(Flag.ASSUMED_GOOD, getSensorValueFlag(sensorValues, 4));
  }

  @Test
  public void onlyFirstPositionTest() throws Exception {

    DatasetSensorValues sensorValues = makeSensorValues();

    PositionQCRoutine routine = new PositionQCRoutine(makeLons(false, false),
      makeLats(false, false), getDataColumnIds(), sensorValues);

    routine.qc(null);

    // Only the first SensorValue should have been flagged
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 2));
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 3));
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 4));
  }

  @Test
  public void twoPositionsTest() throws Exception {

    DatasetSensorValues sensorValues = makeSensorValues();

    PositionQCRoutine routine = new PositionQCRoutine(makeLons(false, true),
      makeLats(false, true), getDataColumnIds(), sensorValues);

    routine.qc(null);

    // Only the first SensorValue should have been flagged
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 2));
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 3));
    assertEquals(Flag.ASSUMED_GOOD, getSensorValueFlag(sensorValues, 4));
  }
}
