package uk.ac.exeter.QCRoutines.data;

public class DataRecordException extends Exception {

	private static final long serialVersionUID = 794200486951764768L;

	public DataRecordException(String message) {
		super(message);
	}
	
	public DataRecordException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
