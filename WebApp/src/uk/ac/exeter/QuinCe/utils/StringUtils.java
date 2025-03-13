package uk.ac.exeter.QuinCe.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Miscellaneous string utilities.
 *
 * <p>
 * This class extends {@link org.apache.commons.lang3.StringUtils} so methods
 * from that class can be called directly through this one, thereby reducing
 * issues with {@code import} statements if a class needs to use methods from
 * both classes.
 * </p>
 */
public final class StringUtils extends org.apache.commons.lang3.StringUtils {

  private static DecimalFormat threeDecimalPoints;

  static {
    threeDecimalPoints = new DecimalFormat();
    threeDecimalPoints.setMinimumFractionDigits(3);
    threeDecimalPoints.setMaximumFractionDigits(3);
    threeDecimalPoints.setGroupingUsed(false);
    threeDecimalPoints.setRoundingMode(RoundingMode.HALF_UP);
    threeDecimalPoints.setDecimalFormatSymbols(
      new DecimalFormatSymbols(new Locale("en", "US")));
  }

  /**
   * Private constructor to prevent instantiation
   */
  private StringUtils() {
    // Do nothing
  }

  /**
   * Converts a collection of values to a single string, with a specified
   * delimiter.
   *
   * <b>Note that this does not handle the case where the delimiter is found
   * within the values themselves.</b>
   *
   * @param collection
   *          The list to be converted
   * @param delimiter
   *          The delimiter to use
   * @return The converted list
   */
  public static String collectionToDelimited(Collection<?> collection,
    String delimiter) {

    String result = "";

    if (null != collection) {
      result = collection.stream().map(c -> c.toString())
        .collect(Collectors.joining(null == delimiter ? "" : delimiter));
    }

    return result;
  }

  /**
   * Extract the specified entries from a {@link List} and combine them into a
   * single {@link String} separated by the specified delimiter.
   *
   * <p>
   * The entries are specified by their list indices.
   * </p>
   *
   * @param list
   *          The {@link List} from which the entries must be extracted.
   * @param entries
   *          The indices of the entries to extract.
   * @param delimiter
   *          The delimiter to use between entries.
   * @return The extracted entries.
   */
  public static String listToDelimited(List<String> list,
    TreeSet<Integer> entries, String delimiter) {

    List<String> selection = new ArrayList<String>();

    entries.forEach(e -> {
      selection.add(list.get(e));
    });

    return collectionToDelimited(selection, delimiter);
  }

  /**
   * Converts a String containing values separated a specified delimiter into a
   * list of String values.
   *
   * <p>
   * <strong>Limitations:</strong>
   * <ul>
   * <li>This does not handle escaped delimiters within the values
   * themselves.</li>
   * <li>Full stops/periods can be used as the delimeter, but other regex
   * special characters will not work.</li>
   * </ul>
   *
   * @param values
   *          The String to be converted
   * @param delimiter
   *          The delimiter
   * @return A list of String values
   * @see #checkDelimiter(String, String...)
   */
  public static List<String> delimitedToList(String values, String delimiter) {

    checkDelimiter(delimiter);

    List<String> result;

    if (null == values) {
      result = new ArrayList<String>(0);
    } else if (values.length() == 0) {
      result = new ArrayList<String>(0);
    } else {
      String regex = delimiter;
      if (delimiter.equals(".")) {
        regex = "\\.";
      }

      result = Arrays.asList(values.split(regex, 0));
    }

    return result;
  }

  /**
   * Convert a delimited list of integers into a list of integers
   *
   * @param values
   *          The list
   * @param delimiter
   *          The delimiter
   * @return The list as integers
   * @see #checkDelimiter(String, String...)
   */
  public static List<Integer> delimitedToIntegerList(String values,
    String delimiter) {

    checkDelimiter(delimiter, "-", ".");

    List<Integer> result;

    if (null == values || values.trim().length() == 0) {
      result = new ArrayList<Integer>(0);
    } else {
      String[] numberList = values.split(delimiter);
      result = new ArrayList<Integer>(numberList.length);

      for (String number : numberList) {
        result.add(Integer.parseInt(number));
      }
    }

    return result;
  }

