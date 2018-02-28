package uk.ac.exeter.QuinCe.utils;

/**
 * Exception for errors raised while processing Strings
 * @author Steve Jones
 * @see StringUtils
 */
public class StringFormatException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -2409873148560162294L;

  /**
   * Basic constructor
   * @param message The error message
   * @param value The invalid value
   */
  public StringFormatException(String message, String value) {
    super(message + ": '" + value + "'");
  }

}
