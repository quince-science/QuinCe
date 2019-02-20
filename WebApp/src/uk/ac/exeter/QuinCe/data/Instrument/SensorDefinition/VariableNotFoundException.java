package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception for variables that can't be found
 * @author Steve Jones
 *
 */
public class VariableNotFoundException extends InstrumentException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 7707562190864340444L;

  /**
   * Variable name not found
   * @param name The variable name
   */
  public VariableNotFoundException(String name) {
    super("The variable with name '" + name + "' does not exist");
  }

  /**
   * Variable ID not found
   * @param variableId The variable ID
   */
  public VariableNotFoundException(long variableId) {
    super("The variable with ID " + variableId + " does not exist");
  }

}
