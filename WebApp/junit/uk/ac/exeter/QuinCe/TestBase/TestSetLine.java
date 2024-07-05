package uk.ac.exeter.QuinCe.TestBase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;

/**
 * Represents a single line from a Test Set {@code .csv} file.
 * <p>
 * Some tests take a large combination of inputs and have a corresponding number
 * of expected results. Instead of providing all these in code, it can be easier
 * to build an input file containing the test criteria.
 * </p>
 * <p>
 * This method provides a means for a test to have its criteria defined in a
 * {@code .csv} file. It reads a given file, and provides a {@link Stream} of
 * {@code TestSetLine} objects each representing a single line in the file,
 * which can be used as input to a {@link ParameterizedTest}.
 * </p>
 * <p>
 * Note that this functionality knows nothing about the structure of any given
 * {@code .csv} file - it is up to the test to know which columns it needs to
 * read for its own purposes.
 * </p>
 *
 * @see TestSetTest#getLines()
 * @author Steve Jones
 */
public class TestSetLine {

  /**
   * The line number in the Test Set file
   */
  private int lineNumber;

  /**
   * The contents of the line split into fields
   */
  private String[] fields;

  /**
   * Formatter for date/times in a field
   */
  private static DateTimeFormatter dateTimeFormatter = null;

  static {
    dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'")
      .withZone(ZoneOffset.UTC);
  }

  /**
   * Basic constructor
   *
   * @param lineNumber
   *          The position of this line in the Test Set file
   * @param fields
   *          The contents of the line split into fields
   */
  protected TestSetLine(int lineNumber, String[] fields) {
    this.lineNumber = lineNumber;
    this.fields = fields;
  }

  /**
   * Get the line number
   *
   * @return The line number
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Get a field value as a String
   *
   * @param fieldNumber
   *          The zero-based field number
   * @param emptyAsNull
   *          Indicates whether empty values should be returned as
   *          ({@code true}) {@code null} or ({@code false}) an empty
   *          {@link String}.
   * @return The field contents
   */
  public String getStringField(int fieldNumber, boolean emptyAsNull) {
    String result = fields[fieldNumber].trim();

    if (result.length() == 0 && emptyAsNull) {
      result = null;
    }

    return result;
  }

  /**
   * Get a field value as a {@code char}.
   * <p>
   * If the field is null or empty, the char value will be zero ({@code NUL}).
   * Otherwise it will be the first value in the trimmed string.
   * </p>
   *
   * @param fieldNumber
   *          The zero-based field number.
   * @return The char value.
   */
  public char getCharField(int fieldNumber) {

    char result = '\0';

    String fieldValue = fields[fieldNumber].trim();
    if (null != fieldValue && fieldValue.length() > 0) {
      result = fieldValue.charAt(0);
    }

    return result;
  }

  /**
   * Get a field value as a boolean
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return The field value
   */
  public boolean getBooleanField(int fieldNumber) {
    return Boolean.parseBoolean(fields[fieldNumber]);
  }

  /**
   * Get a field value as an integer.
   * <p>
   * Returns {@code 0} if the field is empty.
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return The field value
   */
  public int getIntField(int fieldNumber) {
    int result = 0;

    if (!isFieldEmpty(fieldNumber)) {
      result = Integer.parseInt(fields[fieldNumber]);
    }

    return result;
  }

  /**
   * Get a field value as a long
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return The field value
   */
  public long getLongField(int fieldNumber) {
    long result = 0;

    if (!isFieldEmpty(fieldNumber)) {
      result = Long.parseLong(fields[fieldNumber]);
    }

    return result;
  }

  /**
   * Get a field value as a double
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return The field value
   */
  public Double getDoubleField(int fieldNumber) {
    Double result = Double.NaN;

    if (!isFieldEmpty(fieldNumber)
      && !getStringField(fieldNumber, false).equals("NaN")) {
      result = Double.parseDouble(fields[fieldNumber]);
    }

    return result;
  }

  /**
   * Get a field value as a {@link LocalDateTime}
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return The field value
   */
  public LocalDateTime getTimeField(int fieldNumber) {

    LocalDateTime result = null;

    if (!isFieldEmpty(fieldNumber)) {
      result = LocalDateTime.parse(fields[fieldNumber], dateTimeFormatter);
    }

    return result;
  }

  public Flag getFlagField(int fieldNumber) throws InvalidFlagException {
    char flagChar = getCharField(fieldNumber);
    return flagChar == '\0' ? null : new Flag(flagChar);
  }

  /**
   * Get a field of characters as a String list in the order that they are
   * specified
   *
   * @param fieldNumber
   * @return
   */
  public List<String> getCharListField(int fieldNumber) {
    String field = getStringField(fieldNumber, true);

    List<String> result;

    if (null == field) {
      result = new ArrayList<String>(0);
    } else {
      result = Arrays
        .asList(getStringField(fieldNumber, false).trim().split(""));
    }

    return result;
  }

  /**
   * Get a field of characters as a String list in String order
   *
   * @param fieldNumber
   * @return
   */
  public TreeSet<String> getOrderedCharListField(int fieldNumber) {
    return new TreeSet<String>(getCharListField(fieldNumber));
  }

  /**
   * See if a field is empty
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return {@code true} if the field is empty; {@code false} if not
   */
  public boolean isFieldEmpty(int fieldNumber) {
    return fields[fieldNumber].trim().length() == 0;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Line ");
    result.append(lineNumber);
    result.append(": ");
    result.append(Arrays.stream(fields).collect(Collectors.joining(",")));

    return result.toString();
  }
}
