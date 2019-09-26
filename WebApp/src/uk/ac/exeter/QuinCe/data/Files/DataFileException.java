package uk.ac.exeter.QuinCe.data.Files;

/**
 * Exception for errors in data files
 * 
 * @author Steve Jones
 *
 */
public class DataFileException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -4759860456383818380L;

  /**
   * Dummy file ID for a file that isn't in the database yet
   */
  public static final long NO_FILE_ID = -999L;

  /**
   * Line number value indicating that an error did not occur on any specific
   * line
   */
  public static final int NO_LINE_NUMBER = -999;

  /**
   * The line number where the error was found
   */
  private long fileId;

  /**
   * The line number where the error was found
   */
  private int lineNumber;

  /**
   * Constructor for a simple error message
   * 
   * @param message
   *          The error message
   */
  public DataFileException(long fileId, int lineNumber, String message) {
    super(message);
    this.fileId = fileId;
    this.lineNumber = lineNumber;
  }

  /**
   * Constructor for an error with an underlying cause
   * 
   * @param cause
   *          The cause
   */
  public DataFileException(long fileId, int lineNumber, Throwable cause) {
    super(cause);
    this.fileId = fileId;
    this.lineNumber = lineNumber;
  }

  /**
   * Constructor for an error with an underlying cause
   * 
   * @param message
   *          The error message
   * @param cause
   *          The cause
   */
  public DataFileException(long fileId, int lineNumber, String message,
    Throwable cause) {
    super(message, cause);
    this.fileId = fileId;
    this.lineNumber = lineNumber;
  }

  @Override
  public String getMessage() {
    if (lineNumber == NO_LINE_NUMBER) {
      return "File ID " + fileId + ": " + super.getMessage();
    } else {
      return "File ID " + fileId + ", Line " + lineNumber + ": "
        + super.getMessage();
    }
  }
}
