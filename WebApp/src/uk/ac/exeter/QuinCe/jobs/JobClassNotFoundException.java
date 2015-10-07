package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception indicating that a specified job class cannot be found.
 * @author Steve Jones
 *
 */
public class JobClassNotFoundException extends Exception {

	private static final long serialVersionUID = 4905635867554343158L;

	public JobClassNotFoundException(String className) {
		super("The specified job class '" + className + "' could not be found");
	}
	
}
