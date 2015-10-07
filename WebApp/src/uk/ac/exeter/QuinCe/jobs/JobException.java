package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception to indicate a general problem with a job.
 * Issues that lead to this exception might include failures
 * while instantiating a job object.
 * 
 * This MUST NOT be used to throw exceptions from a job's normal
 * running. Any such errors must be handled by the jobs themselves.
 * 
 * @author Steve Jones
 *
 */
public class JobException extends Exception {

	private static final long serialVersionUID = -3978444072900292582L;

	public JobException(String message, Throwable e) {
		super(message, e);
	}
}
