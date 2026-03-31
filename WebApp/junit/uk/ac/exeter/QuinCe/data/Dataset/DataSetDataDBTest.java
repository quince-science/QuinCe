package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.RangeCheckRoutine;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class DataSetDataDBTest extends BaseTest {

  private static final long DATASET_ID = 1L;

  private static final long COLUMN_ID = 1L;

  private Connection conn = null;

  @BeforeEach
  public void setup() throws SQLException {
    initResourceManager();
    conn = getConnection(false);
  }

  @AfterEach
  public void tearDown() {
    DatabaseUtils.closeConnection(conn);
  }

  private NewSensorValues newSensorValue(long datasetId, long columnId,
    LocalDateTime time, String value)
    throws DatabaseException, RecordNotFoundException, CoordinateException {

    DataSet dataset = Mockito.mock(DataSet.class);
    Mockito.when(dataset.getId()).thenReturn(datasetId);

    NewSensorValues sv = new NewSensorValues(dataset);
    sv.create(columnId, new TimeCoordinate(datasetId, time), value);
    return sv;
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
  private SensorValue retrieveSingleStoredValue(Coordinate coordinate)
    throws Exception {

    DataSet dataset = DataSetDB.getDataSet(conn, DATASET_ID);

    DatasetSensorValues storedValues = DataSetDataDB.getSensorValues(conn,
      dataset, false, false);

    assertEquals(1, storedValues.size());

    SensorValuesList columnValues = storedValues.getColumnValues(COLUMN_ID);

    assertEquals(1, columnValues.rawSize());
    return columnValues.getRawSensorValue(coordinate, COLUMN_ID);
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

    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    SensorValue sensorValue = sensorValues.getSensorValues().stream().findAny()
      .get();

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    SensorValue storedValue = retrieveSingleStoredValue(
      sensorValue.getCoordinate());

    // Show that the SensorValue's ID has been updated
    assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, sensorValue.getId(),
      "Sensor Value ID was not updated");

    // Check the dirty flag
    assertFalse(sensorValue.isDirty(), "Dirty flag not cleared");

    assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, storedValue.getId(),
      "Database ID not set");
    assertEquals(COLUMN_ID, storedValue.getColumnId(), "Incorrect column ID");
    assertEquals(sensorValue.getCoordinate(), storedValue.getCoordinate(),
      "Incorrect time");
    assertEquals(sensorValue.getValue(), storedValue.getValue(),
      "Incorrect value");
    assertEquals(new AutoQCResult(flagScheme), storedValue.getAutoQcResult(),
      "Auto QC result not stored correctly");
    assertEquals(flagScheme.getAssumedGoodFlag(), storedValue.getUserQCFlag(),
      "Incorrect user QC flag");
    assertEquals("", storedValue.getUserQCMessage(),
      "Incorrect user QC message");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesNullValueTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), null);

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    SensorValue sensorValue = sensorValues.getSensorValues().stream().findAny()
      .get();

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    SensorValue storedValue = retrieveSingleStoredValue(
      sensorValue.getCoordinate());

    assertNull(storedValue.getValue(), "Stored value not null");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesCustomUserQCValueTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    SensorValue sensorValue = sensorValues.getSensorValues().stream().findAny()
      .get();

    Flag qcFlag = IcosFlagScheme.QUESTIONABLE_FLAG;
    String qcMessage = "I question this value";
    sensorValue.setUserQC(qcFlag, qcMessage);

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();
    SensorValue storedValue = retrieveSingleStoredValue(
      sensorValue.getCoordinate());

    assertEquals(qcFlag, storedValue.getUserQCFlag(), "Incorrect user QC flag");
    assertEquals(qcMessage, storedValue.getUserQCMessage(),
      "Incorrect user QC message");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void updateSensorValueTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    SensorValue sensorValue = sensorValues.getSensorValues().stream().findAny()
      .get();

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    SensorValue originalStoredValue = retrieveSingleStoredValue(
      sensorValue.getCoordinate());

    Flag userQCFlag = IcosFlagScheme.QUESTIONABLE_FLAG;
    String userQCMessage = "Updated User QC";

    originalStoredValue.addAutoQCFlag(
      new RoutineFlag(flagScheme, Mockito.mock(RangeCheckRoutine.class),
        flagScheme.getBadFlag(), "77", "88"));
    originalStoredValue.setUserQC(userQCFlag, userQCMessage);
    AutoQCResult autoQC = originalStoredValue.getAutoQcResult();

    DataSetDataDB.updateSensorValues(conn, Arrays.asList(originalStoredValue));
    conn.commit();

    // Check that the dirty flag is cleared
    assertFalse(originalStoredValue.isDirty(), "Dirty flag not cleared");

    SensorValue updatedValue = retrieveSingleStoredValue(
      sensorValue.getCoordinate());

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
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(MissingParamException.class, () -> {
      DataSetDataDB.storeNewSensorValues(null, sensorValues);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesEmptyListTest() throws Exception {
    DataSet dataset = Mockito.mock(DataSet.class);
    Mockito.when(dataset.getId()).thenReturn(DATASET_ID);

    assertThrows(MissingParamException.class, () -> DataSetDataDB
      .storeNewSensorValues(conn, new NewSensorValues(dataset)));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesNullListTest() throws Exception {

    assertThrows(MissingParamException.class, () -> {
      DataSetDataDB.storeNewSensorValues(conn, null);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void updateSensorValuesEmptyListTest() throws Exception {
    DataSetDataDB.updateSensorValues(conn, new ArrayList<SensorValue>());
    conn.commit();
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void updateSensorValuesNullListTest() throws Exception {

    assertThrows(MissingParamException.class, () -> {
      DataSetDataDB.updateSensorValues(conn, null);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesInvalidDatasetTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(7000L, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(RecordNotFoundException.class, () -> {
      DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void storeSensorValuesInvalidColumnTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, 7000L,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    assertThrows(InvalidSensorValueException.class, () -> {
      DataSetDataDB.storeNewSensorValues(conn, sensorValues);
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
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    sensorValues.create(7000L,
      new TimeCoordinate(DATASET_ID, LocalDateTime.of(2021, 1, 1, 0, 0, 0)),
      "22");

    boolean exceptionThrown = false;
    try {
      DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    } catch (InvalidSensorValueException e) {
      conn.rollback();
      exceptionThrown = true;
    }

    assertTrue(exceptionThrown, "Expected InvalidSensorValueException");

    assertEquals(0,
      DataSetDataDB.getSensorValues(conn,
        DataSetDB.getDataSet(conn, DATASET_ID), false, false).size(),
      "Value has been stored; should not have been");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void getSensorValuesFlushingNotIgnoredTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    sensorValues.create(COLUMN_ID,
      new TimeCoordinate(DATASET_ID, LocalDateTime.of(2021, 1, 1, 0, 0, 1)),
      "21");

    for (SensorValue value : sensorValues.getSensorValues()) {
      if (value.getCoordinate().getTime().getSecond() == 1) {
        value.setUserQC(FlagScheme.FLUSHING_FLAG, "Flushing");
      }
    }

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    assertEquals(2,
      DataSetDataDB.getSensorValues(conn,
        DataSetDB.getDataSet(conn, DATASET_ID), false, false).size(),
      "Incorrect number of values retrieved");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void getSensorValuesFlushingIgnoredTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    sensorValues.create(COLUMN_ID,
      new TimeCoordinate(DATASET_ID, LocalDateTime.of(2021, 1, 1, 0, 0, 1)),
      "21");

    for (SensorValue value : sensorValues.getSensorValues()) {
      if (value.getCoordinate().getTime().getSecond() == 1) {
        value.setUserQC(FlagScheme.FLUSHING_FLAG, "Flushing");
      }
    }

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    assertEquals(1,
      DataSetDataDB.getSensorValues(conn,
        DataSetDB.getDataSet(conn, DATASET_ID), true, false).size(),
      "Incorrect number of values retrieved");
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/variable", "resources/sql/testbase/instrument",
    "resources/sql/testbase/dataset" })
  @Test
  public void deleteSensorValuesTest() throws Exception {
    NewSensorValues sensorValues = newSensorValue(DATASET_ID, COLUMN_ID,
      LocalDateTime.of(2021, 1, 1, 0, 0, 0), "20");

    sensorValues.create(2L,
      new TimeCoordinate(DATASET_ID, LocalDateTime.of(2021, 1, 1, 0, 0, 0)),
      "21");

    DataSetDataDB.storeNewSensorValues(conn, sensorValues);
    conn.commit();

    assertEquals(2,
      DataSetDataDB.getSensorValues(conn,
        DataSetDB.getDataSet(conn, DATASET_ID), true, false).size(),
      "Values not stored as expected");

    DataSetDataDB.deleteSensorValues(getConnection(), DATASET_ID);

    assertEquals(0,
      DataSetDataDB.getSensorValues(conn,
        DataSetDB.getDataSet(conn, DATASET_ID), true, false).size(),
      "Values not removed");
  }
}
