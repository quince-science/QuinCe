package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Excpeption thrown if an invalid header type is specified
 * @author Steve Jones
 *
 */
public class InvalidHeaderTypeException extends FileDefinitionException {

  /**
   * The Serial Version UID
   */
  private static final long serialVersionUID = 1390046759996174265L;

  /**
   * Constructor with automatic message
   */
  public InvalidHeaderTypeException() {
    super("Header type must be either FileDefinition.HEADER_TYPE_LINE_COUNT or FileDefinition.HEADER_TYPE_STRING");
  }
}
