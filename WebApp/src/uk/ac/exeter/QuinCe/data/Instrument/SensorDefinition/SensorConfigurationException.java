package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception class for errors when handling an instrument's sensor configuration
 * @author Steve Jones
 *
 */
public class SensorConfigurationException extends InstrumentException {

  /**
   *  The Serial Version UID
   */
  private static final long serialVersionUID = -7348012628281277079L;

  /**
   * Simple constructor with an error message
   * @param message The error message
   */
  public SensorConfigurationException(String message) {
    super(message);
  }

  /**
   * Simple constructor with an error message
   * @param message The error message
   */
  public SensorConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor for an error on a given line of the configuration file
   * @param lineNumber The line number on which the error was found
   * @param message The error message
   */
  public SensorConfigurationException(int lineNumber, String message) {
    super("Error in sensor configuration, line " + lineNumber + ": " + message);
  }
}
