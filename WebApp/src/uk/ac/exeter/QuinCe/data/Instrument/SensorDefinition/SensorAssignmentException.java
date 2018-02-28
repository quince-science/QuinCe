package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception class for errors encountered while dealing with sensor assignments
 * @author Steve Jones
 *
 */
public class SensorAssignmentException extends InstrumentException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -8786897548326873126L;

  /**
   * Constructor for a simple error message
   * @param message The error message
   */
  public SensorAssignmentException(String message) {
    super(message);
  }

}
