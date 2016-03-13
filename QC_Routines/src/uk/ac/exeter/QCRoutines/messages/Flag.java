package uk.ac.exeter.QCRoutines.messages;

/**
 * Represents a WOCE-type flag, with Good, Questionable or Bad values.
 * There is also a special case for an Unset flag.
 * @author Steve Jones
 *
 */
public class Flag implements Comparable<Flag> {

	/**
	 * The WOCE value for a good flag
	 */
	private static final int VALUE_GOOD = 2;
	
	/**
	 * The text value for a good flag
	 */
	private static final String TEXT_GOOD = "Good";
	
	/**
	 * The WOCE value for a questionable flag
	 */
	private static final int VALUE_QUESTIONABLE = 3;
	
	/**
	 * The text value for a questionable flag
	 */
	private static final String TEXT_QUESTIONABLE = "Qustionable";

	/**
	 * The WOCE value for a bad flag
	 */
	private static final int VALUE_BAD = 4;
	
	/**
	 * The text value for a bad flag
	 */
	private static final String TEXT_BAD = "Bad";

	/**
	 * The special value for an unset flag
	 */
	private static final int VALUE_NOT_SET = -1;

	/**
	 * The text value for an unset flag
	 */
	private static final String TEXT_NOT_SET = "Not Set";

	/**
	 *  An instance of a Good flag
	 */
	public static final Flag GOOD = makeGoodFlag();
	
	/**
	 *  An instance of a Questionable flag
	 */
	public static final Flag QUESTIONABLE = makeQuestionableFlag();
	
	/**
	 *  An instance of a Good flag
	 */
	public static final Flag BAD = makeBadFlag();
	
	/**
	 *  An instance of a Not Set flag
	 */
	public static final Flag NOT_SET = makeNotSetFlag();
	
	/**
	 * The WOCE value for this flag
	 */
	private int flagValue;
	
	/**
	 * Creates a Flag instance with the specified value
	 * @param flagValue The flag's WOCE value
	 * @throws InvalidFlagException If the flag value is invalid
	 */
	protected Flag(int flagValue) throws InvalidFlagException {
		if (!isValidFlagValue(flagValue)) {
			throw new InvalidFlagException(flagValue);
		}
		
		this.flagValue = flagValue;
	}
	
	/**
	 * Returns the flag's WOCE value
	 * @return The flag's WOCE value
	 */
	public int getFlagValue() {
		return flagValue;
	}
	
	/**
	 * Converts the flag's WOCE value into a String value
	 */
	public String toString() {
		String result;
		
		switch (flagValue) {
		case VALUE_GOOD: {
			result = TEXT_GOOD;
			break;
		}
		case VALUE_QUESTIONABLE: {
			result = TEXT_QUESTIONABLE;
			break;
		}
		case VALUE_BAD: {
			result = TEXT_BAD;
			break;
		}
		case VALUE_NOT_SET: {
			result = TEXT_NOT_SET;
			break;
		}
		default: {
			// This should never happen!
			result = "***INVALID FLAG VALUE***";
		}
		}
		
		return result;
	}
	
	/**
	 * Checks to ensure that a flag value is valid. If the value is valid, the method does nothing.
	 * If it is not valid, an exception is thrown.
	 * @param value The flag value
	 * @throws InvalidFlagException If the flag value is invalid
	 */
	protected static boolean isValidFlagValue(int value) {
		return (value == VALUE_GOOD || value == VALUE_QUESTIONABLE || value == VALUE_BAD || value == VALUE_NOT_SET);
	}
	
	/**
	 * Create an instance of a Good flag
	 * @return A Good flag
	 */
	private static Flag makeGoodFlag() {
		Flag flag = null;
		try {
			flag = new Flag(VALUE_GOOD);
		} catch (InvalidFlagException e) {
			// This won't be thrown; do nothing
		}
		
		return flag;
	}
	
	/**
	 * Create an instance of a Questionable flag
	 * @return A Questionable flag
	 */
	private static Flag makeQuestionableFlag() {
		Flag flag = null;
		try {
			flag = new Flag(VALUE_QUESTIONABLE);
		} catch (InvalidFlagException e) {
			// This won't be thrown; do nothing
		}
		
		return flag;
	}
	
	/**
	 * Create an instance of a Bad flag
	 * @return A Bad flag
	 */
	private static Flag makeBadFlag() {
		Flag flag = null;
		try {
			flag = new Flag(VALUE_BAD);
		} catch (InvalidFlagException e) {
			// This won't be thrown; do nothing
		}
		
		return flag;
	}
	
	/**
	 * Create an instance of a Not Set flag
	 * @return A Not Set flag
	 */
	private static Flag makeNotSetFlag() {
		Flag flag = null;
		try {
			flag = new Flag(VALUE_NOT_SET);
		} catch (InvalidFlagException e) {
			// This won't be thrown; do nothing
		}
		
		return flag;
	}
	
	public boolean equals(Object compare) {
		boolean result = false;
		if (compare instanceof Flag) {
			result = ((Flag) compare).flagValue == flagValue;
		}
		return result;
	}

	@Override
	public int compareTo(Flag flag) {
		return this.flagValue - flag.flagValue;
	}
	
	/**
	 * Determines whether or not this flag is more
	 * significant than the specified flag.
	 * 
	 * The order of significance for flags is (lowest significance first):
	 * Not Set, Good, Questionable, Bad
	 * 
	 * @param flag The flag to be compared
	 * @return {@code true} if this flag is more significant than the supplied flag; {@code false} if it is not.
	 */
	public boolean moreSignificantThan(Flag flag) {
		return (compareTo(flag) > 0);
	}
}
