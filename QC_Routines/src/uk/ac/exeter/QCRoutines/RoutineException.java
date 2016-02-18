package uk.ac.exeter.QCRoutines;

public class RoutineException extends Exception {

	private static final long serialVersionUID = -9197093855452751110L;

	public RoutineException(String message) {
		super(message);
	}
	
	public RoutineException(String message, Throwable cause) {
		super(message, cause);
	}
}
