package uk.ac.exeter.QuinCe.utils;

public class RecordNotFoundException extends Exception {

	private static final long serialVersionUID = -6955543918647293212L;

	public RecordNotFoundException(String message) {
		super(message);
	}
	
	public RecordNotFoundException(String message, String table, long id) {
		super(message + "(Table " + table + ", record " + id);
	}

}
