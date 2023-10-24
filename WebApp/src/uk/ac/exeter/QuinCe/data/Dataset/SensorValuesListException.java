package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Exception for errors encountered while working with {@link SensorValueList}s.
 * It's mostly used as a wrapper.
 */
@SuppressWarnings("serial")
public class SensorValuesListException extends Exception {

  public SensorValuesListException(Throwable cause) {
    super(cause);
  }

  public SensorValuesListException(String message) {
    super(message);
  }

}
