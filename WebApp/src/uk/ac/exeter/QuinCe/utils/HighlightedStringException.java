package uk.ac.exeter.QuinCe.utils;

/**
 * Exception for errors in highlighted strings
 * @author Steve Jones
 *
 */
public class HighlightedStringException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -4692138554842766036L;

  /**
   * Basic constructor
   * @param message The error message
   */
  public HighlightedStringException(String message) {
    super(message);
  }
}
