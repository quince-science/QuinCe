package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception for missing date/time values.
 *
 * <p>
 * Either empty, completely missing (i.e. the line is too short) or 'NaN'.
 * </p>
 *
 */
@SuppressWarnings("serial")
public class MissingDateTimeException extends Exception {
  public MissingDateTimeException() {
    super("No date/time value");
  }
}
