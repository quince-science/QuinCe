package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Exception for errors encountered while working with
 * {@link SensorValuesList}s. It's mostly used as a wrapper.
 */
@SuppressWarnings("serial")
public class SensorValuesListException extends Exception {

  /**
   * Constructs a new exception with the specified cause.
   *
   * @param cause
   *          The cause.
   */
  public SensorValuesListException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message
   *          The message.
   */
  public SensorValuesListException(String message) {
    super(message);
  }
}
