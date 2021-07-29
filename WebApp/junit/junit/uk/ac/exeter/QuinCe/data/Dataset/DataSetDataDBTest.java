package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidSensorValueException;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.RangeCheckRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class DataSetDataDBTest extends BaseTest {

  private static final long INSTRUMENT_ID = 1L;

  private static final long DATASET_ID = 1L;

  private static final long COLUMN_ID = 1L;

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  /**
   * Store a single SensorValue in the database, and retrieve it.
   * <p>
   * Also ensures that the single stored value is the only one returned.
   * </p>
   *
   * @param sensorValue
   *          The sensor value to be stored
   * @return The value retrieved from the database
   * @throws Exception
   */
  private SensorValue retrieveSingleStoredValue() throws Exception {

    DatasetSensorValues storedValues = DataSetDataDB.getSensorValues(
      getConnection(),
      InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID), DATASET_ID,
      false);

    assertEquals(1, storedValues.size());

    SearchableSensorValuesList columnValues = storedValues
      .getColumnValues(COLUMN_ID);

    assertEquals(1, columnValues.size());
    return columnValues.get(0);
  }

  /**
   * Test that a simple value can be stored, that its data is in the database,
   * and that the object is given a database ID.
   *
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesSingleValueTest() throws Exception {

    LocalDateTime valueTime = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
    String value = "20";
    SensorValue sensorValue = new SensorValue(DATASET_ID, COLUMN_ID, valueTime,
      value);

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(sensorValue));
    SensorValue storedValue = retrieveSingleStoredValue();

    // Show that the SensorValue's ID has not been updated
    assertEquals(DatabaseUtils.NO_DATABASE_RECORD, sensorValue.getId(),
      "Sensor Value ID was updated unexpectedly");

    // Check the dirty flag
    assertFalse(sensorValue.isDirty(), "Dirty flag not cleared");

    assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, storedValue.getId(),
      "Database ID not set");
    assertEquals(COLUMN_ID, storedValue.getColumnId(), "Incorrect column ID");
    assertEquals(valueTime, storedValue.getTime(), "Incorrect time");
    assertEquals(value, storedValue.getValue(), "Incorrect value");
    assertEquals(new AutoQCResult(), storedValue.getAutoQcResult(),
      "Auto QC result not stored correctly");
    assertEquals(Flag.ASSUMED_GOOD, storedValue.getUserQCFlag(),
      "Incorrect user QC flag");
    assertEquals("", storedValue.getUserQCMessage(),
      "Incorrect user QC message");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesNullValueTest() throws Exception {
    SensorValue sensorValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), null);

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(sensorValue));
    SensorValue storedValue = retrieveSingleStoredValue();

    assertNull(storedValue.getValue(), "Stored value not null");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesCustomUserQCValueTest() throws Exception {
    SensorValue sensorValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    Flag qcFlag = Flag.QUESTIONABLE;
    String qcMessage = "I question this value";
    sensorValue.setUserQC(qcFlag, qcMessage);

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(sensorValue));
    SensorValue storedValue = retrieveSingleStoredValue();

    assertEquals(qcFlag, storedValue.getUserQCFlag(), "Incorrect user QC flag");
    assertEquals(qcMessage, storedValue.getUserQCMessage(),
      "Incorrect user QC message");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void updateSensorValueTest() throws Exception {
    SensorValue sensorValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(sensorValue));

    SensorValue originalStoredValue = retrieveSingleStoredValue();

    Flag userQCFlag = Flag.QUESTIONABLE;
    String userQCMessage = "Updated User QC";

    originalStoredValue.addAutoQCFlag(
      new RoutineFlag(new RangeCheckRoutine(), Flag.BAD, "77", "88"));
    originalStoredValue.setUserQC(userQCFlag, userQCMessage);
    AutoQCResult autoQC = originalStoredValue.getAutoQcResult();

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(originalStoredValue));

    // Check that the dirty flag is cleared
    assertFalse(originalStoredValue.isDirty(), "Dirty flag not cleared");

    SensorValue updatedValue = retrieveSingleStoredValue();

    assertEquals(autoQC, updatedValue.getAutoQcResult(), "Incorrect Auto QC");
    assertEquals(userQCFlag, updatedValue.getUserQCFlag(),
      "Incorrect user QC flag");
    assertEquals(userQCMessage, updatedValue.getUserQCMessage(),
      "Incorrect user QC message");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesNoConnTest() throws Exception {
    SensorValue sensorValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(MissingParamException.class, () -> {
      DataSetDataDB.storeSensorValues(null, Arrays.asList(sensorValue));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesEmptyListTest() throws Exception {
    DataSetDataDB.storeSensorValues(getConnection(),
      new ArrayList<SensorValue>());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesNullListTest() throws Exception {
    assertThrows(MissingParamException.class, () -> {
      DataSetDataDB.storeSensorValues(getConnection(), null);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesInvalidDatasetTest() throws Exception {
    SensorValue sensorValue = new SensorValue(7000L, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(InvalidSensorValueException.class, () -> {
      DataSetDataDB.storeSensorValues(getConnection(),
        Arrays.asList(sensorValue));
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesInvalidColumnTest() throws Exception {
    SensorValue sensorValue = new SensorValue(DATASET_ID, 7000L,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(InvalidSensorValueException.class, () -> {
      DataSetDataDB.storeSensorValues(getConnection(),
        Arrays.asList(sensorValue));
    });
  }

  /**
   * Test that no values are stored if any values are invalid.
   * 
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeValuesMultipleValuesOneInvalid() throws Exception {
    SensorValue sensorValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");
    SensorValue badValue = new SensorValue(7000L, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(InvalidSensorValueException.class, () -> {
      DataSetDataDB.storeSensorValues(getConnection(),
        Arrays.asList(sensorValue, badValue));
    });

    assertEquals(0,
      DataSetDataDB.getSensorValues(getConnection(),
        InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID), DATASET_ID,
        false).size(),
      "Value has been stored; should not have been");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void getSensorValuesFlushingNotIgnoredTest() throws Exception {

    SensorValue normalValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    SensorValue flushingValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 1, 0), "21");
    flushingValue.setUserQC(Flag.FLUSHING, "Flushing");

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(normalValue, flushingValue));

    assertEquals(2,
      DataSetDataDB.getSensorValues(getConnection(),
        InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID), DATASET_ID,
        false).size(),
      "Incorrect number of values retrieved");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void getSensorValuesFlushingIgnoredTest() throws Exception {

    SensorValue normalValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    SensorValue flushingValue = new SensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 1, 0), "21");
    flushingValue.setUserQC(Flag.FLUSHING, "Flushing");

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(normalValue, flushingValue));

    assertEquals(1,
      DataSetDataDB.getSensorValues(getConnection(),
        InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID), DATASET_ID,
        true).size(),
      "Incorrect number of values retrieved");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void deleteSensorValuesTest() throws Exception {
    SensorValue value1 = new SensorValue(DATASET_ID, 1L,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    SensorValue value2 = new SensorValue(DATASET_ID, 2L,
      LocalDateTime.of(2021, 1, 1, 0, 1, 0), "21");

    DataSetDataDB.storeSensorValues(getConnection(),
      Arrays.asList(value1, value2));

    assertEquals(2,
      DataSetDataDB.getSensorValues(getConnection(),
        InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID), DATASET_ID,
        true).size(),
      "Values not stored as expected");

    DataSetDataDB.deleteSensorValues(getConnection(), DATASET_ID);

    assertEquals(0,
      DataSetDataDB.getSensorValues(getConnection(),
        InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID), DATASET_ID,
        true).size(),
      "Values not removed");
  }
}
