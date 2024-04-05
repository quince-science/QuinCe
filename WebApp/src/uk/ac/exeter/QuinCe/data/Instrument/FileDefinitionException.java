package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Exception for file definitions
 */
@SuppressWarnings("serial")
public class FileDefinitionException extends Exception {

  /**
   * Simple error
   *
   * @param message
   *          The error message
   */
  public FileDefinitionException(String message) {
    super(message);
  }
}
