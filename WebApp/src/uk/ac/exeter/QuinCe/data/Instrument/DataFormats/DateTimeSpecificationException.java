package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception for errors in the Date/Time specification
 * @author Steve Jones
 *
 */
public class DateTimeSpecificationException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 1112647207042191951L;

	/**
	 * Basic constructor
	 * @param message The error message
	 */
	public DateTimeSpecificationException(String message) {
		super(message);
	}
}
