package uk.ac.exeter.QuinCe.jobs;

public class BadStatusException extends Exception {

	private static final long serialVersionUID = -5124308472992974683L;

	public BadStatusException(String status) {
		super("The status '" + status + "' is not recognised");
	}
	
}
