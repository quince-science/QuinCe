package uk.ac.exeter.QuinCe.utils;

public class ParameterException extends Exception {

	private static final long serialVersionUID = 3826060193745981107L;

	public ParameterException(String parameterName, String reason) {
		super(parameterName + ' ' + reason);
	}
	
}
