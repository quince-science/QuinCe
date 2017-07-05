package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception raised when a specified job does not exist in the system
 * @author Steve Jones
 *
 */
public class NoSuchJobException extends Exception {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -3642413780303429060L;

	/**
	 * Constructor
	 * @param jobID The ID of the job that was requested
	 */
	public NoSuchJobException(long jobID) {
		super("The specified job (ID " + jobID + ") does not exist");
	}
	
}
