package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

/**
 * Exception for errors raised within QC routines
 * @author Steve Jones
 *
 */
public class RoutineException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -9197093855452751110L;

	/**
	 * Simple constructor for a basic error message
	 * @param message The error message
	 */
	public RoutineException(String message) {
		super(message);
	}
	
	/**
	 * Constructor for an error with an underlying cause
	 * @param message The error message
	 * @param cause The underlying cause
	 */
	public RoutineException(String message, Throwable cause) {
		super(message, cause);
	}
}
