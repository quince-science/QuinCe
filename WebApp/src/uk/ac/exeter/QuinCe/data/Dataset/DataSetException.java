package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Exception for errors encountered while handling data sets
 * @author Steve Jones
 *
 */
public class DataSetException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 2885877141772725286L;

	/**
	 * Constructor for a simple error message
	 * @param message The error message
	 */
	public DataSetException(String message) {
		super(message);
	}

	/**
	 * Constructor for wrapping an error
	 * @param cause The error
	 */
	public DataSetException(Throwable cause) {
		super("Error while processing data set", cause);
	}

	/**
	 * Constructor for wrapping an error with a message
	 * @param message The error message
	 * @param cause The underlying cause
	 */
	public DataSetException(String message, Throwable cause) {
		super(message, cause);
	}
}
