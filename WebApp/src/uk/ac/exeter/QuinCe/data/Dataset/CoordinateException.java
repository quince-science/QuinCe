package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Exception class for issues with {@link Coordinate} objects.
 */
@SuppressWarnings("serial")
public class CoordinateException extends Exception {

  public CoordinateException(String message) {
    super(message);
  }
}
