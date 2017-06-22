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
	 * Value to indicate that no column has been assigned
	 */
	public static final int NOT_ASSIGNED = -1;
	
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
	 */
	protected DateTimeColumnAssignment() {
		this.column = NOT_ASSIGNED;
		this.properties = new Properties();
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
	 */
	public void setDateFormatString(String format) {
		properties.setProperty(FORMAT_PROPERTY, format);
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
	protected Properties getProperties() {
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
}
