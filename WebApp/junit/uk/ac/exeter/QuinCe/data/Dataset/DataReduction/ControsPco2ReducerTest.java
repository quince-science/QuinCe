package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetMeasurements;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.TimeDataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.DateTimeFormatsBean;
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
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * </tr>
 * <tr>
 * <td>Continuous</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * </tr>
 * <tr>
 * <td>Zero after sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">Y</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * </tr>
 * <tr>
 * <td>Zero before sleep</td>
 * <td style="text-align: center">Y</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">N</td>
 * <td style="text-align: center">Y</td>
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
  private static final long INSTRUMENT_ID = 124L;

  /**
   * The database ID of the {@link DataSet} in the FlyWay test data.
   */
  private static final long DATASET_ID = 2765L;

  /**
   * Date of the {@link Measurement} whose data reduction will be tested.
   */
  private static final LocalDateTime MEASUREMENT_TIME = LocalDateTime
    .parse("2023-06-08T23:50:13.000Z", DateTimeFormatsBean.DT_ISO_MS_F);

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
  private ControsPco2Reducer makeReducer(TimeDataSet dataset) throws Exception {
    CalibrationSet calculationCoefficients = CalculationCoefficientDB
      .getInstance().getCalibrationSet(getConnection(), dataset);
    return new ControsPco2Reducer(getVariable(), dataset.getAllProperties(),
      calculationCoefficients);
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
  private TimeDataSet getDataset(String variableMode) throws Exception {

    TimeDataSet dataSet = (TimeDataSet) DataSetDB.getDataSet(getConnection(),
      DATASET_ID);

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
  private DatasetMeasurements getMeasurements(DataSet dataset)
    throws Exception {
    return DataSetDataDB.getMeasurementsByRunType(getConnection(), dataset);
  }

  /**
   * Get the {@link Measurement} whose data reduction is to be tested.
   *
   * @param allMeasurements
   *          The test {@link DataSet}'s measurements.
   * @return The {@link Measurement} to be tested.
   */
  private Measurement getTestMeasurement(DatasetMeasurements allMeasurements) {
    return allMeasurements.getOrderedMeasurements().stream()
      .filter(m -> m.getCoordinate().getTime().equals(MEASUREMENT_TIME))
      .findAny().get();
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
    TimeDataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4474.686D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(379.587D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(406.287D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(404.817D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4499.48D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(382.405D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(409.303D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(407.823D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.807D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4445.223D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(376.251D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(402.716D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(401.26D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4474.686D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(380.287D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(407.035D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(405.563D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4499.48D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(383.121D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(410.069D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(408.586D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Continuous");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.807D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4445.223D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(376.931D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(403.444D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(401.985D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.807D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4445.223D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(376.251D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(402.716D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(401.26D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("S₂beam"));
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
    TimeDataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.807D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4445.223D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(376.251D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(402.716D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(401.26D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.807D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4445.223D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(376.931D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(403.444D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(401.985D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("S₂beam"));
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
    TimeDataSet dataset = getDataset("Zero before sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.807D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4445.223D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(376.931D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(403.444D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(401.985D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4499.48D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(382.405D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(409.303D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(407.823D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4499.48D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(382.405D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(409.303D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(407.823D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("S₂beam"));
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
    TimeDataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4499.48D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(383.121D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(410.069D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(408.586D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(0.808D, dataReductionRecord.getCalculationValue("Zero S₂beam"),
      0.001D);
    assertEquals(0.749D, dataReductionRecord.getCalculationValue("S₂beam"),
      0.001D);
    assertEquals(4499.48D, dataReductionRecord.getCalculationValue("Sproc"),
      0.001D);
    assertEquals(383.121D, dataReductionRecord.getCalculationValue("xCO₂"),
      0.001D);
    assertEquals(410.069D, dataReductionRecord.getCalculationValue("pCO₂ SST"),
      0.001D);
    assertEquals(408.586D, dataReductionRecord.getCalculationValue("fCO₂"),
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
    TimeDataSet dataset = getDataset("Zero after sleep");
    ControsPco2Reducer reducer = makeReducer(dataset);
    DatasetMeasurements measurements = getMeasurements(dataset);
    DatasetSensorValues allSensorValues = DataSetDataDB
      .getSensorValues(getConnection(), dataset, true, false);

    reducer.preprocess(getConnection(), instrument, dataset,
      measurements.getOrderedMeasurements());

    DataReductionRecord dataReductionRecord = reducer.performDataReduction(
      instrument, getTestMeasurement(measurements), allSensorValues,
      getConnection());

    assertEquals(Double.NaN,
      dataReductionRecord.getCalculationValue("Zero S₂beam"), 0.001D);
    assertEquals(Double.NaN, dataReductionRecord.getCalculationValue("S₂beam"));
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
