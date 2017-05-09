package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate that the specified job class
 * is not of the correct type. Either it doesn't extend the
 * abstract {@link Job} class, or the required constructor couldn't be located.
 * 
 * @author Steve Jones
 *
 */
public class InvalidJobClassTypeException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 8855435038251108392L;

	/**
	 * Constructor
	 * @param className The name of the invalid class
	 */
	public InvalidJobClassTypeException(String className) {
		super("The specified job class '" + className + "' is not an instance of the Job class, or does not have the required constructor");
	}	
}
