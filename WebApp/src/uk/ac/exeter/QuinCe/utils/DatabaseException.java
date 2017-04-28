package uk.ac.exeter.QuinCe.utils;

/**
 * An exception class for any database-level exceptions.
 * Most {@code DatabaseException}s will be wrappers around an
 * {@link java.sql.SQLException}.
 * 
 * @author Steve Jones
 *
 */
public class DatabaseException extends Exception {
	
	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 7975765490556805208L;

	/**
	 * Constructor for an exception without an underlying cause
	 * 
	 * @param message The error message
	 */
	public DatabaseException(String message) {
		super(message);
	}
	
	/**
	 * Constructor for an exception with an underlying cause
	 * 
	 * @param message The error message
	 * @param cause The root cause of the error
	 */
	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}
}
