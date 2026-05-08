package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception class for errors encountered while dealing with sensor assignments
 */
@SuppressWarnings("serial")
public class SensorAssignmentException extends InstrumentException {

  /**
   * Constructor for a simple error message
   *
   * @param message
   *          The error message
   */
  public SensorAssignmentException(String message) {
    super(message);
  }

  public SensorAssignmentException(String message, Throwable cause) {
    super(message, cause);
  }

}
