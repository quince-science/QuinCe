package uk.ac.exeter.QuinCe.jobs;

public class JobThreadPoolNotInitialisedException extends Exception {

	private static final long serialVersionUID = -2087913958516852214L;

	public JobThreadPoolNotInitialisedException() {
		super("The job thread pool has not been initialised");
	}
}
