package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.ValueNotNumericException;

/**
 * Defines how the date and time are stored in a data file
 */
public class DateTimeSpecification {

  /**
   * Key for combined date and time string
   */
  public static final int DATE_TIME = 0;

  /**
   * Name for combined date and time string
   */
  public static final String DATE_TIME_NAME = "Combined Date and Time";

  /**
   * Key for hours since start date of file
   */
  public static final int HOURS_FROM_START = 1;

  /**
   * Name for hours since start date of file
   */
  public static final String HOURS_FROM_START_NAME = "Hours from start of file";

  /**
   * Key for date string
   */
  public static final int DATE = 2;

  /**
   * Name for date string
   */
  public static final String DATE_NAME = "Date";

  /**
   * Key for year
   */
  public static final int YEAR = 3;

  /**
   * Name for year
   */
  public static final String YEAR_NAME = "Year";

  /**
   * Key for Julian day with decimal time
   */
  public static final int JDAY_TIME = 4;

  /**
   * Name for Julian day with time
   */
  public static final String JDAY_TIME_NAME = "Julian Day with Time";

  /**
   * Key for Julian day without time
   */
  public static final int JDAY = 5;

  /**
   * Name for Julian Day
   */
  public static final String JDAY_NAME = "Julian Day";

  /**
   * Key for month
   */
  public static final int MONTH = 6;

  /**
   * Name for month
   */
  public static final String MONTH_NAME = "Month";

  /**
   * Key for day
   */
  public static final int DAY = 7;

  /**
   * Name for day
   */
  public static final String DAY_NAME = "Day";

  /**
   * Key for time string
   */
  public static final int TIME = 8;

  /**
   * Name for time string
   */
  public static final String TIME_NAME = "Time";

  /**
   * Key for hour
   */
  public static final int HOUR = 9;

  /**
   * Name for hour
   */
  public static final String HOUR_NAME = "Hour";

  /**
   * Key for minute
   */
  public static final int MINUTE = 10;

  /**
   * Name for minute
   */
  public static final String MINUTE_NAME = "Minute";

  /**
   * Key for second
   */
  public static final int SECOND = 11;

  /**
   * Name for second
   */
  public static final String SECOND_NAME = "Second";

  /**
   * Key for UNIX time
   */
  public static final int UNIX = 12;

  /**
   * Name for UNIX time
   */
  public static final String UNIX_NAME = "UNIX Time";

  /**
   * Key for seconds since start date of file
   */
  public static final int SECONDS_FROM_START = 13;

  /**
   * Name for seconds since start date of file
   */
  public static final String SECONDS_FROM_START_NAME = "Seconds from start of file";

  /**
   * The largest assignment index
   */
  private static final int MAX_INDEX = 13;

  /**
   * The column assignments
   */
  private Map<Integer, DateTimeColumnAssignment> assignments;

  /**
   * Indicates whether or not the file containing this specification has a
   * header
   */
  private boolean fileHasHeader;

  /**
   * Constructs an empty specification
   *
   * @param fileHasHeader
   *          Indicates whether or not the file containing this specification
   *          has a header
   */
  public DateTimeSpecification(boolean fileHasHeader) {
    assignments = new LinkedHashMap<Integer, DateTimeColumnAssignment>();

    assignments.put(DATE_TIME, new DateTimeColumnAssignment(DATE_TIME));
    assignments.put(DATE, new DateTimeColumnAssignment(DATE));
    assignments.put(HOURS_FROM_START,
      new DateTimeColumnAssignment(HOURS_FROM_START));
    assignments.put(SECONDS_FROM_START,
      new DateTimeColumnAssignment(SECONDS_FROM_START));
    assignments.put(JDAY_TIME, new DateTimeColumnAssignment(JDAY_TIME));
    assignments.put(JDAY, new DateTimeColumnAssignment(JDAY));
    assignments.put(YEAR, new DateTimeColumnAssignment(YEAR));
    assignments.put(MONTH, new DateTimeColumnAssignment(MONTH));
    assignments.put(DAY, new DateTimeColumnAssignment(DAY));
    assignments.put(TIME, new DateTimeColumnAssignment(TIME));
    assignments.put(HOUR, new DateTimeColumnAssignment(HOUR));
    assignments.put(MINUTE, new DateTimeColumnAssignment(MINUTE));
    assignments.put(SECOND, new DateTimeColumnAssignment(SECOND));
    assignments.put(UNIX, new DateTimeColumnAssignment(UNIX));

    this.fileHasHeader = fileHasHeader;
  }

