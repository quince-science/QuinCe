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
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;

/**
 * Tests of multiple calibration edits for the
 * {@link CalculationCoefficientsBean}.
 *
 * <p>
 * Each test consists of two edit actions followed by a check of which DataSets
 * have been marked for reprocessing along with flags indicating whether
 * reprocessing is possible.
 * </p>
 *
 * <p>
 * Much of the code for these tests is inherited from
 * {@link CalculationCoefficientsBeanTest}.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CalculationCoefficientsBeanMultipleEditTest
  extends CalculationCoefficientsBeanEditTest {

  /**
   * TestSet column number of the first edit action.
   */
  private static final int ACTION_1_COL = 0;

  /**
   * TestSet column number of the DataSet ID for the first edit.
   *
   * <p>
   * Will be ignored for ADD actions.
   * </p>
   */
  private static final int CALIBRATION_ID_1_COL = 1;

  /**
   * TestSet column number for the month to set on the first edit.
   *
   * @see #getCalibrationTime(int)
   */
  private static final int MONTH_1_COL = 2;

  /**
   * TestSet column number for the calibration target of the first edit.
   */
  private static final int TARGET_1_COL = 3;

  /**
   * TestSet column number for the flag indicating whether or not the
   * calibration's value should be changed in the first edit.
   */
  private static final int CHANGE_VALUE_1_COL = 4;

  /**
   * TestSet column number of the second edit action.
   */
  private static final int ACTION_2_COL = 5;

  /**
   * TestSet column number of the DataSet ID for the second edit.
   *
   * <p>
   * Will be ignored for ADD actions.
   * </p>
   **/
  private static final int CALIBRATION_ID_2_COL = 6;

  /**
   * TestSet column number for the month to set on the second edit.
   *
   * @see #getCalibrationTime(int)
   */
  private static final int MONTH_2_COL = 7;

  /**
   * TestSet column number for the calibration target of the second edit.
   */
  private static final int TARGET_2_COL = 8;

  /**
   * TestSet column number for the flag indicating whether or not the
   * calibration's value should be changed in the second edit.
   */
  private static final int CHANGE_VALUE_2_COL = 9;

  /**
   * TestSet column number for the flag indicating whether or not the edits
   * performed should be flagged as invalid.
   */
  private static final int INVALID_COL = 10;

  /**
   * TestSet column number for the list of DataSets affected by the two edits.
   */
  private static final int AFFECTED_DATASETS_COL = 11;

  /**
   * TestSet column number for the list of flags specifying whether the affected
   * DataSets can be recalculated.
   */
  private static final int CAN_REPROCESS_COL = 12;

  /**
   * Value to use when editing the value of an existing coefficient on the
   * second edit.
   *
   * <p>
   * The value set on the first edit will be {@link #REPLACEMENT_VALUE}.
   * </p>
   */
  private static final String REPLACEMENT_VALUE_2 = "1020";

  /**
   * Perform the tests from the TestSet.
   *
   * <p>
   * <b>NB:</b> The expected results in the TestSet file must be in ascending
   * DataSet ID order.
   * </p>
   *
   * @param line
   *          A TestSet line.
   * @throws Exception
   *           If any edit fails unexpectedly.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void multipleCalibrationEditTest(TestSetLine line) throws Exception {

    // Initialise bean
    CalculationCoefficientsBean bean = init();

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

  /**
   * Perform an edit action for the test.
   *
   * @param bean
   *          The edit bean.
   * @param action
   *          The edit action.
   * @param calibrationId
   *          The ID of the {@link CalculationCoefficient} being edited.
   * @param calibrationTime
   *          The new time of the coefficient.
   * @param target
   *          The new target coefficient.
   * @param changeValue
   *          Indicates whether or not the coefficient's value should be
   *          changed.
   * @param replacementValue
   *          The new value for the coefficient.
   * @throws Exception
   *           If the edit fails.
   */
  private void doAction(CalculationCoefficientsBean bean, int action,
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
          CalculationCoefficientsBeanTest.makeCoefficients(replacementValue));
      }
    }

    bean.saveCalibration();
  }

  @Override
  protected String getTestSetName() {
    return "CalculationCoefficientsBeanMultipleEditTest";
  }

}
