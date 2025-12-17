package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

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

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;

/**
 * Tests for interpolation of values presented as either {@link BigDecimal} or
 * {@link Map.Entry} objects.
 *
 * <p>
 * For {@link Map.Entry} objects the {@code X} and {@code Y} values are set as
 * the key and value respectively.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CalculatorsInterpolateMapEntryBigDecimalTest extends TestSetTest {

  /**
   * First X value.
   */
  private static final Double X0 = 26.533D;

  /**
   * First Y value.
   */
  private static final Double Y0 = 8.328D;

  /**
   * Second X value.
   */
  private static final Double X1 = 60.952D;

  /**
   * Second Y value.
   */
  private static final Double Y1 = 15.685D;

  /**
   * The target X value for the interpolation.
   */
  private static final Double TARGET_X = 37.765D;

  /**
   * Column index from the {@link TestSetTest} file that indicates whether or
   * not the first X value is set as {@link #X0} or as an alternative value.
   *
   * @see #doMapEntryInterpolation(TestSetLine, Double)
   */
  private static final int X0_PRESENT_COL = 0;

  /**
   * Column index from the {@link TestSetTest} file that indicates whether or
   * not the first Y value is set as {@link #Y0} or as an alternative value.
   *
   * @see #doMapEntryInterpolation(TestSetLine, Double)
   */
  private static final int Y0_PRESENT_COL = 1;

  /**
   * Column index from the {@link TestSetTest} file that indicates whether or
   * not the second X value is set as {@link #X1} or as an alternative value.
   *
   * @see #doMapEntryInterpolation(TestSetLine, Double)
   */
  private static final int X1_PRESENT_COL = 2;

  /**
   * Column index from the {@link TestSetTest} file that indicates whether or
   * not the second Y value is set as {@link #Y1} or as an alternative value.
   *
   * @see #doMapEntryInterpolation(TestSetLine, Double)
   */
  private static final int Y1_PRESENT_COL = 3;

  /**
   * Column index from the {@link TestSetTest} file that contains the expected
   * result of the interpolation.
   */
  private static final int EXPECTED_RESULT_COL = 4;

  /**
   * Interpolate {@link Map.Entry} values where the 'not present' values
   * specified in the {@link TestSetLine} are set to {@code null}.
   *
   * @param line
   *          The {@link TestSetLine}.
   */
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

  /**
   * Interpolate {@link Map.Entry} values where the 'not present' values
   * specified in the {@link TestSetLine} are set to {@link Double#NaN}.
   *
   * @param line
   *          The {@link TestSetLine}.
   */
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

  /**
   * Test an interpolation where the first {@link Map.Entry} object is
   * {@code null}.
   */
  @Test
  public void mapEntryPriorNullTest() {
    @SuppressWarnings("unchecked")
    Map.Entry<Double, Double> post = Mockito.mock(Map.Entry.class);
    Mockito.when(post.getKey()).thenReturn(X1);
    Mockito.when(post.getValue()).thenReturn(Y1);

    assertEquals(Y1, Calculators.interpolate(null, post, TARGET_X), 0.0001D);
  }

  /**
   * Test an interpolation where the second {@link Map.Entry} object is
   * {@code null}.
   */
  @Test
  public void mapEntryPostNullTest() {
    @SuppressWarnings("unchecked")
    Map.Entry<Double, Double> prior = Mockito.mock(Map.Entry.class);
    Mockito.when(prior.getKey()).thenReturn(X0);
    Mockito.when(prior.getValue()).thenReturn(Y0);

    assertEquals(Y0, Calculators.interpolate(prior, null, TARGET_X), 0.0001D);
  }

  /**
   * Test an interpolation where both {@link Map.Entry} objects are
   * {@code null}.
   */
  @Test
  public void mapEntryBothNullTest() {
    assertNull(Calculators.interpolate(null, null, TARGET_X));
  }

  /**
   * Test interpolation of values presented as {@link BigDecimal} objects.
   *
   * <p>
   * The contents of the supplied {@link TestSetLine} dictate whether the
   * various {@link BigDecimal}s are supplied as numeric values or {@code null}.
   * </p>
   *
   * @param line
   *          The test line.
   */
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

  /**
   * Perform an interpolation of two {@link Map.Entry} objects by calling
   * {@link Calculators#interpolate(java.util.Map.Entry, java.util.Map.Entry, Double)}.
   *
   * <p>
   * The contents of the supplied {@link TestSetLine} dictate whether parts of
   * the {@link Map.Entry} objects are populated with normal values, or with the
   * specified {@code notPresentValue}.
   * </p>
   *
   * @param line
   *          The test line.
   * @param notPresentValue
   *          The value to use if the test line indicates that a part of an
   *          object is not to be populated as a normal value.
   * @return The result of the interpolation call.
   */
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

  /**
   * Get the value for part of a test {@link Map.Entry} object based on the
   * requested column from the supplied {@link TestSetLine}.
   *
   * <p>
   * The value from the specified {@code column} indicates whether the returned
   * value is the {@code presentValue} or the {@code notPresentValue}.
   * </p>
   *
   * @param line
   *          The {@link TestSetLine}.
   * @param column
   *          The column from the line that indicates whether the value is
   *          present or not.
   * @param presentValue
   *          The 'present' value.
   * @param notPresentValue
   *          The 'not present' value.
   * @return The computed value.
   */
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
