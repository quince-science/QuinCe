package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Tests for the {@link DataReductionRecord#compareTo(DataReductionRecord)}
 * method.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DataReductionRecordCompareTest extends TestSetTest {

  /**
   * A column index in the Test Set file for {@link #compareTests(TestSetLine)}.
   */
  private static final int MEASUREMENT_ID_COL = 0;

  /**
   * A column index in the Test Set file for {@link #compareTests(TestSetLine)}.
   */
  private static final int VARIABLE_ID_COL = 1;

  /**
   * A column index in the Test Set file for {@link #compareTests(TestSetLine)}.
   */
  private static final int COMPARE_MEASUREMENT_ID_COL = 2;

  /**
   * A column index in the Test Set file for {@link #compareTests(TestSetLine)}.
   */
  private static final int COMPARE_VARIABLE_ID_COL = 3;

  /**
   * A column index in the Test Set file for {@link #compareTests(TestSetLine)}.
   */
  private static final int COMPARE_SIGN_COL = 4;

  /**
   * Create a mock {@link Measurement} object with the specified database ID.
   *
   * @param id
   *          The database ID.
   * @return The Measurement object.
   */
  private Measurement makeMeasurement(long id) {
    Measurement measurement = Mockito.mock(Measurement.class);
    Mockito.when(measurement.getId()).thenReturn(id);
    return measurement;
  }

  /**
   * Create a mock {@link Variable} object with the specified database ID.
   *
   * @param id
   *          The database ID.
   * @return The Variable object.
   */
  private Variable makeVariable(long id) {
    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(id);
    return variable;
  }

  /**
   * Create a dummy set of parameters for a constructed
   * {@link DataReductionRecord}.
   *
   * @return The parameters.
   */
  private List<String> makeParameters() {
    return Arrays.asList(new String[] { "Param 1" });
  }

  /**
   * Run the comparison tests defined in the test set specified by
   * {@link #getTestSetName()}.
   *
   * <p>
   * These are defined in the file
   * {@code WebApp/junit/resources/testsets/DataReductionRecordCompare.csv}. The
   * file defines a number of different {@link DataReductionRecord}s to be
   * compared and their expected outcomes. The column headers in the file are as
   * follows:
   * </p>
   *
   * <table>
   * <caption>Columns required for the {@code DataReductionRecordCompare} test
   * set</caption>
   * <tr>
   * <th>Column Name</th>
   * <th>Purpose</th>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Measurement ID</td>
   * <td>The record's {@link Measurement} ID of the base
   * {@link DataReductionRecord}</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Variable ID</td>
   * <td>The {@link Variable} being measured by the {@link Measurement}</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Compare Measurement ID</td>
   * <td>The {@link Measurement} ID of the {@link DataReductionRecord} to be
   * compared</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Compare Variable ID</td>
   * <td>The {@link Variable} measured by the {@link Measurement} being
   * compared</td>
   * </tr>
   * <tr>
   * <td style="vertical-align: top">Compare Sign</td>
   * <td>The expected result of the comparison: One of Positive ({@code 1}),
   * Zero ({@code 0} for equality, or Negative (@{code -1}).</td>
   * </tr>
   * </table>
   *
   * @param line
   *          The line number for the test.
   * @throws Exception
   *           For any unexpected errors.
   */
  @ParameterizedTest
  @MethodSource("getLines")
  public void compareTests(TestSetLine line) throws Exception {
    Measurement measurement = makeMeasurement(
      line.getLongField(MEASUREMENT_ID_COL));
    Variable variable = makeVariable(line.getLongField(VARIABLE_ID_COL));

    DataReductionRecord base = new DataReductionRecord(measurement, variable,
      makeParameters());

    Measurement compareMeasurement = makeMeasurement(
      line.getLongField(COMPARE_MEASUREMENT_ID_COL));
    Variable compareVariable = makeVariable(
      line.getLongField(COMPARE_VARIABLE_ID_COL));

    DataReductionRecord compare = new DataReductionRecord(compareMeasurement,
      compareVariable, makeParameters());

    int compareResult = compare.compareTo(base);
    assertTrue(sameSign(compareResult, line.getIntField(COMPARE_SIGN_COL)));
  }

  @Override
  protected String getTestSetName() {
    return "DataReductionRecordCompare";
  }
}
