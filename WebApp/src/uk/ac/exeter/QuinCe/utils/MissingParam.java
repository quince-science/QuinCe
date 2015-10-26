package uk.ac.exeter.QuinCe.utils;

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
}
