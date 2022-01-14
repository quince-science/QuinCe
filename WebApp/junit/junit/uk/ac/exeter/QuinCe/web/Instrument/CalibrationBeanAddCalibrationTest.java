package junit.uk.ac.exeter.QuinCe.web.Instrument;

import java.util.TreeSet;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;

@TestInstance(Lifecycle.PER_CLASS)
public class CalibrationBeanAddCalibrationTest
  extends CalibrationBeanEditCalibrationsTest {

  private static final int POSITION_COL = 0;

  private static final int FOLLOWING_ONLY_COL = 1;

  private static final int AFFECTED_COL = 2;

  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calibrationEditing" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getAffectedDatasetsPriorsNotRequiredTest(TestSetLine line)
    throws Exception {

    test(line, PRIORS_NOT_REQUIRED_TARGET);
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calibrationEditing" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getAffectedDatasetsPriorsRequiredTest(TestSetLine line)
    throws Exception {

    test(line, PRIORS_REQUIRED_TARGET);
  }

  private void test(TestSetLine line, String target) throws Exception {

    int position = line.getIntField(POSITION_COL);
    boolean affectFollowingOnly = line.getBooleanField(FOLLOWING_ONLY_COL);
    TreeSet<String> expectedAffectedDatasets = line
      .getOrderedCharListField(AFFECTED_COL);

    runTest(CalibrationBean.ADD_ACTION, -1, position, affectFollowingOnly,
      expectedAffectedDatasets, NO_EXPECTED_FAILED_DATASETS, target);
  }

  @Override
  protected String getTestSetName() {
    return "CalibrationBeanAddCalibration";
  }
}
