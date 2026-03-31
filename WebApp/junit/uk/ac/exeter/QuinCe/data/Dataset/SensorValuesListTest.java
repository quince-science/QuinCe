package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
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
  protected SensorValue makeSensorValue(long column, int hour, int minute)
    throws CoordinateException {
    return new SensorValue(1L, flagScheme, column, new TimeCoordinate(
      DATASET_ID, LocalDateTime.of(2023, 1, 1, hour, minute, 0)), "12");
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
    return result;
  }
}
