package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;

@TestInstance(Lifecycle.PER_CLASS)
public class CalculatorsInterpolateMapEntryBigDecimalTest extends TestSetTest {

  private static final Double X0 = 26.533D;
  private static final Double Y0 = 8.328D;
  private static final Double X1 = 60.952D;
  private static final Double Y1 = 15.685D;
  private static final Double TARGET_X = 37.765D;

  private static final int X0_PRESENT_COL = 0;
  private static final int Y0_PRESENT_COL = 1;
  private static final int X1_PRESENT_COL = 2;
  private static final int Y1_PRESENT_COL = 3;
  private static final int EXPECTED_RESULT_COL = 4;

  @ParameterizedTest
  @MethodSource("getLines")
  public void mapEntryNullTest(TestSetLine line) {

    Double interpolationResult = doMapEntryInterpolation(line, null);

    if (line.isFieldEmpty(EXPECTED_RESULT_COL)) {
      assertNull(interpolationResult, "Incorrect null value returned");
    } else {
      assertEquals(line.getDoubleField(EXPECTED_RESULT_COL),
        interpolationResult, 0.0001D, "Incorrect value returned");
    }

  }

  @ParameterizedTest
  @MethodSource("getLines")
  public void mapEntryNaNTest(TestSetLine line) {

    Double interpolationResult = doMapEntryInterpolation(line, Double.NaN);

    if (line.isFieldEmpty(EXPECTED_RESULT_COL)) {
      assertNull(interpolationResult, "Incorrect null value returned");
    } else {
      assertEquals(line.getDoubleField(EXPECTED_RESULT_COL),
        interpolationResult, 0.0001D, "Incorrect value returned");
    }
  }

  @Test
  public void mapEntryPriorNullTest() {
    @SuppressWarnings("unchecked")
    Map.Entry<Double, Double> post = Mockito.mock(Map.Entry.class);
    Mockito.when(post.getKey()).thenReturn(X1);
    Mockito.when(post.getValue()).thenReturn(Y1);

    assertEquals(Y1, Calculators.interpolate(null, post, TARGET_X), 0.0001D);
  }

  @Test
  public void mapEntryPostNullTest() {
    @SuppressWarnings("unchecked")
    Map.Entry<Double, Double> prior = Mockito.mock(Map.Entry.class);
    Mockito.when(prior.getKey()).thenReturn(X0);
    Mockito.when(prior.getValue()).thenReturn(Y0);

    assertEquals(Y0, Calculators.interpolate(prior, null, TARGET_X), 0.0001D);
  }

  @Test
  public void mapEntryBothNullTest() {
    assertNull(Calculators.interpolate(null, null, TARGET_X));
  }

  @ParameterizedTest
  @MethodSource("getLines")
  public void BigDecimalNullTest(TestSetLine line) {

    Double x0Double = getInputValue(line, X0_PRESENT_COL, X0, null);
    Double y0Double = getInputValue(line, Y0_PRESENT_COL, Y0, null);
    Double x1Double = getInputValue(line, X1_PRESENT_COL, X1, null);
    Double y1Double = getInputValue(line, Y1_PRESENT_COL, Y1, null);

    BigDecimal x0 = null == x0Double ? null : new BigDecimal(x0Double);
    BigDecimal y0 = null == y0Double ? null : new BigDecimal(y0Double);
    BigDecimal x1 = null == x1Double ? null : new BigDecimal(x1Double);
    BigDecimal y1 = null == y1Double ? null : new BigDecimal(y1Double);

    BigDecimal interpolationResult = Calculators.interpolate(x0, y0, x1, y1,
      new BigDecimal(TARGET_X));

    if (line.isFieldEmpty(EXPECTED_RESULT_COL)) {
      assertNull(interpolationResult, "Incorrect null value returned");
    } else {
      assertEquals(line.getDoubleField(EXPECTED_RESULT_COL),
        interpolationResult.doubleValue(), 0.0001D, "Incorrect value returned");
    }

  }

  private Double doMapEntryInterpolation(TestSetLine line,
    Double notPresentValue) {

    Double x0 = getInputValue(line, X0_PRESENT_COL, X0, notPresentValue);
    Double y0 = getInputValue(line, Y0_PRESENT_COL, Y0, notPresentValue);
    Double x1 = getInputValue(line, X1_PRESENT_COL, X1, notPresentValue);
    Double y1 = getInputValue(line, Y1_PRESENT_COL, Y1, notPresentValue);

    @SuppressWarnings("unchecked")
    Map.Entry<Double, Double> prior = Mockito.mock(Map.Entry.class);
    Mockito.when(prior.getKey()).thenReturn(x0);
    Mockito.when(prior.getValue()).thenReturn(y0);

    @SuppressWarnings("unchecked")
    Map.Entry<Double, Double> post = Mockito.mock(Map.Entry.class);
    Mockito.when(post.getKey()).thenReturn(x1);
    Mockito.when(post.getValue()).thenReturn(y1);

    return Calculators.interpolate(prior, post, TARGET_X);
  }

  private Double getInputValue(TestSetLine line, int column,
    Double presentValue, Double notPresentValue) {

    Double result = notPresentValue;

    if (line.getBooleanField(column)) {
      result = presentValue;
    }

    return result;
  }

  @Override
  protected String getTestSetName() {
    return "CalculatorsInterpolateMapEntryBigDecimalTest";
  }

}
