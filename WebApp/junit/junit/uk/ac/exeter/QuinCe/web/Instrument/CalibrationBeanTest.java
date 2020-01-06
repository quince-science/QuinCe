package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;

/**
 * Tests for the {@link CalibrationBean} class.
 *
 * <p>
 * These tests use a mocked {@link CalibrationBean} since it's an abstract
 * class.
 * </p>
 * <p>
 * The database instance (provided by {@code CalibrationBean.getDbInstance()})
 * will also be mocked to report the correct calibration types and targets.
 * These types and targets will exist in the database through the Flyway
 * migrations but require the mocks to ensure the queries search for the correct
 * items.
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
   * The calibration type used for these tests
   */
  private static final String CALIBRATION_TYPE = "CALIBRATION_TEST";

  /**
   * The calibration targets used for these tests
   */
  private static final String[] CALIBRATION_TARGETS = new String[] { "TARGET1",
    "TARGET2", "TARGET3" };

  private static Map<String, String> targets = null;

  static {
    targets = new HashMap<String, String>();
    for (String target : CALIBRATION_TARGETS) {
      targets.put(target, target);
    }
  }

  /**
   * A mocked instance of the {@link CalibrationDB} class.
   */
  private CalibrationDB mockDbInstance;

  @BeforeEach
  public void setup() throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    initResourceManager();

    // Mock CalibrationDB instance
    mockDbInstance = Mockito.mock(CalibrationDB.class);
    Mockito.when(mockDbInstance.getCalibrationType())
      .thenReturn(CALIBRATION_TYPE);
    Mockito.when(mockDbInstance.getTargets(Mockito.any(Connection.class),
      Mockito.eq(INSTRUMENT_ID))).thenReturn(targets);
  }

  /**
   * Create an initialised bean for tests.
   *
   * <p>
   * This method is tested by {@link #initBeanTest()}.
   * </p>
   */
  private CalibrationBean initBean() {

    // Mock a calibration bean
    //
    // The database instance will also be mocked to provide the correct
    // calibration types and targets.
    CalibrationBean bean = Mockito.mock(CalibrationBean.class,
      Mockito.CALLS_REAL_METHODS);
    Mockito.when(ReflectionTestUtils.invokeMethod(bean, "getDbInstance"))
      .thenReturn(mockDbInstance);

    // Set the instrument details
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.setInstrumentName(INSTRUMENT_NAME);
    bean.start();
    return bean;
  }

  /**
   * Test that the bean can be successfully initialised.
   *
   * <p>
   * Specific tests for getters and setters are in separate tests.
   * </p>
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/base",
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/simple" })
  @Test
  public void initBeanTest() {
    CalibrationBean bean = initBean();
    assertNotNull(bean);
  }

  /**
   * Test that {@link CalibrationBean#getInstrumentId()} works.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/base",
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/simple" })
  @Test
  public void getInstrumentIdTest() {
    CalibrationBean bean = initBean();
    assertEquals(INSTRUMENT_ID, bean.getInstrumentId());
  }

  /**
   * Test that {@link CalibrationBean#getInstrumentName()} works.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/base",
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/simple" })
  @Test
  public void getInstrumentNameTest() {
    CalibrationBean bean = initBean();
    assertEquals(INSTRUMENT_NAME, bean.getInstrumentName());
  }
}
