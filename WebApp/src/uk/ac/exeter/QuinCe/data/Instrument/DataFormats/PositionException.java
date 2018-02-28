package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception class for issues with position values
 * @author Steve Jones
 *
 */
public class PositionException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -5002071128888697454L;

  /**
   * Simple error message
   * @param message The error message
   */
  public PositionException(String message) {
    super(message);
  }
}
