package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;

/**
 * Base class for tests of edits applied in the
 * {@link CalculationCoefficientsBean}.
 */
public abstract class CalculationCoefficientsBeanEditTest extends TestSetTest {

  /**
   * Dummy User ID.
   */
  private static final long USER_ID = 1L;

  /**
   * Dummy Instrument ID.
   */
  private static final long INSTRUMENT_ID = 1L;

  /**
   * Value to use when editing the value of an existing coefficient.
   */
  protected static final String REPLACEMENT_VALUE = "1000";

  /**
   * Initialise the {@link CalculationCoefficientsBean} ready for testing.
   *
   * @return The initialised bean.
   * @throws Exception
   *           If the bean cannot be created.
   */
  protected CalculationCoefficientsBean init() throws Exception {
    initResourceManager();
    loginUser(USER_ID);
    CalculationCoefficientsBean bean = new CalculationCoefficientsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();
    return bean;
  }

  /**
   * Generate a {@link String} representing the affected datasets of an edit
   * session for checking test results.
   *
   * <p>
   * Zips together a {@link List} of DatSet IDs and a list of {@code boolean}s
   * (representing whether the DataSet can be reprocessed successully} into a
   * single {@link String} of the form
   * {@code id:canReprocess;id:canReprocess;...}.
   * </p>
   *
   * @param ids
   *          The DataSet IDs
   * @param canReprocess
   *          The 'Can Reprocess' flags.
   * @return The test result string.
   */
  protected String makeTestString(List<Long> ids, List<Boolean> canReprocess) {
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
   * Generate a {@link String} representing the affected datasets of an edit
   * session for checking test results.
   *
   * <p>
   * Converts a {@link Map} of {@code DataSet ID -> Can Reprocess} into a single
   * {@link String} of the form {@code id:canReprocess;id:canReprocess;...}.
   * </p>
   *
   * @param input
   *          The input Map.
   * @return The test result string.
   */
  protected String makeTestString(TreeMap<Long, Boolean> input) {
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
   * Extract the specified column from a {@link TestSetLine} and convert it into
   * a {@link CalibrationEdit} action.
   *
   * @param line
   *          The line.
   * @param actionCol
   *          The column containing the edit action.
   * @return The action value.
   */
  protected int getAction(TestSetLine line, int actionCol) {
    int result;

    switch (line.getStringField(actionCol, false)) {
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
        "Invalid action '" + line.getStringField(actionCol, false));
    }
    }

    return result;
  }

  /**
   * Generate a timestamp for a {@link Calibration} using the specified month.
   *
   * The returned value will represent 1 &lt;month&gt; 2023 at midnight.
   *
   * @param month
   *          The month.
   * @return The timestamp.
   */
  protected LocalDateTime getCalibrationTime(int month) {
    return month == 0 ? null : LocalDateTime.of(2023, month, 1, 0, 0, 0);
  }
}
