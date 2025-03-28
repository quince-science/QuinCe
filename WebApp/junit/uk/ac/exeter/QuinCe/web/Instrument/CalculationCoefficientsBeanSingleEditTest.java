package uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;

@TestInstance(Lifecycle.PER_CLASS)
public class CalculationCoefficientsBeanSingleEditTest
  extends CalculationCoefficientsBeanEditTest {

  private static final int ACTION_COL = 0;

  private static final int CALIBRATION_ID_COL = 1;

  private static final int MONTH_COL = 2;

  private static final int TARGET_COL = 3;

  private static final int CHANGE_VALUE_COL = 4;

  private static final int AFFECTED_DATASETS_COL = 5;

  private static final int CAN_REPROCESS_COL = 6;

  /**
   * <b>NB:</b> The expected results in the {@link TestSet} file must be in
   * ascending Dataset ID order.
   *
   * @param line
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void singleCalibrationEditTest(TestSetLine line) throws Exception {

    // Initialise bean
    CalculationCoefficientsBean bean = init();

    int action = getAction(line, ACTION_COL);
    long calibrationId = line.getLongField(CALIBRATION_ID_COL);
    LocalDateTime calibrationTime = getCalibrationTime(
      line.getIntField(MONTH_COL));
    String target = line.getStringField(TARGET_COL, true);
    boolean changeValue = line.getBooleanField(CHANGE_VALUE_COL);
    List<Long> affectedDatasetIds = line
      .getLongListField(AFFECTED_DATASETS_COL);
    List<Boolean> canReprocessDatasets = line
      .getBooleanListField(CAN_REPROCESS_COL);

    if (affectedDatasetIds.size() != canReprocessDatasets.size()) {
      throw new IllegalArgumentException(
        "Affected Datasets and Can Reprocess lists are different sizes");
    }

    bean.setAction(action);
    if (action == CalibrationEdit.ADD) {
      bean.newCalibration();
    } else {
      bean.setSelectedCalibrationId(calibrationId);
      bean.loadSelectedCalibration();
    }

    if (action != CalibrationEdit.DELETE) {
      bean.getEditedCalibration().setDeploymentDate(calibrationTime);
      bean.getEditedCalibration().setTarget(target);

      if (changeValue || action == CalibrationEdit.ADD) {
        bean.getEditedCalibration().setCoefficients(
          CalculationCoefficientsBeanTest.makeCoefficients(REPLACEMENT_VALUE));
      }
    }

    bean.saveCalibration();
    assertTrue(bean.editedCalibrationValid());

    TreeMap<Long, Boolean> affectedDatasets = bean.getAffectedDatasets();

    assertEquals(affectedDatasetIds.size(), affectedDatasets.size(),
      "Incorrect number of affected datasets");

    String affectedDatasetsTestString = makeTestString(
      bean.getAffectedDatasets());
    String expectedDatasetsTestString = makeTestString(affectedDatasetIds,
      canReprocessDatasets);

    assertEquals(expectedDatasetsTestString, affectedDatasetsTestString);
  }

  @Override
  protected String getTestSetName() {
    return "CalculationCoefficientsSingleCalibrationEditTest";
  }
}
