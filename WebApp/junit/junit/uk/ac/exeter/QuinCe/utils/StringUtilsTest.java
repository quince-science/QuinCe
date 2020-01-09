package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

  /**
   * Test that {@link StringUtils#isInteger(String)} correctly identifies an
   * integer as an integer.
   */
  @Test
  public void isIntegerIntegerTest() {
    assertTrue(StringUtils.isInteger("7"));
  }

  /**
   * Test that {@link StringUtils#isInteger(String)} correctly identifies a
   * negative integer as an integer.
   */
  @Test
  public void isIntegerNegativeIntegerTest() {
    assertTrue(StringUtils.isInteger("-7"));
  }

  /**
   * Test that {@link StringUtils#isInteger(String)} correctly identifies a
   * decimal number as a non-integer.
   */
  @Test
  public void isIntegerFloatTest() {
    assertFalse(StringUtils.isInteger("67.5"));
  }

  /**
   * Test that {@link StringUtils#isInteger(String)} correctly identifies a
   * negative decimal number as a non-integer.
   */
  @Test
  public void isIntegerNegativeFloatTest() {
    assertFalse(StringUtils.isInteger("-67.5"));
  }

  /**
   * Test that {@link StringUtils#isInteger(String)} correctly identifies a
   * string as a non-integer.
   */
  @Test
  public void isIntegerNonNumericTest() {
    assertFalse(StringUtils.isInteger("I am not a number"));
  }

  /**
   * Test that {@link StringUtils#isNumeric(String)} correctly identifies
   * {@code "NaN"} as a non-integer.
   */
  @Test
  public void isIntegerNanTest() {
    assertFalse(StringUtils.isInteger("NaN"));
  }

  /**
   * Test that {@link StringUtils#isInteger(String)} correctly identifies empty
   * values as a non-integer.
   *
   * @param empty
   *          The empty String value (from the method source).
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void isIntegerEmptyTest(String empty) {
    assertFalse(StringUtils.isInteger(empty));
  }

  /**
   * Test that empty String values are converted to {@code ""} by
   * {@link StringUtils#makeCsvString(String)}.
   *
   * @param empty
   *          The empty String value (from the method source).
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void makeCsvStringEmptyTest(String empty) {
    assertTrue("\"\"".equals(StringUtils.makeCsvString(empty)));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} wraps a simple string
   * in quotes
   */
  @Test
  public void makeCsvStringBasicStringTest() {
    assertTrue(
      "\"I am String\"".equals(StringUtils.makeCsvString("I am String")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} doubles up quotes
   */
  @Test
  public void makeCsvQuoteStringTest() {
    assertTrue("\"I \"\"am\"\" String\""
      .equals(StringUtils.makeCsvString("I \"am\" String")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} quotes an already
   * quoted String and escapes quotes
   */
  @Test
  public void makeCsvStringQuotedString() {
    assertTrue("\"\"\"I am String\"\"\""
      .equals(StringUtils.makeCsvString("\"I am String\"")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} converts Unix newlines
   * to semi-colons
   */
  @Test
  public void makeCsvStringUnixNewlineTest() {
    assertTrue("\"a;b\"".equals(StringUtils.makeCsvString("a\nb")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} converts Mac newlines
   * to semi-colons
   */
  @Test
  public void makeCsvStringMacNewlineTest() {
    assertTrue("\"a;b\"".equals(StringUtils.makeCsvString("a\rb")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} converts DOS newlines
   * to semi-colons
   */
  @Test
  public void makeCsvStringDosNewlineTest() {
    assertTrue("\"a;b\"".equals(StringUtils.makeCsvString("a\r\nb")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} does not convert
   * trailing newlines to semi-colons, but removes them.
   */
  @Test
  public void makeCsvStringTrailingNewlineTest() {
    assertTrue("\"ab\"".equals(StringUtils.makeCsvString("ab\n")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} does not convert
   * leading newlines to semi-colons, but removes them.
   */
  @Test
  public void makeCsvStringLeadingNewlineTest() {
    assertTrue("\"ab\"".equals(StringUtils.makeCsvString("\nab")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} converts multiple
   * consecutive newlines to a single semi-colon.
   */
  @Test
  public void makeCsvStringRepeatedNewlineTest() {
    assertTrue("\"a;b\"".equals(StringUtils.makeCsvString("a\n\nb")));
  }

  /**
   * Test that {@link StringUtils#makeCsvString(String)} converts multiple
   * newlines to semi-colons.
   */
  @Test
  public void makeCsvStringMultipleNewlineTest() {
    assertTrue("\"a;b;c\"".equals(StringUtils.makeCsvString("a\nb\nc")));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringIntegerTest() {
    assertEquals(new Double(7.0), StringUtils.doubleFromString("7"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringZeroIntegerTest() {
    assertEquals(new Double(0.0), StringUtils.doubleFromString("0"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNegativeIntegerTest() {
    assertEquals(new Double(-7.0), StringUtils.doubleFromString("-7"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringDoubleTest() {
    assertEquals(new Double(7.657), StringUtils.doubleFromString("7.657"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringZeroDoubleTest() {
    assertEquals(new Double(0.0), StringUtils.doubleFromString("0.0"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNegativeDoubleTest() {
    assertEquals(new Double(-7.657), StringUtils.doubleFromString("-7.657"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringThousandsTest() {
    assertEquals(new Double(7547.54), StringUtils.doubleFromString("7,547.54"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNegativeThousandsTest() {
    assertEquals(new Double(-7547.54),
      StringUtils.doubleFromString("-7,547.54"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringStringTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.doubleFromString("Flurble");
    });
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNaNTest() {
    assertEquals((Double) Double.NaN, StringUtils.doubleFromString("NaN"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void doubleFromStringEmptyStringTest() {
    assertEquals((Double) Double.NaN, StringUtils.doubleFromString(null));
  }
}
