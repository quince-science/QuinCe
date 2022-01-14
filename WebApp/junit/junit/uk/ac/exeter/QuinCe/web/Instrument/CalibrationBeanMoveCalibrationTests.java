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
public class CalibrationBeanMoveCalibrationTests
  extends CalibrationBeanEditCalibrationsTest {

  private static final int CALIBRATION_COL = 0;

  private static final int POSITION_COL = 1;

  private static final int PRIORS_REQUIRED_COL = 2;

  private static final int FOLLOWING_ONLY_COL = 3;

  private static final int AFFECTED_COL = 4;

  private static final int FAILED_COL = 5;

  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calibrationEditing" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void getAffectedDatasetsTest(TestSetLine line) throws Exception {

    boolean priorsRequired = line.getBooleanField(PRIORS_REQUIRED_COL);
    long calibrationId = getCalibrationId(line.getCharField(CALIBRATION_COL),
      priorsRequired);
    int position = line.getIntField(POSITION_COL);
    boolean affectFollowingOnly = line.getBooleanField(FOLLOWING_ONLY_COL);
    TreeSet<String> expectedAffectedDatasets = line
      .getOrderedCharListField(AFFECTED_COL);
    TreeSet<String> expectedFailedDatasets = line
      .getOrderedCharListField(FAILED_COL);

    runTest(CalibrationBean.EDIT_ACTION, calibrationId, position,
      affectFollowingOnly, expectedAffectedDatasets, expectedFailedDatasets,
      getTarget(priorsRequired));

  }

  @Override
  protected String getTestSetName() {
    return "CalibrationBeanMoveCalibration";
  }

}
