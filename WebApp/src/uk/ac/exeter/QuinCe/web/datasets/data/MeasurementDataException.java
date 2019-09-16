package uk.ac.exeter.QuinCe.web.datasets.data;

/**
 * Generic exception for issues arising in the DatasetMeasurementData classes
 *
 * @author Steve Jones
 *
 */
public class MeasurementDataException extends Exception {

  public MeasurementDataException(String message) {
    super(message);
  }

  public MeasurementDataException(String message, Throwable cause) {
    super(message, cause);
  }

}
