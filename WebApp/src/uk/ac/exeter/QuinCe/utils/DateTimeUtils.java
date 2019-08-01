package uk.ac.exeter.QuinCe.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

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
   * Default date-time format used for display
   */
  public static final String DISPLAY_DATE_TIME_FORMAT = "uuuu-MM-dd HH:mm:ss";

  /**
   * A formatter for generating ISO format dates
   */
  private static java.time.format.DateTimeFormatter isoDateTimeFormatter = null;

  /**
   * For formatting LocalDateTime - dates and parsing date time strings
   */
  private static java.time.format.DateTimeFormatter displayDateTimeFormatter = null;

  static {
    isoDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    displayDateTimeFormatter = java.time.format.DateTimeFormatter
        .ofPattern(DISPLAY_DATE_TIME_FORMAT).withZone(ZoneOffset.UTC);
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
   * Format a date/time to YYYY-MM-dd HH:mm:ss format
   * @param dateTime The date/time
   * @return The formatted date/time
   */
  public static String formatDateTime(LocalDateTime dateTime) {
    return displayDateTimeFormatter.format(dateTime);
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
   * Generate an ISOformatted date string for a given date
   * @param date The date
   * @return The ISO date string
   */
  public static String toIsoDate(LocalDateTime date) {
    return isoDateTimeFormatter.format(date);
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

  /**
   * Parses a date-time string on the general format YYYY-MM-DD HH:MM:SS to a
   * LocalDateTime object
   *
   * @param dateTimeString
   * @return
   */
  public static LocalDateTime parseDisplayDateTime(String dateTimeString) {
    return LocalDateTime.parse(dateTimeString, displayDateTimeFormatter);
  }

}
