package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception for uncaught errors that occur in running jobs
 * @author Steve Jones
 *
 */
public class JobFailedException extends Exception {
	
	/**
	 * The Serial Version UID
	 */
	private static final long serialVersionUID = 4456461345595123049L;

	/**
	 * Basic constructor
	 * @param id The job ID
	 * @param cause The error
	 */
	public JobFailedException(long id, Throwable cause) {
		super("Job ID " + id + " failed", cause);
	}
	
	/**
	 * Constructor with an error message
	 * @param id The job ID
	 * @param message An error message
	 * @param cause The error
	 */
	public JobFailedException(long id, String message, Throwable cause) {
		super("Job ID " + id + " failed (" + message + ')', cause);
	}
	
	/**
	 * Constructor for an error that occurred at a specific point in a job.
	 * This is usually used for errors while processing files, to indicate the
	 * line that was processed when the error occurred.
	 * @param id The job ID
	 * @param place The location in the job where the error occurred
	 * @param cause The error
	 */
	public JobFailedException(long id, int place, Throwable cause) {
		super("Job ID " + id + " failed at position " + place, cause);
	}
	
	/**
	 * Constructor for an error condition that occurs without
	 * an underlying cause
	 * @param id The job ID
	 * @param message The error message
	 */
	public JobFailedException(long id, String message) {
		super("Job ID " + id + " failed: " + message);
	}

	/**
	 * Constructor for an error that occurs in a specific place, but
	 * without an underlying cause.
	 * This is usually used for errors while processing files, to indicate the
	 * line that was processed when the error occurred.
	 * @param id The job ID
	 * @param place The location in the job where the error occurred
	 * @param message The error message
	 */
	public JobFailedException(long id, int place, String message) {
		super("Job ID " + id + " failed at position " + place + ": " + message);
	}
}
