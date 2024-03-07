package uk.ac.exeter.QuinCe.data.Dataset.QC;

/**
 * Exception for errors raised within QC routines
 */
@SuppressWarnings("serial")
public class RoutineException extends Exception {

  /**
   * Simple constructor for a basic error message
   *
   * @param message
   *          The error message
   */
  public RoutineException(String message) {
    super(message);
  }

  /**
   * Simple wrapper for another error
   *
   * @param cause
   *          The original error
   */
  public RoutineException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for an error with an underlying cause
   *
   * @param message
   *          The error message
   * @param cause
   *          The underlying cause
   */
  public RoutineException(String message, Throwable cause) {
    super(message, cause);
  }
}
