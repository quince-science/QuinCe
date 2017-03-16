package uk.ac.exeter.QuinCe.utils;

public class BackgroundTaskException extends Exception {
	
	private static final long serialVersionUID = -1588777340186860764L;

	public BackgroundTaskException(Throwable cause) {
		super("Error in background task", cause);
	}

}
