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
 * Tests for the {@link DataReductionRecord#compareTo(DataReductionRecord)
 * method.
 *
 * @author stevej
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DataReductionRecordCompareTest extends TestSetTest {

  private static final int MEASUREMENT_ID_COL = 0;

  private static final int VARIABLE_ID_COL = 1;

  private static final int COMPARE_MEASUREMENT_ID_COL = 2;

  private static final int COMPARE_VARIABLE_ID_COL = 3;

  private static final int COMPARE_SIGN_COL = 4;

  private Measurement makeMeasurement(long id) {
    Measurement measurement = Mockito.mock(Measurement.class);
    Mockito.when(measurement.getId()).thenReturn(id);
    return measurement;
  }

  private Variable makeVariable(long id) {
    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(id);
    return variable;
  }

  private List<String> makeParameters() {
    return Arrays.asList(new String[] { "Param 1" });
  }

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
