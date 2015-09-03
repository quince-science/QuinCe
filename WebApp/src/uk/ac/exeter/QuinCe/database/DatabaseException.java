package uk.ac.exeter.QuinCe.database;

/**
 * A generic database-level exception to handle errors
 * that should not occur.
 * 
 * @author Steve Jones
 *
 */
public class DatabaseException extends Exception {
	
	private static final long serialVersionUID = 7975765490556805208L;

	/**
	 * Basic constructor
	 * 
	 * @param message The error message
	 */
	public DatabaseException(String message) {
		super(message);
	}
	
	/**
	 * Basic constructor
	 * 
	 * @param message The error message
	 * @param cause The root cause of the error
	 */
	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}
}
