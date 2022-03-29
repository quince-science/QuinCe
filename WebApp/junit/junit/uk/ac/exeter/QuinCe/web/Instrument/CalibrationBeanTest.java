package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationDateException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationTargetException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.SensorCalibrationDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;
import uk.ac.exeter.QuinCe.web.Instrument.InvalidCalibrationEditException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the {@link CalibrationBean} class.
 *
 * <p>
 * These tests use a stub {@link CalibrationBean} since it's an abstract class.
 * The test stub can use either {@link ExternalStandardDB} or
 * {@link SensorCalibrationDB} for its database instance according to the needs
 * of the tests.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class CalibrationBeanTest extends BaseTest {

  /**
   * The test instrument ID.
   */
  private static final long INSTRUMENT_ID = 1000000L;

  /**
   * The test instrument name.
   */
  private static final String INSTRUMENT_NAME = "Test Instrument";

  /**
   * An ID for an existing calibration
   */
  private static final long EXISTING_CALIBRATION = 1001L;

  /**
   * An ID for a non-existent calibration
   */
  private static final long NON_EXISTENT_CALIBRATION = 2000L;

  /**
   * Initialise the {@link ResourceManager} anew for each test.
   */
  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  /**
   * Destroy the {@link ResourceManager} after each test.
   */
  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Create an initialised bean using a default (unspecified)
   * {@link CalibrationDB} back end. Use this when the exact back end being used
   * will not affect the outcome of the test.
   *
   * @return The initialised bean.
   * @throws InstrumentException
   *           If the instrument is not defined in the test setup.
   * @throws RecordNotFoundException
   *           If any other required records are missing.
   * @throws DatabaseException
   *           If any other database error is encountered.
   * @throws MissingParamException
   *           If any function calls are invalid.
   */
  public static CalibrationBean initBean() throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    return initBean(ExternalStandardDB.getInstance(), true);
  }

  /**
   * Create an initialised bean with the specified {@link CalibrationDB}
   * instance as its back end.
   *
   * @param dbInstance
   *          The {@link CalibrationDB} instance to use
   * @return The initialised bean
   * @throws InstrumentException
   *           If the instrument is not defined in the test setup.
   * @throws RecordNotFoundException
   *           If any other required records are missing.
   * @throws DatabaseException
   *           If any other database error is encountered.
   * @throws MissingParamException
   *           If any function calls are invalid.
   */
  public static CalibrationBean initBean(CalibrationDB dbInstance,
    boolean affectFollowingOnly) throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {

    CalibrationBean bean = new CalibrationBeanTestStub(dbInstance,
      affectFollowingOnly);

    // Set the instrument details
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();
    return bean;
  }

  /**
   * Create an initialised bean pre-loaded with the specified calibration
   * details.
   *
   * @param calibrationId
   *          The calibration's database ID.
   * @param deploymentDate
   *          The calibration date.
   * @param target
   *          The calibration target.
   * @return The initialised bean.
   * @throws InstrumentException
   *           If the instrument is not defined in the test setup.
   * @throws RecordNotFoundException
   *           If any other required records are missing.
   * @throws DatabaseException
   *           If any other database error is encountered.
   * @throws MissingParamException
   *           If any function calls are invalid.
   */
  public static CalibrationBean initBean(long calibrationId,
    LocalDateTime deploymentDate, String target) throws RecordNotFoundException,
    MissingParamException, DatabaseException, InstrumentException {
    CalibrationBean bean = initBean();

    bean.setSelectedCalibrationId(calibrationId);
    bean.loadSelectedCalibration();
    bean.getCalibration().setDeploymentDate(deploymentDate);
    bean.getCalibration().setTarget(target);

    return bean;
  }

  /**
   * Create an initialised bean pre-loaded with the specified
   * {@link CalibrationDB} back end, pre-loaded with the specified calibration
   * and with an edit action set.
   *
   * @param dbInstance
   *          The {@link CalibrationDB} instance to use
   * @param editAction
   *          The edit action being performed.
   * @param calibrationId
   *          The calibration's database ID.
   * @param deploymentDate
   *          The calibration date.
   * @param target
   *          The calibration target.
   * @param affectFollowingOnly
   *          Indicates whether only datasets following the calibration should
   *          be affected by the edit, or datasets before and after the
   *          calibration.
   * @return The initialised bean.
   * @throws InstrumentException
   *           If the instrument is not defined in the test setup.
   * @throws RecordNotFoundException
   *           If any other required records are missing.
   * @throws DatabaseException
   *           If any other database error is encountered.
   * @throws MissingParamException
   *           If any function calls are invalid.
   */
  public static CalibrationBean initBean(CalibrationDB dbInstance,
    int editAction, long calibrationId, LocalDateTime deploymentDate,
    String target, boolean affectFollowingOnly) throws RecordNotFoundException,
    MissingParamException, DatabaseException, InstrumentException {

    CalibrationBean bean = initBean(dbInstance, affectFollowingOnly);

    bean.setSelectedCalibrationId(calibrationId);
    bean.loadSelectedCalibration();
    bean.setEditAction(editAction);

    if (null != deploymentDate) {
      bean.getCalibration().setDeploymentDate(deploymentDate);
    }

    if (null != target) {
      bean.getCalibration().setTarget(target);
    }

    return bean;
  }

  /**
   * Test that the bean can be successfully initialised.
   *
   * <p>
   * Specific tests for getters and setters are in separate tests.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void initBeanTest() throws Exception {
    CalibrationBean bean = initBean();
    assertNotNull(bean);
  }

  /**
   * {@link CalibrationBean#getInstrumentId()}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getInstrumentIdTest() throws Exception {
    CalibrationBean bean = initBean();
    assertEquals(INSTRUMENT_ID, bean.getInstrumentId());
  }

  /**
   * {@link CalibrationBean#getInstrumentName()}
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getInstrumentNameTest() throws Exception {
    CalibrationBean bean = initBean();
    assertEquals(INSTRUMENT_NAME, bean.getInstrumentName());
  }

  /**
   * {@link CalibrationBean#setInstrumentId(long)}
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void setInstrumentIdTest() throws Exception {
    CalibrationBean bean = initBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    assertEquals(INSTRUMENT_ID, bean.getInstrumentId());
  }

  // ******************************************
  //
  // getAffectedDatasets tests - invalid cases

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} with a
   * {@code null} {@code newTime} and a present {@code newTarget} throws an
   * exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNullTimeOnlyTest() throws Exception {
    CalibrationBean bean = initBean(EXISTING_CALIBRATION, null, "TARGET1");
    assertThrows(InvalidCalibrationEditException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} with a
   * {@code null} {@code newTime} and a present {@code newTarget} throws an
   * exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNullTargetOnlyTest() throws Exception {
    CalibrationBean bean = initBean(EXISTING_CALIBRATION,
      LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), null);
    assertThrows(InvalidCalibrationEditException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} to make a
   * new calibration with a non-existent target throws an exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNewWithNonExistentTargetTest()
    throws Exception {
    CalibrationBean bean = initBean(-1L,
      LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), "Flurble");
    assertThrows(InvalidCalibrationTargetException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} to make
   * edit a calibration with a non-existent target throws an exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsEditWithNonExistentTargetTest()
    throws Exception {
    CalibrationBean bean = initBean(EXISTING_CALIBRATION,
      LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), "Flurble");
    assertThrows(InvalidCalibrationTargetException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} to make a
   * new calibration with a date in the future throws an exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNewWithFutureDateTest() throws Exception {

    LocalDateTime future = LocalDateTime
      .ofInstant(Instant.now(), ZoneId.of("UTC")).plusDays(1);

    CalibrationBean bean = initBean(-1L, future, "TARGET1");

    assertThrows(InvalidCalibrationDateException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} to make
   * edit a calibration with a date in the future throws an exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsEditWithFutureDateTest() throws Exception {

    LocalDateTime future = LocalDateTime
      .ofInstant(Instant.now(), ZoneId.of("UTC")).plusDays(1);

    CalibrationBean bean = initBean(EXISTING_CALIBRATION, future, "TARGET1");

    assertThrows(InvalidCalibrationDateException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} to delete a
   * calibration with a negative ID throws an exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsDeleteNegativeIDTest() throws Exception {
    CalibrationBean bean = initBean(-1, null, null);

    assertThrows(InvalidCalibrationEditException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling {@link CalibrationBean#getAffectedDatasets()} to delete
   * non-existent calibration throws an exception.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void selectNonExistentCalibrationTest() throws Exception {
    CalibrationBean bean = initBean();

    bean.setSelectedCalibrationId(NON_EXISTENT_CALIBRATION);
    assertThrows(RecordNotFoundException.class, () -> {
      bean.loadSelectedCalibration();
    });
  }

  /**
   * Test that adding the first calibration for a target before all datasets
   * works and all datasets are affected (and can be recalculated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/datasets_only" })
  @Test
  public void addFirstCalibrationBeforeAllPriorsRequiredTest()
    throws Exception {

    CalibrationBean bean = initBean(ExternalStandardDB.getInstance(),
      CalibrationBean.ADD_ACTION, -1, LocalDateTime.of(2019, 6, 1, 0, 0, 0),
      "TARGET1", true);

    bean.calcAffectedDataSets();

    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertTrue(affectedDatasetMatches(affected, "A", true));
    assertTrue(affectedDatasetMatches(affected, "B", true));

  }

  /**
   * Test that adding the first calibration for a target after all datasets
   * works and all datasets are affected (and can be recalculated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/datasets_only" })
  @Test
  public void addFirstCalibrationAfterAllPriorsRequiredTest() throws Exception {

    CalibrationBean bean = initBean(ExternalStandardDB.getInstance(),
      CalibrationBean.ADD_ACTION, -1, LocalDateTime.of(2019, 6, 20, 0, 0, 0),
      "TARGET1", true);

    bean.calcAffectedDataSets();
    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertEquals(0, affected.size());
  }

  /**
   * Test that adding the first calibration for a target between datasets works
   * and all datasets are affected (and can be recalculated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/datasets_only" })
  @Test
  public void addFirstCalibrationBetweenPriorsRequiredTest() throws Exception {

    CalibrationBean bean = initBean(ExternalStandardDB.getInstance(),
      CalibrationBean.ADD_ACTION, -1, LocalDateTime.of(2019, 6, 7, 0, 0, 0),
      "TARGET1", true);

    bean.calcAffectedDataSets();
    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertTrue(affectedDatasetMatches(affected, "B", true));
  }

  /**
   * Test that adding the first calibration for a target before all datasets
   * works and all datasets are affected (and can be recalculated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/datasets_only" })
  @Test
  public void addFirstCalibrationBeforeAllPriorsNotRequiredTest()
    throws Exception {

    CalibrationBean bean = initBean(SensorCalibrationDB.getInstance(),
      CalibrationBean.ADD_ACTION, -1, LocalDateTime.of(2019, 6, 1, 0, 0, 0),
      "1001", true);

    bean.calcAffectedDataSets();

    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertTrue(affectedDatasetMatches(affected, "A", true));
    assertTrue(affectedDatasetMatches(affected, "B", true));

  }

  /**
   * Test that adding the first calibration for a target after all datasets
   * works and all datasets are affected (and can be recalculated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/datasets_only" })
  @Test
  public void addFirstCalibrationAfterAllPriorsNotRequiredTest()
    throws Exception {

    CalibrationBean bean = initBean(SensorCalibrationDB.getInstance(),
      CalibrationBean.ADD_ACTION, -1, LocalDateTime.of(2019, 6, 20, 0, 0, 0),
      "1001", true);

    bean.calcAffectedDataSets();
    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertEquals(0, affected.size());
  }

  /**
   * Test that adding the first calibration for a target between datasets works
   * and only following datasets are affected (and can be recalculated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   *
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/datasets_only" })
  @Test
  public void addFirstCalibrationBetweenPriorsNotRequiredTest()
    throws Exception {

    CalibrationBean bean = initBean(SensorCalibrationDB.getInstance(),
      CalibrationBean.ADD_ACTION, -1, LocalDateTime.of(2019, 6, 7, 0, 0, 0),
      "1001", true);

    bean.calcAffectedDataSets();
    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertTrue(affectedDatasetMatches(affected, "B", true));
  }

  /**
   * Test deleting the only calibration when prior calibrations are required.
   *
   * @throws Exception
   *           If an internal error occurs
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/priorsRequiredSingleCalibrationSingleDataset" })
  @Test
  public void deleteOnlyCalibrationPriorsRequiredTest()
    throws RecordNotFoundException, InvalidCalibrationTargetException,
    InvalidCalibrationDateException, MissingParamException,
    InvalidCalibrationEditException, DatabaseException, InstrumentException {

    CalibrationBean bean = initBean(ExternalStandardDB.getInstance(),
      CalibrationBean.DELETE_ACTION, 1001, null, null, true);

    bean.calcAffectedDataSets();
    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertTrue(affectedDatasetMatches(affected, "A", false));
  }

  /**
   * Test deleting the only calibration when prior calibrations are not
   * required.
   *
   * @throws Exception
   *           If an internal error occurs
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/priorsNotRequiredSingleCalibrationSingleDataset" })
  @Test
  public void deleteOnlyCalibrationPriorsNotRequiredTest() throws Exception {

    CalibrationBean bean = initBean(SensorCalibrationDB.getInstance(),
      CalibrationBean.DELETE_ACTION, 1001, null, null, true);

    bean.calcAffectedDataSets();
    Map<String, Boolean> affected = getDatasetNamesMap(
      bean.getAffectedDatasets());

    assertTrue(affectedDatasetMatches(affected, "A", true));
  }

  /**
   * Convert a map of {@code <Dataset, Boolean>} to a map of
   * {@code <Dataset Name, Boolean>}.
   *
   * @param input
   *          The Dataset map
   * @return The dataset name map
   */
  public static TreeMap<String, Boolean> getDatasetNamesMap(
    Map<DataSet, Boolean> input) {
    TreeMap<String, Boolean> result = new TreeMap<String, Boolean>();

    for (Map.Entry<DataSet, Boolean> entry : input.entrySet()) {
      result.put(entry.getKey().getName(), entry.getValue());
    }

    return result;
  }

  /**
   * Test whether or not the named dataset is present in the specified
   * collection of affected datasets, and that the {@code canBeReprocessed} flag
   * matches the specified value.
   *
   * @param affectedDatasets
   *          The affected datasets from a
   *          {@link CalibrationBean#getAffectedDatasets()} call.
   * @param name
   *          The expected dataset name
   * @param canBeReprocessed
   *          The expected "Can be reprocessed" flag value.
   * @return {@code true} if the dataset name an "Can be reprocessed" flag match
   *         the specified values; {@code false} otherwise.
   */
  private static boolean affectedDatasetMatches(
    Map<String, Boolean> affectedDatasets, String name,
    boolean canBeReprocessed) {

    boolean result = false;

    if (affectedDatasets.containsKey(name)) {
      if (affectedDatasets.get(name).equals(canBeReprocessed)) {
        result = true;
      }
    }

    return result;
  }

}
