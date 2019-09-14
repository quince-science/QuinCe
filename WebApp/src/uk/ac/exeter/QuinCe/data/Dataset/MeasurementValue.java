package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Mini class for holding a measurement value link, which is simply a
 * measurement ID and a sensor value ID
 * 
 * @author Steve Jones
 *
 */
public class MeasurementValue {

  /**
   * The measurement ID
   */
  private final long measurementId;

  /**
   * The sensor value ID
   */
  private final long sensorValueId;

  /**
   * Constructor for full objects
   * 
   * @param measurement
   *          The measurement
   * @param sensorValue
   *          The sensor value
   */
  public MeasurementValue(Measurement measurement, SensorValue sensorValue) {
    this.measurementId = measurement.getId();
    this.sensorValueId = sensorValue.getId();
  }

  /**
   * Constructor for IDs
   * 
   * @param measurementId
   *          The measurement ID
   * @param sensorValueId
   *          The sensor value ID
   */
  public MeasurementValue(long measurementId, long sensorValueId) {
    this.measurementId = measurementId;
    this.sensorValueId = sensorValueId;
  }

  /**
   * Get the measurement ID
   * 
   * @return The measurement ID
   */
  public long getMeasurementId() {
    return measurementId;
  }

  /**
   * Get the sensor value ID
   * 
   * @return The sensor value ID
   */
  public long getSensorValueId() {
    return sensorValueId;
  }
}