  /**
   * Convert a delimited list of doubles (with {@code ;} separator) into a list
   * of doubles.
   *
   * @param values
   *          The delimited list.
   * @return The list of doubles.
   */
  public static List<Double> delimitedToDoubleList(String values) {
    return delimitedToDoubleList(values, ";");
  }

  /**
   * Convert a delimited list of double into a list of doubles
   *
   * @param values
   *          The list
   * @param delimiter
   *          The delimiter used in the input string.
   * @return The list as integers
   * @see #checkDelimiter(String, String...)
   */
  public static List<Double> delimitedToDoubleList(String values,
    String delimiter) {

    checkDelimiter(delimiter, "-", ".");

    List<Double> result;

    if (null == values || values.trim().length() == 0) {
      result = new ArrayList<Double>(0);
    } else {
      String[] numberList = values.split(delimiter);
      result = new ArrayList<Double>(numberList.length);

      for (String number : numberList) {
        result.add(Double.parseDouble(number));
      }
    }

    return result;
  }

  /**
   * Convert a comma-separated list of numbers to a list of longs
   *
   * @param values
   *          The numbers
   * @return The longs
   */
  public static List<Long> delimitedToLongList(String values) {
    return delimitedToLongList(values, ',');
  }

  /**
   * Convert a comma-separated list of numbers to a list of longs
   *
   * @param values
   *          The numbers
   * @return The longs
   */
  public static List<Long> delimitedToLongList(String values, char delimiter) {
    // TODO This is the preferred way of doing this. Make the other methods do
    // the same.

    List<Long> result;

    if (null == values || values.trim().length() == 0) {
      result = new ArrayList<Long>(0);
    } else {
      String[] numberList = values.split(String.valueOf(delimiter));
      result = new ArrayList<Long>(numberList.length);

      for (String number : numberList) {
        result.add(Long.parseLong(number));
      }
    }

    return result;
  }

  /**
   * Convert a comma-separated list of numbers to a {@link Set} of longs. The
   * Set will be ordered by value.
   *
   * @param values
   *          The numbers.
   * @return The longs.
   */
  public static SortedSet<Long> delimitedToLongSet(String values) {
    TreeSet<Long> result = new TreeSet<Long>();

    if (null != values && values.trim().length() > 0) {
      String[] numberList = values.split(",");

      for (String number : numberList) {
        result.add(Long.parseLong(number));
      }
    }

    return result;
  }

  /**
   * Extract the stack trace from an Exception (or other Throwable) as a String.
   *
   * @param e
   *          The error
   * @return The stack trace
   */
  public static String stackTraceToString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Trims all items in a list of strings. A string that starts with a single
   * backslash has that backslash removed.
   *
   * @param source
   *          The strings to be converted
   * @return The converted strings
   */
  public static List<String> trimList(List<String> source) {

    List<String> result = null;

    if (null != source) {
      result = source.stream().map(s -> {
        return trimString(s, false);
      }).collect(Collectors.toList());
    }

    return result;
  }

  /**
   * Trims all items in a list of strings, and remotes any leading and/or
   * trailing double quotes.
   *
   * <p>
   * <ul>
   * <li>All whitespace and quotes are removed from the beginning and end of the
   * string.</li>
   * <li>If a single backslash remains on the front of the string, that is
   * removed also.</li>
   * <li>The process is repeated until the string is no longer modified.</li>
   * </ul>
   * </p>
   *
   * @param source
   *          The source list
   * @return The trimmed list
   */
  public static List<String> trimListAndQuotes(List<String> source) {

    List<String> result = null;

    if (null != source) {
      result = source.stream().map(s -> {
        return trimString(s, true);
      }).collect(Collectors.toList());
    }

    return result;
  }