  /**
   * <b>DO NOT USE</b>. Obsolete constructor for a complete specification to be
   * built from raw values.
   *
   * <p>
   * This was used in some old database migrations so cannot be deleted. But it
   * must not be used, and will simply throw an exception if you try.
   * </p>
   *
   * @param fileHasHeader
   *          Indicates whether or not the file containing this specification
   *          has a header
   * @param dateTimeCol
   *          The column where the date/time will be stored
   * @param dateTimeProps
   *          The properties for the date/time column
   * @param dateCol
   *          The column where the date will be stored
   * @param dateProps
   *          The properties for the date column
   * @param hoursFromStartCol
   *          The column where the hours-from-start-of-file will be stored
   * @param hoursFromStartProps
   *          The properties for the hours-from-start-of-file column
   * @param jdayTimeCol
   *          The column where the Julian day/time will be stored
   * @param jdayCol
   *          The column where the Julian day will be stored
   * @param yearCol
   *          The column where the year will be stored
   * @param monthCol
   *          The column where the month will be stored
   * @param dayCol
   *          The column where the day will be stored
   * @param timeCol
   *          The column where the time will be stored
   * @param timeProps
   *          The properties for the time column
   * @param hourCol
   *          The column where the hour will be stored
   * @param minuteCol
   *          The column where the minute will be stored
   * @param secondCol
   *          The column where the second will be stored
   * @throws DateTimeSpecificationException
   *           If the specification is incomplete
   */
  @Deprecated
  public DateTimeSpecification(boolean fileHasHeader, int dateTimeCol,
    Properties dateTimeProps, int dateCol, Properties dateProps,
    int hoursFromStartCol, Properties hoursFromStartProps, int jdayTimeCol,
    int jdayCol, int yearCol, int monthCol, int dayCol, int timeCol,
    Properties timeProps, int hourCol, int minuteCol, int secondCol,
    int unixCol) throws DateTimeSpecificationException {

    System.out.println("NO LONGER USED - DO NOT USE IN PRODUCTION");
  }

  /**
   * Determine whether or not this specification has had both date and time
   * fully assigned
   *
   * @return {@code true} if the date and time have been assigned; {@code false}
   *         if assignments are still required
   */
  public boolean assignmentComplete() {
    boolean dateAssigned = false;
    boolean timeAssigned = false;

    if (isAssigned(DATE_TIME) || isAssigned(JDAY_TIME)
      || isAssigned(HOURS_FROM_START) || isAssigned(SECONDS_FROM_START)
      || isAssigned(UNIX)) {
      dateAssigned = true;
      timeAssigned = true;
    } else {
      if (isAssigned(DATE) || isAssigned(JDAY)
        || (isAssigned(YEAR) && isAssigned(MONTH) && isAssigned(DAY))) {
        dateAssigned = true;
      }

      if (!timeAssigned) {
        if (isAssigned(TIME)
          || (isAssigned(HOUR) && isAssigned(MINUTE) && isAssigned(SECOND))) {
          timeAssigned = true;
        }
      }
    }

    return (dateAssigned && timeAssigned);
  }

