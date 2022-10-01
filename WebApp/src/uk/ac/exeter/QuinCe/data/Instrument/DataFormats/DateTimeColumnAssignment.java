package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Properties;

/**
 * Different date/time fields are assigned to columns in data files. Some have a
 * specific format, and some have another parameter.
 *
 * This class contains the data for such an assignment, and will be part of the
 * mapped assignments in the main date/time specification.
 *
 * @author Steve Jones
 * @see DateTimeSpecification
 */
public class DateTimeColumnAssignment {

  private static final String ISO_FORMAT = "ISO";

  /**
   * Properties key for format strings
   */
  private static final String FORMAT_PROPERTY = "formatString";

  /**
   * Properties key for the header prefix
   */
  private static final String PREFIX_PROPERTY = "prefix";

  /**
   * Properties key for the header suffix
   */
  private static final String SUFFIX_PROPERTY = "suffix";

  /**
   * Value to indicate that no column has been assigned
   */
  public static final int NOT_ASSIGNED = -1;

  /**
   * The assignment index for this assignment
   */
  private int assignmentIndex;

  /**
   * The column index
   */
  private int column;

  /**
   * Additional properties for the assignment.
   */
  private Properties properties;

  /**
   * Create an empty assignment
   *
   * @param assignmentIndex
   *          The assignment index
   */
  protected DateTimeColumnAssignment(int assignmentIndex) {
    this.assignmentIndex = assignmentIndex;
    this.column = NOT_ASSIGNED;
    this.properties = new Properties();
  }

  /**
   * Construct a complete assignment
   *
   * @param assignmentIndex
   *          The assignment index
   * @param column
   *          The column where the value will be stored
   * @param props
   *          The properties for the assignment
   */
  protected DateTimeColumnAssignment(int assignmentIndex, int column,
    Properties props) {
    this.assignmentIndex = assignmentIndex;
    this.column = column;
    if (null == props) {
      this.properties = new Properties();
    } else {
      this.properties = props;
    }
  }

  /**
   * Get the assigned column index
   *
   * @return The column index
   */
  public int getColumn() {
    return column;
  }

  /**
   * Set the assigned column index
   *
   * @param column
   *          The column index
   */
  public void setColumn(int column) {
    this.column = column;
  }

  /**
   * Get the date format as a string
   *
   * @return The date format string
   */
  public String getDateFormatString() {
    return properties.getProperty(FORMAT_PROPERTY);
  }

  /**
   * Set the format string
   *
   * @param format
   *          The format string
   * @throws DateTimeSpecificationException
   *           If an attempt is made to set a format for an assignment that
   *           doesn't need one
   */
  public void setDateFormatString(String format)
    throws DateTimeSpecificationException {
    switch (assignmentIndex) {
    case DateTimeSpecification.DATE_TIME:
    case DateTimeSpecification.DATE:
    case DateTimeSpecification.TIME:
    case DateTimeSpecification.HOURS_FROM_START: {
      properties.setProperty(FORMAT_PROPERTY, format);
      break;
    }
    default: {
      throw new DateTimeSpecificationException(
        "Cannot set date format for spec field " + assignmentIndex);
    }
    }
  }

  /**
   * Get the date format as a formatter object. If this assignment does not have
   * a format, the method returns null
   *
   * @return The formatter
   */
  public DateTimeFormatter getFormatter() {
    DateTimeFormatter result = null;

    if (null != getDateFormatString()) {
      if (getDateFormatString().equals(ISO_FORMAT)) {
        result = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
      } else {

        /*
         * Formats with fractions of a second sometimes contain an arbitrary
         * number of digits after the decimal point.
         *
         * For any format support fractions of a second, we take off the ".SSS"
         * identifier and replace it with a custom fractions parser.
         */
        String formatString = getDateFormatString();
        if (formatString.contains(".S")) {
          formatString = formatString.replaceFirst("\\.S+", "");
          result = new DateTimeFormatterBuilder().appendPattern(formatString)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();
        } else {
          result = DateTimeFormatter.ofPattern(formatString);
        }

      }
    }

    return result;
  }

  /**
   * Get the properties of this assignment
   *
   * @return The assignment properties
   */
  public Properties getProperties() {
    return properties;
  }

  /**
   * Determine whether or not this assignment is populated, i.e. has a column
   * index assigned.
   *
   * @return {@code true} if the assignment is populate; {@code false} if it is
   *         empty
   */
  public boolean isAssigned() {
    return column != NOT_ASSIGNED;
  }

  /**
   * Set the header prefix for the
   * {@link DateTimeSpecification#HOURS_FROM_START} assignment
   *
   * @param prefix
   *          The prefix
   * @throws DateTimeSpecificationException
   *           If an attempt is made to set a prefix for a different assignment
   */
  public void setPrefix(String prefix) throws DateTimeSpecificationException {
    if (assignmentIndex != DateTimeSpecification.HOURS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Cannot set header prefix for spec field " + assignmentIndex);
    }

    properties.setProperty(PREFIX_PROPERTY, prefix);
  }

  /**
   * Get the header prefix for the
   * {@link DateTimeSpecification#HOURS_FROM_START} assignment
   *
   * @return The prefix
   * @throws DateTimeSpecificationException
   *           If an attempt is made to set a prefix for a different assignment
   */
  public String getPrefix() throws DateTimeSpecificationException {
    if (assignmentIndex != DateTimeSpecification.HOURS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Cannot get header prefix for spec field " + assignmentIndex);
    }

    return properties.getProperty(PREFIX_PROPERTY);
  }

  /**
   * Set the header suffix for the
   * {@link DateTimeSpecification#HOURS_FROM_START} assignment
   *
   * @param suffix
   *          The suffix
   * @throws DateTimeSpecificationException
   *           If an attempt is made to set a suffix for a different assignment
   */
  public void setSuffix(String suffix) throws DateTimeSpecificationException {
    if (assignmentIndex != DateTimeSpecification.HOURS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Cannot set header suffix for spec field " + assignmentIndex);
    }

    properties.setProperty(SUFFIX_PROPERTY, suffix);
  }

  /**
   * Get the header suffix for the
   * {@link DateTimeSpecification#HOURS_FROM_START} assignment
   *
   * @return suffix The suffix
   * @throws DateTimeSpecificationException
   *           If an attempt is made to set a suffix for a different assignment
   */
  public String getSuffix() throws DateTimeSpecificationException {
    if (assignmentIndex != DateTimeSpecification.HOURS_FROM_START) {
      throw new DateTimeSpecificationException(
        "Cannot set header suffix for spec field " + assignmentIndex);
    }

    return properties.getProperty(SUFFIX_PROPERTY);
  }

  /**
   * Reset the assigned column
   */
  public void clearAssignment() {
    column = NOT_ASSIGNED;
  }
}
