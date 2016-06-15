package uk.ac.exeter.QuinCe.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
	 * The number of milliseconds in a day
	 */
	public static final long MILLIS_PER_DAY = 86400000;
	
	private static SimpleDateFormat dateFormatter = null;
	
	private static SimpleDateFormat dateTimeFormatter = null;

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
	
	/**
	 * Set the time of a Calendar object to midnight by truncation.
	 * This returns a new object - the original remains untouched.
	 * 
	 * @param date The Calendar object to be set to midnight
	 * @return A copy of the Calendar object with the time set to midnight
	 */
	public static Calendar setMidnight(Calendar date) {
		Calendar result = (Calendar) date.clone();
		result.set(Calendar.HOUR, 0);
		result.set(Calendar.MINUTE, 0);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);
		return result;
	}
	
	/**
	 * Return the number of whole days between two dates
	 * @param firstDate The first date
	 * @param lastDate The second date
	 * @return The number of days' difference
	 */
	public static int getDaysBetween(Calendar firstDate, Calendar lastDate) {
		long diffMillis = lastDate.getTimeInMillis() - firstDate.getTimeInMillis();
		return (int) Math.floorDiv(diffMillis, MILLIS_PER_DAY);
	}
	
	public static int getSecondsBetween(Calendar firstDate, Calendar lastDate) {
		long diffMillis = lastDate.getTimeInMillis() - firstDate.getTimeInMillis();
		return (int) Math.floorDiv(diffMillis, 1000);
	}
	
	/**
	 * Format a date to YYYY-MM-dd format
	 * @param date The date
	 * @return The formatted date
	 */
	public static String formatDate(Calendar date) {
		if (null == dateFormatter) {
			dateFormatter = new SimpleDateFormat("YYYY-MM-dd");
		}
		
		return dateFormatter.format(date.getTime());
	}
	
	/**
	 * Format a date/time to YYYY-MM-dd HH:mm:ss format
	 * @param dateTime The date/time
	 * @return The formatted date/time
	 */
	public static String formatDateTime(Date dateTime) {
		if (null == dateTimeFormatter) {
			dateTimeFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		}
		
		return dateTimeFormatter.format(dateTime);
	}
}
