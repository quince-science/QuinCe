package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * Different date/time fields are assigned to columns in data files.
 * Some have a specific format, and some have another parameter.
 * 
 * This class contains the data for such an assignment, and will
 * be part of the mapped assignments in the main date/time specification.
 * 
 * @author Steve Jones
 * @see DateTimeSpecification 
 */
public class DateTimeColumnAssignment {

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
	 * @param assignmentIndex The assignment index
	 */
	protected DateTimeColumnAssignment(int assignmentIndex) {
		this.assignmentIndex = assignmentIndex;
		this.column = NOT_ASSIGNED;
		this.properties = new Properties();
	}
	
	/**
	 * Construct a complete assignment
	 * @param assignmentIndex The assignment index
	 * @param column The column where the value will be stored
	 * @param props The properties for the assignment
	 */
	protected DateTimeColumnAssignment(int assignmentIndex, int column, Properties props) {
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
	 * @return The column index
	 */
	public int getColumn() {
		return column;
	}
	
	/**
	 * Set the assigned column index
	 * @param column The column index
	 */
	public void setColumn(int column) {
		this.column = column;
	}
	
	/**
	 * Get the date format as a string
	 * @return The date format string
	 */
	public String getDateFormatString() {
		return properties.getProperty(FORMAT_PROPERTY);
	}
	
	/**
	 * Set the format string
	 * @param format The format string
	 * @throws DateTimeSpecificationException If an attempt is made to set a format for an assignment that doesn't need one 
	 */
	public void setDateFormatString(String format) throws DateTimeSpecificationException {
		switch (assignmentIndex) {
		case DateTimeSpecification.DATE_TIME:
		case DateTimeSpecification.DATE:
		case DateTimeSpecification.TIME:
		case DateTimeSpecification.HOURS_FROM_START: {
			properties.setProperty(FORMAT_PROPERTY, format);
			break;
		}
		default: {
			throw new DateTimeSpecificationException("Cannot set date format for spec field " + assignmentIndex);
		}
		}
	}
	
	/**
	 * Get the date format as a formatter object
	 * @return The DateFormat object
	 */
	public DateFormat getDateFormat() {
		return new SimpleDateFormat(getDateFormatString());
	}
	
	/**
	 * Get the properties of this assignment
	 * @return The assignment properties
	 */
	public Properties getProperties() {
		return properties;
	}
	
	/**
	 * Determine whether or not this assignment is populated, i.e.
	 * has a column index assigned.
	 * @return {@code true} if the assignment is populate; {@code false} if it is empty
	 */
	public boolean isAssigned() {
		return column != NOT_ASSIGNED;
	}
	
	/**
	 * Set the header prefix for the {@link DateTimeSpecification#HOURS_FROM_START} assignment
	 * @param prefix The prefix
	 * @throws DateTimeSpecificationException If an attempt is made to set a prefix for a different assignment
	 */
	public void setPrefix(String prefix) throws DateTimeSpecificationException {
		if (assignmentIndex != DateTimeSpecification.HOURS_FROM_START) {
			throw new DateTimeSpecificationException("Cannot set header prefix for spec field " + assignmentIndex);
		}
		
		properties.setProperty(PREFIX_PROPERTY, prefix);
	}
	
	/**
	 * Set the header suffix for the {@link DateTimeSpecification#HOURS_FROM_START} assignment
	 * @param suffix The suffix
	 * @throws DateTimeSpecificationException If an attempt is made to set a suffix for a different assignment
	 */
	public void setSuffix(String suffix) throws DateTimeSpecificationException {
		if (assignmentIndex != DateTimeSpecification.HOURS_FROM_START) {
			throw new DateTimeSpecificationException("Cannot set header suffix for spec field " + assignmentIndex);
		}
		
		properties.setProperty(SUFFIX_PROPERTY, suffix);
	}
	
	/**
	 * Reset the assigned column
	 */
	public void clearAssignment() {
		column = NOT_ASSIGNED;
	}
}
