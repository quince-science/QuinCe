package uk.ac.exeter.QuinCe.data.Dataset;

@SuppressWarnings("serial")
public class MeasurementValueCalculatorException extends Exception {

  public MeasurementValueCalculatorException(String message) {
    super(message);
  }

  public MeasurementValueCalculatorException(String message, Throwable cause) {
    super(message, cause);
  }

  public MeasurementValueCalculatorException(Throwable cause) {
    super("Error calculating measurement value", cause);
  }
}
