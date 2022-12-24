package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception for errors in the Date/Time specification
 */
@SuppressWarnings("serial")
public class DateTimeSpecificationException extends Exception {

  /**
   * Basic constructor
   *
   * @param message
   *          The error message
   */
  public DateTimeSpecificationException(String message) {
    super(message);
  }
}
