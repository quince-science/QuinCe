package uk.ac.exeter.QuinCe.utils;

import java.util.Collection;

public class MissingParam {

	public static void checkMissing(Object parameter, String parameterName) throws MissingParamException {
		checkMissing(parameter, parameterName, false);
	}

	public static void checkMissing(char[] parameter, String parameterName) throws MissingParamException {
		checkMissing(parameter, parameterName, false);
	}

	public static void checkMissing(Object parameter, String parameterName, boolean canBeEmpty) throws MissingParamException {
		boolean isMissing = false;
		
		if (null == parameter) {
			isMissing = true;
		} else {
			if (!canBeEmpty) {
				if (parameter instanceof String) {
					if (((String) parameter).length() == 0) {
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
	
	public static void checkMissing(char[] parameter, String parameterName, boolean canBeEmpty) throws MissingParamException {
		boolean isMissing = false;
		
		if (null == parameter) {
			isMissing = true;
		} else {
			if (canBeEmpty && parameter.length == 0) {
					isMissing = true;
			}
		}
		
		if (isMissing) {
			throw new MissingParamException(parameterName);
		}
	}
	
	public static void checkPositive(int parameter, String parameterName) throws MissingParamException {
		if (parameter <= 0) {
			throw new MissingParamException(parameterName);
		}
	}

	public static void checkPositive(long parameter, String parameterName) throws MissingParamException {
		if (parameter <= 0) {
			throw new MissingParamException(parameterName);
		}
	}
	
	public static void checkZeroPositive(int parameter, String parameterName) throws MissingParamException {
		if (parameter < 0) {
			throw new MissingParamException(parameterName);
		}
	}

	public static void checkZeroPositive(double parameter, String parameterName) throws MissingParamException {
		if (parameter < 0) {
			throw new MissingParamException(parameterName);
		}
	}
	
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
