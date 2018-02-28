package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Exception thrown when an invalid file separator is specified
 * @author Steve Jones
 *
 */
public class InvalidSeparatorException extends Exception {

  /**
   * The Serial Version UID
   */
  private static final long serialVersionUID = -6254985690962023936L;

  /**
   * Constructor for a specified invalid separator
   * @param separator The invalid separator
   */
  public InvalidSeparatorException(String separator) {
    super("Invalid separator '" + separator + "': Must be one of tab, comma, semi-colon or space");
  }
}
