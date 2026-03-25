package uk.ac.exeter.QuinCe.data.Dataset.QC;

/**
 * Exception class for any errors encountered while dealing with QC flags or
 * flag schemes.
 * 
 * <p>
 * This is a {@link RunTimeException} because we anticipate that errors raised
 * from a {@link FlagScheme} will be coding errors that shouldn't occur, and
 * there's nothing that can be done if they're raised.
 * </p>
 */
@SuppressWarnings("serial")
public class FlagException extends RuntimeException {
  public FlagException(String message) {
    super(message);
  }
}
