package uk.ac.exeter.QuinCe.utils;

/**
 * A simple Exception class to wrap errors encountered in background tasks
 * @author Steve Jones
 * @see BackgroundTask
 */
public class BackgroundTaskException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -1588777340186860764L;

	/**
	 * Basic constructor
	 * @param cause The underlying error
	 */
	public BackgroundTaskException(Throwable cause) {
		super("Error in background task", cause);
	}

}
