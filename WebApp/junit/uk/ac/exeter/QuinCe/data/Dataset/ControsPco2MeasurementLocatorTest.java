package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ControsPco2ReducerTest;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the {@link ControsPco2MeasurementLocator}.
 *
 * <p>
 * Uses the same Flyway test data as {@link ControsPco2ReducerTest}.
 * </p>
 */
public class ControsPco2MeasurementLocatorTest extends BaseTest {

  /**
   * The database ID of the {@link Instrument} in the FlyWay test data.
   */
  private static final long INSTRUMENT_ID = 124L;

  /**
   * The database ID of the {@link DataSet} in the FlyWay test data.
   */
  private static final long DATASET_ID = 2765L;

  /**
   * Initialise the Resource Manager.
   */
  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  /**
   * Destroy the Resource Manager.
   */
  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Get the configured {@link Instrument} for the testing {@link DataSet}ß.
   *
   * @return The Instrument.
   * @throws Exception
   *           If the Instrument cannot be retrieved.
   */
  private Instrument getInstrument() throws Exception {
    return InstrumentDB.getInstrument(getConnection(), INSTRUMENT_ID);
  }

  /**
   * Get the test {@link DataSet} from the database, overriding the measurement
   * mode as specified.
   *
   * @param variableMode
   *          The measurement mode.
   * @return The DataSet.
   * @throws Exception
   *           If the DataSet cannot be retrieved.
   */
  private DataSet getDataset() throws Exception {
    return DataSetDB.getDataSet(getConnection(), DATASET_ID);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void noZeroFlushingTimeTest() throws Exception {

    ControsPco2MeasurementLocator locator = new ControsPco2MeasurementLocator();

    Connection conn = getConnection(false);
    DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
      getDataset(), false, true);
    conn.commit();

    List<Measurement> measurements = locator.locateMeasurements(conn,
      getInstrument(), getDataset(), sensorValues);
    conn.commit();

    List<LocalDateTime> locatedMeasurementTimes = measurements.stream()
      .map(m -> m.getCoordinate().getTime()).toList();

    assertTrue(CollectionUtils.isEqualCollection(
      getNoZeroExpectedMeasurementTimes(), locatedMeasurementTimes));
  }

  private List<LocalDateTime> getNoZeroExpectedMeasurementTimes() {
    List<LocalDateTime> times = new ArrayList<LocalDateTime>();

    times.add(LocalDateTime.of(2023, 6, 8, 18, 9, 23));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 9, 33));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 9, 43));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 9, 53));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 10, 3));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 10, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 10, 23));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 10, 33));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 10, 43));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 10, 53));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 11, 3));
    times.add(LocalDateTime.of(2023, 6, 8, 18, 11, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 53, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 54, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 55, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 56, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 57, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 58, 13));
    times.add(LocalDateTime.of(2023, 6, 8, 23, 59, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 0, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 1, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 2, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 3, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 4, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 5, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 6, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 7, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 8, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 9, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 9, 23));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 9, 33));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 9, 43));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 9, 53));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 10, 3));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 10, 13));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 10, 23));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 10, 33));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 10, 43));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 10, 53));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 11, 3));
    times.add(LocalDateTime.of(2023, 6, 9, 0, 11, 13));

    return times;
  }
}
