package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ControsPco2ReducerTest;
import uk.ac.exeter.QuinCe.data.Dataset.ControsPco2MeasurementLocator;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
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
  private static final long INSTRUMENT_ID = 116L;

  /**
   * The database ID of the {@link DataSet} in the FlyWay test data.
   */
  private static final long DATASET_ID = 2717L;

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

    List<Measurement> measurements = locator.locateMeasurements(getConnection(),
      getInstrument(), getDataset());

    List<LocalDateTime> locatedMeasurementTimes = measurements.stream()
      .map(m -> m.getTime()).toList();

    assertTrue(
      listsEqual(getNoZeroExpectedMeasurementTimes(), locatedMeasurementTimes));
  }

  private List<LocalDateTime> getNoZeroExpectedMeasurementTimes() {
    List<LocalDateTime> times = new ArrayList<LocalDateTime>();

    times.add(LocalDateTime.of(2021, 6, 28, 16, 51, 18));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 51, 28));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 51, 38));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 51, 48));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 51, 58));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 52, 8));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 52, 18));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 52, 28));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 52, 38));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 52, 48));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 52, 58));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 53, 8));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 53, 18));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 53, 28));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 53, 38));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 53, 48));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 53, 58));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 54, 8));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 54, 18));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 54, 28));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 54, 38));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 54, 48));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 54, 58));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 55, 8));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 55, 18));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 55, 28));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 55, 38));
    times.add(LocalDateTime.of(2021, 6, 28, 16, 55, 48));
    times.add(LocalDateTime.of(2021, 6, 28, 17, 5, 58));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 52, 9));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 52, 19));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 52, 29));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 52, 39));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 52, 49));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 52, 59));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 53, 9));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 53, 19));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 53, 29));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 53, 39));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 53, 49));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 53, 59));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 54, 9));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 54, 19));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 54, 29));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 54, 39));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 54, 49));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 54, 59));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 55, 9));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 55, 19));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 55, 29));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 55, 39));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 55, 49));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 55, 59));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 56, 9));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 56, 19));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 56, 29));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 56, 39));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 56, 49));
    times.add(LocalDateTime.of(2021, 6, 29, 16, 56, 59));

    return times;
  }
}
