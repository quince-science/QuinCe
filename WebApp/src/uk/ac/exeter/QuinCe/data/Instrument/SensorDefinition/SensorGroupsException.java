package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

/**
 * Exception class for errors encountered while processing {@link SensorGroups}.
 */
@SuppressWarnings("serial")
public class SensorGroupsException extends Exception {

  public SensorGroupsException(String message) {
    super(message);
  }

  public SensorGroupsException(String message, Throwable cause) {
    super(message, cause);
  }
}
