package uk.ac.exeter.QuinCe.utils;

public class StringFormatException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2409873148560162294L;

	public StringFormatException(String message, String value) {
		super(message + ": '" + value + "'");
	}
	
}