  /**
   * Remove leading and trailing whitespace from a {@link String}, with extra
   * features over the standard {@link String#trim()}.
   *
   * <p>
   * If the string starts with a single {@code \}, it is treated as if it were a
   * whitespace character and trimmed. If the string starts with multiple
   * {@code \} characters, the first is removed and the rest are kept.
   * </p>
   *
   * <p>
   * If {@code replaceQuotes} is {@code true}, quote characters ({@code "} and
   * {@code '}) are treated as if they were whitespace characters and trimmed
   * accordingly.
   * </p>
   *
   * @param string
   *          The {@link String} to be trimmed.
   * @param replaceQuotes
   *          Indicates whether quote characters should be treated as
   *          whitespace.
   * @return The trimmed {@link String}.
   * @see #trimWhitespaceAndQuotes(String)
   */
  private static String trimString(String string, boolean replaceQuotes) {

    String trimmed = null;

    if (null != string) {
      if (replaceQuotes) {
        trimmed = trimWhitespaceAndQuotes(string);
      } else {
        trimmed = string.trim();
      }

      boolean done = false;
      while (!done) {
        if (trimmed.startsWith("\\\\")) {
          // If multiple \s, remove the first and stop
          trimmed = trimmed.substring(1);
          done = true;
        } else if (trimmed.startsWith("\\")) {

          // Trim off the single \ and trim the front again
          if (replaceQuotes) {
            trimmed = trimWhitespaceAndQuotes(trimmed.substring(1));
          } else {
            trimmed = trimmed.substring(1).trim();
          }
        } else {
          done = true;
        }
      }
    }

    return trimmed;
  }

  /**
   * Remove leading and trailing whitespace and quotes from a {@link String}.
   *
   * <p>
   * Algorithm based on {@link String#trim()}.
   * </p>
   *
   * @param string
   *          The String to be trimmed.
   * @return The trimmed String.
   */
  private static String trimWhitespaceAndQuotes(String string) {
    char[] chars = string.toCharArray();

    int length = chars.length;
    int len = length;
    int st = 0;
    while (st < len
      && (chars[st] <= ' ' || chars[st] == '"' || chars[st] == '\'')) {
      st++;
    }
    while (st < len && (chars[len - 1] <= ' ' || chars[len - 1] == '"'
      || chars[len - 1] == '\'')) {
      len--;
    }
    return ((st > 0) || (len < chars.length))
      ? new String(Arrays.copyOfRange(chars, st, len))
      : string;
  }

  /**
   * Create a {@link Properties} object from a string.
   *
   * <p>
   * This method is deprecated and should not be used; it is only kept because
   * it is used by a database migration.
   * </p>
   *
   * @param propsString
   *          The properties String
   * @return The Properties object
   * @throws IOException
   *           If the string cannot be parsed
   */
  @Deprecated
  public static Properties propertiesFromString(String propsString)
    throws IOException {
    Properties result = null;

    if (null != propsString && propsString.length() > 0) {
      StringReader reader = new StringReader(propsString);
      Properties props = new Properties();
      props.load(reader);
      return props;
    }

    return result;
  }

  /**
   * Make a valid CSV String from the given text.
   *
   * <p>
   * This always performs three steps:
   * </p>
   * <ul>
   * <li>Surround the value in quotes</li>
   * <li>Any " are replaced with "", per the CSV spec</li>
   * <li>Newlines are replaced with semi-colons</li>
   * </ul>
   *
   * <p>
   * While these are not strictly necessary for all values, they are appropriate
   * for this application and the target audiences of exported CSV files.
   * </p>
   *
   * @param text
   *          The value
   * @return The CSV value
   */
  public static String makeCsvString(String text) {
    StringBuilder csv = new StringBuilder();

    if (null == text) {
      text = "";
    }

    csv.append('"');
    csv.append(text.trim().replace("\"", "\"\"").replaceAll("[\\r\\n]+", ";"));
    csv.append('"');

    return csv.toString();
  }

  /**
   * Generate a Double value from a String, handling thousands separators
   *
   * @param value
   *          The string value
   * @return The double value
   */
  public static Double doubleFromString(String value) {
    Double result = Double.NaN;
    if (null != value && value.trim().length() > 0) {
      result = Double
        .parseDouble(trimString(removeFromString(value, ','), false));
    }

    return result;
  }

