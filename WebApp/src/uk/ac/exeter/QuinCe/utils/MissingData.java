package uk.ac.exeter.QuinCe.utils;

public class MissingData {

	public static void checkMissing(Object parameter, String parameterName) throws MissingDataException {
		checkMissing(parameter, parameterName, false);
	}

	public static void checkMissing(char[] parameter, String parameterName) throws MissingDataException {
		checkMissing(parameter, parameterName, false);
	}

	public static void checkMissing(Object parameter, String parameterName, boolean canBeEmpty) throws MissingDataException {
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
			throw new MissingDataException(parameterName);
		}
	}

	public static void checkMissing(char[] parameter, String parameterName, boolean canBeEmpty) throws MissingDataException {
		boolean isMissing = false;
		
		if (null == parameter) {
			isMissing = true;
		} else {
			if (canBeEmpty && parameter.length == 0) {
					isMissing = true;
			}
		}
		
		if (isMissing) {
			throw new MissingDataException(parameterName);
		}
	}
}
