package uk.ac.exeter.QuinCe.web.system;

/**
 * Exception for errors raised while accessing resources required for the web application
 * @author Steve Jones
 *
 */
public class ResourceException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -4508422717634373177L;

  /**
   * Constructor for an error with an underlying cause
   * @param message The error message
   * @param cause The underlying cause
   */
  public ResourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
