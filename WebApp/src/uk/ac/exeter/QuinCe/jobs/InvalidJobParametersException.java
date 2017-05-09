package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate that the parameters passed to
 * a specific job did not meet that job's requirements.
 * 
 * @author Steve Jones
 *
 */
public class InvalidJobParametersException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 3595337728820380558L;

	/**
	 * Constructor with a simple error message
	 * @param message The error message
	 */
	public InvalidJobParametersException(String message) {
		super(message);
	}
	
	/**
	 * Constructor with an error message and an underlying cause.
	 * @param message The error message
	 * @param cause The underlying cause of the error
	 */
	public InvalidJobParametersException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
