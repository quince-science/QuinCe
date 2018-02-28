package uk.ac.exeter.QuinCe.User;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/**
 * User Preferences
 * @author Steve Jones
 *
 */
public class UserPreferences extends Properties {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 196884326828074580L;

	/**
	 * The property name for the last instrument that the user
	 * interacted with
	 * @see #setLastInstrument(long)
	 */
	private static final String LAST_INSTRUMENT = "lastInstrument";

	/**
	 * The user ID
	 */
	private long userId;

	/**
	 * Construct an empty preferences object for a user
	 * @param userId The user ID
	 */
	public UserPreferences(long userId) {
		super();
		this.userId = userId;
	}

	/**
	 * Construct a preferences object from a String representation
	 * @param userId The user ID
	 * @param preferencesString The preferences string
	 */
	protected UserPreferences(long userId, String preferencesString) {
		super();
		this.userId = userId;

		if (null != preferencesString) {
			StringReader reader = new StringReader(preferencesString);
			try {
				load(reader);
			} catch (IOException e) {
				/*
				 * We fail silently. If the properties can't be
				 * read, they must be corrupted in the database.
				 * They will get overwritten the next time a preference is set.
				 */
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the ID of the user to which these preferences belong
	 * @return The user ID
	 */
	protected long getUserId() {
		return userId;
	}

	/**
	 * Store the last instrument that the user interacted with
	 * @param instrumentId The instrument's database ID
	 */
	public void setLastInstrument(long instrumentId) {
		setProperty(LAST_INSTRUMENT, String.valueOf(instrumentId));
	}

	/**
	 * Retrieve the last instrument that the user interacted with
	 * @return The instrument's database ID
	 */
	public long getLastInstrument() {
		long result = -1;

		if (null != getProperty(LAST_INSTRUMENT)) {
			try {
				result = Long.parseLong(getProperty(LAST_INSTRUMENT));
			} catch (NumberFormatException e) {
				// Reset the stored value because it's invalid
				setLastInstrument(-1);
			}
		}

		return result;
	}

	/**
	 * Write the user preferences to a String
	 * @return The user preferences string
	 */
	public String writeToString() {
		String result = null;

		try {
			StringWriter writer = new StringWriter();
			store(writer, null);
			result = writer.toString();
		} catch (IOException e) {
			/*
			 * We fail silently. If the properties can't be
			 * converted to a String we return null so they'll
			 * be reset in the database.
			 *
			 * Log the error to the console
			 */
			e.printStackTrace();
		}

		return result;
	}
}
