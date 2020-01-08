package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import junit.uk.ac.exeter.QuinCe.TestBase.TestLineException;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;

public abstract class GetAffectedDatasetsTests extends TestSetTest {

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
    return line.getStringField(TARGET1_FIELD, true);
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
    return line.getStringField(TARGET2_FIELD, true);
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
  private Map<String, Boolean> getExpectedAffectedDatasets(TestSetLine line)
    throws TestLineException {

    Map<String, Boolean> result = new HashMap<String, Boolean>();

    List<String> datasets = StringUtils.delimitedToList(
      line.getStringField(AFFECTED_DATASETS_FIELD, false), ";");

    List<Boolean> canBeReprocessed = StringUtils
      .delimitedToList(line.getStringField(CAN_BE_REPROCESSED_FIELD, false),
        ";")
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

  protected void runTest(TestSetLine line) throws TestLineException {

    try {
      CalibrationBean bean = CalibrationBeanTest.initBean(getDbInstance());

      // Run the first call
      Map<DataSet, Boolean> affectedDatasets = bean
        .getAffectedDataSets(getId1(line), getTime1(line), getTarget1(line));

      assertNotNull(affectedDatasets);

      // Run the second call if it's specified
      if (!line.isFieldEmpty(ID2_FIELD)) {
        Map<DataSet, Boolean> secondAffectedDataSets = bean
          .getAffectedDataSets(getId2(line), getTime2(line), getTarget2(line));

        affectedDatasets.putAll(secondAffectedDataSets);
      }

      // The affected data sets and boolean flags should all match
      assertTrue(affectedDatasets.equals(getExpectedAffectedDatasets(line)));
    } catch (Exception e) {
      throw new TestLineException(line, e);
    }

  }

  protected abstract CalibrationDB getDbInstance();
}
