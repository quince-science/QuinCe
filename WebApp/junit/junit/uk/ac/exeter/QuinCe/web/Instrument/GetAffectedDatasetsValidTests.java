package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestLineException;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetException;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import junit.uk.ac.exeter.QuinCe.User.UserDBTest;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.NonExistentCalibrationTargetException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;
import uk.ac.exeter.QuinCe.web.Instrument.InvalidCalibrationEditException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Complex combination tests for the
 * {@link UserDB#checkEmailVerificationCode(javax.sql.DataSource, String, String)}
 * and
 * {@link UserDB#checkPasswordResetCode(javax.sql.DataSource, String, String)}
 * tests.
 *
 * <p>
 * Some of the tests for these methods are in the main {@link UserDBTest} class,
 * but some are more complex and required a Test Set.
 * </p>
 *
 * @author Steve Jones
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public class GetAffectedDatasetsValidTests extends TestSetTest {

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int ID1_FIELD = 0;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int TIME1_FIELD = 1;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int TARGET1_FIELD = 2;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int ID2_FIELD = 3;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int TIME2_FIELD = 4;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int TARGET2_FIELD = 5;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int AFFECTED_DATASETS_FIELD = 6;

  /**
   * A column in the Test Set file for
   * {@link #getAffectedDatasetsTest(TestSetLine)}.
   */
  private static final int CAN_BE_REPROCESSED_FIELD = 7;

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Tests for code checks on users with various combinations of email and
   * password reset codes.
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   * @throws TestLineException
   *           If the test set line is invalid.
   * @throws NonExistentCalibrationTargetException
   * @throws RecordNotFoundException
   * @throws InvalidCalibrationEditException
   * @throws InstrumentException
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/base",
    "resources/sql/web/Instrument/ExternalStandardsBeanTest/edit" })
  @ParameterizedTest
  @MethodSource("getGetAffectedDatasetsValidTestSet")
  public void getAffectedDatasetsTest(TestSetLine line)
    throws MissingParamException, DatabaseException, TestLineException,
    InvalidCalibrationEditException, RecordNotFoundException,
    NonExistentCalibrationTargetException, InstrumentException {

    CalibrationBean bean = CalibrationBeanTest.initBean();

    // Run the first call
    Map<DataSet, Boolean> beanAffectedDataSets = bean
      .getAffectedDataSets(getId1(line), getTime1(line), getTarget1(line));

    assertNotNull(beanAffectedDataSets);

    // Run the second call if it's specified
    if (!line.isFieldEmpty(ID2_FIELD)) {
      Map<DataSet, Boolean> secondAffectedDataSets = bean
        .getAffectedDataSets(getId2(line), getTime2(line), getTarget2(line));

      beanAffectedDataSets.putAll(secondAffectedDataSets);
    }

    assertTrue(
      checkAgainstTestSpec(beanAffectedDataSets, getAffectedDatasets(line)));

  }

  private boolean checkAgainstTestSpec(Map<DataSet, Boolean> beanDatasets,
    Map<String, Boolean> testDatasets) {

    boolean result = true;

    if (null == beanDatasets) {
      result = false;
    } else {
      Map<String, Boolean> beanDataSetsByName = new HashMap<String, Boolean>();
      for (Map.Entry<DataSet, Boolean> entry : beanDatasets.entrySet()) {
        beanDataSetsByName.put(entry.getKey().getName(), entry.getValue());
      }

      if (beanDataSetsByName.size() != testDatasets.size()) {
        result = false;
      } else {

        for (String key : beanDataSetsByName.keySet()) {
          if (!beanDataSetsByName.get(key).equals(testDatasets.get(key))) {
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Retrieves the Test Set for {@link #getAffectedDatasetsTest(TestSetLine)}.
   *
   * @return The test set
   * @throws IOException
   *           If the Test Set file cannot be read
   */
  @SuppressWarnings("unused")
  private Stream<TestSetLine> getGetAffectedDatasetsValidTestSet()
    throws TestSetException {
    return getTestSet("CalibrationBean_getAffectedDatasetsValidTests");
  }

  /**
   * Get the first calibration ID from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The calibration ID
   */
  private long getId1(TestSetLine line) {
    return line.getLongField(ID1_FIELD);
  }

  /**
   * Get the first time from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The time
   */
  private LocalDateTime getTime1(TestSetLine line) {
    return line.getTimeField(TIME1_FIELD);
  }

  /**
   * Get the first target from a {@link TestSetLine}
   *
   * @param line
   *          The line
   * @return The target
   */
  private String getTarget1(TestSetLine line) {
    return line.getStringField(TARGET1_FIELD);
  }

  /**
   * Get the second calibration ID from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The calibration ID
   */
  private long getId2(TestSetLine line) {
    return line.getLongField(ID2_FIELD);
  }

  /**
   * Get the second time from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The time
   */
  private LocalDateTime getTime2(TestSetLine line) {
    return line.getTimeField(TIME2_FIELD);
  }

  /**
   * Get the second target from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The target
   */
  private String getTarget2(TestSetLine line) {
    return line.getStringField(TARGET2_FIELD);
  }

  /**
   * Get the affected datasets and their reprocessing status from a
   * {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The affected datasets
   * @throws TestLineException
   *           If the datasets and statuses do not match
   */
  private Map<String, Boolean> getAffectedDatasets(TestSetLine line)
    throws TestLineException {

    Map<String, Boolean> result = new HashMap<String, Boolean>();

    List<String> datasets = StringUtils
      .delimitedToList(line.getStringField(AFFECTED_DATASETS_FIELD), ";");

    List<Boolean> canBeReprocessed = StringUtils
      .delimitedToList(line.getStringField(CAN_BE_REPROCESSED_FIELD), ";")
      .stream().map(v -> Boolean.parseBoolean(v)).collect(Collectors.toList());

    if (datasets.size() != canBeReprocessed.size()) {
      throw new TestLineException(line,
        "Affected datasets and reprocessed statuses are different lengths");
    }

    for (int i = 0; i < datasets.size(); i++) {
      result.put(datasets.get(i), canBeReprocessed.get(i));
    }

    return result;
  }
}