  /**
   * As column assignments are filled in, some options become unavailable as
   * they are incompatible with the populated ones. This method returns the keys
   * that have either been assigned or still can be assigned.
   *
   * @return The available entries
   */
  protected List<Integer> getAvailableEntries() {

    // A bit mask for available assignments. Start with nothing available,
    // and build from there.
    Integer availableMask = 0;

    // If the assignments are internally consistent, then we can take a few
    // shortcuts
    if (nothingAssigned()) {
      availableMask = setMaskBits(availableMask, DATE_TIME, DATE, YEAR,
        JDAY_TIME, JDAY, MONTH, DAY, TIME, HOUR, MINUTE, SECOND);
      if (fileHasHeader) {
        availableMask = setMaskBits(availableMask, HOURS_FROM_START);
        availableMask = setMaskBits(availableMask, SECONDS_FROM_START);
      }
    } else if (isAssigned(DATE_TIME)) {
      availableMask = setMaskBits(availableMask, DATE_TIME);
    } else {
      boolean dateProcessed = false;
      boolean timeProcessed = false;

      // The Date/Time string is complete in itself
      if (isAssigned(DATE_TIME)) {
        availableMask = setMaskBits(availableMask, DATE_TIME);
        dateProcessed = true;
        timeProcessed = true;
      }

      // Hours/Seconds from start of file requires no other entries
      if (!dateProcessed && isAssigned(HOURS_FROM_START)) {
        availableMask = setMaskBits(availableMask, HOURS_FROM_START);
        dateProcessed = true;
        timeProcessed = true;
      }

      if (!dateProcessed && isAssigned(SECONDS_FROM_START)) {
        availableMask = setMaskBits(availableMask, SECONDS_FROM_START);
        dateProcessed = true;
        timeProcessed = true;
      }

      // DATE string requires no other date columns
      if (!dateProcessed) {
        if (isAssigned(DATE)) {
          availableMask = setMaskBits(availableMask, DATE);
          dateProcessed = true;
        }
      }

      // Julian day with time requires the date
      // If the year is in the file though, we need it
      if (!dateProcessed && isAssigned(JDAY_TIME)) {
        availableMask = setMaskBits(availableMask, JDAY_TIME);
        availableMask = setMaskBits(availableMask, YEAR);

        dateProcessed = true;
        timeProcessed = true;
      }

      // Julian day alone requires the year
      if (!dateProcessed && isAssigned(JDAY)) {
        availableMask = setMaskBits(availableMask, JDAY);
        availableMask = setMaskBits(availableMask, YEAR);

        dateProcessed = true;
      }

      // If the MONTH or DAY are set, then those and YEAR are available
      if (!dateProcessed && (isAssigned(MONTH) || isAssigned(DAY))) {
        availableMask = setMaskBits(availableMask, YEAR, MONTH, DAY);
        dateProcessed = true;
      }

      // If only the YEAR is assigned, then anything except Date/Time is allowed
      if (!dateProcessed && isAssigned(YEAR)) {
        availableMask = setMaskBits(availableMask, JDAY_TIME, JDAY, YEAR, MONTH,
          DAY);
        dateProcessed = true;
      }

      // Otherwise all date values are available
      if (!dateProcessed) {
        availableMask = setMaskBits(availableMask, DATE, JDAY_TIME, JDAY, YEAR,
          MONTH, DAY);
        dateProcessed = true;
      }

      // TIME string requires no other values
      if (!timeProcessed && isAssigned(TIME)) {
        availableMask = setMaskBits(availableMask, TIME);
        timeProcessed = true;
      }

      // If any of HOUR, MINUTE, SECOND are assigned, they are available and no
      // others
      if (!timeProcessed
        && (isAssigned(HOUR) || isAssigned(MINUTE) || isAssigned(SECOND))) {
        availableMask = setMaskBits(availableMask, HOUR, MINUTE, SECOND);
        timeProcessed = true;
      }

      // All times are available
      if (!timeProcessed) {
        availableMask = setMaskBits(availableMask, TIME, HOUR, MINUTE, SECOND);
      }
    }

    // Now we know which assignments are available,
    // put their indices in a list
    List<Integer> result = new ArrayList<Integer>();

    for (int i = 0; i <= MAX_INDEX; i++) {
      if ((availableMask & 1 << i) > 0) {
        result.add(i);
      }
    }

    return result;
  }

  /**
   * Determine whether a column has been assigned to the specified index.
   *
   * @param assignmentIndex
   *          The index
   * @return {@code true} if a column has been assigned; {@code false} if it is
   *         not
   * @see DateTimeColumnAssignment#isAssigned()
   */
  public boolean isAssigned(int assignmentIndex) {
    boolean result = false;

    if (assignments.containsKey(assignmentIndex)) {
      result = assignments.get(assignmentIndex).isAssigned();
    }

    return result;
  }

  /**
   * Get the assignment for a given date/time field
   *
   * @param assignmentIndex
   *          The date/time field index
   * @return The assignment
   */
  public DateTimeColumnAssignment getAssignment(int assignmentIndex) {
    return assignments.get(assignmentIndex);
  }

