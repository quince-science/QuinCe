package junit.uk.ac.exeter.QuinCe.TestBase;

/**
 * An {@link Exception} that a test can throw if it encounters an error while
 * processing a {@link TestSetLine}.
 *
 * @see TestSetLine
 * @see BaseTest#getTestSet(String)
 *
 * @author Steve Jones
 */
@SuppressWarnings("serial")
public class TestLineException extends Exception {

  /**
   * The line number in the Test Set where the error occurred
   */
  private int lineNumber;

  /**
   * Constructor for a wrapped exception
   *
   * @param line
   *          The line where the error was encountered
   * @param cause
   *          The exception that was encountered
   */
  public TestLineException(TestSetLine line, Throwable cause) {
    super("", cause);
    lineNumber = line.getLineNumber();
  }

  /**
   * Constructor for a simple message
   *
   * @param line
   *          The line where the error was encountered
   * @param message
   *          The error message
   */
  public TestLineException(TestSetLine line, String message) {
    super(message);
    lineNumber = line.getLineNumber();
  }

  @Override
  public String getMessage() {
    String result = super.getMessage();
    if (result.length() == 0) {
      result = "Test set line " + lineNumber + ": " + getCause().getMessage();
    }

    return result;
  }
}
