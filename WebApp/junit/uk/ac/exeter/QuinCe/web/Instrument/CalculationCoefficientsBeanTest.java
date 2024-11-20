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

  private static final int ACTION_COL = 0;

  private static final int CALIBRATION_ID_COL = 1;

  private static final int MONTH_COL = 2;

  private static final int TARGET_COL = 3;

  private static final int CHANGE_VALUE_COL = 4;

  private static final int AFFECTED_DATASETS_COL = 5;

  private static final int CAN_REPROCESS_COL = 6;

  private static final long USER_ID = 1L;

  private static final long INSTRUMENT_ID = 1L;

  private static final String TARGET_1 = "6.F";

  private static final String TARGET_2 = "6.Runtime";

  private static final String REPLACEMENT_VALUE = "1000";

  private CalculationCoefficientsBean init() throws Exception {
    initResourceManager();
    loginUser(USER_ID);
    CalculationCoefficientsBean bean = new CalculationCoefficientsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();
    return bean;
  }

  protected static Map<String, String> makeCoefficients(String value) {
    Map<String, String> result = new HashMap<String, String>();
    result.put("Value", value);
    return result;
  }

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

  private LocalDateTime getCalibrationTime(TestSetLine line) {
    int month = line.getIntField(MONTH_COL);

    return month == 0 ? null : LocalDateTime.of(2023, month, 1, 0, 0, 0);
  }
}
