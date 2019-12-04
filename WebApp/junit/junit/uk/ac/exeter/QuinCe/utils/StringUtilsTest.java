package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Tests for the StringUtils class
 *
 * @author Steve Jones
 *
 */
public class StringUtilsTest extends BaseTest {

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies an
   * integer as numeric.
   */
  @Test
  public void isNumericIntegerTest() {
    assertTrue(StringUtils.isNumeric("7"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies a
   * negative integer as numeric.
   */
  @Test
  public void isNumericNegativeIntegerTest() {
    assertTrue(StringUtils.isNumeric("-7"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies a
   * decimal number as numeric.
   */
  @Test
  public void isNumericFloatTest() {
    assertTrue(StringUtils.isNumeric("67.5"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies a
   * negative decimal number as numeric.
   */
  @Test
  public void isNumericNegativeFloatTest() {
    assertTrue(StringUtils.isNumeric("-67.5"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies a
   * string as non-numeric.
   */
  @Test
  public void isNumericNonNumericTest() {
    assertFalse(StringUtils.isNumeric("I am not a number"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies a
   * {@code null} as non-numeric.
   */
  @Test
  public void isNumericNullTest() {
    assertFalse(StringUtils.isNumeric(null));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies
   * {@code "NaN"} as non-numeric.
   */
  @Test
  public void isNumericNanTest() {
    assertFalse(StringUtils.isNumeric("NaN"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies empty
   * values as non-numeric.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void isNumericEmptyTest(String empty) {
    assertFalse(StringUtils.isNumeric(empty));
  }
}
