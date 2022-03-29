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

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

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
 * <caption>Presence of positions and sensor values</caption>
 * <tr>
 * <td><b>Time</b></td>
 * <td><b>Position</b></td>
 * <td><b>Sensor Value</b></td>
 * </tr>
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
 * <p>
 * All times in the dataset are within the same minute, so times are specified
 * purely by a seconds values.
 * </p>
 *
 * @author Steve Jones
 *
 */
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
  "resources/sql/testbase/instrument", "resources/sql/testbase/variable" })
@TestInstance(Lifecycle.PER_CLASS)
public class PositionQCRoutinePropagationTest extends PositionQCTestBase {

  /**
   * Create a {@link LocalDateTime} object for the dataset with the specified
   * seconds value.
   *
   * <p>
   * All times in the test dataset are within the same minute so this is
   * sufficient for all tests.
   * </p>
   *
   * @param second
   *          The seconds value.
   * @return The {@link LocalDateTime} object.
   */
  private LocalDateTime makeTime(int second) {
    return LocalDateTime.of(2000, 1, 1, 0, 0, second);
  }

  /**
   * Create the {@link SensorValue}s for the test dataset.
   *
   * @return The {@link SensorValue}s.
   * @throws Exception
   *           If an internal error occurs.
   */
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

  /**
   * Create the longitude {@link SensorValue}s for the test dataset.
   *
   * <p>
   * The second and third longitudes can be included or not using the
   * {@code include2} and {@code include3} parameters. There is an assumption
   * that if {@code include3} is {@code true} then so is {@code include2}, but
   * it is not enforced. The method's behaviour and results of related tests is
   * undefined in this case.
   * </p>
   *
   * <p>
   * The three longitude values provided by this method are:
   * <ol start="0">
   * <li>999 (invalid)</li>
   * <li>0</li>
   * <li>0</li>
   * </ol>
   * </p>
   *
   * @param include2
   *          Indicates whether or not the second longitude should be included
   *          in the result.
   * @param include3
   *          Indicates whether or not the third longitude should be included in
   *          the result. Assumes that {@code include2} is {@code true}.
   * @return The longitudes {@link SensorValue}s.
   */
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

  /**
   * Create the latitude {@link SensorValue}s for the test dataset.
   *
   * <p>
   * The second and third latitudes can be included or not using the
   * {@code include2} and {@code include3} parameters. There is an assumption
   * that if {@code include3} is {@code true} then so is {@code include2}, but
   * it is not enforced. The method's behaviour and results of related tests is
   * undefined in this case.
   * </p>
   *
   * <p>
   * The three latitude values provided by this method are:
   * <ol start="0">
   * <li>999 (invalid)</li>
   * <li>0</li>
   * <li>0</li>
   * </ol>
   * </p>
   *
   * @param include2
   *          Indicates whether or not the second latitude should be included in
   *          the result.
   * @param include3
   *          Indicates whether or not the third latitude should be included in
   *          the result. Assumes that {@code include2} is {@code true}.
   * @return The latitudes {@link SensorValue}s.
   */
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

  @Override
  protected List<Long> makeDataColumnIds() {
    return Arrays.asList(new Long[] { 1L });
  }

  /**
   * Get the QC flag for the {@link SensorValue} at the specified time.
   *
   * @param sensorValues
   *          The complete set of {@link SensorValue}s.
   * @param second
   *          The second of the target {@link SensorValue}'s time.
   * @return The value's QC flag.
   */
  private Flag getSensorValueFlag(DatasetSensorValues sensorValues,
    int second) {
    return sensorValues.get(makeTime(second)).get(1L).getUserQCFlag();
  }

  /**
   * Initialise the {@link ResourceManager}.
   */
  @BeforeEach
  public void init() {
    initResourceManager();
  }

  /**
   * Test the propagated QC flags when all positions are defined.
   *
   * @throws Exception
   *           If an internal error occurs.
   */
  @Test
  public void allPositionsTest() throws Exception {

    DatasetSensorValues sensorValues = makeSensorValues();

    PositionQCRoutine routine = new PositionQCRoutine(makeLons(true, true),
      makeLats(true, true), makeInstrument(), sensorValues,
      makeEmptyRunTypes());

    routine.qc(null);

    // Only the first SensorValue should have been flagged
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 2));
    assertEquals(Flag.ASSUMED_GOOD, getSensorValueFlag(sensorValues, 3));
    assertEquals(Flag.ASSUMED_GOOD, getSensorValueFlag(sensorValues, 4));
  }

  /**
   * Test the propagated QC flags when only the first (bad) position is defined.
   *
   * @throws Exception
   *           If an internal error occurs.
   */
  @Test
  public void onlyFirstPositionTest() throws Exception {

    DatasetSensorValues sensorValues = makeSensorValues();

    PositionQCRoutine routine = new PositionQCRoutine(makeLons(false, false),
      makeLats(false, false), makeInstrument(), sensorValues,
      makeEmptyRunTypes());

    routine.qc(null);

    // Only the first SensorValue should have been flagged
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 2));
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 3));
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 4));
  }

  /**
   * Test the propagated QC flags when only the first two positions are defined.
   *
   * @throws Exception
   *           If an internal error occurs.
   */
  @Test
  public void twoPositionsTest() throws Exception {

    DatasetSensorValues sensorValues = makeSensorValues();

    PositionQCRoutine routine = new PositionQCRoutine(makeLons(false, true),
      makeLats(false, true), makeInstrument(), sensorValues,
      makeEmptyRunTypes());

    routine.qc(null);

    // Only the first SensorValue should have been flagged
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 2));
    assertEquals(Flag.BAD, getSensorValueFlag(sensorValues, 3));
    assertEquals(Flag.ASSUMED_GOOD, getSensorValueFlag(sensorValues, 4));
  }
}
