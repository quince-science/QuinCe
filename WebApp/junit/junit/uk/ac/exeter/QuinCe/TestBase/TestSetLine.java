package junit.uk.ac.exeter.QuinCe.TestBase;

/**
 * Utility class to represent a line from a test set CSV file.
 * It is assumed that the calling methods know the structure of the line.
 * @author Steve Jones
 *
 */
public class TestSetLine {

  /**
   * The line number
   */
  private int lineNumber;

  /**
   * The line split into fields
   */
  private String[] fields;

  protected TestSetLine(int lineNumber, String[] fields) {
    this.lineNumber = lineNumber;
    this.fields = fields;
  }

  /**
   * Get the line number
   * @return The line number
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Get a field value as a String
   * @param fieldNumber The zero-based field number
   * @return The field value
   */
  public String getStringField(int fieldNumber) {
    return fields[fieldNumber];
  }

  /**
   * Get a field value as a boolean
   * @param fieldNumber The zero-based field number
   * @return The field value
   */
  public boolean getBooleanField(int fieldNumber) {
    return Boolean.parseBoolean(fields[fieldNumber]);
  }

  /**
   * See if a field is empty
   * @param fieldNumber The zero-based field number
   * @return {@code true} if the field is empty; {@code false} if not
   */
  public boolean isFieldEmpty(int fieldNumber) {
    return fields[fieldNumber].trim().length() == 0;
  }
}
