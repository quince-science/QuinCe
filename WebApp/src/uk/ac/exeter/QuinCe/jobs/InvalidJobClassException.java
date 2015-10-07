package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate that the specified job class
 * is not of the correct type. Either it doesn't extend the
 * abstract Job class, or the required constructor couldn't be located.
 * 
 * @author Steve Jones
 *
 */
public class InvalidJobClassException extends Exception {

	private static final long serialVersionUID = 8855435038251108392L;

	public InvalidJobClassException() {
		super("The specified job class is not an instance of the Job class");
	}
	
}
