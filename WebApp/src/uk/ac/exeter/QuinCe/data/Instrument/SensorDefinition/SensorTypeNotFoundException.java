package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception for sensor types that can't be found
 */
@SuppressWarnings("serial")
public class SensorTypeNotFoundException extends InstrumentException {

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

  /**
   * Special case when looking up something based on a {@link SensorType}
   * object.
   *
   * @param sensorType
   *          The SensorType we were looking for.
   */
  public SensorTypeNotFoundException(SensorType sensorType) {
    super("The SensorType object for " + sensorType.getId() + ":"
      + sensorType.getShortName() + " is not valid");
  }
}
