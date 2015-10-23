package uk.ac.exeter.QuinCe.jobs;

public class UnrecognisedStatusException extends Exception {

	private static final long serialVersionUID = -5124308472992974683L;

	public UnrecognisedStatusException(String status) {
		super("The status '" + status + "' is not recognised");
	}
	
}
