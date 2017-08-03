package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Defines how the date and time are stored in a data file
 * @author Steve Jones
 *
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
	 * Name for date string
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
	 * The largest assignment index
	 */
	private static final int MAX_INDEX = 11;
	
	/**
	 * The column assignments
	 */
	private Map<Integer, DateTimeColumnAssignment> assignments;
	
	/**
	 * The parent file definition
	 */
	private FileDefinition parentDefinition;
	
	/**
	 * Constructs an empty specification
	 * @param parentDefinition The file definition that this spec belongs to
	 */
	public DateTimeSpecification(FileDefinition parentDefinition) {
		assignments = new TreeMap<Integer, DateTimeColumnAssignment>();
		
		assignments.put(DATE_TIME, new DateTimeColumnAssignment(DATE_TIME));
		assignments.put(DATE, new DateTimeColumnAssignment(DATE));
		assignments.put(HOURS_FROM_START, new DateTimeColumnAssignment(HOURS_FROM_START));
		assignments.put(JDAY_TIME, new DateTimeColumnAssignment(JDAY_TIME));
		assignments.put(JDAY, new DateTimeColumnAssignment(JDAY));
		assignments.put(YEAR, new DateTimeColumnAssignment(YEAR));
		assignments.put(MONTH, new DateTimeColumnAssignment(MONTH));
		assignments.put(DAY, new DateTimeColumnAssignment(DAY));
		assignments.put(TIME, new DateTimeColumnAssignment(TIME));
		assignments.put(HOUR, new DateTimeColumnAssignment(HOUR));
		assignments.put(MINUTE, new DateTimeColumnAssignment(MINUTE));
		assignments.put(SECOND, new DateTimeColumnAssignment(SECOND));

		this.parentDefinition = parentDefinition;
	}
	
	/**
	 * Get the JSON representation of this specification.
	 * 
	 * <p>
	 *   The JSON string is as follows:
	 * </p>
	 * <pre>
	 *   {
	 *   }
	 * </pre>
	 * <p>
	 *   The format will be the integer value corresponding
	 *   to the chosen format. The JSON processor will need
	 *   to know how to translate these.
	 * </p>
	 * 
	 * @return The JSON string
	 * @throws DateTimeSpecificationException If an error occurs while building the string
	 */
	//TODO Make JSON string in comment
	public String getJsonString() throws DateTimeSpecificationException {
		StringBuilder json = new StringBuilder();
		
		json.append('[');
		
		List<Integer> entries = getAvailableEntries();
		for (int i = 0; i < entries.size(); i++) {
			DateTimeColumnAssignment assignment = assignments.get(entries.get(i));
			
			json.append("{\"id\":");
			json.append(i);
			json.append(",\"name\":\"");
			json.append(getAssignmentName(entries.get(i)));
			json.append("\",\"column\":");
			json.append(assignment.getColumn());
			json.append(",\"properties\":");
			json.append(StringUtils.getPropertiesAsJson(assignment.getProperties()));
			json.append("}");
			
			if (i < entries.size() - 1) {
				json.append(',');
			}
		}
		
		json.append(']');
		
		return json.toString();
	}
	
	/**
	 * As column assignments are filled in, some options become
	 * unavailable as they are incompatible with the populated
	 * ones. This method returns the keys that have either been
	 * assigned or still can be assigned.
	 * @return The available entries
	 */
	private List<Integer> getAvailableEntries() {
		
		// A bit mask for available assignments. Start with nothing available,
		// and build from there.
		Integer availableMask = 0;
		
		// If the assignments are internally consistent, then we can take a few shortcuts
		if (nothingAssigned()) {
			availableMask = setMaskBits(availableMask, DATE_TIME, DATE, YEAR, JDAY_TIME, JDAY, MONTH, DAY, TIME, HOUR, MINUTE, SECOND);
			if (parentDefinition.hasHeader()) {
				availableMask = setMaskBits(availableMask, HOURS_FROM_START);
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
			
			// Julian day/time from start of file requires no other entries
			if (!dateProcessed && isAssigned(HOURS_FROM_START)) {
				availableMask = setMaskBits(availableMask, HOURS_FROM_START);
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
				availableMask = setMaskBits(availableMask, JDAY_TIME, JDAY, YEAR, MONTH, DAY);
				dateProcessed = true;
			}
			
			// Otherwise all date values are available
			if (!dateProcessed) {
				availableMask = setMaskBits(availableMask, DATE, JDAY_TIME, JDAY, YEAR, MONTH, DAY);
				dateProcessed = true;
			}
			
			// TIME string requires no other values
			if (!timeProcessed && isAssigned(TIME)) {
				availableMask = setMaskBits(availableMask, TIME);
				timeProcessed = true;
			}
			
			// If any of HOUR, MINUTE, SECOND are assigned, they are available and no others 
			if (!timeProcessed && (isAssigned(HOUR) || isAssigned(MINUTE) || isAssigned(SECOND))) {
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
	 * Determine whether a column has been assigned to the specified
	 * index.
	 * @param assignmentIndex The index
	 * @return {@code true} if a column has been assigned; {@code false} if it is not
	 * @see DateTimeColumnAssignment#isAssigned()
	 */
	private boolean isAssigned(int assignmentIndex) {
		return assignments.get(assignmentIndex).isAssigned();
	}
	
	/**
	 * Determine whether no date/time entries have been assigned
	 * @return {@code true} if no assignments have been made; {@code false} if it one or more assignments have been made
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
	 * @param mask The mask
	 * @param bits The bits to set
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
	 * @param index The index
	 * @return The name
	 * @throws DateTimeSpecificationException If the index is not recognised
	 */
	private String getAssignmentName(int index) throws DateTimeSpecificationException {
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
		default: {
			throw new DateTimeSpecificationException("Unrecognised specification index " + index);
		}
		}
		
		return result;
	}
	
	/**
	 * Get the index  for a specified date/time assignment name
	 * @param name The name
	 * @return The index
	 * @throws DateTimeSpecificationException If the index is not recognised
	 */
	public static int getAssignmentIndex(String name) throws DateTimeSpecificationException {
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
		case  HOUR_NAME: {
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
		default: {
			throw new DateTimeSpecificationException("Unrecognised specification index '" + name + "'");
		}
		}
		
		return result;
	}
	
	/**
	 * Assign a column to a date/time variable
	 * @param variable The variable name
	 * @param column The column index
	 * @param format The format (can be null for fields that don't need it)
	 * @throws DateTimeSpecificationException Is the assignment cannot be made
	 */
	public void assign(String variable, int column, String format) throws DateTimeSpecificationException {
		int assignmentIndex = getAssignmentIndex(variable);
		if (assignmentIndex == HOURS_FROM_START) {
			throw new DateTimeSpecificationException("Cannot use assign with " + variable + "; use assignHoursFromStart");
		}
		
		DateTimeColumnAssignment assignment = assignments.get(assignmentIndex);
		assignment.setColumn(column);
		
		if (assignmentIndex == DATE_TIME || assignmentIndex == DATE || assignmentIndex == TIME) {
			assignment.setDateFormatString(format);
		}
	}
	
	/**
	 * Assign a column to the {@link #HOURS_FROM_START} assignment
	 * @param column The column index
	 * @param headerPrefix The header prefix
	 * @param headerSuffix The header suffix
	 * @param format The date format
	 * @throws DateTimeSpecificationException If the assignment cannot be made
	 */
	public void assignHoursFromStart(int column, String headerPrefix, String headerSuffix, String format) throws DateTimeSpecificationException {
		DateTimeColumnAssignment assignment = assignments.get(HOURS_FROM_START);
		assignment.setColumn(column);
		assignment.setDateFormatString(format);
		assignment.setPrefix(headerPrefix);
		assignment.setSuffix(headerSuffix);
	}
	
	/**
	 * Remove a column from any of the assignments
	 * @param column The index of the column to be unassigned
	 * @return {@code true} if the column index was found and removed; {@code false} if not.
	 */
	public boolean removeAssignment(int column) {
		
		boolean assignmentRemoved = false;
		
		for (Map.Entry<Integer, DateTimeColumnAssignment> entry : assignments.entrySet()) {
			DateTimeColumnAssignment assignment = entry.getValue();
			if (assignment.getColumn() == column) {
				assignment.clearAssignment();
				assignmentRemoved = true;
			}
		}
		
		return assignmentRemoved;
		
	}
}
