package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

@SuppressWarnings("serial")
public class ValueCalculatorException extends Exception {

  public ValueCalculatorException(SensorType sensorType, Throwable cause) {
    super("Error in value calculator for SensorType " + sensorType.toString(),
      cause);
  }

  public ValueCalculatorException(String sensorType, Throwable cause) {
    super("Error in value calculator for SensorType " + sensorType, cause);
  }

  public ValueCalculatorException(SensorType sensorType, String message) {
    super("Error in value calculator for SensorType " + sensorType.toString()
      + ": " + message);
  }

  public ValueCalculatorException(SensorType sensorType, String message,
    Throwable cause) {
    super("Error in value calculator for SensorType " + sensorType.toString()
      + ": " + message, cause);
  }

  public ValueCalculatorException(String message) {
    super(message);
  }
}
