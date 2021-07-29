package uk.ac.exeter.QuinCe.data.Dataset;

@SuppressWarnings("serial")
public class InvalidSensorValueException extends Exception {

  private SensorValue sensorValue;

  public InvalidSensorValueException(String message, SensorValue sensorValue) {
    super(message);
    this.sensorValue = sensorValue;
  }

  public SensorValue getSensorValue() {
    return sensorValue;
  }
}
