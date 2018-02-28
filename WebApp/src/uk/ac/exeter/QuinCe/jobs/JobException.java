package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate a general problem with a job.
 * Issues that lead to this exception might include failures
 * while instantiating a job object.
 *
 * This MUST NOT be used to throw exceptions from a job's normal
 * running. Any such errors must be handled by the jobs themselves.
 *
 * @author Steve Jones
 *
 */
public class JobException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 8771569630286515893L;

	/**
	 * Constructor with a simple error message
	 * @param message The error message
	 */
	public JobException(String message) {
		super(message);
	}

	/**
	 * Constructor with an error message and an underlying cause
	 * @param message The error message
	 * @param cause The underlying cause
	 */
	public JobException(String message, Throwable cause) {
		super(message, cause);
	}
}
