package uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;

/**
 * Tests of calibration edits for the {@link CalculationCoefficientsBean}.
 *
 * <p>
 * See
 * {@code WebApp/junit/resources/sql/web/Instrument/CalibrationBeanTest/initial_setup.html}
 * for the initial setup ({@code TARGET_1} and {@code TARGET_1} are specified in
 * this test file as fixed fields {@link #TARGET_1} and {@link #TARGET_2}). This
 * will use the CONTROS pCO2 system as the testing basis. Most coefficients will
 * be set with prior and post values for the full period, and we will adjust the
 * coefficients for two parameters ({@code F} and {@code Runtime}) to perform
 * the tests. The {@code k1}, {@code k2} and {@code k3} coefficients will remain
 * constant.
 * </p>
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CalculationCoefficientsBeanTest extends TestSetTest {

  /**
   * TestSet column number of the edit action.
   *
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int ACTION_COL = 0;

  /**
   * TestSet column number of the DataSet ID for the calibration to edit.
   *
   * <p>
   * Will be ignored for ADD actions.
   * </p>
   *
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int CALIBRATION_ID_COL = 1;

  /**
   * TestSet column number for the month to set on the edit.
   *
   * @see #getCalibrationTime(TestSetLine)
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int MONTH_COL = 2;

  /**
   * TestSet column number for the calibration target of the edit.
   *
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int TARGET_COL = 3;

  /**
   * TestSet column number for the flag indicating whether or not the
   * calibration's value should be changed in the edit.
   *
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int CHANGE_VALUE_COL = 4;

  /**
   * TestSet column number for the list of DataSets affected by the edit.
   *
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int AFFECTED_DATASETS_COL = 5;

  /**
   * TestSet column number for the list of flags specifying whether the affected
   * DataSets can be recalculated.
   *
   * @see CalculationCoefficientsBeanTest#singleCalibrationEditTest(TestSetLine)
   */
  private static final int CAN_REPROCESS_COL = 6;

  /**
   * Dummy User ID.
   */
  private static final long USER_ID = 1L;

  /**
   * Dummy Instrument ID.
   */
  private static final long INSTRUMENT_ID = 1L;

  /**
   * Name of the first target in a pair of created calibrations.
   */
  private static final String TARGET_1 = "6.F";

  /**
   * Name of the second target in a pair of created calibrations.
   */
  private static final String TARGET_2 = "6.Runtime";

  /**
   * Value to use when editing the value of an existing coefficient.
   */
  private static final String REPLACEMENT_VALUE = "1000";

  /**
   * Initialise the bean.
   *
   * @return The bean.
   * @throws Exception
   *           If the bean cannot be initialised.
   */
  private CalculationCoefficientsBean init() throws Exception {
    initResourceManager();
    loginUser(USER_ID);
    CalculationCoefficientsBean bean = new CalculationCoefficientsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();
    return bean;
  }

  /**
   * Generate a calibration coefficients map containing the specified value.
   *
   * <p>
   * `CalculationCoefficient`s only contain one value, which is called `Value`
   * in the map.
   * </p>
   *
   * @param value
   *          The coefficient value.
   * @return The coefficients map.
   */
  protected static Map<String, String> makeCoefficients(String value) {
    Map<String, String> result = new HashMap<String, String>();
    result.put("Value", value);
    return result;
  }

  /**
   * Basic tests of calibration edits.
   *
   * <p>
   * <b>NB:</b> The expected results in the TestSet file must be in ascending
   * Dataset ID order.
   * </p>
   *
   * <p>
   * More complex tests are performed in
   * {@link CalculationCoefficientsBeanSingleEditTest} and
   * {@link CalculationCoefficientsBeanMultipleEditTest}.
   * </p>
   *
   * @param line
   *          The test line.
   * @throws Exception
   *           If any test action throws an Exception.
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

    int action = getAction(line);
    long calbrationId = line.getLongField(CALIBRATION_ID_COL);
    LocalDateTime calibrationTime = getCalibrationTime(line);
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
      bean.setSelectedCalibrationId(calbrationId);
      bean.loadSelectedCalibration();
    }

    if (action != CalibrationEdit.DELETE) {
      bean.getEditedCalibration().setDeploymentDate(calibrationTime);
      bean.getEditedCalibration().setTarget(target);

      if (changeValue || action == CalibrationEdit.ADD) {
        bean.getEditedCalibration()
          .setCoefficients(makeCoefficients(REPLACEMENT_VALUE));
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

  /**
   * Get the expected calibrations as a {@link String} for comparison with the
   * {@link TestSetLine}.
   *
   * <p>
   * The {@link CalculationCoefficientsBean} returns the datasets affected by
   * edits as a {@link Map} of {@code Long -> Boolean}. The {@link TestSetLine}s
   * contain the expected outcome as a String of {@code long:boolean;...}. This
   * method converts the {@link Map} to the {@link String} format for
   * comparison.
   * </p>
   *
   * @param input
   *          The map from the bean.
   * @return The string representation.
   */
  private String makeTestString(TreeMap<Long, Boolean> input) {
    StringBuilder result = new StringBuilder();

    for (Map.Entry<Long, Boolean> entry : input.entrySet()) {
      result.append(entry.getKey());
      result.append(':');
      result.append(entry.getValue());
      result.append(';');
    }

    return result.toString();
  }

  /**
   * Get the expected calibrations as a {@link String} for comparison with the
   * {@link TestSetLine}.
   *
   * <p>
   * The {@link TestSetLine}s contain the expected set of datasets affected by
   * calibration edits as a String of {@code long:boolean;...} (dataset ID and
   * whether or not recalculation is possible). This method takes two lists of
   * {@code Long} and {@code Boolean} and converts them to a String of that
   * format.
   * </p>
   *
   * @param ids
   *          The dataset IDs.
   * @param canReprocess
   *          The flags indicating whether or not the datasets can be
   *          reprocessed.
   * @return The string representation.
   */
  private String makeTestString(List<Long> ids, List<Boolean> canReprocess) {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < ids.size(); i++) {
      result.append(ids.get(i));
      result.append(':');
      result.append(canReprocess.get(i));
      result.append(';');
    }

    return result.toString();
  }

  /**
   * Test adding a calibration that clashes with an existing calibration.
   *
   * @throws Exception
   *           If any test action throws an Exception.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @Test
  public void addClashTest() throws Exception {
    CalculationCoefficientsBean bean = init();
    bean.setAction(CalibrationEdit.ADD);
    bean.getEditedCalibration()
      .setDeploymentDate(LocalDateTime.of(2023, 2, 1, 0, 0, 0));
    bean.getEditedCalibration().setTarget(TARGET_1);
    bean.getEditedCalibration()
      .setCoefficients(makeCoefficients(REPLACEMENT_VALUE));

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }

  /**
   * Test editing a calibration to clash with another calibration's time.
   *
   * @throws Exception
   *           If any test action throws an Exception.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @Test
  public void editClashTimeTest() throws Exception {
    CalculationCoefficientsBean bean = init();
    bean.setSelectedCalibrationId(1L);
    bean.loadSelectedCalibration();
    bean.setAction(CalibrationEdit.EDIT);
    bean.getEditedCalibration()
      .setDeploymentDate(LocalDateTime.of(2023, 7, 1, 0, 0, 0));

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }

  /**
   * Test editing a calibration to clash with another calibration's target.
   *
   * @throws Exception
   *           If any test action throws an Exception.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @Test
  public void editClashTargetTest() throws Exception {
    CalculationCoefficientsBean bean = init();
    bean.setSelectedCalibrationId(1L);
    bean.loadSelectedCalibration();
    bean.setAction(CalibrationEdit.EDIT);
    bean.getEditedCalibration().setTarget(TARGET_2);

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }

  /**
   * Test editing a calibration to clash with another calibration's time and
   * target.
   *
   * @throws Exception
   *           If any test action throws an Exception.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @Test
  public void editClashTimeAndTargetTest() throws Exception {
    CalculationCoefficientsBean bean = init();
    bean.setSelectedCalibrationId(1L);
    bean.loadSelectedCalibration();
    bean.setAction(CalibrationEdit.EDIT);
    bean.getEditedCalibration()
      .setDeploymentDate(LocalDateTime.of(2023, 5, 1, 0, 0, 0));
    bean.getEditedCalibration().setTarget(TARGET_2);

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }

  /**
   * Test adding a new calibration in the middle of a dataset.
   *
   * @throws Exception
   *           If any test action throws an Exception.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @Test
  public void addInterimTest() throws Exception {
    CalculationCoefficientsBean bean = init();
    bean.setAction(CalibrationEdit.ADD);
    bean.getEditedCalibration()
      .setDeploymentDate(LocalDateTime.of(2023, 3, 1, 0, 0, 0));
    bean.getEditedCalibration().setTarget(TARGET_1);
    bean.getEditedCalibration()
      .setCoefficients(makeCoefficients(REPLACEMENT_VALUE));

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }

  /**
   * Test moving a calibration to the middle of an existing dataset.
   *
   * @throws Exception
   *           If any test action throws an Exception.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/calculationCoefficientsEdit" })
  @Test
  public void editMoveToInterimTest() throws Exception {
    CalculationCoefficientsBean bean = init();
    bean.setSelectedCalibrationId(3L);
    bean.loadSelectedCalibration();
    bean.setAction(CalibrationEdit.EDIT);
    bean.getEditedCalibration()
      .setDeploymentDate(LocalDateTime.of(2023, 3, 1, 0, 0, 0));

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }

  @Override
  protected String getTestSetName() {
    return "CalculationCoefficientsSingleCalibrationEditTest";
  }

  /**
   * Extract the edit action from a test line.
   *
   * @param line
   *          The test line.
   * @return The edit action.
   */
  private int getAction(TestSetLine line) {
    int result;

    switch (line.getStringField(ACTION_COL, false)) {
    case "ADD": {
      result = CalibrationEdit.ADD;
      break;
    }
    case "EDIT": {
      result = CalibrationEdit.EDIT;
      break;
    }
    case "DELETE": {
      result = CalibrationEdit.DELETE;
      break;
    }
    default: {
      throw new IllegalArgumentException(
        "Invalid action '" + line.getStringField(ACTION_COL, false));
    }
    }

    return result;
  }

  /**
   * Extract the calibration time from a test line.
   *
   * @param line
   *          The test line.
   * @return The calibration time.
   */
  private LocalDateTime getCalibrationTime(TestSetLine line) {
    int month = line.getIntField(MONTH_COL);

    return month == 0 ? null : LocalDateTime.of(2023, month, 1, 0, 0, 0);
  }
}
