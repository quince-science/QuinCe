package junit.uk.ac.exeter.QuinCe.TestBase;

public class TestLineException extends Exception {

  /**
   * Serial version UID
   */
  private static final long serialVersionUID = -6868259576713187271L;

  /**
   * The line number where the error occured in the test set
   */
  private int lineNumber;

  public TestLineException(TestSetLine line, Throwable cause) {
    super("", cause);
    lineNumber = line.getLineNumber();
  }

  @Override
  public String getMessage() {
    return "Test set line " + lineNumber + ": " + getCause().getMessage();
  }

}
