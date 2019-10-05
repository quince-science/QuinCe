package junit.uk.ac.exeter.QuinCe.TestBase;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;

/**
 * Represents a single line from a Test Set {@code .csv} file.
 *
 * <p>
 * Some tests take a large combination of inputs and have a corresponding number
 * of expected results. Instead of providing all these in code, it can be easier
 * to build an input file containing the test criteria.
 * </p>
 *
 * <p>
 * This method provides a means for a test to have its criteria defined in a
 * {@code .csv} file. It reads a given file, and provides a {@link Stream} of
 * {@link TestSetLine} objects each representing a single line in the file,
 * which can be used as input to a {@link ParameterizedTest}.
 * </p>
 *
 * <p>
 * Note that this functionality knows nothing about the structure of any given
 * {@code .csv} file - it is up to the test to know which columns it needs to
 * read for its own purposes.
 * </p>
 *
 * @see BaseTest#getTestSet(String)
 *
 * @author Steve Jones
 *
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
   * @return The field contents
   */
  public String getStringField(int fieldNumber) {
    return fields[fieldNumber];
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
   * See if a field is empty
   *
   * @param fieldNumber
   *          The zero-based field number
   * @return {@code true} if the field is empty; {@code false} if not
   */
  public boolean isFieldEmpty(int fieldNumber) {
    return fields[fieldNumber].trim().length() == 0;
  }
}
