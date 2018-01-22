package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Exception for file definitions
 * @author Steve Jones
 *
 */
public class FileDefinitionException extends Exception {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 5133677951791016411L;

	/**
	 * Simple error
	 * @param message The error message
	 */
	public FileDefinitionException(String message) {
		super(message);
	}
}
