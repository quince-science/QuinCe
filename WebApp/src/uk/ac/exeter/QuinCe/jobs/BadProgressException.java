package uk.ac.exeter.QuinCe.jobs;

public class BadProgressException extends Exception {

	private static final long serialVersionUID = 8691544843626836324L;

	public BadProgressException() {
		super("The progress must be between 0 and 100");
	}
}
