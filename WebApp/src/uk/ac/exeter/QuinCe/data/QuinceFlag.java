package uk.ac.exeter.QuinCe.data;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;

public class QuinceFlag extends Flag {

	/**
	 * The special value for an assumed good flag
	 */
	protected static final int VALUE_ASSUMED_GOOD = -2;
	
	/**
	 * The text value for a good flag
	 */
	protected static final String TEXT_ASSUMED_GOOD = "Assumed Good";
	
	/**
	 * The special value for a Needed flag
	 */
	protected static final int VALUE_NEEDED = -3;
	
	/**
	 * The text value for a Needed flag
	 */
	protected static final String TEXT_NEEDED = "Needed";
	
	/**
	 *  An instance of a Good flag
	 */
	public static final Flag ASSUMED_GOOD = makeAssumedGoodFlag();

	/**
	 *  An instance of a Good flag
	 */
	public static final Flag NEEDED = makeNeededFlag();

	/**
	 * Creates a Flag instance with the specified value
	 * @param flagValue The flag's WOCE value
	 * @throws InvalidFlagException If the flag value is invalid
	 */
	public QuinceFlag(int flagValue) throws InvalidFlagException {
		super(flagValue);
	}
	
	/**
	 * Determines whether or not a flag is Good or AssumedGood - 
	 * both implying that the record is OK.
	 * @return {@code true} if the flag is Good or Assumed Good; {@code false} otherwise
	 */
	public boolean isGood() {
		return Math.abs(flagValue) == VALUE_GOOD;
	}
	
	/**
	 * Create an instance of an Assumed Good flag
	 * @return An Assumed Good flag
	 */
	private static Flag makeAssumedGoodFlag() {
		Flag flag = null;
		try {
			flag = new Flag(VALUE_ASSUMED_GOOD);
		} catch (InvalidFlagException e) {
			// This won't be thrown; do nothing
		}
		
		return flag;
	}

	/**
	 * Create an instance of an Assumed Good flag
	 * @return An Assumed Good flag
	 */
	private static Flag makeNeededFlag() {
		Flag flag = null;
		try {
			flag = new Flag(VALUE_NEEDED);
		} catch (InvalidFlagException e) {
			// This won't be thrown; do nothing
		}
		
		return flag;
	}

}