  /**
   * Remove all instances of a character from any position in a {@link String}.
   *
   * @param string
   *          The {@link String} from which the character is to be removed.
   * @param character
   *          The character to be removed.
   * @return The stripped {@link String}.
   */
  public static String removeFromString(String string, char character) {
    char[] output = new char[string.length()];

    int output_length = -1;
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) != character) {
        output_length++;
        output[output_length] = string.charAt(i);
      }
    }

    String result;

    if (output_length == -1) {
      result = "";
    } else {
      result = String.valueOf(output, 0, output_length + 1);
    }

    return result;
  }

  /**
   * Remove all instances of a character from the start of a {@link String}.
   *
   * @param string
   *          The {@link String} to be stripped.
   * @param character
   *          The character to be removed.
   * @return The stripped {@link String}.
   */
  public static String stripStart(String string, char character) {

    String result = null;

    if (null != string) {
      int stripPos = 0;
      while (stripPos < string.length()
        && string.charAt(stripPos) == character) {
        stripPos++;
      }

      if (stripPos >= string.length()) {
        result = "";
      } else {
        result = string.substring(stripPos);
      }
    }

    return result;
  }

  /**
   * Format a {@link String} containing a numeric value to three decimal places.
   *
   * <p>
   * If the {@link String} does not contain a numeric value, the passed in value
   * is returned unchanged.
   * </p>
   *
   * @param value
   *          The number.
   * @return The formatted number.
   * @see #threeDecimalPoints
   */
  public static String formatNumber(String value) {
    String result = value;

    if (NumberUtils.isCreatable(value)) {
      result = threeDecimalPoints.format(new BigDecimal(value));
    }

    return result;
  }

  /**
   * Format a number to three decimal places.
   *
   * @param value
   *          The number.
   * @return The formatted number.
   * @see #threeDecimalPoints
   */
  public static String formatNumber(Double value) {
    String result = null;

    if (null != value) {
      result = threeDecimalPoints.format(value);
    }

    return result;
  }

  /**
   * Replace all instances of a tab character in a {@link String} with a single
   * space.
   *
   * @param in
   *          The {@link String} to be processed.
   * @return The {@link String} with all tabs replaced.
   */
  public static String tabToSpace(String in) {

    String result = null;

    if (null != in) {
      return in.replaceAll("\t", " ");
    }

    return result;
  }

  /**
   * Check whether or not a specified delimiter is valid.
   *
   * <p>
   * The following delimiters are invalid:
   * </p>
   * <ul>
   * <li>{@code null}</li>
   * <li>Double or single quotes</li>
   * <li>The digits 0-9</li>
   * <li>Any strings specified in {@code invalidDelimiters}</li>
   * </ul>
   *
   * <p>
   * The method throws an {@link IllegalArgumentException} if the delimiter is
   * invalid.
   * </p>
   *
   * @param delimiter
   *          The delimiter to test.
   * @param invalidDelimiters
   *          The additional invalid delimiters
   */
  private static void checkDelimiter(String delimiter,
    String... invalidDelimiters) {

    if (null == delimiter) {
      throw new IllegalArgumentException("null delimiter is invalid");
    }

    if (delimiter.length() != 1) {
      throw new IllegalArgumentException("Delimiter must be one character");
    }

    if (Pattern.matches("[\"'0-9a-zA-Z]", delimiter)) {
      throw new IllegalArgumentException(
        "Invalid delimiter '" + delimiter + "'");
    }

    for (int i = 0; i < invalidDelimiters.length; i++) {
      if (delimiter.equals(invalidDelimiters[i])) {
        throw new IllegalArgumentException(
          "Invalid delimiter '" + delimiter + "'");
      }
    }
  }

  /**
   * Sort a list of {@link String}s by length.
   *
   * <p>
   * Nulls are considered to be shorter than zero-length strings. The ordering
   * of strings of the same length in the sorted list is not defined.
   * </p>
   *
   * @param list
   *          The list to be sorted.
   * @param descending
   *          Indicates whether the list entries should be sorted by descending
   *          length.
   *
   * @see DescendingLengthComparator
   * @see AscendingLengthComparator
   */
  public static void sortByLength(List<String> list, boolean descending) {

    Comparator<String> comparator = descending
      ? new DescendingLengthComparator()
      : new AscendingLengthComparator();

    Collections.sort(list, comparator);
  }

  /**
   * Format a {@link String} so it can be parsed by Javascript in a literal
   * string argument.
   *
   * <p>
   * Replaces {@code '} with {@code \'}.
   * </p>
   *
   * @param string
   *          The String to be converted.
   * @return The converted String.
   */
  public static String javascriptString(String string) {
    return string.replaceAll("'", Matcher.quoteReplacement("\\'"));
  }

  /**
   * Replace all newlines in a {@link String} with semicolons.
   *
   * @param str
   *          The {@link String} to be processed.
   * @return The processed {@link String}.
   */
  public static String replaceNewlines(String str) {
    return null == str ? null : str.replaceAll("\\r?\\n", ";");
  }

  /**
   * Take a {@link List} of {@link String}s and remove from the end any blank
   * lines (including those with just whitespace characters).
   *
   * @param list
   *          The {@link List}.
   */
  public static void removeBlankTailLines(List<String> list) {
    boolean blankLine = true;
    while (blankLine) {
      String lastLine = list.get(list.size() - 1);
      if (null == lastLine || lastLine.trim().length() == 0) {
        list.remove(list.size() - 1);
      } else {
        blankLine = false;
      }
    }
  }

  /**
   * Determine whether or not a {@link String} contains a numeric value.
   *
   * <p>
   * This simply calls {@link Double#parseDouble(String)} and determines whether
   * or not it succeeds. If a {@code null} value is passed, {@code false} is
   * returned so it is treated as a non-numeric value.
   * </p>
   *
   * @param value
   *          The value to be checked.
   * @return {@code true} if the value is numeric; {@code false} if it is not.
   * @see Double#parseDouble(String)
   */
  public static boolean isNumeric(String value) {
    boolean result = true;
    if (null == value) {
      result = false;
    } else {
      try {
        Double.parseDouble(value);
      } catch (NumberFormatException e) {
        result = false;
      }
    }

    return result;
  }

  /**
   * Combine two {@link String}s into a single {@link String} with the specified
   * combining {@link String} between them.
   *
   * <p>
   * The strings are only combined if both are non-empty (by the criteria of
   * {@link #isEmpty(CharSequence)}). Otherwise only the non-empty string is
   * returned, or an empty string if both are empty.
   * </p>
   *
   * <p>
   * If {@code unique} is {@code true}, only one of the strings will be returned
   * if they are both equal.
   * </p>
   *
   * @param string1
   *          The first {@link String}.
   * @param string2
   *          The second {@link String}.
   * @param combiner
   *          The combining {@link String}.
   * @param unique
   *          Indicates whether duplicate strings should be combined.
   * @return The combined {@link String}.
   * @see #isEmpty(CharSequence)
   */
  public static String combine(String string1, String string2, String combiner,
    boolean unique) {
    String result = "";

    if (null != string1 && !isEmpty(string1.trim())) {
      result = string1.trim();
    }

    if (null != string2 && !isEmpty(string2.trim())) {
      if (!unique || !string2.equals(string1)) {
        if (!isEmpty(result)) {
          result += combiner;
          result += string2.trim();
        } else {
          result = string2.trim();
        }
      }
    }

    return result;
  }

  /**
   * Search a {@link String} for instances where a specified character is
   * repeated, and keep only one instance of the character in each case.
   *
   * @param string
   *          The {@link String} to be edited.
   * @param character
   *          The character.
   * @return The edited String.
   */
  public static String removeRepeats(String string, char character) {

    String result = null;

    if (null != string) {
      char[] output = new char[string.length()];

      boolean inRepeat = false;
      int output_length = -1;

      for (int i = 0; i < string.length(); i++) {
        if (string.charAt(i) != character) {
          output_length++;
          output[output_length] = string.charAt(i);
          inRepeat = false;
        } else if (!inRepeat) {
          output_length++;
          output[output_length] = string.charAt(i);
          inRepeat = true;
        }
      }

      if (output_length == -1) {
        result = "";
      } else {
        result = String.valueOf(output, 0, output_length + 1);
      }
    }

    return result;
  }
}
