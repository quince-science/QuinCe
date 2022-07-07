package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception class for issues with position values
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class PositionException extends Exception {

  /**
   * Simple error message
   *
   * @param message
   *          The error message
   */
  public PositionException(String message) {
    super(message);
  }
}
