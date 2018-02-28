package uk.ac.exeter.QuinCe.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

	/**
	 * A formatter for generating date strings. The output format is {@code YYYY-MM-DD}.
	 */
	private static SimpleDateFormat dateFormatter = null;

	/**
	 * A formatter for generating date/time strings. The output format is {@code YYYY-MM-DD HH:mm:ss}.
	 */
	private static SimpleDateFormat dateTimeFormatter = null;

	/**
	 * A formatter for generating JSON format dates
	 */
	private static java.time.format.DateTimeFormatter jsonFormatter = null;

	/**
	 * A formatter for parsing date/time strings returned by SQL queries.
	 * The format is {@code yyyy-MM-dd HH:mm:ss.S}.
	 */
	private static DateTimeFormatter sqlDateTimeFormatter = null;

	static {
		dateTimeFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		jsonFormatter = java.time.format.DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
	}

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
	 * Set the time of a {@link Calendar} object to midnight by truncation.
	 * This returns a new object - the original remains untouched.
	 *
	 * @param date The {@link Calendar} object to be set to midnight
	 * @return A copy of the {@link Calendar} object with the time set to midnight
	 */
	@Deprecated
	public static Calendar setMidnight(Calendar date) {
		Calendar result = (Calendar) date.clone();
		result.set(Calendar.HOUR_OF_DAY, 0);
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
	@Deprecated
	public static int getDaysBetween(Calendar firstDate, Calendar lastDate) {
		long diffMillis = lastDate.getTimeInMillis() - firstDate.getTimeInMillis();
		return (int) Math.floorDiv(diffMillis, MILLIS_PER_DAY);
	}

	/**
	 * Returns the number of seconds between two dates. If the second date is
	 * before the first date, the result will be negative.
	 * @param firstDate The first date
	 * @param lastDate The last date
	 * @return The number of seconds between the dates.
	 */
	@Deprecated
	public static int getSecondsBetween(Calendar firstDate, Calendar lastDate) {
		long diffMillis = lastDate.getTimeInMillis() - firstDate.getTimeInMillis();
		return (int) Math.floorDiv(diffMillis, 1000);
	}

	/**
	 * Format a date to YYYY-MM-dd format
	 * @param date The date
	 * @return The formatted date
	 */
	@Deprecated
	public static String formatDate(Calendar date) {
		return dateFormatter.format(date.getTime());
	}

	/**
	 * Format a date/time to YYYY-MM-dd HH:mm:ss format
	 * @param dateTime The date/time
	 * @return The formatted date/time
	 */
	@Deprecated
	public static String formatDateTime(Date dateTime) {
		return dateTimeFormatter.format(dateTime);
	}

	/**
	 * Format a date/time to YYYY-MM-dd HH:mm:ss format
	 * @param dateTime The date/time
	 * @return The formatted date/time
	 */
	public static String formatDateTime(LocalDateTime dateTime) {
		return dateTimeFormatter.format(dateTime);
	}

	/**
	 * Format a date/time to YYYY-MM-dd HH:mm:ss format
	 * @param dateTime The date/time
	 * @return The formatted date/time
	 */
	@Deprecated
	public static String formatDateTime(Calendar dateTime) {
		return formatDateTime(dateTime.getTime());
	}

	/**
	 * Determines whether or not two dates are equal.
	 * The dates are compared to a resolution of one second (milliseconds are ignored).
	 * @param date1 The first date
	 * @param date2 The second date
	 * @return {@code true} if the dates are equal; {@code false} if they are different.
	 */
	@Deprecated
	public static boolean datesEqual(Calendar date1, Calendar date2) {

		boolean equal = true;

		if (date1.get(Calendar.YEAR) != date2.get(Calendar.YEAR)) {
			equal = false;
		} else if (date1.get(Calendar.MONTH) != date2.get(Calendar.MONTH)) {
			equal = false;
		} else if (date1.get(Calendar.DATE) != date2.get(Calendar.DATE)) {
			equal = false;
		} else if (date1.get(Calendar.HOUR_OF_DAY) != date2.get(Calendar.HOUR_OF_DAY)) {
			equal = false;
		} else if (date1.get(Calendar.MINUTE) != date2.get(Calendar.MINUTE)) {
			equal = false;
		} else if (date1.get(Calendar.SECOND) != date2.get(Calendar.SECOND)) {
			equal = false;
		}

		return equal;

	}

	/**
	 * Get an instance of a {@link Calendar} object with a UTC time zone.
	 * The calendar object will be set to the current time, and can have its
	 * value updated as required.
	 *
	 * @return A {@link Calendar} object with the time zone set to UTC.
	 * @see Calendar#getInstance(TimeZone, Locale)
	 */
	@Deprecated
	public static Calendar getUTCCalendarInstance() {
		return Calendar.getInstance(new SimpleTimeZone(0, "UTC"), Locale.ENGLISH);
	}

	/**
	 * Construct a {@link DateTime} object from a date/time string returned by an SQL query.
	 * @param dateTime The date/time string from the SQL query
	 * @return The {@link DateTime} object
	 * @throws InvalidDateTimeStringException If the date/time string cannot be parsed.
	 */
	@Deprecated
	public static DateTime makeDateTimeFromSql(String dateTime) throws InvalidDateTimeStringException {
		if (null == sqlDateTimeFormatter) {
			sqlDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S");
		}

		DateTime result = null;

		try {
			result = sqlDateTimeFormatter.parseDateTime(dateTime);
		} catch (IllegalArgumentException e) {
			throw new InvalidDateTimeStringException(dateTime);
		}

		return result;
	}


	/**
	 * Convert a UTC {@link LocalDateTime} to a {@code long}
	 * milliseconds value for storage in the database
	 * @param date The date
	 * @return The long value
	 */
	public static long dateToLong(LocalDateTime date) {
		return date.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	/**
	 * Convert a {@code long} milliseconds from the database
	 * into a UTC {@link LocalDateTime}.
	 * @param milliseconds The milliseconds value
	 * @return The {@code LocalDateTime} object
	 */
	public static LocalDateTime longToDate(long milliseconds) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds) , ZoneOffset.UTC);
	}

	/**
	 * Generate a JSON-formatted date string for a given date
	 * @param date The date
	 * @return The JSON string
	 */
	public static String toJsonDate(LocalDateTime date) {
		return jsonFormatter.format(date);
	}

	/**
	 * Calculate the time between two dates, in seconds
	 * @param date1 The first date
	 * @param date2 The second date
	 * @return The number of seconds between the dates
	 */
	public static long secondsBetween(LocalDateTime date1, LocalDateTime date2) {
		return ChronoUnit.SECONDS.between(date1, date2);
	}
}
