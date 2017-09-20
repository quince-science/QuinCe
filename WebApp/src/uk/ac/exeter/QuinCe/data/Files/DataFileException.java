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
	private static final long serialVersionUID = -8978866766949060074L;

	/**
	 * Constructor for a simple error message
	 * @param message The error message
	 */
	public DataFileException(String message) {
		super(message);
	}
}
