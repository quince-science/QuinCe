package uk.ac.exeter.QuinCe.utils;

/**
 * Exception thrown when a date/time string cannot be parsed
 * @author Steve Jones
 *
 */
public class DateTimeParseException extends Exception {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 4894503384132198690L;

	/**
	 * Simple constructor
	 * @param message The error message
	 */
	public DateTimeParseException(String message) {
		super(message);
	}

}
