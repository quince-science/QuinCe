package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
   * Generate a list of commonly used list delimiters.
   *
   * @return The delimiters.
   */
  private static final List<String> makeDelimiters() {
    List<String> delimiters = new ArrayList<String>(3);
    delimiters.add(",");
    delimiters.add(";");
    delimiters.add(" ");
    return delimiters;
  }

  /**
   * Generate a list of disallowed delimiters.
   *
   * @return The disallowed delimiters.
   */
  private static final List<String> makeStandardInvalidDelimiters() {
    List<String> delimiters = new ArrayList<String>(67);
    delimiters.add(null);
    delimiters.add("\"");
    delimiters.add("'");
    delimiters.add("");
    delimiters.add(",,");

    for (char c = 'a'; c <= 'z'; c++) {
      delimiters.add(String.valueOf(c));
    }

    for (char c = 'A'; c <= 'Z'; c++) {
      delimiters.add(String.valueOf(c));
    }

    for (char c = '0'; c <= '9'; c++) {
      delimiters.add(String.valueOf(c));
    }

    return delimiters;
  }

  /**
   * Make a list of delimiters that are disallowed for lists of numbers.
   *
   * @return The disallowed delimiters.
   */
  private static final List<String> makeNumericInvalidDelimiters() {
    List<String> delimiters = new ArrayList<String>(2);
    delimiters.add(".");
    delimiters.add("-");

    return delimiters;
  }

  /**
   * Check that a list of {@link Double}s contains the specified set of values.
   *
   * @param list
   *          The list to check.
   * @param strings
   *          The values that the list should contain.
   * @return {@code true} if the list contains the specified values;
   *         {@code false} otherwise.
   */
  private static final boolean checkStringList(List<String> list,
    String... strings) {
    boolean ok = true;

    if (list.size() != strings.length) {
      ok = false;
    } else {
      for (int i = 0; i < list.size(); i++) {
        if (!list.get(i).equals(strings[i])) {
          ok = false;
          break;
        }
      }
    }

    return ok;
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
  public void makeCsvStringQuotedStringTest() {
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
    assertEquals(Double.valueOf(7.0), StringUtils.doubleFromString("7"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringZeroIntegerTest() {
    assertEquals(Double.valueOf(0.0), StringUtils.doubleFromString("0"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNegativeIntegerTest() {
    assertEquals(Double.valueOf(-7.0), StringUtils.doubleFromString("-7"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringDoubleTest() {
    assertEquals(Double.valueOf(7.657), StringUtils.doubleFromString("7.657"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringZeroDoubleTest() {
    assertEquals(Double.valueOf(0.0), StringUtils.doubleFromString("0.0"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNegativeDoubleTest() {
    assertEquals(Double.valueOf(-7.657),
      StringUtils.doubleFromString("-7.657"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringThousandsTest() {
    assertEquals(Double.valueOf(7547.54),
      StringUtils.doubleFromString("7,547.54"));
  }

  /**
   * Test that {@link StringUtils#doubleFromString(String)} works with an
   * integer.
   */
  @Test
  public void doubleFromStringNegativeThousandsTest() {
    assertEquals(Double.valueOf(-7547.54),
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
   * with a null delimiter joins strings with no delimiter.
   */
  @Test
  public void listToDelimitedNullDelimiterTest() {
    ArrayList<String> list = new ArrayList<String>(2);
    list.add("a");
    list.add("b");

    assertTrue("ab".equals(StringUtils.collectionToDelimited(list, null)));
  }

  /**
   * Test
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * with a tab delimiter.
   */
  @Test
  public void listToDelimitedTabDelimiterTest() {
    ArrayList<String> list = new ArrayList<String>(2);
    list.add("a");
    list.add("b");

    assertTrue("a\tb".equals(StringUtils.collectionToDelimited(list, "\t")));
  }

  /**
   * Test
   * {@link StringUtils#collectionToDelimited(java.util.Collection, String)}
   * with a comma delimiter.
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

  /**
   * Test {@link StringUtils#tabToSpace(String)} with empty strings.
   * 
   * @param empty
   *          The test empty value.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void tabToSpaceEmptyStringTest(String empty) {

    // Tab will get replaced. Duh.
    if (null != empty && empty.equals("\t")) {
      assertEquals(" ", StringUtils.tabToSpace(empty));
    } else {
      assertEquals(empty, StringUtils.tabToSpace(empty));
    }
  }

  /**
   * Test {@link StringUtils#tabToSpace(String)} with various strings.
   */
  @Test
  public void tabToSpaceTest() {

    String noTabs = "I am a string with no tabs";
    assertEquals(noTabs, StringUtils.tabToSpace(noTabs));

    assertEquals("I have a tab", StringUtils.tabToSpace("I have a\ttab"));
    assertEquals(" Tab at start", StringUtils.tabToSpace("\tTab at start"));
    assertEquals("Tab at end ", StringUtils.tabToSpace("Tab at end\t"));
    assertEquals("Escape test\\t1", StringUtils.tabToSpace("Escape test\\t1"));
    assertEquals("Escape test\\ 2", StringUtils.tabToSpace("Escape test\\\t2"));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToLongList(String)} with
   * empty strings returns an empty list.
   *
   * @param empty
   *          The generated empty string
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void delimitedToLongListEmptyTest(String empty) {
    assertEquals(0, StringUtils.delimitedToLongList(empty).size());
  }

  /**
   * Test calling {@link StringUtils#delimitedToLongList(String)} with a single
   * value.
   */
  @Test
  public void delimitedToLongListOneTest() {
    assertTrue(checkLongList(StringUtils.delimitedToLongList("4"), 4L));
  }

  /**
   * Test calling {@link StringUtils#delimitedToLongList(String)} with a several
   * values.
   */
  @Test
  public void delimitedToLongListMultipleTest() {
    assertTrue(
      checkLongList(StringUtils.delimitedToLongList("4,5,6"), 4L, 5L, 6L));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToLongList(String)} with
   * spaces between values will fail.
   */
  @Test
  public void delimitedToLongListSpaceBetweenValuesTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToLongList("4, 5, 6");
    });
  }

  /**
   * Test that calling {@link StringUtils#delimitedToLongList(String)} with end
   * padding will fail.
   */
  @Test
  public void delimitedToLongListPaddedListTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToLongList("4,5,6 ");
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToLongList(String)} with negative,
   * zero and positive values.
   */
  @Test
  public void delimitedToLongListPositiveNegativeTest() {
    assertTrue(
      checkLongList(StringUtils.delimitedToLongList("-1,0,1"), -1L, 0L, 1L));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToLongList(String)} with a
   * string value fails.
   */
  @Test
  public void delimitedToLongListCharTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToLongList("flurble");
    });
  }

  /**
   * Test that calling {@link StringUtils#delimitedToLongList(String)} with a
   * decimal value fails.
   */
  @Test
  public void delimitedToLongListDecimalTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToLongList("4.5");
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToLongList(String)} with an empty
   * value fails.
   */
  @Test
  public void delimitedToLongListEmptyValueTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToLongList("4,,6");
    });
  }

  /**
   * Check that a list of {@link Long}s contains the specified set of values.
   *
   * @param list
   *          The list to check.
   * @param longs
   *          The values that the list should contain.
   * @return {@code true} if the list contains the specified values;
   *         {@code false} otherwise.
   */
  private static final boolean checkLongList(List<Long> list, Long... longs) {
    boolean ok = true;

    if (list.size() != longs.length) {
      ok = false;
    } else {
      for (int i = 0; i < list.size(); i++) {
        if (!list.get(i).equals(longs[i])) {
          ok = false;
          break;
        }
      }
    }

    return ok;
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with an
   * empty String returns an empty list.
   * 
   * @param empty
   *          The test empty values.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void delimitedToDoubleListEmptyTest(String empty) {
    assertEquals(0, StringUtils.delimitedToDoubleList(empty).size());
  }

  /**
   * Test calling {@link StringUtils#delimitedToDoubleList(String)} with an
   * integer value.
   */
  @Test
  public void delimitedToDoubleListOneIntTest() {
    assertTrue(checkDoubleList(StringUtils.delimitedToDoubleList("4"), 4D));
  }

  /**
   * Test calling {@link StringUtils#delimitedToDoubleList(String)} with a
   * decimal value.
   */
  @Test
  public void delimitedToDoubleListOneTest() {
    assertTrue(
      checkDoubleList(StringUtils.delimitedToDoubleList("4.35454"), 4.35454D));
  }

  /**
   * Test calling {@link StringUtils#delimitedToDoubleList(String)} with
   * multiple values.
   */
  @Test
  public void delimitedToDoubleListMultipleTest() {
    assertTrue(
      checkDoubleList(StringUtils.delimitedToDoubleList("4.5;5.6;6.7;7"), 4.5D,
        5.6D, 6.7D, 7D));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with a
   * spaces between values fails.
   */
  @Test
  public void delimitedToDoubleListSpaceBetweenValuesTest() {
    assertTrue(
      checkDoubleList(StringUtils.delimitedToDoubleList("4.5; 5.6; 6.7; 7"),
        4.5D, 5.6D, 6.7D, 7D));
  }

  /**
   * Test calling {@link StringUtils#delimitedToDoubleList(String)} with a
   * variety of negative, zero and positive values.
   */
  @Test
  public void delimitedToDoubleListPositiveNegativeTest() {
    assertTrue(checkDoubleList(
      StringUtils
        .delimitedToDoubleList("-1.234;-1;-0.001;0;0.001;1;1.234;1.23e4"),
      -1.234D, -1D, -0.001D, 0D, 0.001D, 1D, 1.234, 1.23e4D));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with a
   * string value fails.
   */
  @Test
  public void delimitedToDoubleListCharTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToDoubleList("flurble");
    });
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with an
   * empty value fails.
   */
  @Test
  public void delimitedToDoubleListEmptyValueTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToDoubleList("4.5;;6.7");
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToDoubleList(String)} with a
   * variety of delimiters.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeDelimiters")
  public void delimitedToDoubleListDelimitersTest(String delimiter) {

    StringBuffer input = new StringBuffer();
    input.append("4.5");
    input.append(delimiter);
    input.append("5.6");
    input.append(delimiter);
    input.append("6.7");

    assertTrue(checkDoubleList(
      StringUtils.delimitedToDoubleList(input.toString(), delimiter), 4.5D,
      5.6D, 6.7D));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with
   * invalid delimiters fails.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeStandardInvalidDelimiters")
  public void delimitedToDoubleListStandardInvalidDelimitersTest(
    String delimiter) {

    assertThrows(IllegalArgumentException.class, () -> {
      StringUtils.delimitedToDoubleList("4.5,5.6", delimiter);
    });
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with
   * invalid delimiters for numbers fails.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeNumericInvalidDelimiters")
  public void delimitedToDoubleListNumericInvalidDelimitersTest(
    String delimiter) {

    assertThrows(IllegalArgumentException.class, () -> {
      StringUtils.delimitedToDoubleList("4.5,5.6", delimiter);
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToDoubleList(String)} with a space
   * delimiter.
   */
  @Test
  public void delimitedToDoubleListSpaceDelimiterTest() {
    assertTrue(checkDoubleList(
      StringUtils.delimitedToDoubleList("4.5 5.6 6.7", " "), 4.5D, 5.6D, 6.7D));
  }

  /**
   * Test that calling {@link StringUtils#delimitedToDoubleList(String)} with a
   * padded string fails.
   */
  @Test
  public void delimitedToDoubleListPaddedListTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToIntegerList("4.5,5.6,6.7 ", ",");
    });
  }

  /**
   * Check that a list of {@link Double}s contains the specified set of values.
   *
   * @param list
   *          The list to check.
   * @param doubles
   *          The values that the list should contain.
   * @return {@code true} if the list contains the specified values;
   *         {@code false} otherwise.
   */
  private static final boolean checkDoubleList(List<Double> list,
    Double... doubles) {
    boolean ok = true;

    if (list.size() != doubles.length) {
      ok = false;
    } else {
      for (int i = 0; i < list.size(); i++) {
        if (!list.get(i).equals(doubles[i])) {
          ok = false;
          break;
        }
      }
    }

    return ok;
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with an empty
   * String returns an empty list.
   * 
   * @param empty
   *          The test empty value.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void delimitedToIntegerListEmptyTest(String empty) {
    assertEquals(0, StringUtils.delimitedToIntegerList(empty, ",").size());
  }

  /**
   * Test calling {@link StringUtils#delimitedToIntegerList(String, String)}
   * with a single value.
   */
  @Test
  public void delimitedToIntegerListOneTest() {
    assertTrue(checkIntList(StringUtils.delimitedToIntegerList("4", ","), 4));
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with a decimal
   * value fails.
   */
  @Test
  public void delimitedToIntegerListSingleFloatTest() {

    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToIntegerList("4.35454", ",");
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToIntegerList(String, String)}
   * with a several value.
   */
  @Test
  public void delimitedToIntegerListMultipleTest() {
    assertTrue(checkIntList(StringUtils.delimitedToIntegerList("4,5,6,7", ","),
      4, 5, 6, 7));
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with spaces
   * between values fails.
   */
  @Test
  public void delimitedToIntegerListSpaceBetweenValuesTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToIntegerList("4, 5, 6, 7", ",");
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToIntegerList(String, String)}
   * with a set of positive and negative values.
   */
  @Test
  public void delimitedToIntegerListPositiveNegativeTest() {
    assertTrue(checkIntList(StringUtils.delimitedToIntegerList("-1,0,1", ","),
      -1, 0, 1));
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with a string
   * value fails.
   */
  @Test
  public void delimitedToIntegerListCharTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToIntegerList("flurble", ",");
    });
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with an empty
   * value fails.
   */
  @Test
  public void delimitedToIntegerListEmptyValueTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToIntegerList("4.5,,6.7", ",");
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToIntegerList(String, String)}
   * with a variety of delimiters.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeDelimiters")
  public void delimitedToIntgerListDelimitersTest(String delimiter) {

    StringBuffer input = new StringBuffer();
    input.append("4");
    input.append(delimiter);
    input.append("5");
    input.append(delimiter);
    input.append("6");

    assertTrue(checkIntList(
      StringUtils.delimitedToIntegerList(input.toString(), delimiter), 4, 5,
      6));
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with invalid
   * delimiters fails.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeStandardInvalidDelimiters")
  public void delimitedToIntegerListStandardInvalidDelimitersTest(
    String delimiter) {

    assertThrows(IllegalArgumentException.class, () -> {
      StringUtils.delimitedToIntegerList("4,5", delimiter);
    });
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with invalid
   * numeric delimiters fails.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeNumericInvalidDelimiters")
  public void delimitedToIntegerListNumericInvalidDelimitersTest(
    String delimiter) {

    assertThrows(IllegalArgumentException.class, () -> {
      StringUtils.delimitedToIntegerList("4,5", delimiter);
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToIntegerList(String, String)}
   * with a space delimiter.
   */
  @Test
  public void delimitedToIntegerListSpaceDelimiterTest() {
    assertTrue(
      checkIntList(StringUtils.delimitedToIntegerList("4 5 6", " "), 4, 5, 6));
  }

  /**
   * Test that calling
   * {@link StringUtils#delimitedToIntegerList(String, String)} with a padded
   * list fails.
   */
  @Test
  public void delimitedToIntegerListPaddedListTest() {
    assertThrows(NumberFormatException.class, () -> {
      StringUtils.delimitedToIntegerList("4,5,6 ", ",");
    });
  }

  /**
   * Check that a list of {@link Double}s contains the specified set of values.
   *
   * @param list
   *          The list to check.
   * @param ints
   *          The values that the list should contain.
   * @return {@code true} if the list contains the specified values;
   *         {@code false} otherwise.
   */
  private static final boolean checkIntList(List<Integer> list,
    Integer... ints) {
    boolean ok = true;

    if (list.size() != ints.length) {
      ok = false;
    } else {
      for (int i = 0; i < list.size(); i++) {
        if (!list.get(i).equals(ints[i])) {
          ok = false;
          break;
        }
      }
    }

    return ok;
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * null or empty string fails, and any other whitespace character gives a list
   * size of one containing that character.
   * 
   * @param empty
   *          The test empty value.
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void delimitedToListEmptyTest(String empty) {

    if (null == empty || empty.length() == 0) {
      assertEquals(0, StringUtils.delimitedToList(empty, ",").size());
    } else {
      assertTrue(
        checkStringList(StringUtils.delimitedToList(empty, ","), empty));
    }
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * single value.
   */
  @Test
  public void delimitedToListOneTest() {
    assertTrue(
      checkStringList(StringUtils.delimitedToList("flurble", ","), "flurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with
   * several value.
   */
  @Test
  public void delimitedToListMultipleTest() {
    assertTrue(
      checkStringList(StringUtils.delimitedToList("flurble,hurble,nurble", ","),
        "flurble", "hurble", "nurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * various numeric values (all should retain their exact input format).
   */
  @Test
  public void delimitedToListNumbersTest() {
    assertTrue(
      checkStringList(StringUtils.delimitedToList("-3,0,4.345,5.400", ","),
        "-3", "0", "4.345", "5.400"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * spaces between values (spaces should be preserved).
   */
  @Test
  public void delimitedToListSpaceBetweenValuesTest() {
    assertTrue(checkStringList(
      StringUtils.delimitedToList("flurble, hurble, nurble", ","), "flurble",
      " hurble", " nurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with an
   * empty value (the empty value is retained as an empty string).
   */
  @Test
  public void delimitedToListEmptyValueTest() {
    assertTrue(
      checkStringList(StringUtils.delimitedToList("flurble,,hurble", ","),
        "flurble", "", "hurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * various delimiters.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeDelimiters")
  public void delimitedToIntgerDelimitersTest(String delimiter) {

    StringBuffer input = new StringBuffer();
    input.append("flurble");
    input.append(delimiter);
    input.append("hurble");
    input.append(delimiter);
    input.append("nurble");

    assertTrue(
      checkStringList(StringUtils.delimitedToList(input.toString(), delimiter),
        "flurble", "hurble", "nurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with
   * invalid delimiters.
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeStandardInvalidDelimiters")
  public void delimitedToListStandardInvalidDelimitersTest(String delimiter) {

    assertThrows(IllegalArgumentException.class, () -> {
      StringUtils.delimitedToList("4,5", delimiter);
    });
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with
   * invalid numeric delimiters (these are allowed for strings).
   * 
   * @param delimiter
   *          The test delimiter.
   */
  @ParameterizedTest
  @MethodSource("makeNumericInvalidDelimiters")
  public void delimitedToListNumericInvalidDelimitersTest(String delimiter) {

    StringBuffer input = new StringBuffer();
    input.append("flurble");
    input.append(delimiter);
    input.append("hurble");
    input.append(delimiter);
    input.append("nurble");

    assertTrue(
      checkStringList(StringUtils.delimitedToList(input.toString(), delimiter),
        "flurble", "hurble", "nurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * space delimiter.
   */
  @Test
  public void delimitedToListSpaceDelimiterTest() {
    assertTrue(
      checkStringList(StringUtils.delimitedToList("flurble hurble nurble", " "),
        "flurble", "hurble", "nurble"));
  }

  /**
   * Test calling {@link StringUtils#delimitedToList(String, String)} with a
   * padded list (the padding is retained in the list values).
   */
  @Test
  public void delimitedToListPaddedListTest() {
    assertTrue(checkStringList(
      StringUtils.delimitedToList("flurble,hurble,nurble ", ","), "flurble",
      "hurble", "nurble "));
  }

  /**
   * Test that stackTraceToString returns a string with the correct trace.
   *
   * <p>
   * The test is not comprehensive, but does check that the trace contains
   * details of this class at the least.
   * </p>
   */
  @Test
  public void stackTraceToStringTest() {
    Exception e = new Exception("Test Throwable");
    String string = StringUtils.stackTraceToString(e);

    boolean stringOK = true;

    if (string.indexOf("java.lang.Exception: Test Throwable") == -1) {
      stringOK = false;
    }

    if (string.indexOf(
      "at junit.uk.ac.exeter.QuinCe.utils.StringUtilsTest.stackTraceToStringTest(StringUtilsTest.java:") == -1) {
      stringOK = false;
    }

    assertTrue(stringOK);
  }

  /**
   * Generate a list of various strings to be used for testing
   * {@link StringUtils#trimList(List)} and
   * {@link StringUtils#trimListAndQuotes(List)}.
   *
   * @return The test list
   * @see #trimListTest()
   * @see #trimListAndQuotesTest()
   */
  private static final List<String> makeTrimListInput() {
    List<String> list = new ArrayList<String>();

    list.add("plain");
    list.add(" space front");
    list.add("space end ");
    list.add("\\leading backslash");
    list.add("\\\\two leading backslashes");
    list.add("any \\ other backslash\\");
    list.add("\\leading backslash trailing space ");
    list.add("\\leading backslash trailing tab\t");
    list.add("\\ leading backlash and space");
    list.add("\\\tleading backlash and tab");
    list.add(" \\leading space and backslash");
    list.add("\t\\leading tab and backslash");
    list.add("\\\\ two leading backslashes and space");
    list.add(" \\\\space and two leading backslashes");
    list.add("   \n\r\tall the whitespace and a backlash");
    list.add("all the trailing whitespace  \r\n\t  ");
    list.add("\\ \\spaced backslashes");
    list.add("\\ \\\\spaced two backslashes");

    // And now all the same things but quoted
    list.add("\"plain\"");
    list.add("\" space front\"");
    list.add("\"space end \"");
    list.add("\"\\leading backslash\"");
    list.add("\"\\\\two leading backslashes\"");
    list.add("\"any \\ other backslash\\\"");
    list.add("\"\\leading backslash trailing space \"");
    list.add("\"\\leading backslash trailing tab\t\"");
    list.add("\"\\ leading backlash and space\"");
    list.add("\"\\\tleading backlash and tab\"");
    list.add("\" \\leading space and backslash\"");
    list.add("\"\t\\leading tab and backslash\"");
    list.add("\"\\\\ two leading backslashes and space\"");
    list.add("\" \\\\space and two leading backslashes\"");
    list.add("\"   \n\r\t\\all the whitespace and a backlash\"");
    list.add("\"all the trailing whitespace  \r\n\t  \"");
    list.add("\"\\ \\spaced backslashes\"");
    list.add("\"\\ \\\\spaced two backslashes\"");

    // Other quote nonsense
    list.add("\"front quote only");
    list.add(" \"space and front quote");
    list.add("\" quote and space");
    list.add("trailing quote only\"");
    list.add("trailing quote and space\" ");
    list.add("trailing space and quote \"");
    list.add(" \"space and quote at both ends \"");
    list.add("\" quote and space at both ends\" ");
    list.add("\"\"two lots of quotes\"\"");
    list.add("\" \"two quotes with space\" \"");

    // Other general nonsense
    list.add(""); // ''
    list.add("\""); // '"'
    list.add("\"\""); // '""'
    list.add("\" \""); // '" "'
    list.add("\"\"\""); // '"""'
    list.add("\\\""); // '\"'
    list.add("\\\\\""); // '\\"'
    list.add("\"  \t\r\""); // ' \t\r"'

    return list;
  }

  /**
   * Test the basic {@link StringUtils#trimList(List)} method with a variety of
   * input strings.
   *
   * @see #makeTrimListInput()
   */
  @Test
  public void trimListTest() {
    List<String> expectedOutput = new ArrayList<String>();

    expectedOutput.add("plain");
    expectedOutput.add("space front");
    expectedOutput.add("space end");
    expectedOutput.add("leading backslash");
    expectedOutput.add("\\two leading backslashes");
    expectedOutput.add("any \\ other backslash\\");
    expectedOutput.add("leading backslash trailing space");
    expectedOutput.add("leading backslash trailing tab");
    expectedOutput.add("leading backlash and space");
    expectedOutput.add("leading backlash and tab");
    expectedOutput.add("leading space and backslash");
    expectedOutput.add("leading tab and backslash");
    expectedOutput.add("\\ two leading backslashes and space");
    expectedOutput.add("\\space and two leading backslashes");
    expectedOutput.add("all the whitespace and a backlash");
    expectedOutput.add("all the trailing whitespace");
    expectedOutput.add("spaced backslashes");
    expectedOutput.add("\\spaced two backslashes");

    // And now all the same things but quoted
    expectedOutput.add("\"plain\"");
    expectedOutput.add("\" space front\"");
    expectedOutput.add("\"space end \"");
    expectedOutput.add("\"\\leading backslash\"");
    expectedOutput.add("\"\\\\two leading backslashes\"");
    expectedOutput.add("\"any \\ other backslash\\\"");
    expectedOutput.add("\"\\leading backslash trailing space \"");
    expectedOutput.add("\"\\leading backslash trailing tab\t\"");
    expectedOutput.add("\"\\ leading backlash and space\"");
    expectedOutput.add("\"\\\tleading backlash and tab\"");
    expectedOutput.add("\" \\leading space and backslash\"");
    expectedOutput.add("\"\t\\leading tab and backslash\"");
    expectedOutput.add("\"\\\\ two leading backslashes and space\"");
    expectedOutput.add("\" \\\\space and two leading backslashes\"");
    expectedOutput.add("\"   \n\r\t\\all the whitespace and a backlash\"");
    expectedOutput.add("\"all the trailing whitespace  \r\n\t  \"");
    expectedOutput.add("\"\\ \\spaced backslashes\"");
    expectedOutput.add("\"\\ \\\\spaced two backslashes\"");

    // Other quote nonsense
    expectedOutput.add("\"front quote only");
    expectedOutput.add("\"space and front quote");
    expectedOutput.add("\" quote and space");
    expectedOutput.add("trailing quote only\"");
    expectedOutput.add("trailing quote and space\"");
    expectedOutput.add("trailing space and quote \"");
    expectedOutput.add("\"space and quote at both ends \"");
    expectedOutput.add("\" quote and space at both ends\"");
    expectedOutput.add("\"\"two lots of quotes\"\"");
    expectedOutput.add("\" \"two quotes with space\" \"");

    // Other general nonsense
    expectedOutput.add(""); // '' -> ''
    expectedOutput.add("\""); // '"' -> '"'
    expectedOutput.add("\"\""); // '""' -> '""'
    expectedOutput.add("\" \""); // '" "' -> '" "'
    expectedOutput.add("\"\"\""); // '"""' -> '"""'
    expectedOutput.add("\""); // '\"' -> '"'
    expectedOutput.add("\\\""); // '\\"' -> '\"'
    expectedOutput.add("\"  \t\r\""); // '" \t\r"' -> '" \t\r"'

    List<String> trimmedList = StringUtils.trimList(makeTrimListInput());

    // You can use the below to identify individual failing strings
    /*
     * for (int i = 0; i < trimmedList.size(); i++) { System.out.println("Got '"
     * + trimmedList.get(i) + "' Expected '" + expectedOutput.get(i) + "'");
     *
     * assertTrue(trimmedList.get(i).equals(expectedOutput.get(i))); }
     */

    assertEquals(trimmedList, expectedOutput);
  }

  /**
   * Test the basic {@link StringUtils#trimListAndQuotes(List)} method with a
   * variety of input strings
   *
   * @see #makeTrimListInput()
   */
  @Test
  public void trimListAndQuotesTest() {
    List<String> expectedOutput = new ArrayList<String>();

    expectedOutput.add("plain");
    expectedOutput.add("space front");
    expectedOutput.add("space end");
    expectedOutput.add("leading backslash");
    expectedOutput.add("\\two leading backslashes");
    expectedOutput.add("any \\ other backslash\\");
    expectedOutput.add("leading backslash trailing space");
    expectedOutput.add("leading backslash trailing tab");
    expectedOutput.add("leading backlash and space");
    expectedOutput.add("leading backlash and tab");
    expectedOutput.add("leading space and backslash");
    expectedOutput.add("leading tab and backslash");
    expectedOutput.add("\\ two leading backslashes and space");
    expectedOutput.add("\\space and two leading backslashes");
    expectedOutput.add("all the whitespace and a backlash");
    expectedOutput.add("all the trailing whitespace");
    expectedOutput.add("spaced backslashes");
    expectedOutput.add("\\spaced two backslashes");

    // And now all the same things but quoted
    expectedOutput.add("plain");
    expectedOutput.add("space front");
    expectedOutput.add("space end");
    expectedOutput.add("leading backslash");
    expectedOutput.add("\\two leading backslashes");
    expectedOutput.add("any \\ other backslash\\");
    expectedOutput.add("leading backslash trailing space");
    expectedOutput.add("leading backslash trailing tab");
    expectedOutput.add("leading backlash and space");
    expectedOutput.add("leading backlash and tab");
    expectedOutput.add("leading space and backslash");
    expectedOutput.add("leading tab and backslash");
    expectedOutput.add("\\ two leading backslashes and space");
    expectedOutput.add("\\space and two leading backslashes");
    expectedOutput.add("all the whitespace and a backlash");
    expectedOutput.add("all the trailing whitespace");
    expectedOutput.add("spaced backslashes");
    expectedOutput.add("\\spaced two backslashes");

    // Other quote nonsense
    expectedOutput.add("front quote only");
    expectedOutput.add("space and front quote");
    expectedOutput.add("quote and space");
    expectedOutput.add("trailing quote only");
    expectedOutput.add("trailing quote and space");
    expectedOutput.add("trailing space and quote");
    expectedOutput.add("space and quote at both ends");
    expectedOutput.add("quote and space at both ends");
    expectedOutput.add("two lots of quotes");
    expectedOutput.add("two quotes with space");

    // Other general nonsense
    expectedOutput.add(""); // '' -> ''
    expectedOutput.add(""); // '"' -> ''
    expectedOutput.add(""); // '""' -> ''
    expectedOutput.add(""); // '" "' -> ''
    expectedOutput.add(""); // '"""' -> ''
    expectedOutput.add(""); // '\"' -> ''
    expectedOutput.add("\\"); // '\\"' -> '\'
    expectedOutput.add(""); // '" \t\r"' -> ''

    List<String> trimmedList = StringUtils
      .trimListAndQuotes(makeTrimListInput());

    // You can use the below to identify individual failing strings
    /*
     * for (int i = 0; i < trimmedList.size(); i++) { System.out.println("Got '"
     * + trimmedList.get(i) + "' Expected '" + expectedOutput.get(i) + "'");
     *
     * assertTrue(trimmedList.get(i).equals(expectedOutput.get(i))); }
     */

    assertEquals(trimmedList, expectedOutput);
  }

  /**
   * Test {@link StringUtils#trimList(List)} with a {@code null} list.
   */
  @Test
  public void trimListNullListTest() {
    assertNull(StringUtils.trimList(null));
  }

  /**
   * Test {@link StringUtils#trimList(List)} with an empty list.
   */
  @Test
  public void trimListEmptyTest() {
    List<String> list = new ArrayList<String>();
    assertEquals(0, StringUtils.trimList(list).size());
  }

  /**
   * Test {@link StringUtils#trimList(List)} with list containing a {@code null}
   * element.
   */
  @Test
  public void trimListNullEntryTest() {
    List<String> list = new ArrayList<String>();
    list.add(null);

    List<String> trimmedList = StringUtils.trimList(list);

    assertEquals(1, trimmedList.size());
    assertNull(trimmedList.get(0));
  }

  /**
   * Test {@link StringUtils#trimListAndQuotes(List)} with a {@code null} list.
   */
  @Test
  public void trimListAndQuotesNullListTest() {
    assertNull(StringUtils.trimListAndQuotes(null));
  }

  /**
   * Test {@link StringUtils#trimListAndQuotes(List)} with an empty list.
   */
  @Test
  public void trimListAndQuotesEmptyTest() {
    List<String> list = new ArrayList<String>();
    assertEquals(0, StringUtils.trimListAndQuotes(list).size());
  }

  /**
   * Test {@link StringUtils#trimListAndQuotes(List)} with list containing a
   * {@code null} element.
   */
  @Test
  public void trimListAndQuotesNullEntryTest() {
    List<String> list = new ArrayList<String>();
    list.add(null);

    List<String> trimmedList = StringUtils.trimListAndQuotes(list);

    assertEquals(1, trimmedList.size());
    assertNull(trimmedList.get(0));
  }

  /**
   * Generate a list of {@link String}s for testing
   * {@link StringUtils#sortByLength(List, boolean)}.
   *
   * @return The list
   * @see #sortByLengthAscendingTest()
   * @see #sortByLengthDescendingTest()
   */
  private static final List<String> makeLengthSortList() {
    List<String> list = new ArrayList<String>();

    list.add("aaaa");
    list.add("aa");
    list.add(null);
    list.add("bbb");
    list.add("a");
    list.add(null);
    list.add("aa");

    return list;
  }

  /**
   * Test {@link StringUtils#sortByLength(List, boolean)} with ascending order.
   *
   * @see #makeLengthSortList()
   */
  @Test
  public void sortByLengthAscendingTest() {

    List<String> expectedOutput = new ArrayList<String>();
    expectedOutput.add(null);
    expectedOutput.add(null);
    expectedOutput.add("a");
    expectedOutput.add("aa");
    expectedOutput.add("aa");
    expectedOutput.add("bbb");
    expectedOutput.add("aaaa");

    List<String> source = makeLengthSortList();
    StringUtils.sortByLength(source, false);
    assertEquals(expectedOutput, source);
  }

  /**
   * Test {@link StringUtils#sortByLength(List, boolean)} with descending order.
   *
   * @see #makeLengthSortList()
   */
  @Test
  public void sortByLengthDescendingTest() {

    List<String> expectedOutput = new ArrayList<String>();
    expectedOutput.add("aaaa");
    expectedOutput.add("bbb");
    expectedOutput.add("aa");
    expectedOutput.add("aa");
    expectedOutput.add("a");
    expectedOutput.add(null);
    expectedOutput.add(null);

    List<String> source = makeLengthSortList();
    StringUtils.sortByLength(source, true);
    assertEquals(expectedOutput, source);
  }

  /**
   * Generate a list of {@link String}s for testing
   * {@link StringUtils#sortByLength(List, boolean)}.
   *
   * <p>
   * The list contains strings of length 1, 2, 3, 3, 4. The two three-length
   * strings will not be ordered, but we can test the lengths of the strings to
   * ensure they are in the correct order.
   * </p>
   *
   * @return The list
   * @see #sortBySameLengthAscendingTest()
   * @see #sortBySameLengthDescendingTest()
   */
  private static final List<String> makeSameLengthSortList() {
    List<String> list = new ArrayList<String>();

    list.add("aaaa");
    list.add("aaa");
    list.add("bbb");
    list.add("a");
    list.add("aa");

    return list;
  }

  /**
   * Test {@link StringUtils#sortByLength(List, boolean)} with ascending order.
   *
   * @see #makeSameLengthSortList()
   */
  @Test
  public void sortBySameLengthAscendingTest() {

    List<String> source = makeSameLengthSortList();
    StringUtils.sortByLength(source, false);

    assertEquals(1, source.get(0).length());
    assertEquals(2, source.get(1).length());
    assertEquals(3, source.get(2).length());
    assertEquals(3, source.get(3).length());
    assertEquals(4, source.get(4).length());
  }

  /**
   * Test {@link StringUtils#sortByLength(List, boolean)} with ascending order.
   *
   * @see #makeSameLengthSortList()
   */
  @Test
  public void sortBySameLengthDescendingTest() {

    List<String> source = makeSameLengthSortList();
    StringUtils.sortByLength(source, true);

    assertEquals(4, source.get(0).length());
    assertEquals(3, source.get(1).length());
    assertEquals(3, source.get(2).length());
    assertEquals(2, source.get(3).length());
    assertEquals(1, source.get(4).length());
  }

  /**
   * Make the set of test values for
   * {@link #formatDoubleStringTest(String, String)}.
   *
   * @return The test values
   */
  private static final Object[] getFormatDoubleStringValues() {
    return new Object[] { new Object[] { "0.0", "0.000" },
      new Object[] { "0.000000000", "0.000" }, new Object[] { "-0.0", "0.000" },
      new Object[] { "1", "1.000" }, new Object[] { "1.2", "1.200" },
      new Object[] { "1.27", "1.270" }, new Object[] { "1.270", "1.270" },
      new Object[] { "1.2700", "1.270" }, new Object[] { "1.2703", "1.270" },
      new Object[] { "1.2705", "1.271" }, new Object[] { "1.2707", "1.271" },
      new Object[] { "-12.3", "-12.300" },
      new Object[] { "-12.3705", "-12.371" },
      new Object[] { "123e4", "1230000.000" }, new Object[] { null, null },
      new Object[] { "", "" }, new Object[] { " ", " " },
      new Object[] { "letters", "letters" },
      new Object[] { "123abc", "123abc" } };

  }

  /**
   * Test {@link StringUtils#formatNumber(String)} with various values.
   *
   * @param in
   *          The input
   * @param out
   *          The expected output
   * @see #getFormatDoubleStringValues()
   */
  @ParameterizedTest
  @MethodSource("getFormatDoubleStringValues")
  public void formatDoubleStringTest(String in, String out) {
    assertEquals(out, StringUtils.formatNumber(in));
  }

  /**
   * Make the set of test values for
   * {@link #formatDoubleDoubleTest(Double, String)}.
   *
   * @return The test values.
   */
  private static final Object[] getFormatDoubleDoubleValues() {
    return new Object[] { new Object[] { Double.valueOf(0.0), "0.000" },
      new Object[] { Double.valueOf(0.000000000), "0.000" },
      new Object[] { Double.valueOf(-0.00), "-0.000" },
      new Object[] { Double.valueOf(1), "1.000" },
      new Object[] { Double.valueOf(1.2), "1.200" },
      new Object[] { Double.valueOf(1.27), "1.270" },
      new Object[] { Double.valueOf(1.270), "1.270" },
      new Object[] { Double.valueOf(1.2700), "1.270" },
      new Object[] { Double.valueOf(1.2703), "1.270" },
      new Object[] { Double.valueOf(1.2707), "1.271" },
      new Object[] { Double.valueOf(-12.3), "-12.300" },
      new Object[] { Double.valueOf(123e4), "1230000.000" },
      new Object[] { null, null } };
  }

  /**
   * Test {@link StringUtils#formatNumber(Double)} with various values.
   *
   * @param in
   *          The input
   * @param out
   *          The expected output
   * @see #getFormatDoubleDoubleValues()
   */
  @ParameterizedTest
  @MethodSource("getFormatDoubleDoubleValues")
  public void formatDoubleDoubleTest(Double in, String out) {
    assertEquals(out, StringUtils.formatNumber(in));
  }
}
