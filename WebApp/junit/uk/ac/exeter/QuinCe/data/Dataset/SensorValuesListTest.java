package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class SensorValuesListTest extends BaseTest {

  protected static final long DATASET_ID = 1L;

  protected long sensorValueId = 0L;

  @BeforeEach
  protected void setUp() {
    initResourceManager();
    sensorValueId = 0L;
  }

  @AfterEach
  protected void tearDown() {
    ResourceManager.destroy();
  }

  protected long getSensorValueId() {
    sensorValueId++;
    return sensorValueId;
  }

  /**
   * Make a {@link SensorValue} for a given column, hour and minute.
   *
   * <p>
   * The time will always have the same date - just the time will change
   * according to the parameters. The value will always be the same.
   * </p>
   *
   * @param column
   *          The column ID
   * @param hour
   *          The hour.
   * @param minute
   *          The minute.
   * @return The {@link SensorValue}.
   * @throws CoordinateException
   */
  protected SensorValue makeSensorValue(long column, int hour, int minute) {
    return new SensorValue(DATASET_ID, IcosFlagScheme.getInstance(), column,
      makeTime(hour, minute), "12");
  }

  private TimeCoordinate makeTime(int hour, int minute) {
    return new TimeCoordinate(DATASET_ID,
      LocalDateTime.of(2023, 1, 1, hour, minute, 0));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void nullAddTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(null);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void singleColumnValidAddTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    list.add(makeSensorValue(1L, 1, 1));
    assertEquals(1, list.rawSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void singleColumnInvalidColumnTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(2L, 1, 1));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void onlyValueDuplicateTimestampTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    list.add(makeSensorValue(1L, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(1L, 1, 1));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void maintainsOrderTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);

    // First value
    list.add(makeSensorValue(1L, 1, 5));

    // End
    list.add(makeSensorValue(1L, 1, 10));

    // Start
    list.add(makeSensorValue(1L, 1, 1));

    // Middle
    list.add(makeSensorValue(1L, 1, 3));

    assertTrue(timesOrdered(
      list.getRawCoordinates().stream().map(c -> c.getTime()).toList()));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void multipleValuesDuplicateTimestampTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);

    // First value
    list.add(makeSensorValue(1L, 1, 5));

    // End
    list.add(makeSensorValue(1L, 1, 10));

    // Start
    list.add(makeSensorValue(1L, 1, 1));

    // Middle
    list.add(makeSensorValue(1L, 1, 3));

    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(1L, 1, 5));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void multipleColumnsDifferentSensorTypesTest() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      new TimestampSensorValuesList(Arrays.asList(1L, 2L),
        getDatasetSensorValues(), false);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/SensorValuesListTest/multipleColumns" })
  @Test
  public void multipleColumnsValidAddTest() throws Exception {

    SensorValuesList list = new TimestampSensorValuesList(
      Arrays.asList(1L, 10L), getDatasetSensorValues(), false);

    list.add(makeSensorValue(1L, 1, 2));
    list.add(makeSensorValue(10L, 1, 4));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/SensorValuesListTest/multipleColumns" })
  @Test
  public void multipleColumnsInvalidAddTest() throws Exception {

    SensorValuesList list = new TimestampSensorValuesList(
      Arrays.asList(1L, 10L), getDatasetSensorValues(), false);

    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(2L, 1, 4));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void emptyIsEmptyTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    assertTrue(list.isEmpty());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void notEmptyIsEmptyTest() throws Exception {
    SensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    list.add(makeSensorValue(1L, 1, 2));
    assertFalse(list.isEmpty());
  }

  /**
   * Create a {@link DatasetSensorValues} object.
   *
   * @return The {@link DatasetSensorValues}.
   * @throws Exception
   *           If the construction fails.
   */
  protected DatasetSensorValues getDatasetSensorValues() throws Exception {
    DatasetSensorValues result = new DatasetSensorValues(
      Mockito.mock(DataSet.class));
    Mockito.when(result.getInstrument())
      .thenReturn(InstrumentDB.getInstrument(getConnection(), DATASET_ID));
    Mockito.when(result.getFlagScheme())
      .thenReturn(IcosFlagScheme.getInstance());
    Mockito.when(result.getDatasetId()).thenReturn(DATASET_ID);
    return result;
  }

  private static Stream<Arguments> getValuesBetweenTestParams() {
    return Stream.of(Arguments.of(35, 35, Arrays.asList()),
      Arguments.of(35, 32, Arrays.asList(32, 33, 34)),
      Arguments.of(35, 38, Arrays.asList(36, 37, 38)),
      Arguments.of(35, 29, Arrays.asList(30, 31, 32, 33, 34)),
      Arguments.of(35, 41, Arrays.asList(36, 37, 38, 39)));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @ParameterizedTest
  @MethodSource("getValuesBetweenTestParams")
  public void getValuesBetweenTest(int minute1, int minute2,
    List<Integer> expectedMinutes) throws Exception {

    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);

    for (int i = 30; i < 40; i++) {
      list.add(makeSensorValue(1L, 1, i));
    }

    TimeCoordinate time1 = makeTime(1, minute1);
    TimeCoordinate time2 = makeTime(1, minute2);

    List<SensorValuesListValue> foundValues = list.getValuesBetween(time1,
      time2);

    List<Integer> foundMinutes = foundValues.stream()
      .map(v -> v.getCoordinate().getTime().getMinute()).sorted().toList();

    assertTrue(listsEqual(expectedMinutes, foundMinutes));
  }
}
