package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationEdit;
import uk.ac.exeter.QuinCe.web.Instrument.ExternalStandardsBean;

/**
 * Tests of calibration edits for the {@link ExternalStandardsBean}.
 * 
 * See
 * {@code WebApp/junit/resources/sql/web/Instrument/CalibrationBeanTest/initial_setup.html}
 * for the initial setup.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ExternalStandardsBeanTest extends TestSetTest {

  private static final int ACTION_COL = 0;

  private static final int CALIBRATION_ID_COL = 1;

  private static final int MONTH_COL = 2;

  private static final int TARGET_COL = 3;

  private static final int AFFECTED_DATASETS_COL = 4;

  private static final int CAN_REPROCESS_COL = 5;

  private static final long USER_ID = 1L;

  private static final long INSTRUMENT_ID = 1L;

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
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void singleCalibrationEditTest(TestSetLine line) throws Exception {

    // Initialise bean
    initResourceManager();
    loginUser(USER_ID);
    ExternalStandardsBean bean = new ExternalStandardsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();

    int action = getAction(line);
    long calbrationId = line.getLongField(CALIBRATION_ID_COL);
    LocalDateTime calibrationTime = getCalibrationTime(line);
    String target = line.getStringField(TARGET_COL, true);
    List<Long> affectedDatasetIds = line
      .getLongListField(AFFECTED_DATASETS_COL);
    List<Boolean> canReprocessDatasets = line
      .getBooleanListField(CAN_REPROCESS_COL);

    if (affectedDatasetIds.size() != canReprocessDatasets.size()) {
      throw new IllegalArgumentException(
        "Affected Datasets and Can Reprocess lists are different sizes");
    }

    bean.setAction(action);
    if (action != CalibrationEdit.ADD) {
      bean.setSelectedCalibrationId(calbrationId);
    }
    bean.loadSelectedCalibration();

    if (action != CalibrationEdit.DELETE) {
      bean.getEditedCalibration().setDeploymentDate(calibrationTime);
      bean.getEditedCalibration().setTarget(target);
    }

    bean.saveCalibration();

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
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  public void addClashTest() {
    assertFalse(true);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  public void editClashTimeTest() {
    assertFalse(true);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  public void editClashTargetTest() {
    assertFalse(true);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  public void editClashTimeAndTargetTest() {
    assertFalse(true);
  }

  @Override
  protected String getTestSetName() {
    return "ExternalStandardsSingleCalibrationEditTest";
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

    return month == 0 ? null : LocalDateTime.of(2024, month, 1, 0, 0, 0);
  }
}
