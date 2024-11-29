package uk.ac.exeter.QuinCe.web;

/**
 * Exception class for miscellaneous errors in beans.
 */
@SuppressWarnings("serial")
public class BeanException extends Exception {

  /**
   * Constructor for a basic error with just a message.
   *
   * @param message
   *          The error message.
   */
  public BeanException(String message) {
    super(message);
  }

  /**
   * Constructor for an error with a message and an underlying cause.
   *
   * @param message
   *          The error message.
   * @param cause
   *          The underlying cause.
   */
  public BeanException(String message, Throwable cause) {
    super(message, cause);
  }
}
