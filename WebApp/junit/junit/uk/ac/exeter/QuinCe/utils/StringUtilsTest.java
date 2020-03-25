package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Tests for the StringUtils class
 *
 * @author Steve Jones
 *
 */
public class StringUtilsTest extends BaseTest {

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

  /**
   * Test that
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * returns an empty string with null input.ß
   */
  @Test
  public void listToDelimitedNullTest() {
    assertTrue("".equals(StringUtils.collectionToDelimited(null, ",")));
  }

  /**
   * Test that
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * returns an empty string with an empty input collection.
   */
  @Test
  public void listToDelimitedEmptyTest() {
    assertTrue(""
      .equals(StringUtils.collectionToDelimited(new ArrayList<String>(), ",")));
  }

  /**
   * Test that
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * with an empty/null delimiter joins strings with no delimiter.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void listToDelimitedNullDelimiterTest(String delimiter) {
    ArrayList<String> list = new ArrayList<String>(2);
    list.add("a");
    list.add("b");

    assertTrue("ab".equals(StringUtils.collectionToDelimited(list, delimiter)));
  }

  /**
   * Test
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * with a delimiter.
   */
  @Test
  public void listToDelimitedOneCharDelimiterTest() {
    ArrayList<String> list = new ArrayList<String>(2);
    list.add("a");
    list.add("b");

    assertTrue("a,b".equals(StringUtils.collectionToDelimited(list, ",")));
  }

  /**
   * Test
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * with a multi-character delimiter.
   */
  @Test
  public void listToDelimitedTwoCharDelimiterTest() {
    ArrayList<String> list = new ArrayList<String>(2);
    list.add("a");
    list.add("b");

    assertTrue("a;;b".equals(StringUtils.collectionToDelimited(list, ";;")));
  }

  /**
   * Test
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * with a non-String list.
   */
  @Test
  public void listToDelimitedNonStringListTest() {
    ArrayList<Flag> list = new ArrayList<Flag>(2);
    list.add(Flag.BAD);
    list.add(Flag.GOOD);

    assertTrue("Bad,Good".equals(StringUtils.collectionToDelimited(list, ",")));
  }
}
