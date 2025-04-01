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
 * Tests of single calibration edits for the
 * {@link CalculationCoefficientsBean}.
 *
 * <p>
 * Each test consists of an edit action followed by a check of which DataSets
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
public class CalculationCoefficientsBeanSingleEditTest
  extends CalculationCoefficientsBeanEditTest {

  /**
   * TestSet column number of the edit action.
   */
  private static final int ACTION_COL = 0;

  /**
   * TestSet column number of the DataSet ID for the calibration to edit.
   *
   * <p>
   * Will be ignored for ADD actions.
   * </p>
   */
  private static final int CALIBRATION_ID_COL = 1;

  /**
   * TestSet column number for the month to set on the edit.
   *
   * @see #getCalibrationTime(int)
   */
  private static final int MONTH_COL = 2;

  /**
   * TestSet column number for the calibration target of the edit.
   */
  private static final int TARGET_COL = 3;

  /**
   * TestSet column number for the flag indicating whether or not the
   * calibration's value should be changed in the edit.
   */
  private static final int CHANGE_VALUE_COL = 4;

  /**
   * TestSet column number for the list of DataSets affected by the edit.
   */
  private static final int AFFECTED_DATASETS_COL = 5;

  /**
   * TestSet column number for the list of flags specifying whether the affected
   * DataSets can be recalculated.
   */
  private static final int CAN_REPROCESS_COL = 6;

  /**
   * Perform the tests from the TestSet.
   *
   * <p>
   * <b>NB:</b> The expected results in the TestSet file must be in ascending
   * Dataset ID order.
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
