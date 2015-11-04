package uk.ac.exeter.QuinCe.jobs;

public class JobFailedException extends Exception {
	
	private static final long serialVersionUID = 216843093020928400L;

	public JobFailedException(long id, Throwable cause) {
		super("Job ID " + id + " failed", cause);
	}
	
	public JobFailedException(long id, String message) {
		super("Job ID " + id + " failed: " + message);
	}
}
