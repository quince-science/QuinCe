package uk.ac.exeter.QuinCe.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;

/**
 * Miscellaneous date/time utilities
 */
public class DateTimeUtils {

  /**
   * The number of milliseconds in an hour
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
  public static java.time.format.DateTimeFormatter isoDateTimeFormatter = java.time.format.DateTimeFormatter
    .ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  /**
   * For formatting LocalDateTime - dates and parsing date time strings
   */
  private static java.time.format.DateTimeFormatter displayDateTimeFormatter = java.time.format.DateTimeFormatter
    .ofPattern(DISPLAY_DATE_TIME_FORMAT).withZone(ZoneOffset.UTC);

  /**
   * Determines whether or not the current time is within a given number of
   * hours of the current time
   *
   * @param time1
   *          The time to be checked against the current time, in milliseconds
   *          since 1st Jan 1970
   * @param hours
   *          The number of hours
   * @return {@code true} if the current time is within the specified number of
   *         hours of the supplied time; {@code false} otherwise.
   */
  public static boolean timeWithinLastHours(long time1, int hours) {
    long diff = System.currentTimeMillis() - time1;
    return !(diff > hours * MILLIS_PER_HOUR);
  }

  /**
   * Format a date/time to YYYY-MM-dd HH:mm:ss format
   *
   * @param dateTime
   *          The date/time
   * @return The formatted date/time
   */
  public static String formatDateTime(LocalDateTime dateTime) {
    return displayDateTimeFormatter.format(dateTime);
  }

  /**
   * Convert a UTC {@link LocalDateTime} to a {@code long} milliseconds value
   * for storage in the database
   *
   * @param date
   *          The date
   * @return The long value
   */
  public static Long dateToLong(LocalDateTime date) {
    return null == date ? null : date.toInstant(ZoneOffset.UTC).toEpochMilli();
  }

  /**
   * Convert a {@code long} milliseconds from the database into a UTC
   * {@link LocalDateTime}.
   *
   * @param milliseconds
   *          The milliseconds value
   * @return The {@code LocalDateTime} object
   */
  public static LocalDateTime longToDate(Long milliseconds) {
    return null == milliseconds ? null
      : LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds),
        ZoneOffset.UTC);
  }

  /**
   * Convert a {@code String} milliseconds value into a UTC
   * {@link LocalDateTime}.
   *
   * @param milliseconds
   *          The milliseconds value
   * @return The {@code LocalDateTime} object
   */
  public static LocalDateTime longToDate(String milliseconds) {
    return longToDate(Long.parseLong(milliseconds));
  }

  /**
   * Generate an ISOformatted date string for a given date.
   *
   * <p>
   * Passing in a {@code null} date will give a {@code null} result.
   * </p>
   *
   * @param date
   *          The date
   * @return The ISO date string
   */
  public static String toIsoDate(LocalDateTime date) {
    return null == date ? null : isoDateTimeFormatter.format(date);
  }

  /**
   * Calculate the time between two dates, in seconds
   *
   * @param date1
   *          The first date
   * @param date2
   *          The second date
   * @return The number of seconds between the dates
   */
  public static long secondsBetween(LocalDateTime date1, LocalDateTime date2) {
    return ChronoUnit.SECONDS.between(date1, date2);
  }

  /**
   * Calculate the time between two {@link TimeCoordinate}s, in seconds
   *
   * @param date1
   *          The first coordinate
   * @param date2
   *          The second coordinate
   * @return The number of seconds between the dates
   */
  public static long secondsBetween(TimeCoordinate date1,
    TimeCoordinate date2) {
    return ChronoUnit.SECONDS.between(date1.getTime(), date2.getTime());
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

  public static List<LocalDateTime> longsToDates(List<Long> millisecondses) {
    return millisecondses.stream().map(x -> longToDate(x))
      .collect(Collectors.toList());
  }

  public static boolean isBetween(LocalDateTime time, LocalDateTime start,
    LocalDateTime end) {

    LocalDateTime realStart = start.isBefore(end) ? start : end;
    LocalDateTime realEnd = start.isBefore(end) ? end : start;

    return !time.isBefore(realStart) && !time.isAfter(realEnd);
  }

  public static boolean overlap(LocalDateTime start1, LocalDateTime end1,
    LocalDateTime start2, LocalDateTime end2) {

    LocalDateTime realStart1 = start1.isBefore(end1) ? start1 : end1;
    LocalDateTime realEnd1 = start1.isBefore(end1) ? end1 : start1;
    LocalDateTime realStart2 = start2.isBefore(end2) ? start2 : end2;
    LocalDateTime realEnd2 = start2.isBefore(end2) ? end2 : start2;

    return isBetween(realStart1, realStart2, realEnd2)
      || isBetween(realEnd1, realStart2, realEnd2)
      || (!realStart1.isAfter(realStart2) && !realEnd1.isBefore(realEnd2));

  }

  /**
   * Return the mean time from a {@link Stream} of {@link LocalDateTime}s.
   *
   * <p>
   * Null values are ignored.
   * </p>
   *
   * <p>
   * This utilises a <a href=
   * "https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/package-summary.html#StreamOps">terminal
   * operation</a>, so the {@link Stream} cannot be used after this call.
   * </p>
   *
   * @param times
   *          The times to be averaged.
   * @return The mean time.
   */
  public static LocalDateTime meanTime(Stream<LocalDateTime> times) {
    OptionalDouble mean = times.filter(t -> null != t)
      .map(DateTimeUtils::dateToLong).mapToLong(Long::valueOf).average();

    return longToDate(Double.valueOf(mean.getAsDouble()).longValue());
  }

  /**
   * Get the mid point between two times.
   *
   * @param t1
   *          The first time.
   * @param t2
   *          The second time.
   * @return The mid point between the times.
   */
  public static LocalDateTime midPoint(LocalDateTime t1, LocalDateTime t2) {
    return t1.plusSeconds(secondsBetween(t1, t2) / 2);
  }

  /**
   * Create a {@link DateTimeFormatter} of the specified format, ensuring that
   * it uses the UTC timezone.
   *
   * @param format
   *          The format string.
   * @return The formatter.
   */
  public static DateTimeFormatter makeDateTimeFormatter(String format) {
    return DateTimeFormatter.ofPattern(format).withZone(ZoneOffset.UTC);
  }

  public static boolean isEqualOrBefore(LocalDateTime item,
    LocalDateTime target) {
    return item.isBefore(target) || item.equals(target);
  }

  public static boolean isEqualOrAfter(LocalDateTime item,
    LocalDateTime target) {
    return item.isAfter(target) || item.equals(target);
  }
}
