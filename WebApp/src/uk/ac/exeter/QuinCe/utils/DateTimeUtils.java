package uk.ac.exeter.QuinCe.utils;

/**
 * Miscellaneous date/time utilities
 * 
 * @author Steve Jones
 *
 */
public class DateTimeUtils {
	
	/**
	 *  The number of milliseconds in an hour
	 */
	public static final long MILLIS_PER_HOUR = 3600000;

	/**
	 * Determines whether or not the current time is within a
	 * given number of hours of the current time 
	 * @param time1 The time to be checked against the current time, in milliseconds since 1st Jan 1970
	 * @param hours The number of hours
	 * @return {@code true} if the current time is within the specified number of hours of the supplied time; {@code false} otherwise.
	 */
	public static boolean timeWithinLastHours(long time1, int hours) {
		long diff = System.currentTimeMillis() - time1;
		return !(diff > hours * MILLIS_PER_HOUR);
	}
}