  /**
   * Determine whether no date/time entries have been assigned
   *
   * @return {@code true} if no assignments have been made; {@code false} if it
   *         one or more assignments have been made
   * @see DateTimeColumnAssignment#isAssigned()
   */
  private boolean nothingAssigned() {
    boolean result = true;

    for (int i = 0; i <= MAX_INDEX; i++) {
      if (isAssigned(i)) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Set the specified bits on a mask
   *
   * @param mask
   *          The mask
   * @param bits
   *          The bits to set
   * @return The updated mask
   */
  private int setMaskBits(Integer mask, int... bits) {
    int result = mask;

    for (int bit : bits) {
      result = result | 1 << bit;
    }

    return result;
  }

  /**
   * Get the name for a specified date/time assignment index
   *
   * @param index
   *          The index
   * @return The name
   * @throws DateTimeSpecificationException
   *           If the index is not recognised
   */
  public static String getAssignmentName(int index)
    throws DateTimeSpecificationException {
    String result = null;

    switch (index) {
    case DATE_TIME: {
      result = DATE_TIME_NAME;
      break;
    }
    case HOURS_FROM_START: {
      result = HOURS_FROM_START_NAME;
      break;
    }
    case SECONDS_FROM_START: {
      result = SECONDS_FROM_START_NAME;
      break;
    }
    case DATE: {
      result = DATE_NAME;
      break;
    }
    case YEAR: {
      result = YEAR_NAME;
      break;
    }
    case JDAY_TIME: {
      result = JDAY_TIME_NAME;
      break;
    }
    case JDAY: {
      result = JDAY_NAME;
      break;
    }
    case MONTH: {
      result = MONTH_NAME;
      break;
    }
    case DAY: {
      result = DAY_NAME;
      break;
    }
    case TIME: {
      result = TIME_NAME;
      break;
    }
    case HOUR: {
      result = HOUR_NAME;
      break;
    }
    case MINUTE: {
      result = MINUTE_NAME;
      break;
    }
    case SECOND: {
      result = SECOND_NAME;
      break;
    }
    case UNIX: {
      result = UNIX_NAME;
      break;
    }
    default: {
      throw new DateTimeSpecificationException(
        "Unrecognised specification index " + index);
    }
    }

    return result;
  }

  /**
   * Get the index for a specified date/time assignment name
   *
   * @param name
   *          The name
   * @return The index
   * @throws DateTimeSpecificationException
   *           If the index is not recognised
   */
  public static int getAssignmentIndex(String name)
    throws DateTimeSpecificationException {
    int result = -1;

    switch (name) {
    case DATE_TIME_NAME: {
      result = DATE_TIME;
      break;
    }
    case HOURS_FROM_START_NAME: {
      result = HOURS_FROM_START;
      break;
    }
    case SECONDS_FROM_START_NAME: {
      result = SECONDS_FROM_START;
      break;
    }
    case DATE_NAME: {
      result = DATE;
      break;
    }
    case YEAR_NAME: {
      result = YEAR;
      break;
    }
    case JDAY_TIME_NAME: {
      result = JDAY_TIME;
      break;
    }
    case JDAY_NAME: {
      result = JDAY;
      break;
    }
    case MONTH_NAME: {
      result = MONTH;
      break;
    }
    case DAY_NAME: {
      result = DAY;
      break;
    }
    case TIME_NAME: {
      result = TIME;
      break;
    }
    case HOUR_NAME: {
      result = HOUR;
      break;
    }
    case MINUTE_NAME: {
      result = MINUTE;
      break;
    }
    case SECOND_NAME: {
      result = SECOND;
      break;
    }
    case UNIX_NAME: {
      result = UNIX;
      break;
    }
    default: {
      throw new DateTimeSpecificationException(
        "Unrecognised specification index '" + name + "'");
    }
    }

    return result;
  }

  /**
   * Assign a column to a date/time variable
   *
   * @param variable
   *          The variable name
   * @param column
   *          The column index
   * @param format
   *          The format (can be null for fields that don't need it)
   * @throws DateTimeSpecificationException
   *           Is the assignment cannot be made
   */
  public void assign(int assignmentIndex, int column, String format)
    throws DateTimeSpecificationException {
    if (assignmentIndex == HOURS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Cannot use assign with Hours From Start Of File; use assignHoursFromStart");
    }

    if (assignmentIndex == SECONDS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Cannot use assign with Hours From Start Of File; use assignSecondsFromStart");
    }

    DateTimeColumnAssignment assignment = assignments.get(assignmentIndex);
    assignment.setColumn(column);

    if (assignmentIndex == DATE_TIME || assignmentIndex == DATE
      || assignmentIndex == TIME) {
      assignment.setDateFormatString(format);
    }
  }

  /**
   * Assign a column to the {@link #HOURS_FROM_START} assignment
   *
   * @param column
   *          The column index
   * @param headerPrefix
   *          The header prefix
   * @param headerSuffix
   *          The header suffix
   * @param format
   *          The date format
   * @throws DateTimeSpecificationException
   *           If the assignment cannot be made
   */
  public void assignHoursFromStart(int column, String headerPrefix,
    String headerSuffix, String format) throws DateTimeSpecificationException {

    assignPeriodFromStart(HOURS_FROM_START, column, headerPrefix, headerSuffix,
      format);
  }

  /**
   * Assign a column to the {@link #SECONDS_FROM_START} assignment
   *
   * @param column
   *          The column index
   * @param headerPrefix
   *          The header prefix
   * @param headerSuffix
   *          The header suffix
   * @param format
   *          The date format
   * @throws DateTimeSpecificationException
   *           If the assignment cannot be made
   */
  public void assignSecondsFromStart(int column, String headerPrefix,
    String headerSuffix, String format) throws DateTimeSpecificationException {

    assignPeriodFromStart(SECONDS_FROM_START, column, headerPrefix,
      headerSuffix, format);
  }

  private void assignPeriodFromStart(int format, int column,
    String headerPrefix, String headerSuffix, String dateFormat)
    throws DateTimeSpecificationException {

    if (format != HOURS_FROM_START && format != SECONDS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Must use HOURS_FROM_START or SECONDS_FROM_START");
    }

    DateTimeColumnAssignment assignment = assignments.get(format);
    assignment.setColumn(column);
    assignment.setDateFormatString(dateFormat);
    assignment.setPrefix(headerPrefix);
    assignment.setSuffix(headerSuffix);
  }

  /**
   * Remove a column from any of the assignments
   *
   * @param column
   *          The index of the column to be unassigned
   * @return {@code true} if the column index was found and removed;
   *         {@code false} if not.
   */
  public boolean removeAssignment(int column) {

    boolean assignmentRemoved = false;

    for (Map.Entry<Integer, DateTimeColumnAssignment> entry : assignments
      .entrySet()) {
      DateTimeColumnAssignment assignment = entry.getValue();
      if (assignment.getColumn() == column) {
        assignment.clearAssignment();
        assignmentRemoved = true;
      }
    }

    return assignmentRemoved;

  }

  /**
   * Specify whether or not the parent file has a header
   *
   * @param fileHasHeader
   *          Flag indicating whether or not the parent file has a header
   */
  public void setFileHasHeader(boolean fileHasHeader) {
    this.fileHasHeader = fileHasHeader;
  }

  /**
   * Get the date and time from a line in a file
   *
   * @param headerDate
   *          The date from the file header
   * @param line
   *          The line
   * @return The date/time
   * @throws MissingDateTimeException
   * @throws DataFileException
   *           If the date/time in the line is missing or invalid
   */
  public LocalDateTime getDateTime(LocalDateTime headerDate, List<String> line)
    throws DateTimeSpecificationException, MissingDateTimeException {
    LocalDateTime result = null;

    if (isAssigned(HOURS_FROM_START)) {
      if (null == headerDate) {
        throw new DateTimeSpecificationException("File header date is null");
      }
      result = getHoursFromStartDate(headerDate, line);
    } else if (isAssigned(SECONDS_FROM_START)) {
      if (null == headerDate) {
        throw new DateTimeSpecificationException("File header date is null");
      }
      result = getSecondsFromStartDate(headerDate, line);
    } else if (isAssigned(DATE_TIME)) {
      result = getDateTime(line);
    } else if (isAssigned(UNIX)) {
      result = getUnixTime(line);
    } else if (isAssigned(JDAY_TIME)) {
      result = getYearJDayTime(line);
    } else {

      LocalDate date = null;
      LocalTime time = null;

      if (isAssigned(DATE)) {
        date = getDate(line);
      } else if (isAssigned(JDAY)) {
        date = getYearJDay(line);
      } else {
        date = getYMDDate(line);
      }

      if (isAssigned(TIME)) {
        time = getTime(line);
      } else {
        time = getHMSTime(line);
      }

      result = LocalDateTime.of(date, time);
    }

    return result;
  }

  /**
   * Get the date of a line using the Hours From Start Date specification
   *
   * @param headerDate
   *          The file's start date from the header
   * @param line
   *          The line whose date is to be extracted
   * @return The date
   * @throws DataFileException
   *           If the hours column is empty
   */
  private LocalDateTime getHoursFromStartDate(LocalDateTime headerDate,
    List<String> line) throws DateTimeSpecificationException {

    DateTimeColumnAssignment assignment = getAssignment(HOURS_FROM_START);
    Double hours;

    try {
      hours = DataFile.extractDoubleFieldValue(line.get(assignment.getColumn()),
        null);
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException("Hours column is not numeric");
    }

    if (null == hours) {
      throw new DateTimeSpecificationException("Hours column is empty");
    }

    long wholeHours = hours.longValue();
    double hourFraction = hours - wholeHours;
    int secondsFraction = (int) (hourFraction * 3600);
    long lineSeconds = (wholeHours * 3600) + secondsFraction;

    LocalDateTime result;
    try {
      result = headerDate.plusSeconds(lineSeconds);
    } catch (DateTimeException e) {
      throw new DateTimeSpecificationException(
        "Invalid hours value: " + e.getMessage());
    }

    return result;
  }

  /**
   * Get the date of a line using the Seconds From Start Date specification
   *
   * @param headerDate
   *          The file's start date from the header
   * @param line
   *          The line whose date is to be extracted
   * @return The date
   * @throws DataFileException
   *           If the hours column is empty
   */
  private LocalDateTime getSecondsFromStartDate(LocalDateTime headerDate,
    List<String> line) throws DateTimeSpecificationException {

    DateTimeColumnAssignment assignment = getAssignment(SECONDS_FROM_START);
    Double seconds;

    try {
      seconds = DataFile
        .extractDoubleFieldValue(line.get(assignment.getColumn()), null);
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException("Hours column is not numeric");
    }

    if (null == seconds) {
      throw new DateTimeSpecificationException("Seconds column is empty");
    }

    LocalDateTime result;
    try {
      result = headerDate.plusSeconds(Math.round(seconds));
    } catch (DateTimeException e) {
      throw new DateTimeSpecificationException(
        "Invalid hours value: " + e.getMessage());
    }

    return result;
  }

  /**
   * Get value of a date/time field
   *
   * @param line
   *          The line whose date is to be extracted
   * @return The date/time
   * @throws DataFileException
   *           If the date/time field is empty or cannot be parsed
   */
  private LocalDateTime getDateTime(List<String> line)
    throws DateTimeSpecificationException {
    LocalDateTime result;

    DateTimeColumnAssignment assignment = getAssignment(DATE_TIME);
    String fieldValue = DataFile
      .extractStringFieldValue(line.get(assignment.getColumn()), null);

    if (null == fieldValue) {
      throw new DateTimeSpecificationException("Date/time column is empty");
    } else {
      try {
        result = LocalDateTime.parse(fieldValue, assignment.getFormatter());
      } catch (DateTimeParseException e) {
        throw new DateTimeSpecificationException(
          "Invalid date/time value '" + fieldValue + "'");
      }
    }

    return result;
  }

  /**
   * Get value of a UNIX time field
   *
   * @param line
   *          The line whose date is to be extracted
   * @return The date/time
   * @throws DataFileException
   *           If the date/time field is empty or cannot be parsed
   */
  private LocalDateTime getUnixTime(List<String> line)
    throws DateTimeSpecificationException {
    LocalDateTime result;

    DateTimeColumnAssignment assignment = getAssignment(UNIX);
    String fieldValue = DataFile
      .extractStringFieldValue(line.get(assignment.getColumn()), null);

    if (null == fieldValue) {
      throw new DateTimeSpecificationException("Date/time column is empty");
    } else {
      try {
        result = LocalDateTime.ofEpochSecond(Integer.parseInt(fieldValue), 0,
          ZoneOffset.UTC);
      } catch (DateTimeParseException e) {
        throw new DateTimeSpecificationException(
          "Invalid date/time value '" + fieldValue + "'");
      }
    }

    return result;
  }

  /**
   * Get the date/time of a Year/Julian DateTime formatted line
   *
   * @param line
   *          The line
   * @return The date/time
   * @throws DataFileException
   *           If any required fields are empty
   */
  private LocalDateTime getYearJDayTime(List<String> line)
    throws DateTimeSpecificationException {
    int yearField = getAssignment(YEAR).getColumn();
    int jdayTimeField = getAssignment(JDAY_TIME).getColumn();

    Integer year;

    try {
      year = DataFile.extractIntFieldValue(line.get(yearField), null);
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException("Year column is not numeric");
    }

    if (null == year) {
      throw new DateTimeSpecificationException("Year column is empty");
    }

    Double jdayTime = null;
    try {
      jdayTime = DataFile.extractDoubleFieldValue(line.get(jdayTimeField),
        null);
      if (null == jdayTime) {
        throw new DateTimeSpecificationException(
          "Julian date/time column is empty");
      }
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException(
        "Julian date/time value is not numeric");
    }

    LocalDateTime result;

    try {
      result = LocalDateTime.of(year, 1, 1, 0, 0);
      result = result.plusDays(jdayTime.intValue() - 1);

      double secondsFraction = jdayTime - jdayTime.intValue();
      result = result.plusSeconds((int) (secondsFraction * 86400));
    } catch (DateTimeException e) {
      throw new DateTimeSpecificationException(
        "Invalid date/time value: " + e.getMessage());
    }

    return result;
  }

  /**
   * Get a date from a line containing a date-only string
   *
   * @param line
   *          The line
   * @return The date
   * @throws MissingDateTimeException
   *           If the line does not contain the date/time field(s) or are empty
   * @throws DataFileException
   *           If the date field(s) is invalid
   */
  private LocalDate getDate(List<String> line)
    throws DateTimeSpecificationException, MissingDateTimeException {
    LocalDate result;

    DateTimeColumnAssignment assignment = getAssignment(DATE);

    if (line.size() <= assignment.getColumn()) {
      throw new MissingDateTimeException();
    } else {
      String fieldValue = DataFile
        .extractStringFieldValue(line.get(assignment.getColumn()), null);

      if (null == fieldValue || fieldValue.equalsIgnoreCase("")
        || fieldValue.equalsIgnoreCase("NaN")) {
        throw new MissingDateTimeException();
      } else {
        try {
          result = LocalDate.parse(fieldValue, assignment.getFormatter());
        } catch (Exception e) {
          throw new DateTimeSpecificationException(
            "Invalid date value '" + fieldValue + "'");
        }
      }
    }

    return result;
  }

  /**
   * Get the date from a line containing a year and Julian day
   *
   * @param line
   *          The line
   * @return The date
   * @throws DataFileException
   *           If any date fields are empty or invalid
   */
  private LocalDate getYearJDay(List<String> line)
    throws DateTimeSpecificationException {
    int yearField = getAssignment(YEAR).getColumn();
    int jdayField = getAssignment(JDAY).getColumn();

    Integer year;
    Integer jday;

    try {
      year = DataFile.extractIntFieldValue(line.get(yearField), null);
      jday = DataFile.extractIntFieldValue(line.get(jdayField), null);
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException(
        "One or more date values is not numeric");
    }

    if (null == year) {
      throw new DateTimeSpecificationException("Year column is empty");
    }

    if (null == jday) {
      throw new DateTimeSpecificationException("Julian day column is empty");
    }

    LocalDate result;
    try {
      result = LocalDate.ofYearDay(year, jday);
    } catch (DateTimeException e) {
      throw new DateTimeSpecificationException(
        "Invalid date/time: " + e.getMessage());
    }

    return result;
  }

  /**
   * Get a date from a line containing year/month/day fields
   *
   * @param line
   *          The line
   * @return The date
   * @throws DataFileException
   *           If the date fields are empty or invalid
   */
  private LocalDate getYMDDate(List<String> line)
    throws DateTimeSpecificationException {
    int yearField = getAssignment(YEAR).getColumn();
    int monthField = getAssignment(MONTH).getColumn();
    int dayField = getAssignment(DAY).getColumn();

    Integer year;
    Integer month;
    Integer day;

    try {
      year = DataFile.extractIntFieldValue(line.get(yearField), null);
      month = DataFile.extractIntFieldValue(line.get(monthField), null);
      day = DataFile.extractIntFieldValue(line.get(dayField), null);
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException(
        "One or more date values is not numeric");
    }

    if (null == year) {
      throw new DateTimeSpecificationException("Year column is empty");
    } else if (year < 100) {
      // If date is two digits, add 2000 to it. so 15 -> 2015
      // This matches the Java specification of two-digit years
      // It means we can't handle two-digit years before 2000, but
      // I think we can safely say that's not supported.
      year += 2000;
    }

    if (null == month) {
      throw new DateTimeSpecificationException("Month column is empty");
    }

    if (null == day) {
      throw new DateTimeSpecificationException("Day column is empty");
    }

    LocalDate result;
    try {
      result = LocalDate.of(year, month, day);
    } catch (DateTimeException e) {
      throw new DateTimeSpecificationException(
        "Invalid date value: " + e.getMessage());
    }

    return result;
  }

  /**
   * Get the time from a line with a single time column
   *
   * @param line
   *          The line
   * @return The time
   * @throws DataFileException
   *           If the time field is empty or invalid
   */
  private LocalTime getTime(List<String> line)
    throws DateTimeSpecificationException {
    LocalTime result;

    DateTimeColumnAssignment assignment = getAssignment(TIME);
    String fieldValue = DataFile
      .extractStringFieldValue(line.get(assignment.getColumn()), null);

    if (null == fieldValue) {
      throw new DateTimeSpecificationException("Time column is empty");
    } else {
      try {
        result = LocalTime.parse(fieldValue, assignment.getFormatter());
      } catch (DateTimeParseException e) {
        throw new DateTimeSpecificationException(
          "Invalid time value '" + fieldValue + "'");
      }
    }

    return result;
  }

  /**
   * Get the time from a line containing hour/minute/second fields
   *
   * @param line
   *          The line
   * @return The time
   * @throws DataFileException
   *           If any fields are empty or invalid
   */
  private LocalTime getHMSTime(List<String> line)
    throws DateTimeSpecificationException {
    int hourField = getAssignment(HOUR).getColumn();
    int minuteField = getAssignment(MINUTE).getColumn();
    int secondField = getAssignment(SECOND).getColumn();

    Integer hour;
    Integer minute;
    Integer second;

    try {
      hour = DataFile.extractIntFieldValue(line.get(hourField), null);
      minute = DataFile.extractIntFieldValue(line.get(minuteField), null);
      second = DataFile.extractIntFieldValue(line.get(secondField), null);
    } catch (ValueNotNumericException e) {
      throw new DateTimeSpecificationException(
        "One or more time values are not numeric");
    }

    if (null == hour) {
      throw new DateTimeSpecificationException("Hour column is empty");
    }

    if (null == minute) {
      throw new DateTimeSpecificationException("Minute column is empty");
    }

    if (null == second) {
      throw new DateTimeSpecificationException("Second column is empty");
    }

    LocalTime result;
    try {
      result = LocalTime.of(hour, minute, second);
    } catch (DateTimeException e) {
      throw new DateTimeSpecificationException(
        "Invalid time value: " + e.getMessage());
    }

    return result;
  }

  public List<Integer> getRequiredTypes() {
    List<Integer> result = new ArrayList<Integer>();

    boolean dateTimeRequired = !isAssigned(DATE_TIME);
    boolean hoursFromStartRequired = fileHasHeader
      && !isAssigned(HOURS_FROM_START);
    boolean secondsFromStartRequired = fileHasHeader
      && !isAssigned(SECONDS_FROM_START);
    boolean dateRequired = !isAssigned(DATE);
    boolean yearRequired = !isAssigned(YEAR);
    boolean jdayTimeRequired = !isAssigned(JDAY_TIME);
    boolean jdayRequired = !isAssigned(JDAY);
    boolean monthRequired = !isAssigned(MONTH);
    boolean dayRequired = !isAssigned(DAY);
    boolean timeRequired = !isAssigned(TIME);
    boolean hourRequired = !isAssigned(HOUR);
    boolean minuteRequired = !isAssigned(MINUTE);
    boolean secondRequired = !isAssigned(SECOND);
    boolean unixRequired = !isAssigned(UNIX);

    if (isAssigned(DATE_TIME) || isAssigned(HOURS_FROM_START)
      || isAssigned(SECONDS_FROM_START) || isAssigned(UNIX)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      dateRequired = false;
      yearRequired = false;
      jdayTimeRequired = false;
      jdayRequired = false;
      monthRequired = false;
      dayRequired = false;
      timeRequired = false;
      hourRequired = false;
      minuteRequired = false;
      secondRequired = false;
      unixRequired = false;
    }

    if (isAssigned(DATE)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      yearRequired = false;
      jdayTimeRequired = false;
      jdayRequired = false;
      monthRequired = false;
      dayRequired = false;
      unixRequired = false;
    }

    if (isAssigned(YEAR)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      dateRequired = false;
      unixRequired = false;
    }

    if (isAssigned(JDAY_TIME)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      dateRequired = false;
      jdayRequired = false;
      monthRequired = false;
      dayRequired = false;
      timeRequired = false;
      hourRequired = false;
      minuteRequired = false;
      secondRequired = false;
      unixRequired = false;
    }

    if (isAssigned(JDAY)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      dateRequired = false;
      jdayTimeRequired = false;
      monthRequired = false;
      dayRequired = false;
      unixRequired = false;
    }

    if (isAssigned(MONTH) || isAssigned(DAY)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      dateRequired = false;
      jdayTimeRequired = false;
      jdayRequired = false;
      unixRequired = false;
    }

    if (isAssigned(TIME)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      jdayTimeRequired = false;
      hourRequired = false;
      minuteRequired = false;
      secondRequired = false;
      unixRequired = false;
    }

    if (isAssigned(HOUR) || isAssigned(MINUTE) || isAssigned(SECOND)) {
      dateTimeRequired = false;
      hoursFromStartRequired = false;
      secondsFromStartRequired = false;
      jdayTimeRequired = false;
      timeRequired = false;
      unixRequired = false;
    }

    if (dateTimeRequired) {
      result.add(DATE_TIME);
    }
    if (hoursFromStartRequired) {
      result.add(HOURS_FROM_START);
    }
    if (secondsFromStartRequired) {
      result.add(SECONDS_FROM_START);
    }
    if (dateRequired) {
      result.add(DATE);
    }
    if (yearRequired) {
      result.add(YEAR);
    }
    if (jdayTimeRequired) {
      result.add(JDAY_TIME);
    }
    if (jdayRequired) {
      result.add(JDAY);
    }
    if (monthRequired) {
      result.add(MONTH);
    }
    if (dayRequired) {
      result.add(DAY);
    }
    if (timeRequired) {
      result.add(TIME);
    }
    if (hourRequired) {
      result.add(HOUR);
    }
    if (minuteRequired) {
      result.add(MINUTE);
    }
    if (secondRequired) {
      result.add(SECOND);
    }
    if (unixRequired) {
      result.add(UNIX);
    }

    return result;
  }

  public LinkedHashMap<String, Boolean> getAssignedAndRequiredEntries()
    throws DateTimeSpecificationException {

    LinkedHashMap<String, Boolean> result = new LinkedHashMap<String, Boolean>();
    List<Integer> requiredTypes = getRequiredTypes();

    for (int i : assignments.keySet()) {
      if (isAssigned(i)) {
        result.put(getAssignmentName(i), false);
      } else if (requiredTypes.contains(i)) {
        result.put(getAssignmentName(i), true);
      }
    }

    return result;
  }
}
