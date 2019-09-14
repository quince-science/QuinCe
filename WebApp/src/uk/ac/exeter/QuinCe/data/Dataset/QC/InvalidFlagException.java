package uk.ac.exeter.QuinCe.data.Dataset.QC;

/**
 * Exception thrown when an invalid numeric flag value is encountered
 * 
 * @author Steve Jones
 *
 */
public class InvalidFlagException extends Exception {

  /**
   * The Serial Version UID
   */
  private static final long serialVersionUID = 1787207060024257945L;

  /**
   * Constructor
   * 
   * @param flagValue
   *          The invalid flag value
   */
  public InvalidFlagException(int flagValue) {
    super("Invalid flag value " + flagValue);
  }
}
