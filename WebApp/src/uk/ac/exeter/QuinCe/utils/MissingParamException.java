package uk.ac.exeter.QuinCe.utils;

public class MissingParamException extends ParameterException {

	private static final long serialVersionUID = -5143400042710795233L;

	/**
	 * Basic constructor
	 * @param message The error message
	 */
	public MissingParamException(String varName) {
		super(varName, "parameter is null");
	}
}
