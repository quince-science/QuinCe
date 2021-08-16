package junit.uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationBean;

public abstract class GetAffectedDatasetsTests extends TestSetTest {

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int ACTION1_FIELD = 0;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int ID1_FIELD = 1;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int TIME1_FIELD = 2;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int TARGET1_FIELD = 3;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int ACTION2_FIELD = 4;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int ID2_FIELD = 5;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int TIME2_FIELD = 6;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int TARGET2_FIELD = 7;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int AFFECTED_DATASETS_FIELD = 8;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int CAN_BE_REPROCESSED_FIELD = 9;

  /**
   * A column in the Test Set file for {@link #runTest(TestSetLine)}.
   */
  private static final int DATASETS_STATUS_FIELD = 10;

  /**
   * Get the first action from a {@link TestSetLine}.
   *
   * <p>
   * The column must contain one of {@code ADD}, {@code EDIT} or {@code DELETE}
   * corresponding to {@link CalibrationBean#ADD_ACTION},
   * {@link CalibrationBean#EDIT_ACTION} or
   * {@link CalibrationBean#DELETE_ACTION} respectively. This method will return
   * the codes for use directly in the {@link Calibration}.
   * </p>
   *
   * @param line
   *          The line
   * @param column
   *          The column from which to retrieve the action value
   * @return The action code
   * @throws Exception
   *           If the action value is not recognised
   */
  private int getAction(TestSetLine line, int column) throws Exception {
    String columnText = line.getStringField(column, false);

    int result;

    switch (columnText) {
    case "ADD": {
      result = CalibrationBean.ADD_ACTION;
      break;
    }
    case "EDIT": {
      result = CalibrationBean.EDIT_ACTION;
      break;
    }
    case "DELETE": {
      result = CalibrationBean.DELETE_ACTION;
      break;
    }
    default: {
      throw new Exception("Unrecognised action '" + columnText + "'");
    }
    }

    return result;
  }

  /**
   * Get the first action from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The action code
   * @throws Exception
   *           If the action in the line is not recognised.
   * @see #getAction(TestSetLine, int)
   */
  private int getAction1(TestSetLine line) throws Exception {
    return getAction(line, ACTION1_FIELD);
  }

  /**
   * Get the second action from a {@link TestSetLine}.
   *
   * @param line
   *          The line
   * @return The action code
   * @throws Exception
   *           If the action in the line is not recognised.
   * @see #getAction(TestSetLine, int)
   */
  private int getAction2(TestSetLine line) throws Exception {
    return getAction(line, ACTION2_FIELD);
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
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private Map<String, Boolean> getExpectedAffectedDatasets(TestSetLine line)
    throws Exception {

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

  /**
   * Get the overall affected data sets status from a {@link TestSetLine}.
   *
   * @param line
   *          The line.
   * @return The affected datasets status.
   * @throws Exception
   *           If any internal errors are encountered.
   * @see CalibrationBean#getAffectedDatasetsStatus()
   */
  private int getExpectedAffectedDatasetsStatus(TestSetLine line)
    throws Exception {

    return line.getIntField(DATASETS_STATUS_FIELD);
  }

  protected void runTest(TestSetLine line) throws Exception {

    try {
      CalibrationBean bean = CalibrationBeanTest.initBean(getDbInstance(),
        getAction1(line), getId1(line), getTime1(line), getTarget1(line));

      boolean checkAffectedCount = true;

      // Run the first call
      bean.calcAffectedDataSets();

      // Get the affected datasets from the first call
      Map<DataSet, Boolean> affectedDatasets = bean.getAffectedDatasets();
      assertNotNull(affectedDatasets);

      // Run the second call if it's specified
      if (!line.isFieldEmpty(ID2_FIELD)) {

        // If we run two tests back to back, we can't test the affected count
        // from the bean
        checkAffectedCount = false;

        // Bean for the second call
        CalibrationBean bean2 = CalibrationBeanTest.initBean(getDbInstance(),
          getAction2(line), getId2(line), getTime2(line), getTarget2(line));

        // Calculate the affected DataSets for the second call
        bean2.calcAffectedDataSets();

        // Add them to the first set
        affectedDatasets.putAll(bean2.getAffectedDatasets());
      }

      // The affected data sets and boolean flags should all match
      Map<String, Boolean> affectedDatasetNames = CalibrationBeanTest
        .getDatasetNamesMap(affectedDatasets);

      assertTrue(
        affectedDatasetNames.equals(getExpectedAffectedDatasets(line)));

      if (checkAffectedCount) {
        assertEquals(bean.getAffectedDatasetsStatus(),
          getExpectedAffectedDatasetsStatus(line));
      }
    } catch (Exception e) {
      throw new TestLineException(line, e);
    }

  }

  /**
   * Get the database instance to be used for these tests
   *
   * @return The database instance
   */
  protected abstract CalibrationDB getDbInstance();
}
