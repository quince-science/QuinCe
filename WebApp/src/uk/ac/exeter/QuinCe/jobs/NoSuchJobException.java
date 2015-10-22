package uk.ac.exeter.QuinCe.jobs;

public class NoSuchJobException extends Exception {

	private static final long serialVersionUID = -3642413780303429060L;

	public NoSuchJobException(long jobID) {
		super("The specified job (ID " + jobID + ") does not exist");
	}
	
}
