package uk.ac.exeter.QuinCe.jobs;

public class InvalidThreadCountException extends Exception {

	private static final long serialVersionUID = 5225485603801625923L;

	public InvalidThreadCountException() {
		super("The number of threads must be positive");
	}
	
}
