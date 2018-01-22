package uk.ac.exeter.QuinCe.data.Files;

/**
 * Exception for errors in data files
 * @author Steve Jones
 *
 */
public class DataFileException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 1726463915415567883L;

	/**
	 * Constructor for a simple error message
	 * @param message The error message
	 */
	public DataFileException(String message) {
		super(message);
	}
	
	/**
	 * Constructor for an error with an underlying cause
	 * @param cause The cause
	 */
	public DataFileException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Constructor for an error with an underlying cause
	 * @param message The error message
	 * @param cause The cause
	 */
	public DataFileException(String message, Throwable cause) {
		super(message, cause);
	}
}
