package uk.ac.exeter.QuinCe.data.Dataset;

@SuppressWarnings("serial")
/**
 * Exception for errors encountered while handling data sets
 */
public class DataSetException extends Exception {


  /**
   * Constructor for a simple error message
   *
   * @param message
   *          The error message
   */
  public DataSetException(String message) {
    super(message);
  }

  /**
   * Constructor for wrapping an error
   *
   * @param cause
   *          The error
   */
  public DataSetException(Throwable cause) {
    super("Error while processing data set", cause);
  }

  /**
   * Constructor for wrapping an error with a message
   *
   * @param message
   *          The error message
   * @param cause
   *          The underlying cause
   */
  public DataSetException(String message, Throwable cause) {
    super(message, cause);
  }
}
