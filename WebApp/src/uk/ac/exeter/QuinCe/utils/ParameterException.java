package uk.ac.exeter.QuinCe.utils;

/**
 * Exception for errors in method parameters
 * @author Steve Jones
 *
 */
public class ParameterException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 3826060193745981107L;

	/**
	 * Constructor
	 * @param parameterName The name of the parameter for which the exception is being raised
	 * @param reason The reason for the exception
	 */
	public ParameterException(String parameterName, String reason) {
		super(parameterName + ' ' + reason);
	}
	
}
