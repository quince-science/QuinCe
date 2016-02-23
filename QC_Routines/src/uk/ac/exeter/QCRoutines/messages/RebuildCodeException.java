package uk.ac.exeter.QCRoutines.messages;

public class RebuildCodeException extends MessageException {

	private static final long serialVersionUID = -8843114125586814644L;

	public RebuildCodeException(String message) {
		super("Invalid message rebuild code: " + message);
	}
}
