package uk.ac.exeter.QuinCe.data.Export;

/**
 * Exception class for errors encountered in data export mechanism
 * @author Steve Jones
 *
 */
public class ExportException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -3739928561949968607L;

	/**
	 * Simple constructor for errors occurring in the general export system.
	 * These errors are not linked to any specific export configuration.
	 * @param message The error message
	 */
	public ExportException(String message) {
		super(message);
	}

	/**
	 * Constructor for errors that occur in a specific export configuration.
	 * @param name The name of the export configuration
	 * @param message The error message
	 */
	public ExportException(String name, String message) {
		super("Error in exporter '" + name + "': " + message);
	}

	/**
	 * Constructor for errors that occur in a specific export configuration
	 * that have an underlying cause.
	 * @param name The name of the export configuration
	 * @param cause The root cause of the error
	 */
	public ExportException(String name, Throwable cause) {
		super("Error in exporter '" + name + "': " + cause.getMessage(), cause);
	}

}
