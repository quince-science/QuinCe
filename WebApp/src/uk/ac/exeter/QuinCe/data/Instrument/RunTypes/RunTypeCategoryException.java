package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception for run type category errors
 * 
 * @author Steve Jones
 *
 */
public class RunTypeCategoryException extends InstrumentException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 4075420329813549093L;

  /**
   * Basic constructor
   * 
   * @param message
   *          The error message
   */
  public RunTypeCategoryException(String message) {
    super(message);
  }

  /**
   * Constructor for an error with an underlying cause
   * 
   * @param message
   *          The error message
   * @param cause
   *          The cause
   */
  public RunTypeCategoryException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor for an error on a given line of the configuration file
   * 
   * @param lineNumber
   *          The line number on which the error was found
   * @param message
   *          The error message
   */
  public RunTypeCategoryException(int lineNumber, String message) {
    super("Error in sensor configuration, line " + lineNumber + ": " + message);
  }
}
