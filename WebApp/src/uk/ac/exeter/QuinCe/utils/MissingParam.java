package uk.ac.exeter.QuinCe.utils;

import java.util.Collection;

/**
 * Utility methods for checking method parameters
 * @author Steve Jones
 *
 */
public class MissingParam {

	/**
	 * Ensure that a parameter value is not {@code null}. {@link String} and {@link Collection}
	 * objects must not be empty.
	 * @param parameter The parameter value to be checked
	 * @param parameterName The parameter name
	 * @throws MissingParamException If the parameter is {@code null} or empty.
	 */
	public static void checkMissing(Object parameter, String parameterName) throws MissingParamException {
		checkMissing(parameter, parameterName, false);
	}

	/**
	 * Check that a character array is not {@code null} or empty.
	 * @param parameter The character array
	 * @param parameterName The parameter name
	 * @throws MissingParamException If the array {@code null} or empty
	 */
	public static void checkMissing(char[] parameter, String parameterName) throws MissingParamException {
		checkMissing(parameter, parameterName, false);
	}

	/**
	 * Ensure that a parameter value is not {@code null}. {@link String} and {@link Collection} objects
	 * may be empty if {@code canBeEmpty} is set to {@code true}.
	 * @param parameter The parameter value to be checked
	 * @param parameterName The parameter name
	 * @param canBeEmpty Indicates whether Strings and Collections can be empty
	 * @throws MissingParamException If the parameter is {@code null} or empty (if {@code canBeEmpty} is {@code false})
	 */
	public static void checkMissing(Object parameter, String parameterName, boolean canBeEmpty) throws MissingParamException {
		boolean isMissing = false;
		
		if (null == parameter) {
			isMissing = true;
		} else {
			if (!canBeEmpty) {
				if (parameter instanceof String) {
					if (((String) parameter).trim().length() == 0) {
						isMissing = true;
					}
				} else if (parameter instanceof Collection) {
					if (((Collection<?>) parameter).size() == 0) {
						isMissing = true;
					}
				}
			}
		}

		if (isMissing) {
			throw new MissingParamException(parameterName);
		}
	}
	
	/**
	 * Check that a character array is not {@code null}. It can be empty if
	 * {@code canBeEmpty} is set to {@code true}.
	 * @param parameter The character array
	 * @param parameterName The parameter name
	 * @param canBeEmpty Indicates whether the array can be empty
	 * @throws MissingParamException If the array is {@code null} or empty (if {@code canBeEmpty} is {@code false})
	 */
	public static void checkMissing(char[] parameter, String parameterName, boolean canBeEmpty) throws MissingParamException {
		boolean isMissing = false;
		
		if (null == parameter) {
			isMissing = true;
		} else {
			if (!canBeEmpty && parameter.length == 0) {
					isMissing = true;
			}
		}
		
		if (isMissing) {
			throw new MissingParamException(parameterName);
		}
	}
	
	/**
	 * Check that an integer value is positive
	 * @param parameter The value
	 * @param parameterName The parameter name
	 * @throws MissingParamException If the value is not positive
	 */
	public static void checkPositive(int parameter, String parameterName) throws MissingParamException {
		if (parameter <= 0) {
			throw new MissingParamException(parameterName);
		}
	}

	/**
	 * Check that a long value is positive
	 * @param parameter The value
	 * @param parameterName The parameter name
	 * @throws MissingParamException If the value is not positive
	 */
	public static void checkPositive(long parameter, String parameterName) throws MissingParamException {
		if (parameter <= 0) {
			throw new MissingParamException(parameterName);
		}
	}
	
	/**
	 * Check that an integer value is zero or positive
	 * @param parameter The value
	 * @param parameterName The parameter name
	 * @throws MissingParamException If the value is not zero or positive
	 */
	public static void checkZeroPositive(int parameter, String parameterName) throws MissingParamException {
		if (parameter < 0) {
			throw new MissingParamException(parameterName);
		}
	}

	/**
	 * Check that a double value is zero or positive
	 * @param parameter The value
	 * @param parameterName The parameter name
	 * @throws MissingParamException If the value is not zero or positive
	 */
	public static void checkZeroPositive(double parameter, String parameterName) throws MissingParamException {
		if (parameter < 0) {
			throw new MissingParamException(parameterName);
		}
	}
	
	/**
	 * Check that a String value contains a comma-separated list of integers 
	 * @param list The String value
	 * @param parameterName The parameter name
	 * @throws ParameterException If the String format is invalid
	 */
	public static void checkListOfIntegers(String list, String parameterName) throws ParameterException {
		
		checkMissing(list, parameterName);

		boolean ok = true;

		try {
			String[] entries = list.split(",");
			for (String entry : entries) {
				Integer.parseInt(entry);
			}
		} catch (NumberFormatException e) {
			ok = false;
		}
		
		if (!ok) {
			throw new ParameterException(parameterName, "is not a list of integers");
		}
	}
}
