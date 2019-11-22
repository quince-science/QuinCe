package junit.uk.ac.exeter.QuinCe.TestBase;

/**
 * Exception class to wrap errors encountered while setting up test sets
 * 
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class TestSetException extends Exception {

  public TestSetException(String testSet, Throwable cause) {
    super("Error in test set " + testSet, cause);
  }

}
