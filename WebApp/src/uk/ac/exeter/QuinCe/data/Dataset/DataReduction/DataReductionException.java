package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

/**
 * Exception for errors encountered during data reduction.
 */
@SuppressWarnings("serial")
public class DataReductionException extends Exception {

  /**
   * Exception with a simple message.
   *
   * @param message
   *          The error message.
   */
  public DataReductionException(String message) {
    super(message);
  }

  /**
   * Simple wrapper exception.
   *
   * @param cause
   *          The underlying error.
   */
  public DataReductionException(Throwable cause) {
    super(cause);
  }

  /**
   * Wrapper for an exception with an additional explanatory message.
   *
   * @param message
   *          The explanatory message.
   * @param cause
   *          The underlying error.
   */
  public DataReductionException(String message, Throwable cause) {
    super(message, cause);
  }
}
