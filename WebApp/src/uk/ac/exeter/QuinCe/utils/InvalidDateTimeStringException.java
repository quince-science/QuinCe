package uk.ac.exeter.QuinCe.utils;

/**
 * Exception for date/time strings that can't be parsed.
 * @author Steve Jones
 *
 */
public class InvalidDateTimeStringException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -7995629389317770488L;

	/**
	 * Generates an exception for the specified date-time string.
	 * @param dateTimeString The invalid date-time string
	 */
	public InvalidDateTimeStringException(String dateTimeString) {
		super("The date-time string '" + dateTimeString + "' is invalid");
	}
}
