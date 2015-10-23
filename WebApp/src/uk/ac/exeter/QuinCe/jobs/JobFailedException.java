package uk.ac.exeter.QuinCe.jobs;

public class JobFailedException extends Exception {
	
	private static final long serialVersionUID = -7445867674269871834L;

	public JobFailedException(long id, Throwable cause) {
		super("Job ID " + id + " failed", cause);
	}

}
