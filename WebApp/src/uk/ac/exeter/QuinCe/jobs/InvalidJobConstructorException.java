package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate that the specified job class
 * does not require a constructor of the correct type.
 * 
 * @author Steve Jones
 * @see Job#Job(uk.ac.exeter.QuinCe.web.system.ResourceManager, java.util.Properties, long, java.util.Map)
 */
public class InvalidJobConstructorException extends Exception {

	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 8855435038251108392L;

	/**
	 * Constructor
	 * @param className The name of the invalid class
	 */
	public InvalidJobConstructorException(String className) {
		super("The specified job class '" + className + "' does not have a valid constructor");
	}	
}
