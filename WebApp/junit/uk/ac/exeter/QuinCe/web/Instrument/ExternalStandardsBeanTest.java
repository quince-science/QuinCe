package uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

/**
 * Tests of calibration edits for the {@link ExternalStandardsBean}.
 *
 * See
 * {@code WebApp/junit/resources/sql/web/Instrument/CalibrationBeanTest/initial_setup.html}
 * for the initial setup ({@code TARGET_1} and {@code TARGET_1} are specified in
 * this test file).
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ExternalStandardsBeanTest extends BaseTest {

  private static final long USER_ID = 1L;

  private static final long INSTRUMENT_ID = 1L;

  private static final String TARGET_1 = "std1";

  private static final String TARGET_2 = "std2";

  protected static final String REPLACEMENT_VALUE = "1000";

  protected ExternalStandardsBean init() throws Exception {
    initResourceManager();
    loginUser(USER_ID);
    ExternalStandardsBean bean = new ExternalStandardsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();
    return bean;
  }

  protected static Map<String, String> makeCoefficients(String value) {
    Map<String, String> result = new HashMap<String, String>();
    result.put("xH₂O (with standards)", "0");
    result.put("xCO₂ (with standards)", value);
    return result;
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/variable",
    "resources/sql/web/Instrument/CalibrationBeanTest/base",
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  @Test
  public void addClashTest() throws Exception {
    ExternalStandardsBean bean = init();
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
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  @Test
  public void editClashTimeTest() throws Exception {
    ExternalStandardsBean bean = init();
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
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  @Test
  public void editClashTargetTest() throws Exception {
    ExternalStandardsBean bean = init();
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
    "resources/sql/web/Instrument/CalibrationBeanTest/externalStandardsEdit" })
  @Test
  public void editClashTimeAndTargetTest() throws Exception {
    ExternalStandardsBean bean = init();
    bean.setSelectedCalibrationId(1L);
    bean.loadSelectedCalibration();
    bean.setAction(CalibrationEdit.EDIT);
    bean.getEditedCalibration()
      .setDeploymentDate(LocalDateTime.of(2023, 5, 1, 0, 0, 0));
    bean.getEditedCalibration().setTarget(TARGET_2);

    bean.saveCalibration();
    assertFalse(bean.editedCalibrationValid());
  }
}
