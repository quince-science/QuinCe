package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception thrown if the specified size for the job pool
 * is invalid (i.e. not a positive number).
 * @author Steve Jones
 *
 */
public class InvalidThreadCountException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 5225485603801625923L;

	/**
	 * Simple constructor.
	 */
	public InvalidThreadCountException() {
		super("The number of threads must be positive");
	}
	
}
