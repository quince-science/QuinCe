package uk.ac.exeter.QuinCe.jobs;

public class JobFailedException extends Exception {
	
	private static final long serialVersionUID = 4456461345595123049L;

	public JobFailedException(long id, Throwable cause) {
		super("Job ID " + id + " failed", cause);
	}
	
	public JobFailedException(long id, String message, Throwable cause) {
		super("Job ID " + id + " failed (" + message + ')', cause);
	}
	
	public JobFailedException(long id, int place, Throwable cause) {
		super("Job ID " + id + " failed at position " + place, cause);
	}
	
	public JobFailedException(long id, String message) {
		super("Job ID " + id + " failed: " + message);
	}

	public JobFailedException(long id, int place, String message) {
		super("Job ID " + id + " failed at position " + place + ": " + message);
	}
}
