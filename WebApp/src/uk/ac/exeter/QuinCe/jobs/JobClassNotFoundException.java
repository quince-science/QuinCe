package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception indicating that a specified job class cannot be found.
 * @author Steve Jones
 *
 */
public class JobClassNotFoundException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 4905635867554343158L;

	/**
	 * Constructor
	 * @param className The name of the class
	 */
	public JobClassNotFoundException(String className) {
		super("The specified job class '" + className + "' could not be found");
	}

}
