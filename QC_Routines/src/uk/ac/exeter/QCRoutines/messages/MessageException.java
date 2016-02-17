package uk.ac.exeter.QCRoutines.messages;

public class MessageException extends Exception {

	private static final long serialVersionUID = -1501643116718246827L;

	public MessageException(String message) {
		super(message);
	}
	
	public MessageException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
