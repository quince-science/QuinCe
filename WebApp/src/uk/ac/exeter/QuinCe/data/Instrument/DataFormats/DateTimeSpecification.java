package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	 * Key for Julian day with decimal time
	 */
	public static final int JDAY_TIME = 1;

	/**
	 * Key for date string
	 */
	public static final int DATE = 2;
	
	/**
	 * Key for Julian day without time
	 */
	public static final int JDAY = 3;
	
	/**
	 * Key for year
	 */
	public static final int YEAR = 4;
	
	/**
	 * Key for year
	 */
	public static final int MONTH = 5;
	
	/**
	 * Key for year
	 */
	public static final int DAY = 6;
	
	/**
	 * Key for time string
	 */
	public static final int TIME = 7;
	
	/**
	 * Key for hour
	 */
	public static final int HOUR = 8;
	
	/**
	 * Key for month
	 */
	public static final int MINUTE = 9;
	
	/**
	 * Key for second
	 */
	public static final int SECOND = 10;
	
	/**
	 * The largest assignment index
	 */
	private static final int MAX_INDEX = 10;
	
	/**
	 * The column assignments
	 */
	private Map<Integer, DateTimeColumnAssignment> assignments;
	
	/**
	 * Constructs an empty specification
	 */
	public DateTimeSpecification() {
		assignments = new TreeMap<Integer, DateTimeColumnAssignment>();
		
		assignments.put(DATE_TIME, new DateTimeColumnAssignment());
		assignments.put(JDAY_TIME, new DateTimeColumnAssignment());
		assignments.put(DATE, new DateTimeColumnAssignment());
		assignments.put(JDAY, new DateTimeColumnAssignment());
		assignments.put(YEAR, new DateTimeColumnAssignment());
		assignments.put(MONTH, new DateTimeColumnAssignment());
		assignments.put(DAY, new DateTimeColumnAssignment());
		assignments.put(TIME, new DateTimeColumnAssignment());
		assignments.put(HOUR, new DateTimeColumnAssignment());
		assignments.put(MINUTE, new DateTimeColumnAssignment());
		assignments.put(SECOND, new DateTimeColumnAssignment());
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
	public String getJsonString() throws DateTimeSpecificationException {
		StringBuilder json = new StringBuilder();
		
		json.append('[');
		
		List<Integer> entries = getAvailableEntries();
		for (int i = 0; i < entries.size(); i++) {
			DateTimeColumnAssignment assignment = assignments.get(entries.get(i));
			
			json.append("{\"name\":\"");
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
			availableMask = setMaskBits(availableMask, DATE_TIME, JDAY_TIME, DATE, JDAY, YEAR, MONTH, DAY, TIME, HOUR, MINUTE, SECOND);
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
			
			// DATE string requires no other date columns
			if (!dateProcessed) {
				if (isAssigned(DATE)) {
					availableMask = setMaskBits(availableMask, DATE);
					dateProcessed = true;
				}
			}
			
			// Julian day with time requires no other date or time parameters.
			// If the year is in the file though, we need it
			if (!dateProcessed && isAssigned(JDAY_TIME)) {
				availableMask = setMaskBits(availableMask, JDAY_TIME);
				if (Boolean.parseBoolean(assignments.get(JDAY_TIME).getProperty(DateTimeColumnAssignment.YEAR_IN_FILE_PROPERTY))) {
					availableMask = setMaskBits(availableMask, YEAR);
				}
				
				dateProcessed = true;
				timeProcessed = true;
			}
			
			// Julian day alone requires no other date parameters other than optionally the year
			if (!dateProcessed && isAssigned(JDAY)) {
				availableMask = setMaskBits(availableMask, JDAY);
				if (Boolean.parseBoolean(assignments.get(JDAY).getProperty(DateTimeColumnAssignment.YEAR_IN_FILE_PROPERTY))) {
					availableMask = setMaskBits(availableMask, YEAR);
				}
				
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
		case 0: {
			result = "Combined Date and Time";
			break;
		}
		case 1: {
			result = "Julian Day with Time";
			break;
		}
		case 2: {
			result = "Date";
			break;
		}
		case 3: {
			result = "Julian Day";
			break;
		}
		case 4: {
			result = "Year";
			break;
		}
		case 5: {
			result = "Month";
			break;
		}
		case 6: {
			result = "Day";
			break;
		}
		case 7: {
			result = "Time";
			break;
		}
		case 8: {
			result = "Hour";
			break;
		}
		case 9: {
			result = "Minute";
			break;
		}
		case 10: {
			result = "Second";
			break;
		}
		default: {
			throw new DateTimeSpecificationException("Unrecognised specification index " + index);
		}
		}
		
		return result;
	}
}
