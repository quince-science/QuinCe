package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
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
 * <p>
 * Tests for invalid calls to
 * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
 * are provided by this class. Tests for valid calls are provided by
 * {@link GetAffectedDatasetsPriorsRequiredTests}.
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

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Create an initialised bean using a default {@CalibrationDB} back end. Use
   * this when the exact back end being used will not affect the outcome of the
   * test.
   *
   * @return The initialised bean.
   */
  public static CalibrationBean initBean() {
    return initBean(ExternalStandardDB.getInstance());
  }

  /**
   * Create an initialised bean for tests.
   *
   * <p>
   * This method is tested by {@link #initBeanTest()}.
   * </p>
   */

  /**
   * Create an initialised bean with the specified {@link CalibrationDB}
   * instance as its back end.
   *
   * @param dbInstance
   *          The {@link CalibrationDB} instance to use
   * @return The initialised bean
   */
  public static CalibrationBean initBean(CalibrationDB dbInstance) {

    CalibrationBean bean = new CalibrationBeanTestStub(dbInstance);

    // Set the instrument details
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.setInstrumentName(INSTRUMENT_NAME);
    bean.start();
    return bean;
  }

  public static CalibrationBean initBean(long calibrationId,
    LocalDateTime deploymentDate, String target)
    throws RecordNotFoundException {
    CalibrationBean bean = initBean();

    bean.setSelectedCalibrationId(calibrationId);
    bean.loadSelectedCalibration();
    bean.getCalibration().setDeploymentDate(deploymentDate);
    bean.getCalibration().setTarget(target);

    return bean;
  }

  public static CalibrationBean initBean(CalibrationDB dbInstance,
    int editAction, long calibrationId, LocalDateTime deploymentDate,
    String target) throws RecordNotFoundException {

    CalibrationBean bean = initBean(dbInstance);

    bean.setSelectedCalibrationId(calibrationId);
    bean.loadSelectedCalibration();
    bean.setEditAction(editAction);
    bean.getCalibration().setDeploymentDate(deploymentDate);
    bean.getCalibration().setTarget(target);

    return bean;
  }

  /**
   * Test that the bean can be successfully initialised.
   *
   * <p>
   * Specific tests for getters and setters are in separate tests.
   * </p>
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void initBeanTest() throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {
    CalibrationBean bean = initBean();
    assertNotNull(bean);
  }

  /**
   * Test that {@link CalibrationBean#getInstrumentId()} works.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getInstrumentIdTest() throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    CalibrationBean bean = initBean();
    assertEquals(INSTRUMENT_ID, bean.getInstrumentId());
  }

  /**
   * Test that {@link CalibrationBean#getInstrumentName()} works.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getInstrumentNameTest() throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    CalibrationBean bean = initBean();
    assertEquals(INSTRUMENT_NAME, bean.getInstrumentName());
  }

  /**
   * Test that {@link CalibrationBean#setInstrumentId()} works.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void setInstrumentIdTest() throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    CalibrationBean bean = initBean();
    bean.setInstrumentId(1000L);
    assertEquals(1000L, bean.getInstrumentId());
  }

  /**
   * Test that {@link CalibrationBean#setInstrumentName()} works.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void setInstrumentNameTest() throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    CalibrationBean bean = initBean();
    bean.setInstrumentName("NEW NAME");
    assertEquals("NEW NAME", bean.getInstrumentName());
  }

  // ******************************************
  //
  // getAffectedDataSets tests - invalid cases

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * with a {@code null} {@code newTime} and a present {@code newTarget} throws
   * an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNullTimeOnlyTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {
    CalibrationBean bean = initBean(EXISTING_CALIBRATION, null, "TARGET1");
    assertThrows(InvalidCalibrationEditException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * with a {@code null} {@code newTime} and a present {@code newTarget} throws
   * an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNullTargetOnlyTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {
    CalibrationBean bean = initBean(EXISTING_CALIBRATION,
      LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), null);
    assertThrows(InvalidCalibrationEditException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * to make a new calibration with a non-existent target throws an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNewWithNonExistentTargetTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {
    CalibrationBean bean = initBean(-1L,
      LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), "Flurble");
    assertThrows(InvalidCalibrationTargetException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * to make edit a calibration with a non-existent target throws an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsEditWithNonExistentTargetTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {
    CalibrationBean bean = initBean(EXISTING_CALIBRATION,
      LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), "Flurble");
    assertThrows(InvalidCalibrationTargetException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * to make a new calibration with a date in the future throws an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsNewWithFutureDateTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {

    LocalDateTime future = LocalDateTime
      .ofInstant(Instant.now(), ZoneId.of("UTC")).plusDays(1);

    CalibrationBean bean = initBean(-1L, future, "TARGET1");

    assertThrows(InvalidCalibrationDateException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * to make edit a calibration with a date in the future throws an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsEditWithFutureDateTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {

    LocalDateTime future = LocalDateTime
      .ofInstant(Instant.now(), ZoneId.of("UTC")).plusDays(1);

    CalibrationBean bean = initBean(EXISTING_CALIBRATION, future, "TARGET1");

    assertThrows(InvalidCalibrationDateException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * to delete a calibration with a negative ID throws an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void getAffectedDatasetsDeleteNegativeIDTest()
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {
    CalibrationBean bean = initBean(-1, null, null);

    assertThrows(InvalidCalibrationEditException.class, () -> {
      bean.calcAffectedDataSets();
    });
  }

  /**
   * Test that calling
   * {@link CalibrationBean#getAffectedDataSets(long, java.time.LocalDateTime, String)}
   * to delete non-existent calibration throws an exception.
   *
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/simple" })
  @Test
  public void selectNonExistentCalibrationTest() throws MissingParamException,
    DatabaseException, RecordNotFoundException, InstrumentException {
    CalibrationBean bean = initBean();

    bean.setSelectedCalibrationId(NON_EXISTENT_CALIBRATION);
    assertThrows(RecordNotFoundException.class, () -> {
      bean.loadSelectedCalibration();
    });
  }
}
