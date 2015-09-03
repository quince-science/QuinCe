package uk.ac.exeter.QuinCe.utils;

public class MissingDataException extends Exception {

	private static final long serialVersionUID = -6528362731464449458L;

	/**
	 * Basic constructor
	 * @param message The error message
	 */
	public MissingDataException(String varName) {
		super(varName + " parameter is null");
	}
}
