package uk.ac.exeter.QuinCe.data;

public class RawDataFileException extends Exception {
	
	private static final long serialVersionUID = -4488468987912051593L;

	public RawDataFileException(int line, String message) {
		super("Line " + line + ": " + message);
	}
	
	public RawDataFileException(int line, String message, Throwable cause) {
		super("Line " + line + ": " + message, cause);
	}

}
