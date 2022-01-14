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
public class CalibrationBeanEditCalibrationValueTest
  extends CalibrationBeanEditCalibrationsTest {

  private static final int DATASET_COL = 0;

  private static final int FOLLOWING_ONLY_COL = 1;

  private static final int AFFECTED_COL = 2;

  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calibrationEditing" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getAffectedDatasetsTest(TestSetLine line) throws Exception {

    long calibrationId = getCalibrationId(line.getCharField(DATASET_COL),
      false);
    boolean affectFollowingOnly = line.getBooleanField(FOLLOWING_ONLY_COL);
    TreeSet<String> expectedAffectedDatasets = line
      .getOrderedCharListField(AFFECTED_COL);

    runTest(CalibrationBean.EDIT_ACTION, calibrationId, NO_POSITION,
      affectFollowingOnly, expectedAffectedDatasets,
      NO_EXPECTED_FAILED_DATASETS, PRIORS_NOT_REQUIRED_TARGET);
  }

  @Override
  protected String getTestSetName() {
    return "CalibrationBeanEditCalibrationValue";
  }
}
