package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.DateTimeFormatsBean;

public class TimestampSensorValuesListTest extends SensorValuesListTest {

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void nullAddTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(null);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void singleColumnValidAddTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    list.add(makeSensorValue(1L, 1, 1));
    assertEquals(1, list.rawSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void singleColumnInvalidColumnTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(2L, 1, 1));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void onlyValueDuplicateTimestampTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    list.add(makeSensorValue(1L, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(1L, 1, 1));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void multipleValuesDuplicateTimestampTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
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

    TimestampSensorValuesList list = new TimestampSensorValuesList(
      Arrays.asList(1L, 10L), getDatasetSensorValues(), false);

    list.add(makeSensorValue(1L, 1, 2));
    list.add(makeSensorValue(10L, 1, 4));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/SensorValuesListTest/multipleColumns" })
  @Test
  public void multipleColumnsInvalidAddTest() throws Exception {

    TimestampSensorValuesList list = new TimestampSensorValuesList(
      Arrays.asList(1L, 10L), getDatasetSensorValues(), false);

    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(2L, 1, 4));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void emptyIsEmptyTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    assertTrue(list.isEmpty());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void notEmptyIsEmptyTest() throws Exception {
    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);
    list.add(makeSensorValue(1L, 1, 2));
    assertFalse(list.isEmpty());
  }

  private static Stream<Arguments> measurementModeParams() {
    return Stream.of(Arguments.of(1, TimestampSensorValuesList.MODE_PERIODIC),
      Arguments.of(2, TimestampSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(3, TimestampSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(4, TimestampSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(5, TimestampSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(6, TimestampSensorValuesList.MODE_PERIODIC),
      Arguments.of(7, TimestampSensorValuesList.MODE_PERIODIC),
      Arguments.of(8, TimestampSensorValuesList.MODE_PERIODIC),
      Arguments.of(9, TimestampSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(10, TimestampSensorValuesList.MODE_PERIODIC));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @ParameterizedTest
  @MethodSource("measurementModeParams")
  public void measurementModeTest(int fileNumber, int expectedMode)
    throws Exception {

    // Load dates
    File timesFile = context.getResource(
      "classpath:resources/testdata/data/DataSet/SensorValuesList/measurementMode"
        + fileNumber + ".csv")
      .getFile();

    TimestampSensorValuesList list = new TimestampSensorValuesList(1L,
      getDatasetSensorValues(), false);

    BufferedReader in = new BufferedReader(new FileReader(timesFile));
    String line;
    while ((line = in.readLine()) != null) {
      LocalDateTime timestamp = LocalDateTime.parse(line,
        DateTimeFormatsBean.DT_ISO_MS_F);
      list.add(new SensorValue(1L, flagScheme, 1L,
        new TimeCoordinate(DATASET_ID, timestamp), "1"));
    }
    in.close();

    assertEquals(expectedMode, list.getMeasurementMode());
  }

  private DatasetSensorValues makeDatasetSensorValues(String filename,
    long columnId, Flag flag) throws Exception {
    // Build the sensor values, then get the list from it for testing
    File file = context.getResource(
      "classpath:resources/testdata/data/DataSet/SensorValuesList/" + filename)
      .getFile();

    Instrument instrument = InstrumentDB.getInstrument(getConnection(),
      DATASET_ID);
    DatasetSensorValues result = new DatasetSensorValues(
      Mockito.mock(DataSet.class));
    Mockito.when(result.getInstrument()).thenReturn(instrument);
    Mockito.when(result.getDatasetId()).thenReturn(DATASET_ID);

    BufferedReader in = new BufferedReader(new FileReader(file));
    String line;
    while ((line = in.readLine()) != null) {
      String[] fields = line.split(",");
      LocalDateTime timestamp = LocalDateTime.parse(fields[0],
        DateTimeFormatsBean.DT_ISO_MS_F);
      SensorValue sensorValue = new SensorValue(getSensorValueId(), 1L,
        flagScheme, columnId, new TimeCoordinate(DATASET_ID, timestamp),
        fields[1], new AutoQCResult(flagScheme), flag, "");
      result.add(sensorValue);
    }
    in.close();

    return result;
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void continuousRawSize() throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "numericValuesContinuous.csv", 6L, flagScheme.getGoodFlag());

    SensorValuesList list = allSensorValues.getColumnValues(6L);
    assertEquals(30, list.rawSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void continuousValuesSize() throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "numericValuesContinuous.csv", 6L, flagScheme.getGoodFlag());

    SensorValuesList list = allSensorValues.getColumnValues(6L);
    assertEquals(30, list.valuesSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void stringPeriodicRawSize() throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "stringValuesPeriodic.csv", 6L, flagScheme.getGoodFlag());

    SensorValuesList list = allSensorValues.getColumnValues(6L);
    assertEquals(50, list.rawSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void stringPeriodicValuesSize() throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "stringValuesPeriodic.csv", 6L, flagScheme.getGoodFlag());

    SensorValuesList list = allSensorValues.getColumnValues(6L);
    assertEquals(5, list.valuesSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void numericPeriodicRawSize() throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "numericValuesPeriodic.csv", 6L, flagScheme.getGoodFlag());

    SensorValuesList list = allSensorValues.getColumnValues(6L);
    assertEquals(50, list.rawSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void numericPeriodicValuesSize() throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "numericValuesPeriodic.csv", 6L, flagScheme.getGoodFlag());

    SensorValuesList list = allSensorValues.getColumnValues(6L);
    assertEquals(3, list.valuesSize());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @ParameterizedTest
  @ValueSource(strings = { "stringValuesContinuous1.csv",
    "stringValuesContinuous2.csv" })
  public void stringValuesContinuousTest(String file) throws Exception {

    // Column 6 = Run Type
    DatasetSensorValues allSensorValues = makeDatasetSensorValues(
      "stringValuesContinuous1.csv", 6L, flagScheme.getGoodFlag());

    TimestampSensorValuesList list = (TimestampSensorValuesList) allSensorValues
      .getColumnValues(6L);
    assertEquals(TimestampSensorValuesList.MODE_CONTINUOUS,
      list.getMeasurementMode());
  }
}
