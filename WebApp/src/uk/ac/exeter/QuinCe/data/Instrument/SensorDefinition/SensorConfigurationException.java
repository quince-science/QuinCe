package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception class for errors when handling an instrument's sensor configuration
 * 
 * @author Steve Jones
 *
 */
public class SensorConfigurationException extends InstrumentException {

  /**
   * The Serial Version UID
   */
  private static final long serialVersionUID = -7348012628281277079L;

  /**
   * The database ID of the invalid sensor type
   */
  private long id = -1;

  /**
   * Simple constructor with an error message
   * 
   * @param message
   *          The error message
   */
  public SensorConfigurationException(String message) {
    super(message);
  }

  /**
   * Simple constructor with an error message
   * 
   * @param message
   *          The error message
   */
  public SensorConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception for a specific sensor type
   * 
   * @param id
   *          The sensor type's ID
   * @param message
   *          The error message
   */
  public SensorConfigurationException(long id, String message) {
    super(message);
    this.id = id;
  }

  @Override
  public String getMessage() {
    StringBuilder message = new StringBuilder();

    if (id != -1) {
      message.append("Sensor Type " + id + ": ");
    }

    message.append(super.getMessage());

    return message.toString();
  }
}
