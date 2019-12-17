package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.web.Instrument.ExternalStandardsBean;

public class ExternalStandardsBeanTest extends BaseTest {

  /**
   * The test instrument ID.
   */
  private static final long INSTRUMENT_ID = 1000000L;

  /**
   * The test instrument name.
   */
  private static final String INSTRUMENT_NAME = "Test Instrument";

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  /**
   * Create a new bean and initialise it.
   */
  private ExternalStandardsBean initBean() {
    ExternalStandardsBean bean = new ExternalStandardsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.setInstrumentName(INSTRUMENT_NAME);
    bean.start();
    return bean;
  }

  /**
   * Test that the bean can be successfully initialised.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/base" })
  @Test
  public void initBeanTest() {
    ExternalStandardsBean bean = initBean();
    assertNotNull(bean);
  }
}
