package junit.uk.ac.exeter.QuinCe.web.Instrument;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 *
 *
 * @author Steve Jones
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public class GetAffectedDatasetsPriorsRequiredTests
  extends GetAffectedDatasetsTests {

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Tests that editing calibrations detects the correct set of affected
   * datasets.
   *
   * @param line
   *          The test line.
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/editPriorsRequired" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getAffectedDatasetsTest(TestSetLine line) throws Exception {

    runTest(line);
  }

  @Override
  protected CalibrationDB getDbInstance() {
    return ExternalStandardDB.getInstance();
  }

  @Override
  protected String getTestSetName() {
    return "CalibrationBean_getAffectedDatasetsPriorsRequiredTests";
  }
}
