package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Simple exception for errors in file sets
 * @author Steve Jones
 *
 */
public class FileSetException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -6555293481544429846L;

	/**
	 * Basic constructor
	 * @param message The error message
	 */
	public FileSetException(String message) {
		super(message);
	}
}
