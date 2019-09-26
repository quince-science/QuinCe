package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception for sensor types that can't be found
 * 
 * @author Steve Jones
 *
 */
public class SensorTypeNotFoundException extends InstrumentException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 7707562190864340444L;

  /**
   * Sensor name not found
   * 
   * @param sensorName
   *          The sensor name
   */
  public SensorTypeNotFoundException(String sensorName) {
    super("The sensor type with name '" + sensorName + "' does not exist");
  }

  /**
   * Sensor ID not found
   * 
   * @param sensorId
   *          The sensor ID
   */
  public SensorTypeNotFoundException(long sensorId) {
    super("The sensor type with ID " + sensorId + " does not exist");
  }

}
