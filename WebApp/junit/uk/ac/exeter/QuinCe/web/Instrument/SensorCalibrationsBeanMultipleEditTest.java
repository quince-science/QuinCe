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

/**
 * Tests of multiple calibration edits for the
 * {@link CalculationCoefficientsBean}.
 *
 * <p>
 * Much of the code for these tests is inherited from
 * {@link CalculationCoefficientsBeanTest}.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SensorCalibrationsBeanMultipleEditTest
  extends SensorCalibrationsBeanEditTest {

  private static final int ACTION_1_COL = 0;

  private static final int CALIBRATION_ID_1_COL = 1;

  private static final int MONTH_1_COL = 2;

  private static final int TARGET_1_COL = 3;

  private static final int CHANGE_VALUE_1_COL = 4;

  private static final int ACTION_2_COL = 5;

  private static final int CALIBRATION_ID_2_COL = 6;

  private static final int MONTH_2_COL = 7;

  private static final int TARGET_2_COL = 8;

  private static final int CHANGE_VALUE_2_COL = 9;

  private static final int INVALID_COL = 10;

  private static final int AFFECTED_DATASETS_COL = 11;

  private static final int CAN_REPROCESS_COL = 12;

  private static final String REPLACEMENT_VALUE_2 = "1020";

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
    "resources/sql/web/Instrument/CalibrationBeanTest/sensorCalibrationsEdit" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void multipleCalibrationEditTest(TestSetLine line) throws Exception {

    // Initialise bean
    SensorCalibrationsBean bean = init();

    int action1 = getAction(line, ACTION_1_COL);
    long calibrationId1 = line.getLongField(CALIBRATION_ID_1_COL);
    LocalDateTime calibrationTime1 = getCalibrationTime(
      line.getIntField(MONTH_1_COL));
    String target1 = line.getStringField(TARGET_1_COL, true);
    boolean changeValue1 = line.getBooleanField(CHANGE_VALUE_1_COL);

    int action2 = getAction(line, ACTION_2_COL);
    long calibrationId2 = line.getLongField(CALIBRATION_ID_2_COL);
    LocalDateTime calibrationTime2 = getCalibrationTime(
      line.getIntField(MONTH_2_COL));
    String target2 = line.getStringField(TARGET_2_COL, true);
    boolean changeValue2 = line.getBooleanField(CHANGE_VALUE_2_COL);

    boolean shouldBeInvalid = line.getBooleanField(INVALID_COL);
    List<Long> affectedDatasetIds = line
      .getLongListField(AFFECTED_DATASETS_COL);
    List<Boolean> canReprocessDatasets = line
      .getBooleanListField(CAN_REPROCESS_COL);

    if (affectedDatasetIds.size() != canReprocessDatasets.size()) {
      throw new IllegalArgumentException(
        "Affected Datasets and Can Reprocess lists are different sizes");
    }

    doAction(bean, action1, calibrationId1, calibrationTime1, target1,
      changeValue1, REPLACEMENT_VALUE);
    assertTrue(bean.editedCalibrationValid(), "Action 1 invalid");

    doAction(bean, action2, calibrationId2, calibrationTime2, target2,
      changeValue2, REPLACEMENT_VALUE_2);

    assertEquals(shouldBeInvalid, !bean.editedCalibrationValid(),
      "Action 2 valid status incorrect");

    if (!shouldBeInvalid) {
      TreeMap<Long, Boolean> affectedDatasets = bean.getAffectedDatasets();

      assertEquals(affectedDatasetIds.size(), affectedDatasets.size(),
        "Incorrect number of affected datasets");

      String affectedDatasetsTestString = makeTestString(
        bean.getAffectedDatasets());
      String expectedDatasetsTestString = makeTestString(affectedDatasetIds,
        canReprocessDatasets);

      assertEquals(expectedDatasetsTestString, affectedDatasetsTestString);
    }
  }

  private void doAction(SensorCalibrationsBean bean, int action,
    long calibrationId, LocalDateTime calibrationTime, String target,
    boolean changeValue, String replacementValue) throws Exception {

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
          SensorCalibrationsBeanTest.makeCoefficients(replacementValue));
      }
    }

    bean.saveCalibration();
  }

  @Override
  protected String getTestSetName() {
    return "SensorCalibrationsBeanMultipleEditTest";
  }

}
