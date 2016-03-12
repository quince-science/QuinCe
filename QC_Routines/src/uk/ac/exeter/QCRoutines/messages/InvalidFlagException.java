package uk.ac.exeter.QCRoutines.messages;

public class InvalidFlagException extends Exception {

	private static final long serialVersionUID = -3142128667942833563L;

	public InvalidFlagException(int flagValue) {
		super("Invalid flag value " + flagValue);
	}
}
