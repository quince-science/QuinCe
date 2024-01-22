package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetMeasurements;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ControsPco2Reducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the 4H-Jena CONTROS sensor's data reduction.
 *
 * <p>
 * Pre- and post-calibrations are set for {@code 2021-06-21T00:00:00Z} and
 * {@code 2021-07-11T00:00:00Z} respectively as Flyway database entries.
 * </p>
 *
 * <p>
 * Migrations are also prepared for datasets with different sets of
 * measurements:
 * </p>
 * <ul>
 * <li>Zero runs before and after measurements.</li>
 * <li>Zero run before measurements only.</li>
 * <li>Zero run after measurements only.</li>
 * </ul>
 *
 * <p>
 * The following test combinations are covered:
 * </p>
 * <table>
 * <caption>Test combinations for CONTROS data reduction.</caption>
 * <tr>
 * <th>Variable Mode</th>
 * <th>Has pre-calibration?</th>
 * <th>Has post-calibration?</th>
 * <th>Data has pre-zero?</th>
 * <th>Data has post-zero?</th>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * <td align="center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td align="center">Y</td>
 * <td align="center">N</td>
 * <td align="center">N</td>
 * <td align="center">Y</td>
 * </tr>
 * </table>
 *
 * <p>
 * Tests for the situation where pre-calibrations are not present are not
 * conducted, because datasets cannot be created in such a situation. Similarly,
 * the behaviour of the data reduction where there are no Zeroing values is
 * undefined, so there are no tests for this case.
 * </p>
 */
public class ControsPco2ReducerTest extends DataReducerTest {

  /**
   * The database ID of the {@link Instrument} in the FlyWay test data.
   */
  private static final long INSTRUMENT_ID = 116L;

  /**
   * The database ID of the {@link DataSet} in the FlyWay test data.
   */
  private static final long DATASET_ID = 2717L;

  /**
   * Date of the {@link Measurement} whose data reduction will be tested.
   */
  private static final LocalDateTime MEASUREMENT_TIME = DateTimeUtils
    .parseISODateTime("2021-06-28T17:05:58.000Z");

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
   * Create the data reducer for testing.
   *
   * @return The data reducer.
   * @throws Exception
   *           If the reducer cannot be created.
   */
  private ControsPco2Reducer makeReducer(
    Map<String, Properties> datasetProperties) throws Exception {
    return new ControsPco2Reducer(getVariable(), datasetProperties);
  }

  /**
   * Get the CONTROS {@link Variable}.
   *
   * @return The Variable.
   * @throws Exception
   *           If the Variable cannot be retrieved.
   */
  private Variable getVariable() throws Exception {
    List<Variable> variables = InstrumentDB.getAllVariables(getDataSource());
    return variables.stream().filter(v -> v.getName().equals("CONTROS pCO₂"))
      .findAny().get();
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
  private DataSet getDataset(String variableMode) throws Exception {

    DataSet dataSet = DataSetDB.getDataSet(getConnection(), DATASET_ID);

    Properties varProps = dataSet.getAllProperties()
      .get(getVariable().getName());
    varProps.setProperty("zero_mode", variableMode);

    return dataSet;
  }

  /**
   * Get the {@link Measurement}s for the test {@link DataSet}.
   *
   * @param instrument
   *          The parent {@link Instrument}.
   * @return The measurements.
   * @throws Exception
   *           If the measurements cannot be retrieved.
   */
  private DatasetMeasurements getMeasurements(Instrument instrument)
    throws Exception {
    return DataSetDataDB.getMeasurementsByRunType(getConnection(), instrument,
      DATASET_ID);
  }

  /**
   * Get the {@link Measurement} whose data reduction is to be tested.
   *
   * @param allMeasurements
   *          The test {@link DataSet}'s measurements.
   * @return The {@link Measurement} to be tested.
   */
  private Measurement getTestMeasurement(DatasetMeasurements allMeasurements) {
    return allMeasurements.getTimeOrderedMeasurements().stream()
      .filter(m -> m.getTime().equals(MEASUREMENT_TIME)).findAny().get();
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Continuous mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Pre- and post-zeros are present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void continuousCalPrePostZeroPrePost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5964.458D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(589.452D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(607.312D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(613.229D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Continuous mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Pre-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_pre_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void continuousCalPrePostZeroPre() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5966.390D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(589.733D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(607.601D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(613.521D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Continuous mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Post-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_post_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void continuousCalPrePostZeroPost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.809D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5741.454D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(557.532D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(574.425D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(580.022D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Continuous mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Pre- and Post-zero are present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void continuousCalPreZeroPrePost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5964.458D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(588.651D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(606.486D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(612.396D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Continuous mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Pre-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_pre_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void continuousCalPreZeroPre() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5966.390D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(588.932D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(606.775D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(612.688D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Continuous mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Post-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_post_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void continuousCalPreZeroPost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.809D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5741.454D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(556.740D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(573.608D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(579.198D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero Before Sleep mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Pre- and post-zeros are present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void zeroBeforeSleepCalPrePostZeroPrePost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.809D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5741.454D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(557.532D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(574.425D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(580.022D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero Before Sleep mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Pre-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_pre_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void zeroBeforeSleepCalPrePostZeroPre() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("pCO₂ SST"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero Before Sleep mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Post-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_post_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void zeroBeforeSleepCalPrePostZeroPost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.809D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5741.454D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(557.532D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(574.425D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(580.022D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero Before Sleep mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Pre- and Post-zero are present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void zeroBeforeSleepCalPreZeroPrePost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.809D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5741.454D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(556.740D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(573.608D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(579.198D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero Before Sleep mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Pre-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_pre_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void zeroBeforeSleepCalPreZeroPre() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("pCO₂ SST"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero Before Sleep mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Post-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_post_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void zeroBeforeSleepCalPreZeroPost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.809D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5741.454D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(556.740D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(573.608D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(579.198D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero After Sleep mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Pre- and post-zeros are present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void zeroAfterSleepCalPrePostZeroPrePost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5966.390D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(589.733D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(607.601D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(613.521D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero After Sleep mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Pre-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_pre_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void zeroAfterSleepCalPrePostZeroPre() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5966.390D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(589.733D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(607.601D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(613.521D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero After Sleep mode.</li>
   * <li>Pre- and post calibrations are present.</li>
   * <li>Post-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_post_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/post-calibration" })
  @Test
  public void zeroAfterSleepCalPrePostZeroPost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("pCO₂ SST"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero After Sleep mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Pre- and Post-zero are present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_both_zeros",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void zeroAfterSleepCalPreZeroPrePost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5966.390D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(588.932D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(606.775D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(612.687D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero After Sleep mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Pre-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_pre_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void zeroAfterSleepCalPreZeroPre() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(0.812D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(5966.390D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(588.932D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(606.775D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(612.688D, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }

  /**
   * Test for the reducer with the following setup:
   *
   * <ul>
   * <li>Zero After Sleep mode.</li>
   * <li>Pre-calibration is present.</li>
   * <li>Post-zero is present.</li>
   * </ul>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/dataset_post_zero",
    "resources/sql/data/DataSet/DataReduction/ControsPco2ReducerTest/pre-calibration" })
  @Test
  public void zeroAfterSleepCalPreZeroPost() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset.getAllProperties());
    DatasetMeasurements measurements = getMeasurements(instrument);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getTimeOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(0.733D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("pCO₂ SST"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("fCO₂"),
      0.001D);
  }
}
